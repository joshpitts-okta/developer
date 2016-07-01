/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import java.util.List;
import java.util.Set;

import org.opendof.core.oal.DOFDuplicateException;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;

import com.pslcl.service.util.provide.internal.ProvideMultiplexer;

/**
 * The Provide Manager.
 * </p>
 * This class works closely with <code>ProvideFactory</code>s to simplify the complexities 
 * of implementing reactive provide systems and/or provides requiring multiple domain support.
 * </p>
 * Care has been taken to follow the system-utils encouraged pattern of the separation
 * of the configuration and platform (system layer), and the service/application level.
 * This class spans these two levels by requiring instantiation and lifecycle control from the
 * system level, and being activated by registration of provide factories at the application 
 * level.  
 * </p>   
 * This class should be instantiated by the System level of an EMIT application where credentials 
 * are available for the <code>DOFSystem</code> which will be used by the manager.  The System level
 * thus owns the lifecycle of the provide manager object and will also call the <code>ProvideManager.destroy</code>
 * method at system level shutdown.  
 * </p>
 * Typically the ProvideManager instance is added to the custom configuration object  which is injected into
 * the system-utils <code>Service.init</code> method. The service's init method would 
 * then call the <code>ProvideManatger.addFactory</code> method to register factory/interfaceID associations and 
 * enable the manager to start requesting <code>ProvideRequest</code>s from the <code>ProvideFactories</code>.
 * Likewise, the <code>Service.destroy</code> method would call the <code>removeFactory</code>
 * methods to cleanup the application level side of the manager.
 * </p>
 * The <code>ProvideManager</code> registers with the <code>DOFSystem.ActivateInterestListener</code> and will 
 * receive flooded reactive interests from the OAL.  If a <code>ProvideFactory</code> has been registered with 
 * the <code>ProvideManager</code> for interest in the <code>DOFInterfaceID</code> of a flooded interest 
 * (or if it is a wildcard interest), the registered factories <code>ProvideFactory.getProvideRequestSet</code> 
 * for the triggering interest of not.
 * </p>
 * The <code>ProvideManager</code> will then begin and manage the provides which it has received requests for from the
 * <code>ProvideFactory</code>s, including applying grace period delays for providers that implement the 
 * <code>GracePeriodProvider</code> interface.
 * 
 * </p>
 * The provide manager also provides several search/list methods which allows the application level to obtain detailed
 * provider information from the managers collections avoiding the need for the application to duplicate these collections.   
 * @see ProvideFactory
 * @see GracePeriodProvider
 * @see BindingRequest
 * @see ProvideRequest
 */
public class ProvideManager
{
    // isolate the public DOFSystem.ActivateInterestListener methods from both the System and Application Layers
    private volatile ProvideMultiplexer manager;

    /**
     * The default constructor.
     * </p>
     * The init method must be called to initialize the manager.
     */
    public ProvideManager()
    {
    }
    
    /**
     * Initialize the manager.
     * </p>Initialize the manager with the given configuration. 
     * @param config the manager configuration to initialize to.  Must not be null.
     * @throws Exception if the manager fails to initialize.
     * @throws IllegalArgumentException if the config is null.
     */
    public void init(ProvideManagerConfig config) throws Exception
    {
        if (config == null)
            throw new IllegalArgumentException("config == null");
        manager = new ProvideMultiplexer(config);
        //.getDof(), config.getSystemConfig(), config.getAuthTimeout(), config.getDomains(), config.getExecutor(), config.getTimer(), config.isDhtEnabled());
    }
    
//    private ProvideManager(DOF dof, DOFSystem.Config systemConfig, int authTimeout, List<Domain> domains, ExecutorService executor, ScheduledExecutorService timer) throws InitializationException
//    {
//        if (dof == null || systemConfig == null || executor == null || timer == null)
//            throw new IllegalArgumentException("dof == null || parentSystem == null || executor == null || timer == null");
//        manager = new ProvideMultiplexer(this, dof, systemConfig, authTimeout, domains, executor, timer);
//    }

//    /**
//     * Create a <code>ProvideManager</code> based on the given configuration.
//     * </p>
//     * Multiple managers can be created by an application.  A unique 
//     * instance will be returned for each call.
//     * @param config the configuration to use to create the manager.  Must not be null
//     * @return a <code>ProvideManager</code> based on the given config.
//     * @throws InitializationException if the manager could not be instantiated.
//     * @throws IllegalArgumentException if an illegal config is given.
//     */
//    public static ProvideManager createManager(Config config) throws InitializationException
//    {
//        if (config == null)
//            throw new IllegalArgumentException("config == null");
//        return new ProvideManager(config.dof, config.systemConfig, config.authTimeout, config.domains, config.executor, config.scheduledExecutor);
//    }
    
//    public void activateRequest(Domain domain, DOFObjectID objectID, DOFInterfaceID interfaceID)
//    {
//        if (domain == null || objectID == null || interfaceID == null)
//            throw new IllegalArgumentException("domain == null || objectID == null || interfaceID == null");
//        manager.activateRequest(domain, objectID, interfaceID);
//    }
//
    /**
     * Activate Request
     * <p>
     * Used by an application to inject non-reactive provide requests into the <code>ProvideManager</code>.
     * These will then be treated as if they were reactive interests.
     * @param bindingRequest the triggering <code>BindingRequest</code> to statically inject into the <code>ProvideManager</code>.  Must not be null 
     * @throws DOFDuplicateException if the given bindingRequest has already been seen by the <code>ProvideManager</code>.
     * @throws IllegalArgumentException if the given bindingRequest is null.
     */
    public void activateRequest(BindingRequest bindingRequest) throws DOFDuplicateException
    {
        if (bindingRequest == null)
            throw new IllegalArgumentException("bindingRequest == null");
        manager.activateRequest(bindingRequest, true);
    }
    
//    public void activateRequest(Integer remoteDomainID, DOFObjectID objectID, DOFInterfaceID interfaceID)
//    {
//        if (objectID == null || interfaceID == null)
//            throw new IllegalArgumentException("objectID == null || interfaceID == null");
//        manager.activateRequest(remoteDomainID, objectID, interfaceID);
//    }
    
    // I thought there was a use case here somewhere that the interest reference count does not want to increment ... but I forgot what it was.
    // still valid??  outstandinginterest handler may have done away with this one.
    // actually this is needed in addition to the outstanding so the handler could call back in at a later time to start/provide for the interest, without bumping the interest count.
//    public void retryActivateRequest(Domain domain, DOFObjectID objectID, DOFInterfaceID interfaceID)
//    {
//        if (domain == null || objectID == null || interfaceID == null)
//            throw new IllegalArgumentException("domain == null || objectID == null || interfaceID == null");
//        manager.retryActivateRequest(domain, objectID, interfaceID);
//    }

    /**
     * Retry Activate Request
     * <p>
     * An application can register an <code>OutstandingInterestHandler</code> with the 
     * <code>ProvideManager.setOutstandingInterestHandler</code> method.
     * </p>
     * It is possible that a reactive interest is seen which was been registered for by one or more 
     * <code>ProvideFactory</code>s and yet none of the registered factories supply any <code>ProvideRequest</code>s
     * at the time of the interest.  It is also possible that a given <code>ProvideRequest</code> fails when the 
     * <code>ProvideManager</code> attempts the begin provide on it.  In these cases, a registered <code>OutstandingInterestHandler</code>
     * will be called to notify the application of these conditions.
     * </p>
     * The application may have a means of resolving why no <code>ProvideRequest</code>s were given or why the begin provide failed.
     * The <code>ProvideManager.retryActivateRequest</code> method can then be called by the application to retry the request.
     * 
     * @param bindingRequest the <code>BindingRequest</code> to retry.  Must not be null.
     * @throws IllegalArgumentException if the given bindingRequest is null.
     * @see ProvideManager#setOutstandingInterestHandler(OutstandingInterestHandler)   
     * @see OutstandingInterestHandler   
     */
    public void retryActivateRequest(BindingRequest bindingRequest)
    {
        if (bindingRequest == null)
            throw new IllegalArgumentException("bindingRequest == null");
        manager.activateRequest(bindingRequest, false);
    }
    
    /**
     * Set Outstanding Interest Handler
     * <p>
     * It is possible that a reactive interest is seen which was been registered for by one or more 
     * <code>ProvideFactory</code>s and yet none of the registered factories supply any <code>ProvideRequest</code>s
     * at the time of the interest.  It is also possible that a given <code>ProvideRequest</code> fails when the 
     * <code>ProvideManager</code> attempts the begin provide on it.  In these cases, a registered <code>OutstandingInterestHandler</code>
     * will be called to notify the application of these conditions.
     * </p>
     * The application may have a means of resolving why no <code>ProvideRequest</code>s were given or why the begin provide failed.
     * The <code>ProvideManager.retryActivateRequest</code> method can then be called by the application to retry the request.
     * 
     * @param handler the <code>OutstandingInterestHandler</code> being set.  Must not be null.
     * @throws IllegalArgumentException if the given handler is null.
     * @see ProvideManager#retryActivateRequest(BindingRequest)   
     */
    public void setOutstandingInterestHandler(OutstandingInterestHandler handler)
    {
        if (handler == null)
            throw new IllegalArgumentException("handler == null");
        manager.setOutstandingInterestHandler(handler);
    }
    
    /**
     * Cancel Activate Request
     * <p>
     * Used by an application that has injected non-reactive provide requests into the <code>ProvideManager</code> to now cancel those requests.
     * @param bindingRequests the triggering <code>BindingRequest</code> to statically inject into the <code>ProvideManager</code>. Must not be null
     * @throws Exception if the reference count for any of the <code>BindingRequest</code>s went negative. 
     * @throws IllegalArgumentException if the given bindingRequest is null.
     */
    public void cancelActivateRequest(Set<BindingRequest> bindingRequests) throws Exception
    {
        if (bindingRequests == null)
            throw new IllegalArgumentException("bindingRequests == null");
        for(BindingRequest request : bindingRequests)
            manager.cancelActivateRequest(request, false);
    }
    
//    public void cancelActivateRequest(Domain domain, DOFObjectID objectID, DOFInterfaceID interfaceID)
//    {
//        if (domain == null || objectID == null || interfaceID == null)
//            throw new IllegalArgumentException("domain == null || objectID == null || interfaceID == null");
//        manager.cancelActivateRequest(domain, objectID, interfaceID);
//    }
    
    /**
     * Cancel Provide
     * <p>
     * Normally a requestor dropping interest will trigger the <code>ProvideManager</code> to automatically
     * manage the provide by cleaning up properly.  However the <code>Provide</code> application may have 
     * other external factors which require it to stop.  This method will cause a list of provides to be destroyed 
     * even if outstanding interest exists.
     * @param bindingRequests the <code>BindingRequest</code>s to be cleaned up. 
     * @throws Exception if the reference count for any of the <code>BindingRequest</code>s went negative. 
     * @throws IllegalArgumentException if the given bindingRequests is null.
     */
    public void cancelProvide(Set<BindingRequest> bindingRequests) throws Exception
    {
        if (bindingRequests == null)
            throw new IllegalArgumentException("bindingRequests == null");
        for(BindingRequest request : bindingRequests)
            manager.cancelActivateRequest(request, true);
    }
    
    /**
     * Cancel Provide
     * <p>
     * Normally a requestor dropping interest will trigger the <code>ProvideManager</code> to automatically
     * manage the provide by cleaning up properly.  However the <code>Provide</code> application may have 
     * other external factors which require it to stop.  This method will cause the provides to be destroyed 
     * even if outstanding interest for it exists.
     * @param bindingRequest the <code>BindingRequest</code> to be cleaned up. 
     * @throws Exception if the reference count for the <code>BindingRequest</code> went negative. 
     * @throws IllegalArgumentException if the given bindingRequest is null.
     */
    public void cancelProvide(BindingRequest bindingRequest) throws Exception
    {
        if (bindingRequest == null)
            throw new IllegalArgumentException("bindingRequest == null");
        manager.cancelActivateRequest(bindingRequest, true);
    }
    
    /**
     * Add Factory
     * </p>
     * Adds a <code>ProvideFactory</code> and the <code>DOFInterfaceID</code>s to be associated with the factory.
     * Multiple factories can be added to any given manager and a given factory can be added to different managers.  
     * Factories can be added and removed from a manager at any time during the lifetime of the manager (after 
     * init and before destroy). 
     * </p>
     * The <code>ProvideFactory</code> must be ready to handle manager call to the <code>ProvideFactory.getProvideRequestSet</code>
     *  method before making this call.
     * </p>
     * If multiple factories are added with <code>DOFInterfaceID</code>'s that are common to other registered factories in
     * this manager, the <code>ProvideFactory.getProvideRequestSet</code> method of all factories registered for a given 
     * <code>DOFInterfaceID</code> on this manager will be called.
     * It will be up to the individual factories to determine if they should respond with <code>ProvideRequest</code>s.  
     * @param providerFactory the ProvideFactory instance being added to this manager.  Must not be null. 
     * @param interfaceIDs the interfaces to be associated with the given factory. Must not be null. Must have at least one. No elements can be null.
     * @see ProvideFactory
     * @see #removeFactory
     * @throws IllegalArgumentException if any illegal values are passed into the method.
     * @throws DOFDuplicateException if the given factory has been previously added.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public void addFactory(ProvideFactory providerFactory, Set<DOFInterfaceID> interfaceIDs) throws DOFDuplicateException
    {
        if (providerFactory == null || interfaceIDs == null || interfaceIDs.size() < 1)
            throw new IllegalArgumentException("providerFactory == null || interfaceIDs == null || interfaceIDs.size() < 1");
        for (DOFInterfaceID iid : interfaceIDs)
        {
            if (iid == null)
                throw new IllegalArgumentException("interfaceIDs contains a null");
        }
        manager.addFactory(providerFactory, interfaceIDs);
    }

    /**
     * Remove a Provide Factory from Provide Manager. 
     * </p>
     * Multiple factories can be added to any given manager with the addFactory method.  This method allows for
     * them to be removed from the manager.  Factories can be added and removed from a manager at
     * any time during the lifetime of the manager (after init and before destroy).
     * </p>
     * The <code>ProvideFactory</code> must be ready to handle manager call to its implemented methods methods 
     * until this method returns.  Any provides currently activate which were started by <code>ProvideRequest</code>s 
     * from the given factory will be canceled. 
     * @param providerFactory the ProvideFactory instance which this manager should remove.  Must not be null.  
     * If the given factory is unknown to the manager it is ignored.
     * @see ProvideFactory
     * @see #addFactory
     * @throws IllegalArgumentException if an illegal value is passed into the method.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public void removeFactory(ProvideFactory providerFactory)
    {
        if (providerFactory == null)
            throw new IllegalArgumentException("providerFactory == null");
        manager.removeFactory(providerFactory);
    }

    /**
     * Cancel a provide.
     * </p>
     * The application is also free to modify the <code>ProviderInfo.getProviderOperation()</code>
     * timeout as might be appropriate to cause an automatic future cancel.
     * @param providerInfo the provider to cancel. Must not be null.
     * @throws IllegalArgumentException if providerInfo is null.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public void cancel(ProviderInfo providerInfo)
    {
        if (providerInfo == null)
            throw new IllegalArgumentException("providerFactory == null");
        providerInfo.getProvideOperation().cancel();
    }

    /**
     * Destroy.
     * </p>
     * Cleanup method typically called by the System Level's shutdown/cleanup code. 
     * If this manager currently has any provides for any factories registered with it,
     * they will all be canceled. 
     * This manager will be non-functional after making this call.
     */
    public void destroy()
    {
        manager.destroy();
    }

    /**
     * Get all registered factories.
     * @return a list of all factories that have been added.
     * @see #addFactory
     * @throws IllegalStateException if the destroy method has been called.
     */
    public List<ProvideFactory> getFactories()
    {
        return manager.getFactories();
    }

    /**
     * Get Provider for given binding request.
     * @param factory the factory associated with the providers being requested.  Must not be null.
     * @param bindingRequest scope, remoteDomainID, objectID, interfaceID to get the provider information for.  Must not be null.
     * @return the activate provider information associated with the bindingRequest if currently being controlled.  Returns null otherwise.
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public ProviderInfo getProvider(ProvideFactory factory, BindingRequest bindingRequest)
    {
        if (factory == null || bindingRequest == null)
            throw new IllegalArgumentException("factory == null || bindingRequest == null");
        return manager.getProvider(factory, bindingRequest);
    }

    /**
     * Get all Providers with nearest match to the given objectID.
     * @param factory The factory to get the list of providers for.  Must not be null.
     * @param patternObjectID the object ID to use as a filter.  Must not be null. 
     * <code>DOFObjectID.BROADCAST</code> can be used as a wild card to return all providers
     * associated with the given factory.
     * @return a list of providers with nearest match.  Will not return null, may return an empty set.  
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public List<ProviderInfo> getMatchingProvides(ProvideFactory factory, DOFObjectID patternObjectID)
    {
        if (factory == null || patternObjectID == null)
            throw new IllegalArgumentException("factory == null || patternObjectID == null");
        return manager.getProviders(factory, patternObjectID);
    }

    /**
     * Get all Providers associated with the given trigger binding request.
     * @param factory The factory to get the list of providers for.  Must not be null.
     * @param triggerRequest the binding request to obtain related provides for.  Must not be null. 
     * @return a list of providers related with the triggerRequest.  Will not return null, may return an empty set.  
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public List<ProviderInfo> getRelatedProvides(ProvideFactory factory, BindingRequest triggerRequest)
    {
        if (factory == null || triggerRequest == null)
            throw new IllegalArgumentException("factory == null || triggerRequest == null");
        return manager.getRelatedProviders(factory, triggerRequest);
    }

    /**
     * Get all Providers matching the given remote domain ID.
     * </p>
     * @param factory The factory to get the list of providers for.  Must not be null
     * @param remoteDomainID the remote domain ID to use as the filter. 
     * Where -1 == non-secure or domain connected with, >=0 a remote domain.
     * @return the activate provider informations associated with the given remote domain ID.
     * Never returns null, but the list can be empty.
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     * @throws IllegalStateException if the destroy method has been called.
     */
    public List<ProviderInfo> getDomainProviders(ProvideFactory factory, int remoteDomainID)
    {
        if (factory == null)
            throw new IllegalArgumentException("factory == null");
        return manager.getDomainProviders(factory, remoteDomainID);
    }

//    /**
//     * Begin Providing with the given information (itemized interest by scope).
//     *  
//     * @param factory the factory to associate with this provide. Must not be null
//     * @param triggerBinding the binding which triggered this provide.  May be null. 
//     * @param scope the securityScope of the domain associated with this provide.  May be null if non-secure system. 
//     * @param remoteDomainID the remoteDomainID of the domain associated with this provide. 
//     * @param objectID the objectID to provide.  Must not be null
//     * @param dofInterface the EMIT interface to provide.  Must not be null.
//     * @param provider the provider implementation to handle the provide.  Must not be null
//     * @param context optional application context object to be associated with this provide.  May be null.
//     * @return the provide information object for the provide.
//     * @throws IllegalArgumentException if an illegal value is passed into the method.
//     * @throws MiddlewareException if unable to obtain a cross domain system.  May be a subclass i.e. TimeoutException.
//     * @throws NotFoundException if trusted domains list given but requested remoteDomainID is not in the list, 
//     * or if the factory given in providerInfo has not been registered.
//     * @throws DuplicateException if the requested binding request is already being provided.
//     */
//
//    public ProviderInfo beginProviding(ProvideFactory factory, BindingRequest triggerBinding, DOFSecurityScope scope, int remoteDomainID, DOFObjectID objectID, DOFInterface dofInterface, Provider provider, Object context) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        if (factory == null || objectID == null || dofInterface == null || provider == null)
//            throw new IllegalArgumentException("factory == null || objectID == null || dofInterface == null || provider == null");
//        return manager.beginProviding(factory, triggerBinding, scope, remoteDomainID, objectID, dofInterface, provider, context);
//    }

//    /**
//     * Begin Providing with the given information (itemized interest by Domain).
//     *  
//     * @param factory the factory to associate with this provide. Must not be null
//     * @param triggerBinding the binding which triggered this provide.  May be null. 
//     * @param domain the domain this provide should be in.  Must not be null 
//     * @param objectID the objectID to provide.  Must not be null
//     * @param dofInterface the EMIT interface to provide.  Must not be null.
//     * @param provider the provider implementation to handle the provide.  Must not be null
//     * @param context optional application context object to be associated with this provide.  May be null.
//     * @return the provide information object for the provide.
//     * @throws IllegalArgumentException if an illegal value is passed into the method.
//     * @throws MiddlewareException if unable to obtain a cross domain system.  May be a subclass i.e. TimeoutException.
//     * @throws NotFoundException if trusted domains list given but requested remoteDomainID is not in the list, 
//     * or if the factory given in providerInfo has not been registered.
//     * @throws DuplicateException if the requested binding request is already being provided.
//     */
//    
//    public ProviderInfo beginProviding(ProvideFactory factory, BindingRequest triggerBinding, Domain domain, DOFObjectID objectID, DOFInterface dofInterface, Provider provider, Object context) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        if (factory == null || domain == null || objectID == null || dofInterface == null || provider == null)
//            throw new IllegalArgumentException("factory == null || objectID == null || dofInterface == null || provider == null");
//        return manager.beginProviding(factory, triggerBinding, domain, objectID, dofInterface, provider, context);
//    }

//    /**
//     * Begin Providing with the given information (BindingRequest interest).
//     *  
//     * @param factory the factory to associate with this provide. Must not be null
//     * @param triggerBinding the binding which triggered this provide.  May be null. 
//     * @param bindingRequest the binding which should be provided.  May not be null.
//     * @param dofInterface the EMIT interface to provide.  Must not be null.
//     * @param provider the provider implementation to handle the provide.  Must not be null
//     * @param context optional application context object to be associated with this provide.  May be null.
//     * @return the provide information object for the provide.
//     * @throws IllegalArgumentException if an illegal value is passed into the method.
//     * @throws MiddlewareException if unable to obtain a cross domain system.  May be a subclass i.e. TimeoutException.
//     * @throws NotFoundException if trusted domains list given but requested remoteDomainID is not in the list, 
//     * or if the factory given in providerInfo has not been registered.
//     * @throws DuplicateException if the requested binding request is already being provided.
//     */
//    
//    public ProviderInfo beginProviding(ProvideFactory factory, BindingRequest triggerBinding, BindingRequest bindingRequest, DOFInterface dofInterface, Provider provider, Object context) throws DuplicateException, NotFoundException, MiddlewareException
//    {
//        if (factory == null || bindingRequest == null || dofInterface == null || provider == null)
//            throw new IllegalArgumentException("factory == null || objectID == null || dofInterface == null || provider == null");
//        return manager.beginProviding(factory, triggerBinding, bindingRequest, dofInterface, provider, context);
//    }
}