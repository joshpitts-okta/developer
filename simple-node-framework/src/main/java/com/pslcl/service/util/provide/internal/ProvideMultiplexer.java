/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDuplicateException;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFNotFoundException;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObject.Provider;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFSecurityScope;
import org.opendof.core.oal.DOFSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.dht.DistributedActivateProvideService;
import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.GracePeriodProvider;
import com.pslcl.service.util.provide.OutstandingInterestHandler;
import com.pslcl.service.util.provide.ProvideFactory;
import com.pslcl.service.util.provide.ProvideManager;
import com.pslcl.service.util.provide.ProvideManagerConfig;
import com.pslcl.service.util.provide.ProvideRequest;
import com.pslcl.service.util.provide.ProviderInfo;
import com.pslcl.service.util.provide.internal.interest.InterestKey;
import com.pslcl.service.util.provide.internal.interest.InterestValue;
import com.pslcl.service.util.provide.internal.interest.ProvideRequests;
import com.pslcl.service.util.provide.internal.interest.ProvideSet;
import com.pslcl.service.util.provide.internal.provide.FactoryWrapper;
import com.pslcl.service.util.provide.internal.provide.ProvideInfoEntry;
import com.pslcl.service.util.provide.internal.provide.ProviderHook;

//1. O1.I1 -> O1.I1
//            01.I2
//2. O1.I1 -> 01.I2 
//3. 01.I2 -> 01.I2
//4. app -> 01.I2
//5. O1.I*
// oid's need to support augmentation ... i.e. serviceId0

//@ThreadSafe
@SuppressWarnings("javadoc")
public class ProvideMultiplexer implements DOFSystem.ActivateInterestListener
{
    public static final String EmtpySetOid = "[3:serviceUtils-emptyset@internal.serviceutils.opendof.org]";
    private final Logger log;
//  @GuardedBy("factories")
    private final HashSet<FactoryWrapper> factories; 
    
    private final ProvideManager manager;
    private final DOF dof;
    private final ExecutorService executor;
    private final ScheduledExecutorService timerExecutor;
    private final int authTimeout;
    private final DOFSystem parentSystem;
    // @GuardedBy("only written in the constructor")
    private final HashMap<Integer, DOFSystem> domainMap;  
    // @GuardedBy("rdidMap")
    private final HashMap<Integer, DOFSystem> rdidMap;
    private final DOFCredentials credentials;
    // @GuardedBy("this")
    OutstandingInterestHandler outstandingInterestHandler;
    private final AtomicBoolean destroyed;
    private final DistributedActivateProvideService dhtService;
    
//    // @GuardedBy("triggerInterestToSetMap")
//    final List<InterestKey> outstandingInterestKeys;
//    final InterestKey outstandingInterestKey;
//    // @GuardedBy("triggerInterestToSetMap")
//    final HashMap<BindingRequest, AtomicInteger> outstandingInterestMap;
    
    // @GuardedBy("triggerInterestToSetMap")
    protected final HashMap<InterestKey, ProvideSet> triggerInterestToProvideSetMap;
    // @GuardedBy("triggerInterestToSetMap")
    protected final HashMap<BindingRequest, InterestKey> bindingRequestToTriggerInterestMap;
    

    /**
     * Used by System Layer only
     * @param manager my public manager
     * @param dof the DOF to use for obtaining cross domain DOFSystem's.  Must not be null. 
     * @param systemConfig the DOFSystem.Config used to create the parent system with.  Must not be null.
     * @param authTimeout the timeout value for obtaining authentication of remote secure DOFSystem's. 
     * @param domains trusted domains which should be allowed in multi-domain systems.   
     * @param executor ExecutorService threadpool to use.  Must not be null.
     * @param timer ScheduledExecutorService timer to use.  Must not be null.
     * @throws Exception if the given list of trusted domains could not establish <code>DOFSystem</code>'s.
     */
    public ProvideMultiplexer(ProvideManagerConfig config) throws Exception
    {   
//                    ProvideManager manager, DOF dof, DOFSystem.Config systemConfig, int authTimeout, List<Domain> domains, 
//                    ExecutorService executor, ScheduledExecutorService timer, DHTConfig dhtConfig) throws Exception

        String msg = null;
        try
        {
            msg = "failed to obtain parent DOFSystem";
            msg = "failed to obtain parent DOFSystem";
            this.parentSystem = config.getSystem();
        	this.manager = config.getManager();
        	this.dhtService = config.getDhtService();
            factories = new HashSet<FactoryWrapper>();
            
            log = LoggerFactory.getLogger(getClass());
            
//            if(config.isDhtEnabled())
//            {
//                dhtService = new VirtualNodeDhtService();
//                ((VirtualNodeDhtServiceConfig)config).setDofSystem(parentSystem);
//                dhtService.init((VirtualNodeDhtServiceConfig)config);
//            }else
//                dhtService = null;
            
            credentials = config.getCredentials();
            dof = config.getDof();
            executor = config.getExecutor();
            timerExecutor = config.getScheduledExecutor();
            authTimeout = config.getAuthTimeout();
            destroyed = new AtomicBoolean(false);
            domainMap = new HashMap<Integer, DOFSystem>();
            rdidMap = new HashMap<Integer, DOFSystem>();
            domainMap.put(new Integer(-1), parentSystem); // if rdid == -1 its the connection credentialed system.
            triggerInterestToProvideSetMap = new HashMap<InterestKey, ProvideSet>();
            bindingRequestToTriggerInterestMap = new HashMap<BindingRequest, InterestKey>();
            DOFObjectID oid = DOFObjectID.create(EmtpySetOid);
            BindingRequest outstandingKey = new BindingRequest(null, 0, oid, DOFInterfaceID.WILDCARD);
//            outstandingInterestKeys = new ArrayList<InterestKey>();
//            outstandingInterestKey = new InterestKey(new TriggerFactory(), outstandingKey);
//            outstandingInterestKeys.add(outstandingInterestKey);
//            triggerInterestToProvideSetMap.put(outstandingInterestKey, null);
//            outstandingInterestMap = new HashMap<BindingRequest, AtomicInteger>();
            
            List<Domain>domains = config.getDomains();
            if(domains == null || domains.size() == 0)
                return;
            
            for(int i=0; i < domains.size(); i++)
            {
                Domain domain = domains.get(i);
                //TODO: fixme not working.
                Domain domain1 = domains.get(0);
                Domain domain2 = domains.get(1);
                log.info("creating system for domain1: " + domain1.toStandardString());
                log.info("creating system for domain2: " + domain2.toStandardString());
                msg = "failed to obtain DOFSystem for trusted domain " + domain.toStandardString();  
//                DOFCredentials credsInDomain = DOFCredentials.create(credentials, domain2);
                DOFCredentials credsInDomain = DOFCredentials.create(credentials, domain2);
                
//                log.info("credentials: " + credentials);
                log.info("credsInDomain: " + credsInDomain);
    //            DOFSystem.Config systemConfig = new DOFSystem.Config.Builder().setCredentials(credentials).build();
                DOFSystem.Config dsystemConfig = new DOFSystem.Config.Builder().setCredentials(credsInDomain).build();
                DOFSystem system = null;
//                    system = DynamicSystemFactory.getSystemFuture(executor, dof, systemConfig, authTimeout).get();
//                    systemConfig = new DOFSystem.Config.Builder()
//                        .setCredentials(credsInDomain).build(); 
        
               system = DynamicSystemFactory.getSystemFuture(executor, dof, dsystemConfig, authTimeout).get();
                
                DOFSecurityScope scope = system.getState().getRemoteDomain();
                if(scope == null)
                    throw new Exception("could not obtain the scope from the DOFSystem created from domain: " + domains.get(i).toStandardString());
                int rdid = parentSystem.getRemoteDomainID(scope);
                domainMap.put(rdid, system);
            }
        }catch(Exception e)
        {
            throw e;
        }
    }

    /* *****************************************************************************
     * public interface that the manager wrapper calls
    *******************************************************************************/

    public List<ProvideFactory> getFactories()
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        synchronized(factories)
        {
            ArrayList<ProvideFactory> list = new ArrayList<ProvideFactory>();
            for(FactoryWrapper wrapper : factories)
                list.add(wrapper.factory);
            return list;
        }
    }

    public ProviderInfo getProvider(ProvideFactory factory, BindingRequest bindingRequest)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        synchronized(factories)
        {
            ProvideInfoEntry entry = findProviderEntry(findFactoryWrapper(factory), bindingRequest, false);
            if(entry == null)
                return null;
            return entry.info;
        }
    }
    
    public List<ProviderInfo> getDomainProviders(ProvideFactory factory, int rdid)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        List<ProviderInfo> list = new ArrayList<ProviderInfo>();
        synchronized(factories)
        {
            FactoryWrapper wrapper = findFactoryWrapper(factory);
            if(wrapper == null)
                return list;
            List<DOFObjectID> boids = wrapper.rdidBaseOidsMap.get(rdid);
            if(boids == null)
                return list;
            for(DOFObjectID boid : boids)
            {
                List<DOFObjectID> oids = wrapper.baseOidOidsMap.get(boid);
                for(DOFObjectID oid : oids)
                {
                    List<ProvideInfoEntry> providers = wrapper.oidProvidersMap.get(oid);
                    for(ProvideInfoEntry provider : providers)
                        list.add(provider.info);
                }
            }
        }
        return list;
    }
    
    //FIXME: implement nearest match
    public List<ProviderInfo> getProviders(ProvideFactory factory, DOFObjectID baseOid)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        List<ProviderInfo> list = new ArrayList<ProviderInfo>();
        
        if(baseOid != null)
            baseOid = baseOid.getBase();
        synchronized(factories)
        {
            FactoryWrapper wrapper = findFactoryWrapper(factory);
            if(wrapper == null)
                return list;
            for(Entry<Integer, List<DOFObjectID>> bentry : wrapper.rdidBaseOidsMap.entrySet())
            {
                for(DOFObjectID boid : bentry.getValue())
                {
                    if(baseOid.equals(DOFObjectID.BROADCAST) || boid.equals(baseOid))
                    {
                        List<DOFObjectID> oids = wrapper.baseOidOidsMap.get(boid);
                        if(oids == null)
                            continue;
                        for(DOFObjectID oid : oids)
                        {
                            List<ProvideInfoEntry> providers = wrapper.oidProvidersMap.get(oid);
                            if(providers == null)
                                continue;
                            for(ProvideInfoEntry provider : providers)
                                list.add(provider.info);
                        }
                    }
                }
            }
        }
        return list;
    }
    
    public List<ProviderInfo> getRelatedProviders(ProvideFactory factory, BindingRequest triggerRequest)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        List<ProviderInfo> list = new ArrayList<ProviderInfo>();
        
        synchronized(factories)
        {
            FactoryWrapper wrapper = findFactoryWrapper(factory);
            if(wrapper == null)
                return list;
            for(Entry<Integer, List<DOFObjectID>> bentry : wrapper.rdidBaseOidsMap.entrySet())
            {
//log.info(bentry.getKey() + "="+bentry.getValue());                
                for(DOFObjectID boid : bentry.getValue())
                {
//log.info(boid.toString());                
                    List<DOFObjectID> oids = wrapper.baseOidOidsMap.get(boid);
                    if(oids == null)
                        continue;
                    for(DOFObjectID oid : oids)
                    {
//log.info(oid.toString());                
                        List<ProvideInfoEntry> providers = wrapper.oidProvidersMap.get(oid);
                        if(providers == null)
                            continue;
                        for(ProvideInfoEntry provider : providers)
                        {
//log.info(provider.info.getBindingRequest().getInterfaceID().toStandardString());
                            //FIXME:
                            BindingRequest tbr = null; //provider.info.getTriggerBindingRequest();
                            if(tbr == null)
                                continue;
                            if(tbr.equals(triggerRequest))
                                list.add(provider.info);
                        }
                    }
                }
            }
        }
        return list;
    }
    
//    public ProviderInfo beginProviding(
//                    ProvideFactory factory, BindingRequest triggerBinding, DOFSecurityScope scope,
//                    int remoteDomainID, DOFObjectID objectID, DOFInterface dofInterface, Provider provider,
//                    Object context
//                    ) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        BindingRequest bindingRequest = new BindingRequest(manager, remoteDomainID, objectID, dofInterface.getInterfaceID());
//        return beginProviding(factory, triggerBinding, bindingRequest, null, dofInterface, provider, context);
//    }
//    
//    public ProviderInfo beginProviding(
//                    ProvideFactory factory, BindingRequest triggerBinding, Domain domain,
//                    DOFObjectID objectID, DOFInterface dofInterface, Provider provider,
//                    Object context
//                    ) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        BindingRequest bindingRequest = new BindingRequest(manager, null, objectID, dofInterface.getInterfaceID());
//        return beginProviding(factory, triggerBinding, bindingRequest, domain, dofInterface, provider, context);
//    }
//    
//    public ProviderInfo beginProviding(
//                    ProvideFactory factory, BindingRequest triggerBinding, BindingRequest bindingRequest, 
//                    DOFInterface dofInterface, Provider provider, Object context) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        return beginProviding(factory, triggerBinding, bindingRequest, null, dofInterface, provider, context);
//    }
//    
//    /*
//     * This is currently a potential blocking call.
//     * The ProvideFactory.activate and cancelActivate callbacks will be done on the executor pool.
//     * It is expected that the user would likely call this method on the activate thread, or they are aware of this being 
//     * a blocker and the activate caused them to spawn off other worker threads that would make the call (i.e. non-oal threads)
//     */
//    private ProviderInfo beginProviding(
//                    ProvideFactory factory, BindingRequest triggerBinding, BindingRequest bindingRequest, 
//                    Domain domain, DOFInterface dofInterface, Provider provider, Object context) throws DuplicateException, NotFoundException, MiddlewareException
//    {
////    public DOFOperation.Provide beginProviding(ProviderInfo providerInfo) throws DuplicateException, NotFoundException, MiddlewareException
////    {
//        /*
//         * 1. see if the given binding already exists
//         *    a. if so, throw exception if true.
//         *    b. if not add it to the collections.
//         * 2. obtain the correct DOFSystem.
//         *    a. if trustedDomains configured, only allow systems obtained in the init method.
//         *    b. else check for systems already created for the rdid, and create if non-existent.
//         * 3. begin providing on the correct DOFSystem.
//         */
//         
//        // since we are declaring this method a potential blocker, this synch block will surround the potential blocker.
//        // FIXME: this is bad
//        synchronized(factories)
//        {
////TODO: findFactoryWrapper calls findProviderEntry ... can we avoid double search?            
//            FactoryWrapper factoryWrapper = findFactoryWrapper(factory, bindingRequest);
//            if(factoryWrapper == null)
//                throw new NotFoundException("factory given in providerInfo has not been registered with the manager");
//        
//            if(findProviderEntry(factoryWrapper, bindingRequest, false) != null)
//                return null;
////                throw new DuplicateException("Already have a registered provider for the given providerInfo: " + providerInfo.toString());
//            
//            // if rdid == null it's a non-secure system - combined this with -1
//            // if rdid == -1 it's the connection's domain
//            // other rdid's == an actual remote domain.
//            // the trusted domain list has priority and will be checked first, if it exists, only this list will be allowed.
//            int rdid = bindingRequest.getRemoteDomainID();
//            DOFSystem system = domainMap.get(rdid);
//            if(system == null)
//            {
//                if(domainMap.size() > 1)
//                    throw new NotFoundException("Domain rdid: " + rdid + " is unknown");
//                system = rdidMap.get(rdid);
//                if(system == null)
//                {
//                    //FIXME:  see where marks team goes here
////                    DOFSystem.Config config = new DOFSystem.Config.Builder().setRemoteDomain(bindingRequest.getSecurityScope()).setCredentials(credentials).build();
////                    try
////                    {
////                        system = DynamicSystemFactory.getSystemFuture(executor, dof, config, authTimeout).get();
////                    }catch (Exception e)
////                    {
////                        throw MiddlewareException.causeToMiddlewareException(e, null);
////                    }
////                    rdidMap.put(rdid, system);
//                }
//            }
//            
//            DOFObject provideObject = system.createObject(bindingRequest.getObjectID());
//            ProviderHook hook = new ProviderHook(this);
//            if(log.isDebugEnabled())
//            {
//                StringBuilder sb = new StringBuilder("\n"+getClass().getSimpleName() + ".beginProviding:\n");
//                sb.append("\tmanager:  " + manager.toString() + "\n")
//                .append("\tfactory:  " + factory.toString() + "\n")
//                .append("\tdomain:   " + (domain == null ? "null" : domain.toStandardString()) + "\n")
//                .append("\tinterest: " + bindingRequest.toString() + "\n")
//                .append("\ttrigger:  " + (triggerBinding == null ? "null" : triggerBinding.toString()) + "\n")
//                .append("\tprovider: " + provider.toString() + "\n")
//                .append("\tcontext:  " + (context == null ? "null" : context.toString()) + "\n");
//                log.debug(sb.toString());
//            }
//            
//            ProviderInfo providerInfo = null;
//            synchronized(this) // make sure hook.providerInfo is called before hook.complete can be called
//            { 
//                DOFOperation.Provide provideOp = provideObject.beginProvide(dofInterface, DOF.TIMEOUT_NEVER, hook, context);
////FIXME: commented out for now                
////                providerInfo = new ProviderInfo(manager, factory, triggerBinding, bindingRequest, provider, provideOp);
//                hook.setProviderInfo(providerInfo);
//            }
//            log.info("out of synch block");
//            addProviderEntry(factoryWrapper, bindingRequest, new ProvideInfoEntry(providerInfo));
//            log.info("done adding");
//            return providerInfo;
//        }
//    }
    
    public void addFactory(ProvideFactory providerFactory, Set<DOFInterfaceID> interfaceIDs) throws DOFDuplicateException
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        synchronized (factories)
        {
            if(findFactoryWrapper(providerFactory) != null)
                throw new DOFDuplicateException("Given factory previously added");
            factories.add(new FactoryWrapper(providerFactory, new HashSet<DOFInterfaceID>(interfaceIDs)));
            if(factories.size() == 1)
            {
//                if(number == 0)
                    parentSystem.addActivateInterestListener(this);
            }
        }
    }
    
    public void removeFactory(ProvideFactory providerFactory)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        synchronized (factories)
        {
            for(FactoryWrapper wrapper : factories)
            {
                if(wrapper.factory != providerFactory)
                    continue;
log.info("wrapper: " + wrapper);                
                for(Entry<Integer, List<DOFObjectID>> rdidEntry : wrapper.rdidBaseOidsMap.entrySet())
                {
log.info("rdidEntryValue: " + rdidEntry.getValue());                
                    for(Entry<DOFObjectID, List<DOFObjectID>> boidEntry : wrapper.baseOidOidsMap.entrySet())
                    {
log.info("fulloid: " + boidEntry.getValue());                
                        for(Entry<DOFObjectID, List<ProvideInfoEntry>> provEntry : wrapper.oidProvidersMap.entrySet())
                        {
log.info("proventryKey: " + boidEntry.getKey());                
log.info("proventryVal: " + boidEntry.getValue());                
                            for(ProvideInfoEntry provider : provEntry.getValue())
                            {
//log.info("provider: " + provider.info.getBindingRequest());                
                                Provide provide = provider.info.getProvideOperation();
                                if(provide != null)
                                    provide.cancel();
                            }
                        }
                    }
                }
                wrapper.clear();
                factories.remove(wrapper);
                break;
            }
        }
        providerFactory.removed(manager);
    }
    
    public void destroy()
    {
        synchronized (factories)
        {
            parentSystem.removeActivateInterestListener(this);
            for(FactoryWrapper wrapper : factories)
            {
                wrapper.rdidBaseOidsMap.clear();
                wrapper.baseOidOidsMap.clear();
                wrapper.oidProvidersMap.clear();
                wrapper.iidFilters.clear();
                wrapper.factory.removed(manager);
            }
            factories.clear();
            for(Entry<Integer, DOFSystem> system : domainMap.entrySet())
                system.getValue().destroy();
            for(Entry<Integer, DOFSystem> system : rdidMap.entrySet())
                system.getValue().destroy();
            domainMap.clear();
            rdidMap.clear();
            destroyed.set(true);
        }
    }
    
    public synchronized void setOutstandingInterestHandler(OutstandingInterestHandler handler)
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        outstandingInterestHandler = handler;
    }

//    public void activateRequest(Domain domain, DOFObjectID oid, DOFInterfaceID iid)
//    {
//        activateRequest(domain, null, 0, oid, iid, true);
//    }
    
//    public void retryActivateRequest(BindingRequest bindingRequest)
//    {
//        activateRequest(bindingdomain, null, 0, oid, iid, false);
//    }
    
//    public void activateRequest(int rdid, DOFObjectID oid, DOFInterfaceID iid)
//    {
//        activateRequest(null, rdid, oid, iid);
//    }
    
    public void activateRequest(BindingRequest bindingRequest, boolean referenceCount)// throws DuplicateException
    {
        // you are in a DOF callback thread here, do not block it
        
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");
        
        /*
         * This is the common entry point for reactive (emit dynamic flooded interest) as well as non-reactive (application local) interest.
         * The input arguments now become the "Trigger" (and "Key") BindingRequest.  It is the tickler bindingRequest that will be handed into 
         * the ProvideFactory.getProvideRequestSet callback to obtain a list of provides the factory wants started.  The requested provide set
         * may or may not contain the trigger.  The trigger becomes the key for collections of the provide set interest reference counting.
         * 
         *  Since reactive trigger interest will be reference counted by the OAL, non-reactive interest will also be reference counted even if the trigger
         *  provide fails, or no factories respond to the interest with a provide request set containing the trigger.  The referenceCount flag argument is 
         *  required by custom application error handling code to be able to retry the trigger without incrementing the reference count on the trigger. 
         *  see the OutstandingInterestHandler interface. 
         * 
         * All BindingRequests in the ProvideRequest set returned from the factories implies non-reactive interest in that binding.  This allows for
         * future reactive interest in that same binding to reference count correctly (i.e. the application may lose interest, but reactive interest 
         * became true later).  These interest counts will remain even if the provide fails via an exception trying to obtain the DOFSystem or the actual 
         * begin provide.  These are nasty corner cases where custom OutstandingInterestHandlers will be notified, but it will be up to them to understand 
         * the outstanding interest counts and what is and is not actually providing in these cases.  
         * 
         * Typically failures to obtain the DOFSystem or begin the provide will be configuration issues that need to be logged but code level branching 
         * on exception would not be needful or useful (typical applications will not register an OutstandingInterestHandler). 
         *    
         *    FIXME: put notes in here on cancel behavior as you work through this.
         * 
         *  1. see if there are any registered factories interested in the incoming iid.
         *      a. handle incoming wildcard iid.
         *      b. return with no action if not an iid of interest.
         *  2. see if the incoming interest is already in a provideSet.
         *      a. if so, increment the interest count (if not retryActivateRequest).
         *          1) if referenceCount flag is false, it is because outstandingInterestHandler is calling for a retryActivateRequest, don't increment count.
         *  3. see if there is a cancel timer outstanding on the provide
         *      a. cancel the timer if so and return unless we are in retryActivateRequest mode.
         *  4. fire up a GetFactoryProvideRequests task for each factory interested in the iid.
         *  5. fire up a HandleFactoryProvideRequests task to wait for and deal with step 4's futures.
         */
        
        try
        {
            DOFInterfaceID iid = bindingRequest.getInterfaceID();
            boolean found = false;
            
            // see if this is an iid that any factories have registered for, handle the wildcard case.
            // note that broadcast "might" be a big problem for reference counting ... things might be ok where similar trigger not in provide request sets exist.
            synchronized (factories)
            {
                if(iid.equals(DOFInterfaceID.WILDCARD))
                {
                    for(FactoryWrapper wrapper : factories)
                    {
                        for(DOFInterfaceID diid : wrapper.iidFilters)
                        {
                            BindingRequest br = new BindingRequest(bindingRequest, diid);
                            activateRequest(br, referenceCount);
                        }
                    }
                    return;
                }
                for(FactoryWrapper wrapper : factories)
                {
                    if(wrapper.iidFilters.contains(iid))
                    {
                        found = true;
                        break;
                    }
                }
            }
            if(!found)
                return; // not an iid any factory has registered for, nothing to do here
            
            boolean hasActiveProvide = false;
            // see if there is an interest reference counter for this new trigger interest.
            // these should only exists for previous factory provide set interest.
            synchronized(triggerInterestToProvideSetMap)
            {
                InterestKey key = bindingRequestToTriggerInterestMap.get(bindingRequest);
                if(key != null)
                {
                    ProvideSet provideSet =  triggerInterestToProvideSetMap.get(key);
                    if(provideSet.trigger.equals(bindingRequest))
                        provideSet.triggerCount.incrementAndGet();  // its another interest in the trigger/key - only one will every be reactive, but one or additional application interests could be requested.
                    for(InterestValue value : provideSet.provides)
                    {
                        if(value.bindingRequest.equals(bindingRequest))
                        {
                            if(value.getProviderInfo() != null)
                                hasActiveProvide = true;
                            // you already have a provide request for this interest or it would not be in the triggerInterestToSetMap
                            // however it may have failed to provide in which case the outstandingInterestHandler was notified
                            // and it would need to cause a cleanup (cancel the sets interest), or retryActivateRequest.
                            if(referenceCount)
                                value.count.incrementAndGet();  // note: this requires grace period to dec counter when cancel timer is fired up.
                            break;
                        }
                    }
                }
            }

            if(log.isDebugEnabled())
                log.debug(getClass().getSimpleName() + ".activateRequest hasActiveProvide is " + hasActiveProvide + " for: " + bindingRequest.toString());
            if(hasActiveProvide)
            {
                // see if there is an cancel timer active on this provide, if so cancel timer and return;
                ProvideInfoEntry entry = getProvideInfoFromBindingRequest(bindingRequest);
                if(entry != null)
                { 
                    Future<Void> cancelFuture = entry.getCancelFuture();
                    if(cancelFuture != null)
                    {
                        cancelFuture.cancel(true);
                        entry.setCancelFuture(null);
                    }
                }else
                    log.warn(getClass().getSimpleName() + " interest hasActiveProvide shows true, but no provideEntry exists for: " + bindingRequest.toString());
                if(referenceCount)
                    return; // if not retryActivateRequest we have updated reference counting and canceled any cancelProvide tasks
            }
            
            if(dhtService != null)
            {
                // this work can be done here as long as it will never block
                if(!dhtService.shouldIProvide(bindingRequest))
                    return;
            }
            
            ArrayList<Future<ProvideRequests>> factoriesCalled = new ArrayList<Future<ProvideRequests>>();
            synchronized (factories)
            {
                // fire up threads to obtain provide request sets from the factories registered for interest in the iid.
                // capture the futures for the following
                for(FactoryWrapper wrapper : factories)
                {
                    if(wrapper.iidFilters.contains(iid))
                        factoriesCalled.add(executor.submit(new GetFactoryProvideRequests(manager, wrapper.factory, bindingRequest)));
                }
            }
         	executor.submit(new HandleFactoryProvideRequests(this, bindingRequest, factoriesCalled, referenceCount));
        }catch(Exception e)
        {
            //TODO: status tracker??
            log.error(getClass().getSimpleName() + ".activateRequest threw unchecked exception", e);
        }
    }
    

//	private void mapWork(BindingRequest brKey, List<Future<ProvideRequests>> futures)
//	{
//		List<ProvideRequests> provideRequestsList = new ArrayList<ProvideRequests>();
//		try
//		{
//			for (Future<ProvideRequests> future : futures)
//				provideRequestsList.add(future.get());
//
//			// trying to obtain a DOFSystem can block, do that here before moving to locking collection access below.
//			for (ProvideRequests provideRequests : provideRequestsList)
//			{
//				if (provideRequests == null) continue;
//				List<DOFSystem> systems = new ArrayList<DOFSystem>();
//				for (ProvideRequest provideRequest : provideRequests.provideRequests)
//					systems.add(getSystem(provideRequest.getBindingRequest()));
//				// note: that if an element of systems is null, it failed, outstandingInterestHandler has been notified and beginProvide below should
//				// not be called.
//				provideRequests.systems = systems;
//			}
//
//			synchronized (triggerInterestToProvideSetMap)
//			{
//				int setCount = 0;
//				boolean keyProvideFound = false;
//				for (ProvideRequests provideRequests : provideRequestsList) // there is a provideRequests per factory that was interested in the
//																			// trigger iid.
//				{
//					if (provideRequests == null) // however it could be null, if so, move on to next
//						continue;
//					++setCount; // if zero at end of looping, notify outstandingInterestHandler that key interest is outstanding
//
//					// calculate interest reference counting for this factories provide set
//					InterestKey interestKey = new InterestKey(provideRequests.factory, brKey);
//					ProvideSet provideSet = new ProvideSet(brKey);
//
//					int systemIndex = 0;
//					for (ProvideRequest provideRequest : provideRequests.provideRequests)
//					{
//						DOFSystem system = provideRequests.systems.get(systemIndex++);
//						BindingRequest br = provideRequest.getBindingRequest();
//						if (!keyProvideFound && br.equals(brKey)) // see if the trigger interest is in this provide set.
//							keyProvideFound = true;
//						InterestValue interestValue = new InterestValue(br);
//						provideSet.provides.add(interestValue); // build the provide interest set.
//						// build the reverse mapping of interest to factory provide sets.
//						// it is an un-deterministic situation if two factory sets are trying to provide the same bindingRequest. We will log the
//						// situation but not track the multiples.
//						InterestKey key = bindingRequestToTriggerInterestMap.get(br);
//						if (key == null)
//							bindingRequestToTriggerInterestMap.put(br, interestKey);
//						else
//							LoggerFactory.getLogger(getClass()).warn(
//									"multiple factories providing same bindingRequest, triggerInterest:  " + interestKey.toString()
//											+ " bindingRequest: " + br.toString());
//
//						DOFObjectID mappedID = dht.mapWorkRequest(br);
//						if (mappedID == null)
//						{
//							log.info("mappedID == null");
//							return;
//						}
//						log.debug(dht.getGlobalID() + " mapWork for reqID=" + br.toString() + " to nodeID=" + mappedID);
//
//						ProvideSet foundProvideSet = triggerInterestToProvideSetMap.get(brKey);
//						if (foundProvideSet != null && !mappedID.equals(dht.getGlobalID()))
//						{
//							// this node was providing and no longer needs to provide
//							log.info("stop providing for reqid=" + br.toString());
//						}
//						else if (foundProvideSet != null)
//						{
//							log.info("check that this node is still providing for reqid=" + br.toString());
//						}
//						else if (mappedID.equals(dht.getGlobalID()))
//						{
//							log.info("start providing for reqid=" + br.toString());
//							
//							// provide set interests have been captured, Fire up the provide.
//							if (system != null) beginProvide(provideRequests.factory, system, provideRequest, interestValue);
//						}
//						else
//						{
//							log.info("other service node will provide for reqid=" + br.toString());
//						}
//					}
//					triggerInterestToProvideSetMap.put(interestKey, provideSet);
//				}
//				if (setCount == 0 || !keyProvideFound)
//				{
//					// notify the outstandingInterestHandler that there is no provide for this interest.
//					// synchronized(provideMultiplexer)
//					{
//						if (outstandingInterestHandler != null) outstandingInterestHandler.outstandingInterest(brKey);
//					}
//				}
//			}
//		} catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
    
    public void cancelActivateRequest(BindingRequest bindingRequest, boolean ignoreReferenceCount)  throws Exception
    {
        if(destroyed.get())
            throw new IllegalStateException("Provide Manager was destroyed");

        /*
        *   1. see if there are any registered factories interested in the incoming iid.
        *       a. handle incoming wildcard iid.
        *       b. return with no action if not an iid of interest.
        *   2. see if the incoming interest is already in a provideSet.
        *       a. if so, decrement the interest count.
        *       b. did this clear the overall provideSet interest?
        *   3. if there is an active provide, and there is no longer any trigger interest and no provideSet interest, flag a tear down.
        *  
        *      a. cancel the timer if so and return unless we are in retryActivateRequest mode.
        *   4. fire up a GetFactoryProvideRequests task for each factory interested in the iid.
        *   5. fire up a HandleFactoryProvideRequests task to wait for and deal with step 4's futures.
        */
        
        try
        {
            DOFInterfaceID iid = bindingRequest.getInterfaceID();
            List<FactoryWrapper> needCalled = new ArrayList<FactoryWrapper>();
            boolean found = false;
            
            // see if this is an iid that any factories have registered for, handle the wildcard case.
            // note that broadcast "might" be a big problem for reference counting ... things might be ok where similar trigger not in provide request sets exist.
            synchronized (factories)
            {
                if(iid.equals(DOFInterfaceID.WILDCARD))
                {
                    for(FactoryWrapper wrapper : factories)
                    {
                        for(DOFInterfaceID diid : wrapper.iidFilters)
                        {
                            BindingRequest br = new BindingRequest(bindingRequest, diid);
                            cancelActivateRequest(br, ignoreReferenceCount);
                        }
                    }
                    return;
                }
                for(FactoryWrapper wrapper : factories)
                {
                    if(wrapper.iidFilters.contains(iid))
                    {
                        found = true;
                        break;
                    }
                }
            }
            if(!found)
                return; // not an iid any factory has registered for, nothing to do here

            // find the reference count for the interest being canceled and decrement it
            synchronized(triggerInterestToProvideSetMap)
            {
                InterestKey key = bindingRequestToTriggerInterestMap.get(bindingRequest);
                if(key != null)
                {
                    ProvideSet provideSet =  triggerInterestToProvideSetMap.get(key);
                    if(provideSet.trigger.equals(bindingRequest))
                    {
                        int remaining = provideSet.triggerCount.decrementAndGet();
                        if(remaining < 0)
                            throw new Exception(getClass().getSimpleName() + ".cancelActivateRequest triggerCount went negative for: " + provideSet.trigger.toString());
                    }
                    int cancelProvideSetCount = 0;
                    for(InterestValue value : provideSet.provides)
                    {
                        if(value.bindingRequest.equals(bindingRequest))
                        {
                            int remaining = value.count.decrementAndGet();
                            if(remaining < 0)
                                throw new Exception(getClass().getSimpleName() + ".cancelActivateRequest a provideSet count went negative for: " + bindingRequest.toString());
                            if(value.getProviderInfo() != null && provideSet.triggerCount.get() == 0 && value.count.get() == 0)
                                ++cancelProvideSetCount;
                            break;
                        }
                    }
                    if(cancelProvideSetCount == provideSet.provides.size())
                    {
                        for(InterestValue value : provideSet.provides)
                        {
                            int gracePeriod = 0;
                            try
                            {
                                Provider provider = value.getProviderInfo().getProvideRequest().getProvider();
                                if(provider instanceof GracePeriodProvider)
                                    gracePeriod = ((GracePeriodProvider)provider).getGracePeriod(value.bindingRequest);
                            } catch (Exception e)
                            {
                                log.warn("cancelActivate failed to obtain gracePeriod from provider, canceling now: " + bindingRequest.toString(), e);
                            }
                            Future<Void> future = timerExecutor.schedule(new CancelActivateTask(parentSystem, value, executor), gracePeriod, TimeUnit.MILLISECONDS);
                            value.setCancelFuture(future);
//                            value.entry.setCancelFuture(future);
                        }
                    }
                }
            }
            
//            ProvideInfoEntry entry = null;
//            int gracePeriod = 0;
//            entry = getProvideInfoFromBindingRequest(bindingRequest);
//            if(entry != null)
//            {
//                try
//                {
//                    Provider provider = entry.info.getProvideRequest().getProvider();
//                    if(provider instanceof GracePeriodProvider)
//                        gracePeriod = ((GracePeriodProvider)provider).getGracePeriod(bindingRequest);
//                } catch (Exception e)
//                {
//                    log.warn("cancelActivate failed to obtain gracePeriod from provider, canceling now: " + bindingRequest.toString(), e);
//                }
//                if(entry.getCancelFuture() != null)
//                {
//                    log.warn("cancelActivate called with a binding that is already in grace period: " + bindingRequest.toString());
//                    return; 
//                }
//                Future<Void> future = timerExecutor.schedule(new CancelActivateTask(parentSystem, entry.wrapper.factory, entry, executor), gracePeriod, TimeUnit.MILLISECONDS);
//                entry.setCancelFuture(future);
//            }
        }catch(Exception e)
        {
            log.error(getClass().getSimpleName() + ".cancelActivateRequest threw unchecked exception", e);
        }
    }
    

    /* *****************************************************************************
     * DOFSystem.ActivateInterestListener implementation
    *******************************************************************************/

    @Override
    public void activate(DOFSystem system, DOFRequest request, DOFObjectID oid, DOFInterfaceID iid)
    {
        DOFSecurityScope scope = request.getSecurityScope();
        int rdid = -1;
        if(scope != null)
            rdid = system.getRemoteDomainID(scope);
        activateRequest(new BindingRequest(scope, rdid, oid, iid), true);
    }

    @Override
    public void cancelActivate(DOFSystem dofSystem, DOFRequest dofRequest, DOFObjectID oid, DOFInterfaceID iid)
    {
        DOFSecurityScope scope = dofRequest.getSecurityScope();
        int rdid = -1;
        if(scope != null)
            rdid = dofSystem.getRemoteDomainID(scope);
        BindingRequest bindingRequest = new BindingRequest(scope, rdid, oid, iid);
        
        try
        {
            cancelActivateRequest(bindingRequest, false);
            
            // TODO should this be handled here?
//            if (dht != null)
//            {
//    			dht.removeWorkRequest(bindingRequest);	
//            }
            
        } catch (Exception e)
        {
            // nothing to be done other than log it
            log.error(getClass().getSimpleName() + ".cancelActivate cancel interest sent reference count negative");
        }
    }

    @Override
    public void removed(DOFSystem dofSystem, DOFException e)
    {
        // nothing needs done here
    }
    
/* *****************************************************************************
 * default scope interface that other internal package classes needs to call
*******************************************************************************/
    
    DOFSystem getSystem(BindingRequest bindingRequest)
    {
        DOFSystem system = null;
        try
        {
            int rdid = bindingRequest.getRemoteDomainID();
            system = domainMap.get(rdid);
            if(system == null)
            {
                if(domainMap.size() > 1)
                    throw new DOFNotFoundException("Domain rdid: " + rdid + " is unknown");
                synchronized (rdidMap)
                {
                    system = rdidMap.get(rdid);
                }
                if(system == null)
                {
                    DOFSystem.Config config = new DOFSystem.Config.Builder()
                        .setRemoteDomain(bindingRequest.getSecurityScope())
                        .setCredentials(credentials)
                        .build();
                    try
                    {
                        system = DynamicSystemFactory.getSystemFuture(executor, dof, config, authTimeout).get();
                    }catch (Exception e)
                    {
                        //TODO: maybe strip the exec exception
//                        throw MiddlewareException.causeToMiddlewareException(e, null);
                        throw e;
                    }
                    synchronized (rdidMap)
                    {
                        rdidMap.put(rdid, system);
                    }
                }
            }
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error("Could not obtain remote DOFSystem for " + bindingRequest.toString(), e);
            synchronized(this)
            {
                if(outstandingInterestHandler != null)
                    outstandingInterestHandler.beginProvideFailed(bindingRequest, e);
            }
            return null;
        }
        return system;
    }
    
    boolean beginProvide(ProvideFactory factory, DOFSystem system, ProvideRequest provideRequest, InterestValue interestValue)
    {
        String msg = "factory given in providerInfo has not been registered with the manager";
        BindingRequest bindingRequest = provideRequest.getBindingRequest();
        try
        {
            synchronized(factories)
            {
    //TODO: findFactoryWrapper calls findProviderEntry ... can we avoid double search?            
                FactoryWrapper factoryWrapper = findFactoryWrapper(factory, bindingRequest);
                if(factoryWrapper == null)
                    throw new DOFNotFoundException(msg);
                
                msg = "There is already a provide active for the requested provide - factory: " + factory.toString() + " provideRequest: " + provideRequest.toString();
            
                if(findProviderEntry(factoryWrapper, bindingRequest, false) != null)
                    throw new DOFDuplicateException(msg);
                
                DOFObject provideObject = system.createObject(bindingRequest.getObjectID());
                ProviderHook hook = new ProviderHook(this);
                Object context = provideRequest.getContext();
                if(log.isDebugEnabled())
                {
//                    Domain domain = bindingRequest.getDomain();
                    StringBuilder sb = new StringBuilder("\n"+getClass().getSimpleName() + ".beginProviding:\n");
                    sb.append("\tmanager:  " + manager.toString() + "\n")
                    .append("\tfactory:  " + factory.toString() + "\n")
//                    .append("\tdomain:   " + (domain == null ? "null" : domain.toStandardString()) + "\n")
                    .append("\tinterest: " + bindingRequest.toString() + "\n")
                    .append("\tprovider: " + provideRequest.getProvider().toString() + "\n")
                    .append("\tcontext:  " + (context == null ? "null" : context.toString()) + "\n");
                    log.debug(sb.toString());
                }
                
                ProviderInfo providerInfo = null;
                DOFOperation.Provide provideOp = provideObject.beginProvide(provideRequest.getDofInterface(), DOF.TIMEOUT_NEVER, hook, context);
                providerInfo = new ProviderInfo(factory, provideRequest, provideOp);
                factory.provideActive(providerInfo);
                interestValue.setProviderInfo(providerInfo);
                hook.setProviderInfo(providerInfo);
                addProviderEntry(factoryWrapper, bindingRequest, new ProvideInfoEntry(providerInfo));
            }
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error("Failed to beginProvide - " + e.getMessage(), e);
            synchronized (this)
            {
                if(outstandingInterestHandler != null)
                    outstandingInterestHandler.beginProvideFailed(bindingRequest, e);
                return false;
            }
        }
        return true;
    }
    
    // called by ProviderHook on provide.complete 
    public void cleanupProvider(ProviderInfo providerInfo)
    {
        //TODO: does this need to clean up more of the structure leading up to leaf?
        synchronized(factories)
        {
//FIXME:            
            FactoryWrapper wrapper = findFactoryWrapper(/*providerInfo.getFactory()*/null);
            if(wrapper == null)
                return; // removeFactory called op.cancel and cleanup up all structure ... nothing to do here
//FIXME:             
            if(removeProviderEntry(wrapper, null /*providerInfo.getBindingRequest()*/) == null)
                log.warn("complete called on provider not in our collections: " + providerInfo.toString());
        }
    }
    
    private ProvideInfoEntry getProvideInfoFromBindingRequest(BindingRequest bindingRequest)
    {
        FactoryWrapper firstFound = null;
        ProvideInfoEntry entry;
        synchronized (factories)
        {
            for(FactoryWrapper wrapper : factories)
            {
//                if(firstFound == null && wrapper.factory == bindingRequest.getFactory())
//                    firstFound = wrapper;
                    
                if(wrapper.iidFilters.contains(bindingRequest.getInterfaceID()))
                {
                    entry = findProviderEntry(wrapper, bindingRequest, false);
                    if(entry != null)
                        return entry;
                }
            }
            return null; //new ProvideInfoEntry(firstFound);
        }
    }
    
    //TODO: make sure these are all wrapped in a sync
    private FactoryWrapper findFactoryWrapper(ProvideFactory factory)
    {
        for(FactoryWrapper wrapper : factories)
        {
            if(wrapper.factory == factory)
                return wrapper;
        }
        return null;
    }

    private FactoryWrapper findFactoryWrapper(ProvideFactory factory, BindingRequest bindingRequest) throws DOFDuplicateException, DOFNotFoundException
    {
        FactoryWrapper firstFound = null;
        FactoryWrapper next = null;
        for(FactoryWrapper wrapper : factories)
        {
            if(wrapper.factory == factory)
            {
                if(firstFound == null)
                    firstFound = wrapper;
                next = wrapper;
                if(findProviderEntry(next, bindingRequest, false) != null)
                    throw new DOFDuplicateException("Already have a registered provider for the providerInfo: " + bindingRequest.toString());
            }
        }
        if(firstFound == null)
            throw new DOFNotFoundException("factory given in providerInfo has not been registered with the manager");
        return firstFound;
    }

    private ProvideInfoEntry findProviderEntry(FactoryWrapper factoryWrapper, BindingRequest bindingRequest, boolean andRemove)
    {
        DOFObjectID desiredOid = bindingRequest.getObjectID();
        synchronized (factories)
        {
            List<DOFObjectID> baseOids = factoryWrapper.rdidBaseOidsMap.get(bindingRequest.getRemoteDomainID());
            if(baseOids == null)
                return null;
            for(DOFObjectID boid : baseOids)
            {
                if(boid.equals(desiredOid.getBase()))
                {
                    List<DOFObjectID> oids = factoryWrapper.baseOidOidsMap.get(boid);
                    if(oids == null)
                        continue;
                    for(DOFObjectID oid : oids)
                    {
                        List<ProvideInfoEntry> providerEntries = factoryWrapper.oidProvidersMap.get(oid);
                        if(providerEntries == null)
                            return null;
                        int i=0;
                        for(ProvideInfoEntry entry : providerEntries)
                        {
//FIXME:                            
                            BindingRequest br = null; //entry.info.getBindingRequest();
                            if(br.getObjectID().equals(desiredOid) && br.getInterfaceID().equals(bindingRequest.getInterfaceID()))
                            {
                                if(andRemove)
                                    entry = providerEntries.remove(i);
                                return entry;
                            }
                            ++i;
                        }
                    }
                }
            }
            return null;
        }
    }
    
    private ProvideInfoEntry removeProviderEntry(FactoryWrapper factoryWrapper, BindingRequest bindingRequest)
    {
        ProvideInfoEntry removedEntry = findProviderEntry(factoryWrapper, bindingRequest, true);
        if(removedEntry == null)
            return null;
//FIXME:         
//        if(log.isDebugEnabled())
//            log.debug("removeProviderEntry: " + removedEntry.info.getBindingRequest());
if(true)        
    return removedEntry;

StringBuilder sb = new StringBuilder("\nremoveProviderEntry:\n");        
        DOFObjectID desiredOid = bindingRequest.getObjectID();
        synchronized (factories)
        {
            boolean cleaned = false;
            do
            {
                cleaned = false;
                List<DOFObjectID> baseOids = factoryWrapper.rdidBaseOidsMap.get(bindingRequest.getRemoteDomainID());
                if(baseOids == null)
                {
                    sb.append("\tbaseOids list null for " + bindingRequest.getRemoteDomainID()+"\n");
                    log.info(sb.toString());
                    return removedEntry;
                }
                if(baseOids.size() == 0)
                {
                    factoryWrapper.rdidBaseOidsMap.remove(bindingRequest.getRemoteDomainID());
                    sb.append("\tbaseOids list was zero sized for " + bindingRequest.getRemoteDomainID()+"\n");
                    log.info(sb.toString());
                    return removedEntry;
                }
                for(DOFObjectID boid : baseOids)
                {
                    if(boid.equals(desiredOid.getBase()))
                    {
                        if(factoryWrapper.baseOidOidsMap.size() == 0)
                        {
                            baseOids.remove(desiredOid.getBase());
                            sb.append("\tbaseOidOidsMap was zero sized\n");
                            cleaned = true;
                            break;
                        }
                        List<DOFObjectID> oids = factoryWrapper.baseOidOidsMap.get(boid);
                        if(oids != null)
                        {
                            for(DOFObjectID oid : oids)
                            {
                                List<ProvideInfoEntry> providerEntries = factoryWrapper.oidProvidersMap.get(oid);
                                if(providerEntries != null)
                                {
                                    if(providerEntries.size() == 0)
                                    {
                                        factoryWrapper.baseOidOidsMap.remove(boid);
                                        cleaned = true;
                                        sb.append("\tproviderEntries was zero sized\n");
                                        continue;
                                    }
                                    // providerEntries.remove(obj);  note that the call to findProviderEntry above caused this delete to happen
                                }
                            }
                        }
                        if(cleaned)
                            break;
                    }
                }
            }while(cleaned);
log.info(sb.toString());
            return removedEntry;
        }
    }
    
    private void addProviderEntry(FactoryWrapper factoryWrapper, BindingRequest bindingRequest, ProvideInfoEntry infoEntry)
    {
        int rdid = bindingRequest.getRemoteDomainID();
        DOFObjectID newOid = bindingRequest.getObjectID();
        DOFObjectID baseOid = newOid.getBase();
        
        synchronized (factories)
        {
            // all known base oids for a given domain, add in the base if currently unknown
            List<DOFObjectID> baseOids = factoryWrapper.rdidBaseOidsMap.get(rdid);
            if(baseOids == null)
            {
                baseOids = new ArrayList<DOFObjectID>();
                factoryWrapper.rdidBaseOidsMap.put(rdid, baseOids);
                baseOids.add(baseOid);
                if(log.isDebugEnabled())
                    log.debug("addProviderEntry new domain added: " + rdid + " new baseOid: " + baseOid);
            }
            else
            {
                boolean found = false;
                for(DOFObjectID boid : baseOids)
                {
                    if(boid.equals(baseOid))
                    {
                        found = true;
                        break;
                    }
                }
                if(!found)
                {
                    baseOids.add(baseOid);
                    if(log.isDebugEnabled())
                        log.debug("addProviderEntry new baseOid added: " + baseOid + " to: " + rdid);
                }
            }
            
            // all known oids for a given base oid, add in the full oid if currently unknown.
            List<DOFObjectID> oids = factoryWrapper.baseOidOidsMap.get(baseOid);
            if(oids == null)
            {
                oids = new ArrayList<DOFObjectID>();
                factoryWrapper.baseOidOidsMap.put(baseOid, oids);
                oids.add(newOid);
                if(log.isDebugEnabled())
                    log.debug("addProviderEntry new oid added: " + newOid + " to baseOid: " + baseOid);
            }else
            {
                boolean found = false; 
                for(DOFObjectID oid : oids)
                {
                    if(oid.equals(newOid))
                    {
                        found = true;
                        break;
                    }
                }
                if(!found)
                {
                    oids.add(newOid);
                    if(log.isDebugEnabled())
                        log.debug("addProviderEntry new oid added: " + newOid + " to baseOid: " + baseOid);
                }
            }
            
            List<ProvideInfoEntry> providerEntries = factoryWrapper.oidProvidersMap.get(newOid);
            if(providerEntries == null)
            {
                providerEntries = new ArrayList<ProvideInfoEntry>();
                factoryWrapper.oidProvidersMap.put(newOid, providerEntries);
            }
            providerEntries.add(infoEntry);
            if(log.isDebugEnabled())
                log.debug("addProviderEntry new provide: " + infoEntry.info.toString());
        }
    }
}