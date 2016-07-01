package com.pslcl.internal.test.hubManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFSystem.ActivateInterestListener;
import org.opendof.core.oal.DOFSystem.InterestOperationListener;
import org.opendof.core.oal.value.DOFUInt8;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.dsp.hubmanager.HubRequestInterface;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.DofSystemConfiguration.SystemConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class HubRequestingModule implements Module
{
    public static final String PropertyFileNameKey = "emitdo.dsp.test.hubrequesting.properties-file";
    public static final String GroupBaseKey = "emitdo.dsp.test.hubrequesting.group";
    public static final String GracePeriodKey = "emitdo.dsp.test.hubrequesting.grace-period";

    public static final String PropertyFileNameDefault = "hubrequesting.properties";
    public static final String ModuleNameDefault = "hubRequestingModule";
    public static final String GracePeriodDefault = Integer.toString(1000 * 15);
    
    private final Logger log;
    private StatusTracker statusTracker;
    private final String moduleName;
    private volatile DOFOperation.Query queryOp;
    private volatile short gracePeriod;
    private HubRequestHandler hubRequestHandler;
    private volatile NodeConfig nodeConfig;
    private volatile String subSystemName;
    private volatile List<GroupPair> groupPairs;

    public HubRequestingModule()
    {
        this(HubRequestingModule.class.getSimpleName(), 0);
    }
    
    public HubRequestingModule(String moduleName, int index)
    {
        this.moduleName = moduleName;
        log = LoggerFactory.getLogger(getClass());
        groupPairs = new ArrayList<GroupPair>();
    }

    /* *************************************************************************
     * Module interface implementation 
     **************************************************************************/

    @Override
    public void init(Configuration config) throws Exception
    {
        nodeConfig = (NodeConfig) config;
        nodeConfig.tabLevel.set(3);
        nodeConfig.sb.append("\n");
        nodeConfig.toSb(getClass().getName(), " init:");
        nodeConfig.tabLevel.incrementAndGet();
        
        String value = nodeConfig.properties.getProperty(PropertyFileNameKey, PropertyFileNameDefault);
        nodeConfig.toSb(PropertyFileNameKey, "=", value);
        NodeConfiguration.loadProperties(nodeConfig, value);

        String fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);
        subSystemName = StrH.getAtomicName(fdn, '.');
        String pen = StrH.getPenultimateName(fdn, '.');
        if(pen != null)
            subSystemName = pen;

        List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(GroupBaseKey, nodeConfig.properties);
        nodeConfig.toSb("Groups to fire hubs for:");
        nodeConfig.tabLevel.incrementAndGet();
        for(Entry<String, String> entry : list)
        {
            String[] pair = entry.getValue().split(" ");
            nodeConfig.toSb(entry.getKey(), "= domain: ", pair[0], " group: ", pair[1], " system: ", pair[2]);
            Domain domainId = Domain.create(pair[0]);
            Authentication groupId = Authentication.create(pair[1]);
            groupPairs.add(new GroupPair(domainId, groupId, pair[2]));
        }
        
        if(list.size() == 0)
        {
            nodeConfig.toSb("none given");
            throw new Exception("At least one domain/group pair must be given");
        }
        nodeConfig.tabLevel.decrementAndGet();
        
        gracePeriod = Short.parseShort(nodeConfig.properties.getProperty(GracePeriodKey, GracePeriodDefault));
        nodeConfig.toSb(GracePeriodKey, "=", ""+gracePeriod);

        NodeConfiguration.removePropertiesForBaseKey(GroupBaseKey, nodeConfig.properties);
        nodeConfig.properties.remove(PropertyFileNameKey);
        nodeConfig.properties.remove(GracePeriodKey);
        nodeConfig.properties.remove(NodeConfiguration.FdnKey);
    }

    @Override
    public void start(Node node) throws Exception
    {
        synchronized (this)
        {
            statusTracker = node.getStatusTracker();
            statusTracker.setStatus(moduleName, Status.Warn);
            hubRequestHandler = new HubRequestHandler(node);
            new Thread(hubRequestHandler).start();
        }
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug("\n" + getClass().getName() + " stop");

        synchronized (this)
        {
            if (queryOp != null)
            {
                queryOp.cancel();
                queryOp = null;
            }
            //            if(connectionManager != null)
            //                connectionManager.releaseSystemForConnection(dof, connectionConfig);
            if(hubRequestHandler != null)
                hubRequestHandler.close();
            hubRequestHandler = null;
            if(statusTracker != null)
                statusTracker.setStatus(moduleName, Status.Warn);
            statusTracker = null;
        }
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");
        if (statusTracker != null)
            statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
        if (hubRequestHandler != null)
            hubRequestHandler.close();
        hubRequestHandler = null;
    }

    private class HubRequestHandler implements Runnable, InterestOperationListener, ActivateInterestListener
    {
        private final SimpleNodeFramework node;
        private final Hashtable<DOFSystem, InterestTimeoutTask> operations;
        private final List<DOFSystem> systems;
        private final List<DOFObject> providers;
//        private volatile  CredConnSysConfig ccsc;
        private final Hashtable<DOFSystem, String> systemMap;

        private HubRequestHandler(Node node)
        {
            this.node = (SimpleNodeFramework) node;
            operations = new Hashtable<DOFSystem, InterestTimeoutTask>();
            systemMap = new Hashtable<DOFSystem, String>();
            systems = new ArrayList<DOFSystem>();
            providers = new ArrayList<DOFObject>();
        }

        @Override
        public void run()
        {
            StringBuilder sb = new StringBuilder("\nHubRequestHandler run:");
            int level = 1;
            try
            {
                HubActivateInterestListener listener = new HubActivateInterestListener();
                StrH.ttl(sb, level, "groupPairs:");
                ++level;
                for(GroupPair pair : groupPairs)
                {
                    String fdn = subSystemName + ".";
                    fdn += StrH.getPenultimateName(pair.systemName, '.');
                    int timeout = nodeConfig.getCredConnSysConfig(fdn).commTimeout;
                    String systemName = StrH.getAtomicName(pair.systemName, '.');
                    StrH.ttl(sb, level, "domainId: ", pair.domainId, " groupId: ", pair.groupId, " systemName: ", systemName);
//                    SystemConfig systemConfig = nodeConfig.getSystemConfig("hubmanager.cr", "d0bffbff");
                    SystemConfig systemConfig = nodeConfig.getSystemConfig(fdn, systemName);
                    systemConfig.toString(sb, level);
                    DOFSystem system = systemConfig.getSystem();
                    system.addActivateInterestListener(this);
//                    DOFSystem system = nodeConfig.getCredConnSysConfig(fdn).getSystemConfig(pair.systemName).getSystem();
                    system.addActivateInterestListener(listener);
                    DOFObjectID augmentedId = pair.getAugmentedId();
                    systems.add(system);
                    DOFObject provider = system.createObject(augmentedId);
                    InterestTimeoutTask itt = new InterestTimeoutTask(this, system, augmentedId);
                    DOFOperation op = provider.beginProvide(HubRequestInterface.DEF, DOF.TIMEOUT_NEVER, new HubRequestProvider(), itt);
                    synchronized (operations)
                    {
                        systemMap.put(system, fdn);
                        itt.setOp(op);
                        operations.put(system, itt);
                        providers.add(provider);
                        itt.setFuture(nodeConfig.timer.schedule(itt, timeout, TimeUnit.MILLISECONDS));
                    }
                    system.beginInterest(augmentedId, HubRequestInterface.IID, DOFInterestLevel.ACTIVATE, DOF.TIMEOUT_NEVER, this, system);
                    StrH.ttl(sb, level, "expressed interest in: ", augmentedId, ":", HubRequestInterface.IID);
                    StrH.ttl(sb, level, "providing: ", augmentedId, ":", HubRequestInterface.IID, " on dof: ", nodeConfig.getCredConnSysConfig(fdn).subsystem.dofConfig.config.getName() + "\n");
                }
                log.debug(sb.toString());
                statusTracker.setStatus(moduleName, Status.Ok);
                node.setModuleStatus(HubRequestingModule.this, ModuleStatus.Started);
                //TODO: fire an event here to let pointmodule know it can start to work.
                synchronized(this)
                {
                    wait();
                }
//                provideOperation.cancel();
            } catch (Exception e)
            {
                log.debug(sb.toString());
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(HubRequestingModule.this, ModuleStatus.Failed);
            }

            synchronized (operations)
            {
                while (operations.size() > 0)
                {
                    try
                    {
                        operations.wait();
                    } catch (InterruptedException e)
                    {
                        log.warn("unexpected wakeup", e);
                    }
                }
            }
        }

        private void interestFailed(DOFObjectID providerId, DOFSystem system)
        {
            synchronized (operations)
            {
                String fdn = systemMap.get(system);
                SystemConfig systemConfig = nodeConfig.getSystemConfig(fdn, system.getState().getName());
                systemConfig.toString();
                InterestTimeoutTask itt = operations.get(system);
                if(itt.op != null)
                    itt.op.cancel();
            }
            node.setModuleStatus(HubRequestingModule.this, ModuleStatus.Failed);
        }
        
        public void close()
        {
            synchronized (operations)
            {
                for (Entry<DOFSystem, InterestTimeoutTask> entry : operations.entrySet())
                    entry.getValue().op.cancel();
                operations.clear();
                operations.notifyAll();
                for(DOFObject provider : providers)
                    provider.destroy();
                providers.clear();
                for(DOFSystem system : systems)
                    system.destroy();
                systems.clear();
            }
        }

        @Override
        public void complete(DOFOperation operation, DOFException exception)
        {
            if(exception != null)
                log.error("Interest operation failed: ", exception);
        }

        @Override
        public void activate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
        {
            log.info("interested caused activate to hit");
        }

        @Override
        public void cancelActivate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
        {
            log.info("cancelActivate callback hit");
       }

        @Override
        public void removed(DOFSystem system, DOFException exception)
        {
            log.info("removed callback hit");
        }
        
        private class HubRequestProvider extends DOFObject.DefaultProvider
        {
            @Override
            public void get(DOFOperation.Provide operation, DOFRequest.Get request, DOFInterface.Property property)
            {
                if (property.getItemID() == 1)
                {
                    synchronized (operations)
                    {
                        
                        InterestTimeoutTask itt = (InterestTimeoutTask)operation.getContext();
                        if(itt.future != null)
                            itt.future.cancel(true);
                    }
                    request.respond(new DOFUInt8((byte)(gracePeriod)));
                    //TODO: drop interest a few ms after this
                }
            }
        }
    }

    private class HubActivateInterestListener implements DOFSystem.ActivateInterestListener
    {
        @Override
        public void activate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
        {
            log.info("activate - {}:{}" + objectID, interfaceID);

        }

        @Override
        public void cancelActivate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
        {
            log.info("cancelActivate - {}:{}" + objectID, interfaceID);
        }

        @Override
        public void removed(DOFSystem system, DOFException exception)
        {
            log.trace("removed");
        }
    }
    
    private class InterestTimeoutTask implements Callable<Void>
    {
        private volatile Future<Void> future;
        private final DOFSystem system;
        private volatile DOFOperation op;
        private final DOFObjectID providerId;
        private final HubRequestHandler handler;
        
        private InterestTimeoutTask(HubRequestHandler handler, DOFSystem system, DOFObjectID providerId)
        {
            this.handler = handler;
            this.system = system;
            this.op = op;
            this.providerId = providerId;
        }
        
        private void setOp(DOFOperation op)
        {
            this.op = op;
        }
        
        private void setFuture(Future<Void> future)
        {
            this.future = future;
        }
        
        @Override
        public Void call() throws Exception
        {
            handler.interestFailed(providerId, system);
            return null;
        }
    }
    
    private class GroupPair
    {
        private String systemName;
        private Authentication groupId;
        private Domain domainId;
        
        private GroupPair(Domain domainId, Authentication groupId, String systemName)
        {
            this.groupId = groupId;
            this.domainId = domainId;
            this.systemName = systemName;
        }
        
        private DOFObjectID getAugmentedId()
        {
            DOFObjectID.Attribute groupAttribute = DOFObjectID.Attribute.create(DOFObjectID.Attribute.GROUP, domainId);
            return DOFObjectID.create(groupId, groupAttribute);
        }
        
        @Override
        public String toString()
        {
            return "domainId: " + domainId.toStandardString() + " groupId: " + groupId.toStandardString();
        }
    }
}