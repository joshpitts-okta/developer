/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObject.GetOperationListener;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Get;
import org.opendof.core.oal.DOFProviderInfo;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFUInt8;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubProvideFactory implements GetOperationListener
{
    public static final String StatusTrackerName = "HubProvideFactory";

    public final DOFCredentials coreCredentials;
    public final DOF dof;
    public final int commTimeout;
    public final ScheduledExecutorService timerExecutor;
    public final DOFSystem coreSystem;
    public final ExecutorService executor;
    private final ServerMonitor serverMonitor;
    // guarded by self
    private final Map<DOFObjectID, HubRequestMonitor> hubRequestMonitors;
    //    private final Object dataMonitor;
//    private final StatusTracker statusTracker;
    final AtomicInteger coreDomainGracePeriodMinutes;  // Other domain manager's HubProvider are providing in core domain

    private DOFObject coreBroadcastObject;

    private final Logger log;

    public HubProvideFactory(ScheduledExecutorService timerExecutor, ExecutorService executor, StatusTracker statusTracker, ServerMonitor serverMonitor, DOF dof, DOFSystem coreSystem, DOFCredentials coreCredentials, int commTimeout)
    {
        super();
        this.timerExecutor = timerExecutor;
        this.executor = executor;
//        this.statusTracker = statusTracker;
        this.serverMonitor = serverMonitor;
        this.dof = dof;
        this.coreSystem = coreSystem;
        this.coreCredentials = coreCredentials;
        this.commTimeout = commTimeout;
        coreDomainGracePeriodMinutes = new AtomicInteger(Integer.MAX_VALUE) ;  // find the smallest reported from broadcast

        hubRequestMonitors = new HashMap<DOFObjectID, HubRequestMonitor>();
        log = LoggerFactory.getLogger(getClass());
    }

    public void init()
    {
        coreBroadcastObject = coreSystem.createObject(DOFObjectID.BROADCAST);
        coreBroadcastObject.beginGet(HubRequestInterface.DEF.getProperty(HubRequestInterface.GracePeriod_ItemId), commTimeout, this, null);
    }

    public boolean isConnected(DOFConnection.Config hubConfig)
    {
        return serverMonitor.isConnected(hubConfig);
    }

    public void addHubConfig(HubRequestMonitor requestMonitor)
    {
        serverMonitor.addHubConfig(requestMonitor);
    }

    public void removeHubConfig(DOFConnection.Config config)
    {
        serverMonitor.removeHubConfig(config);
    }

    public DOFValue[] getProviderHubList(DOFConnection.Config hubConfig)
    {
        return serverMonitor.getProviderHubList(hubConfig);
    }

    public DOFValue getProviderHubState(DOFConnection.Config hubConfig)
    {
        return serverMonitor.getProviderHubState(hubConfig);
    }

    public void removeHubRequestMonitor(DOFObjectID objectID)
    {
        HubRequestMonitor hubRequestMonitor = null;
        synchronized (hubRequestMonitors)
        {
            hubRequestMonitor = hubRequestMonitors.remove(objectID);
        }
        if (hubRequestMonitor != null)
            hubRequestMonitor.destroy();
    }

    /**************************************************************************
     * ProvideFactory implementation In anticipation of service utils.
     * @param objectID the oid to activate
     * @param interfaceID the iid to activate
     **************************************************************************/
    public void activate(DOFObjectID objectID, DOFInterfaceID interfaceID)
    {
        if (!interfaceID.equals(HubRequestInterface.IID))
            return;
        if (!objectID.hasAttribute(DOFObjectID.Attribute.GROUP))
            return;

        log.trace("HubProviderFactory activate {}", objectID);

        HubRequestMonitor hubRequestMonitor = null;
        synchronized (hubRequestMonitors)
        {
            hubRequestMonitor = hubRequestMonitors.get(objectID);
            if (hubRequestMonitor == null)
            {
                // the coreDomain broadcast get has not initiated the monitor yet.
                hubRequestMonitor = new HubRequestMonitor(this, objectID);
                hubRequestMonitor.setCreateSystemFuture(executor.submit(new CreateSystemTask(hubRequestMonitor)));
                hubRequestMonitors.put(objectID, hubRequestMonitor);
            }
        }
    }

    public void cancelActivate(DOFObjectID objectId, DOFInterfaceID interfaceId)
    {
        if (!interfaceId.equals(HubRequestInterface.IID))
            return;

        log.trace("HubProviderFactory cancelActivate {}", objectId);
        HubRequestMonitor monitor;
        synchronized (hubRequestMonitors)
        {
            monitor = hubRequestMonitors.get(objectId);
        }
        if (monitor != null)
            monitor.cancelActivate();
    }

    public void removed()
    {
        synchronized (hubRequestMonitors)
        {
            for (HubRequestMonitor hubRequestMonitor : hubRequestMonitors.values())
                hubRequestMonitor.destroy();
            hubRequestMonitors.clear();
        }
    }

    /**************************************************************************
     * DOFOperation.OperationListener implementation
     **************************************************************************/

    @Override
    public void complete(DOFOperation operation, DOFException exception)
    {
        String op = null;
        if (operation instanceof DOFOperation.Get)
        {
            DOFOperation.Get getOperation = (DOFOperation.Get) operation;
            op = "Get " + getOperation.getObject() + ":" + getOperation.getProperty().getInterfaceID();
        }

        if (exception == null)
        {
            log.trace("HubProvideFactory " + op + " operation complete.");
        } else
        {
            log.debug("HubProvideFactory " + op + " operation complete. Exception: " + exception);
        }
    }

    /**************************************************************************
     * DOFSystem.GetOperationListener implementation
     **************************************************************************/

    @Override
    public void getResult(Get operation, DOFProviderInfo providerInfo, DOFValue value, DOFException exception)
    {
        if (exception != null || value == null)
        {
            log.debug("Failed to get " + providerInfo.getProviderID() + " gracePeriod.", exception);
            return;
        }

        DOFObjectID providerId = providerInfo.getProviderID();
        HubRequestMonitor monitor = null;
        synchronized (hubRequestMonitors)
        {
            monitor = hubRequestMonitors.get(providerId);
            if (monitor == null)
            {
                // requestor activate in its domain has not hit yet
                monitor = new HubRequestMonitor(this, providerId);
                monitor.setCreateSystemFuture(executor.submit(new CreateSystemTask(monitor)));
                hubRequestMonitors.put(providerId, monitor);
            }
        }
        
        short otherHubManagerGracePeriod = ((DOFUInt8)value).get();
        if(otherHubManagerGracePeriod < coreDomainGracePeriodMinutes.get())
            coreDomainGracePeriodMinutes.set(otherHubManagerGracePeriod);
//        monitor.startWithGracePeriod(coreDomainGracePeriodMinutes.get());
    }

    //    public static class HubRequestMonitor implements QueryOperationListener
    //    {
    //        private static final int timeout = 5000;
    //
    //        private final HubProvideFactory hubProvideFactory;
    //        private final DOFObjectID providerID;
    //        private final AtomicBoolean activated;
    //        private final AtomicBoolean requestProvided;
    //        private final AtomicBoolean gracePeriodExpired;
    //        private volatile Future<Void> createSystemFuture; // only set by instantiating thread
    //        
    //        private volatile DOFCredentials domainCredentials;
    //        
    //        private DOFObjectID.Authentication groupID;
    //        private DOFObjectID.Domain domainID;
    //        private DOFSystem requestorDomainSystem;
    //        private DOFObject hubRequestRequestor;
    //        private DOFConnection.Config hubConfig;
    //        private HubProvider provider;
    //        private DOFOperation.Interest activateInterestOperation;
    //        private DOFOperation.Query queryOperation;
    //        private ScheduledFuture<Void> gracePeriodExpiredFuture;
    //        private Future<Void> gracePeriodFuture;
    //
    //        private short gracePeriodMinutes;
    //
    //        public HubRequestMonitor(HubProvideFactory hubProvideFactory, DOFObjectID providerID)
    //        {
    //            this.hubProvideFactory = hubProvideFactory;
    //            this.providerID = providerID;
    //            activated = new AtomicBoolean(false);
    //            requestProvided = new AtomicBoolean(false);
    //            gracePeriodExpired = new AtomicBoolean(false);
    //        }
    //
    //        public void setCreateSystemFuture(Future<Void> future)
    //        {
    //            createSystemFuture = future;
    //        }
    //        
    //        public void startWithGracePeriod(short gracePeriodMinutes)
    //        {
    //            if (requestorDomainSystem == null)
    //            {
    //                try
    //                {
    //                    createSystemFuture.get();
    //                } catch (InterruptedException e)
    //                {
    //                    hubProvideFactory.log.debug("Create system task interrupted.");
    //                    return;
    //                } catch (ExecutionException e)
    //                {
    //                    hubProvideFactory.log.debug("Create system failed {} - " + e.getCause(), providerID);
    //                    return;
    //                }
    //            }
    //
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                this.gracePeriodMinutes = gracePeriodMinutes;
    //                startQuery();
    //
    //                if (!isRequestProvided)
    //                {
    //                    hubProvideFactory.timerExecutor.schedule(new GracePeriodExpiredTask(), gracePeriodMinutes, TimeUnit.MINUTES);
    //
    //                    if (provider == null)
    //                    {
    //                        provider = new HubProvider();
    //                        try
    //                        {
    //                            provider.init(hubProvideFactory.coreSystem, providerID, gracePeriodMinutes);
    //                        } catch (Exception e)
    //                        {
    //                            hubProvideFactory.log.warn("Failed to initialize provider on {} - Unable to start Hub. Exception: " + e, providerID);
    //                            hubProvideFactory.removeHubRequestMonitor(providerID);
    //                            return;
    //                        }
    //                        provider.requestProvideStopped();
    //                        setHubConfig();
    //                    }
    //                }
    //            }
    //        }
    //
    //        private void startQuery()
    //        {
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                if (activateInterestOperation == null || activateInterestOperation.isComplete())
    //                {
    //                    activateInterestOperation = requestorDomainSystem.beginInterest(groupID, HubRequestInterface.IID, DOFInterestLevel.ACTIVATE, DOF.TIMEOUT_NEVER, null, null);
    //                }
    //                if (queryOperation == null || queryOperation.isComplete())
    //                {
    //                    DOFQuery query = new DOFQuery.Builder().addFilter(providerID).addRestriction(HubRequestInterface.IID).build();
    //                    queryOperation = requestorDomainSystem.beginQuery(query, DOF.TIMEOUT_NEVER, this, null);
    //                }
    //            }
    //        }
    //
    //        public void activate()
    //        {
    //            if (requestorDomainSystem == null)
    //            {
    //                try
    //                {
    //hubProvideFactory.log.debug("createSystemFuture.get() being called");
    //                    createSystemFuture.get();
    //hubProvideFactory.log.debug("createSystemFuture.get() finished");
    //                } catch (InterruptedException e)
    //                {
    //                    hubProvideFactory.log.debug("Create system task interrupted.");
    //                    return;
    //                } catch (ExecutionException e)
    //                {
    //                    hubProvideFactory.log.debug("Create system failed {} - " + e.getCause(), providerID);
    //                    return;
    //                }
    //            }
    //
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                isActivated = true;
    //                startQuery();
    //            }
    //        }
    //
    //        public void cancelActivate()
    //        {
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                isActivated = false;
    //
    //                if (isGracePeriodExpired)
    //                {
    //                    hubProvideFactory.removeHubRequestMonitor(providerID);
    //                }
    //            }
    //        }
    //
    //        //This method generally should only be called by HubProvideFactory.removeHubRequestMonitor()
    //        public void destroy()
    //        {
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                if (hubConfig != null)
    //                    hubProvideFactory.removeHubConfig(hubConfig);
    //
    //                if (createSystemFuture != null)
    //                    createSystemFuture.cancel(false);
    //                if (gracePeriodFuture != null)
    //                    gracePeriodFuture.cancel(false);
    //                if (gracePeriodExpiredFuture != null)
    //                    gracePeriodExpiredFuture.cancel(false);
    //
    //                if (queryOperation != null)
    //                    queryOperation.cancel();
    //                if (activateInterestOperation != null)
    //                    activateInterestOperation.cancel();
    //                if (provider != null)
    //                    provider.destroy();
    //                if (requestorDomainSystem != null)
    //                    requestorDomainSystem.destroy();
    //
    //                queryOperation = null;
    //                activateInterestOperation = null;
    //                provider = null;
    //                requestorDomainSystem = null;
    //                gracePeriodExpiredFuture = null;
    //                gracePeriodFuture = null;
    //                createSystemFuture = null;
    //            }
    //        }
    //
    //        public DOFConnection.Config getHubConfig()
    //        {
    //            return hubConfig;
    //        }
    //
    //        private void setHubConfig()
    //        {
    //            if (requestorDomainSystem == null)
    //                throw new IllegalStateException("System for {}:{} not created. Cannot start hub.");
    //
    //            if (hubConfig == null)
    //            {
    //                hubConfig = new DOFConnection.Config.Builder(DOFConnection.Type.HUB, new DOFGroupAddress(groupID)).setName(groupID.getDataString() + "." + domainID.getDataString() + "-HubConnection").setCredentials(domainCredentials).setTunnelDomains(HubManagerModule.HubsTunnelDomains).setMaxReceiveSilence(HubManagerModule.HubsMaxReceiveSilence).setMaxSendSilence(HubManagerModule.HubsMaxSendSilence).build();
    //            }
    //            hubProvideFactory.addHubConfig(this);
    //        }
    //
    //        /**************************************************************************
    //         * DOFSystem.QueryOperationListener implementation
    //         **************************************************************************/
    //
    //        @Override
    //        public void interfaceAdded(Query operation, DOFObjectID objectID, DOFInterfaceID interfaceID)
    //        {
    //            if (!objectID.equals(providerID) || !interfaceID.equals(HubRequestInterface.IID))
    //            {
    //                return;
    //            }
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                if (isGracePeriodExpired)
    //                {
    //                    return;
    //                }
    //                isRequestProvided = true;
    //            }
    //
    //            hubProvideFactory.log.trace("Hub Request Provider Added {}:{}", objectID, interfaceID);
    //            gracePeriodFuture = hubProvideFactory.executor.submit(new GetGracePeriodTask(objectID, interfaceID));
    //        }
    //
    //        @Override
    //        public void interfaceRemoved(Query operation, DOFObjectID objectID, DOFInterfaceID interfaceID)
    //        {
    //            if (!objectID.equals(providerID) || !interfaceID.equals(HubRequestInterface.IID))
    //            {
    //                return;
    //            }
    //            hubProvideFactory.log.trace("Hub Request Provider Removed {}:{}", objectID, interfaceID);
    //
    //            synchronized (hubProvideFactory.dataMonitor)
    //            {
    //                isRequestProvided = false;
    //                GracePeriodExpiredTask gracePeriodExpiredTask = new GracePeriodExpiredTask();
    //                gracePeriodExpiredFuture = hubProvideFactory.timerExecutor.schedule(gracePeriodExpiredTask, gracePeriodMinutes, TimeUnit.MINUTES);
    //
    //                if (provider != null)
    //                {
    //                    provider.requestProvideStopped();
    //                }
    //            }
    //
    //        }
    //
    //        @Override
    //        public void providerRemoved(Query operation, DOFObjectID objectID)
    //        {
    //            hubProvideFactory.log.trace("providerRemoved {}", objectID);
    //
    //        }
    //
    //        @Override
    //        public void complete(DOFOperation operation, DOFException exception)
    //        {
    //            if (exception == null)
    //            {
    //                hubProvideFactory.log.trace("Hub Request Query Complete {}", providerID);
    //            } else
    //            {
    //                hubProvideFactory.log.debug("Hub Request Query Complete {} - Exception: ", providerID, exception);
    //            }
    //        }
    //
    //        private class GracePeriodExpiredTask implements Callable<Void>
    //        {
    //
    //            @Override
    //            public Void call() throws Exception
    //            {
    //                synchronized (hubProvideFactory.dataMonitor)
    //                {
    //                    gracePeriodExpiredFuture = null;
    //                    if (isRequestProvided)
    //                    {
    //                        return null;
    //                    }
    //                    isGracePeriodExpired = true;
    //
    //                    if (!isActivated)
    //                    {
    //                        hubProvideFactory.removeHubRequestMonitor(providerID);
    //                    } else
    //                    {
    //                        if (provider != null)
    //                            provider.destroy();
    //                        provider = null;
    //                        hubProvideFactory.removeHubConfig(hubConfig);
    //                    }
    //                }
    //                return null;
    //            }
    //        }
    //
    //        private class GetGracePeriodTask implements Callable<Void>
    //        {
    //
    //            private final DOFObjectID objectID;
    //            private final DOFInterfaceID interfaceID;
    //
    //            public GetGracePeriodTask(DOFObjectID objectID, DOFInterfaceID interfaceID)
    //            {
    //                this.objectID = objectID;
    //                this.interfaceID = interfaceID;
    //            }
    //
    //            @Override
    //            public Void call() throws Exception
    //            {
    //                if (hubRequestRequestor == null)
    //                {
    //                    try
    //                    {
    //                        hubRequestRequestor = requestorDomainSystem.waitProvider(objectID, interfaceID, timeout);
    //                    } catch (DOFException e)
    //                    {
    //                        hubProvideFactory.log.warn("Failed waitProvider {}:{} - Unable to start Hub. Exception: " + e, objectID, interfaceID);
    //                        hubProvideFactory.removeHubRequestMonitor(providerID);
    //                        throw e;
    //                    }
    //                }
    //
    //                try
    //                {
    //                    gracePeriodMinutes = HubRequestInterface.getGracePeriod(hubRequestRequestor, timeout);
    //                } catch (DOFException e)
    //                {
    //                    hubProvideFactory.log.warn("Failed to obtain grace period {}:{} - Unable to start Hub. Exception: " + e, objectID, interfaceID);
    //                    hubProvideFactory.removeHubRequestMonitor(providerID);
    //                    throw e;
    //                }
    //
    //                synchronized (hubProvideFactory.dataMonitor)
    //                {
    //                    isGracePeriodExpired = false;
    //
    //                    if (provider == null)
    //                    {
    //                        provider = new HubProvider();
    //                        try
    //                        {
    //                            provider.init(hubProvideFactory.coreSystem, providerID, gracePeriodMinutes);
    //                        } catch (DOFException e)
    //                        {
    //                            hubProvideFactory.log.warn("Failed to initialize provider on {} - Unable to start Hub. Exception: " + e, providerID);
    //                            hubProvideFactory.removeHubRequestMonitor(providerID);
    //                            throw e;
    //                        }
    //                        provider.requestProvideStarted();
    //                        setHubConfig();
    //                    }
    //
    //                    if (gracePeriodExpiredFuture != null)
    //                    {
    //                        gracePeriodExpiredFuture.cancel(false);
    //                        gracePeriodExpiredFuture = null;
    //                    }
    //                }
    //                return null;
    //            }
    //        }
    //
    //        public static class CreateSystemTask implements Callable<Void>
    //        {
    //            public CreateSystemTask( )
    //            {
    //            }
    //            
    //            @Override
    //            public Void call() throws Exception
    //            {
    //                groupID = DOFObjectID.Authentication.create(providerID.getBase());
    //                DOFObjectID.Attribute attr = providerID.getAttribute(DOFObjectID.Attribute.GROUP);
    //                domainID = DOFObjectID.Domain.create(attr.getValueObjectID());
    //                try
    //                {
    //                    domainCredentials = DOFCredentials.create(hubProvideFactory.coreCredentials, domainID);
    //
    //                    DOFSystem.Config domainSystemConfig = new DOFSystem.Config.Builder().setName(groupID.getDataString() + "." + domainID.getDataString() + "-DOFSystem").setCredentials(domainCredentials).setPermissionsExtendAllowed(true).setTunnelDomains(true).build();
    //
    //                    requestorDomainSystem = DynamicSystemFactory.getSystemFuture(hubProvideFactory.executor, hubProvideFactory.dof, domainSystemConfig, hubProvideFactory.commTimeout).get();
    //                } catch (ExecutionException e)
    //                {
    //                    hubProvideFactory.log.warn("Failed to create System in domain {} - Unable to start Hub. Exception: " + e.getCause(), domainID);
    //                    hubProvideFactory.removeHubRequestMonitor(providerID);
    //                    throw e;
    //                }
    //                return null;
    //            }
    //        }
}
