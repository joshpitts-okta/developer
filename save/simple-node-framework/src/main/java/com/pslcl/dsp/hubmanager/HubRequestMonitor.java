/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFDomain.State;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFGroupAddress;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Query;
import org.opendof.core.oal.DOFQuery;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFSystem.QueryOperationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.hubmanager.CreateSystemTask.SystemData;

public class HubRequestMonitor implements QueryOperationListener, DOFDomain.StateListener
{
    public final HubProvideFactory hubProvideFactory;
    public final Logger log;
    private final DOFObjectID providerId;
    //    private final AtomicBoolean activated;
    //    private final AtomicBoolean requestProvided;
    //    private final AtomicBoolean gracePeriodExpired;
    //    private final AtomicInteger coreDomainGracePeriodMinutes;
    private final AtomicInteger requestorGracePeriodMinutes; // in domain actual requestor value
    private final AtomicBoolean domainFound;

    // the following are only set one time by an executor thread

    private volatile SystemData systemData;

    private Future<Void> createSystemFuture;
    private Future<Void> gracePeriodFuture;
    private ScheduledFuture<Void> gracePeriodExpiredFuture;
    private DOFOperation.Interest activateInterestOperation;
    private DOFOperation.Query queryOperation;
    private HubProvider provider;

    private DOFObject hubRequestRequestor;
    private DOFConnection.Config hubConfig;

    public HubRequestMonitor(HubProvideFactory hubProvideFactory, DOFObjectID providerId)
    {
        this.hubProvideFactory = hubProvideFactory;
        this.providerId = providerId;
        //        activated = new AtomicBoolean(false);
        //        requestProvided = new AtomicBoolean(false);
        //        gracePeriodExpired = new AtomicBoolean(false);
        requestorGracePeriodMinutes = new AtomicInteger();
        domainFound = new AtomicBoolean(false);
        log = LoggerFactory.getLogger(getClass());
    }

    public synchronized void setCreateSystemFuture(Future<Void> future)
    {
        createSystemFuture = future;
    }

    //    public DOFObjectID getProviderId()
    //    {
    //        return providerId;
    //    }

    //    public void startWithGracePeriod(short gracePeriodMinutes)
    //    {
    //        this.coreDomainGracePeriodMinutes.set(gracePeriodMinutes);
    //        if (systemData == null)
    //        {
    //            try
    //            {
    ////                systemData = createSystemFuture.get();
    //                createSystemFuture.get();
    //            } catch (InterruptedException e)
    //            {
    //                log.debug("Create system task interrupted.");
    //                return;
    //            } catch (ExecutionException e)
    //            {
    //                hubProvideFactory.removeHubRequestMonitor(providerId);
    //                log.warn("Create system failed {} - " + e.getCause(), providerId);
    //                return;
    //            }
    //        }

    //        getSystemAndStartQuery();

    //        if (requestProvided.get())
    //        {
    //            hubProvideFactory.timerExecutor.schedule(new GracePeriodExpiredTask(this), gracePeriodMinutes, TimeUnit.MINUTES);
    //
    //            boolean newProvider = false;
    //            synchronized (this)
    //            {
    //                if (provider == null)
    //                {
    //                    provider = new HubProvider(hubProvideFactory.coreSystem, providerId, gracePeriodMinutes);
    //                    newProvider = true;
    //                }
    //            }
    //            if (newProvider)
    //            {
    //                try
    //                {
    //                    provider.init();
    //
    //                } catch (Exception e)
    //                {
    //                    log.warn("Failed to initialize provider on {} - Unable to start Hub. Exception: " + e, providerId);
    //                    hubProvideFactory.removeHubRequestMonitor(providerId);
    //                    return;
    //                }
    //                provider.requestProvideStopped();
    //                setHubConfig();
    //            }
    //        }
    //    }

    void getSystemAndStartQuery()
    {
        try
        {
            Authentication groupId = DOFObjectID.Authentication.create(providerId.getBase());
            DOFObjectID.Attribute attr = providerId.getAttribute(DOFObjectID.Attribute.GROUP);
            Domain domainId = DOFObjectID.Domain.create(attr.getValueObjectID());
            DOFCredentials domainCredentials = DOFCredentials.create(hubProvideFactory.coreCredentials, domainId);
            //
            //@formatter:off
            DOFSystem.Config domainSystemConfig = new DOFSystem.Config.Builder()
                .setName(groupId.getDataString() + "." + domainId.getDataString() + "-DOFSystem")
                .setCredentials(domainCredentials)
                .setPermissionsExtendAllowed(true)
                .setTunnelDomains(true).build();
            //@formatter:on

            DOFDomain.Config domainConfig = new DOFDomain.Config.Builder(domainCredentials).build();
            DOFDomain serviceDomain = hubProvideFactory.dof.createDomain(domainConfig);
            serviceDomain.addStateListener(this);
            long t0 = System.currentTimeMillis();
            long to = hubProvideFactory.commTimeout;
            synchronized (domainFound)
            {
                while (!domainFound.get())
                {
                    try
                    {
                        domainFound.wait(to);
                        if (domainFound.get())
                            break;
                        long delta = System.currentTimeMillis() - t0;
                        if (delta >= hubProvideFactory.commTimeout)
                            throw new TimeoutException("timed out: " + to + " waiting for Domain listener to report completed");
                        to = hubProvideFactory.commTimeout - delta; // spurious wakeup, or wait(to) slightly off System.currentTimeMillis
                    } finally
                    {
                        serviceDomain.removeStateListener(this);
                    }
                }
            }
            DOFSystem system = hubProvideFactory.dof.createSystem(domainSystemConfig, hubProvideFactory.commTimeout);
            systemData = new CreateSystemTask.SystemData(groupId, domainId, domainCredentials, system);
        } catch (InterruptedException e)
        {
            hubProvideFactory.removeHubRequestMonitor(providerId);
            log.debug("Create system task interrupted.");
            return;
        } catch (Exception e)
        {
            hubProvideFactory.removeHubRequestMonitor(providerId);
            log.debug("Create system failed {} - " + e.getCause(), providerId);
            return;
        }

        synchronized (this)
        {
            if (activateInterestOperation == null || activateInterestOperation.isComplete())
                activateInterestOperation = systemData.system.beginInterest(systemData.groupId, HubRequestInterface.IID, DOFInterestLevel.ACTIVATE, DOF.TIMEOUT_NEVER, null, null);

            if (queryOperation == null || queryOperation.isComplete())
            {
                DOFQuery query = new DOFQuery.Builder().addFilter(providerId).addRestriction(HubRequestInterface.IID).build();
                queryOperation = systemData.system.beginQuery(query, DOF.TIMEOUT_NEVER, this, null);
            }
            createSystemFuture = null;
            log.debug("HubManager dof: " + hubProvideFactory.dof.getState().getName() + " beginQuery for: " + providerId.toStandardString() + ":" + HubRequestInterface.IID);
        }
    }

    //    public void activate()
    //    {
    //        if (systemData == null)
    //        {
    //            try
    //            {
    //                systemData = createSystemFuture.get();  // this is going to block
    //            } catch (InterruptedException e)
    //            {
    //                log.debug("Create system task interrupted.");
    //                return;
    //            } catch (ExecutionException e)
    //            {
    //                log.debug("Create system failed {} - " + e.getCause(), providerId);
    //                return;
    //            }
    //        }
    //
    //        activated.set(true); // maybe set later
    //        startQuery();
    //    }

    public void cancelActivate()
    {
        boolean cleanup = false;
        synchronized (this)
        {
            if (provider == null)
            {
                //If the hubProvider has not been started its because the grace period has not been obtained from the requestor.
                //The requestor not seeing a get call for the grace period is it's sign that something failed (i.e. create system)
                //this flags the requestor to cancel interest and re-issue interest for retries.
                // thus clean everything up to this point
                if (createSystemFuture != null)
                    createSystemFuture.cancel(true);
                if (gracePeriodFuture != null)
                    gracePeriodFuture.cancel(true);
                cleanup = true;
            }
        }
        if (cleanup)
            hubProvideFactory.removeHubRequestMonitor(providerId); // in a state that only a new interest can get it back up.
        //else if the requestor wanted to drop interest at this point, they could simply keep grace period provide up to keep the hub provider active
        // losing and gaining the grace period interface will be the trigger for gracePeriod timeout and cleanup from here out.
    }

    //This method generally should only be called by HubProvideFactory.removeHubRequestMonitor()
    public void destroy()
    {
        if (systemData != null)
            systemData.system.destroy();

        synchronized (this)
        {
            if (createSystemFuture != null)
                createSystemFuture.cancel(true);
            if (queryOperation != null)
                queryOperation.cancel();
            queryOperation = null;

            if (gracePeriodFuture != null)
                gracePeriodFuture.cancel(true);
            gracePeriodFuture = null;

            if (gracePeriodExpiredFuture != null)
                gracePeriodExpiredFuture.cancel(true);
            gracePeriodExpiredFuture = null;

            if (activateInterestOperation != null)
                activateInterestOperation.cancel();
            activateInterestOperation = null;
            if (provider != null)
                provider.destroy();
            provider = null;
            if (hubConfig != null)
                hubProvideFactory.removeHubConfig(hubConfig);
            hubConfig = null;

            if (hubRequestRequestor != null)
                hubRequestRequestor.destroy();
            hubRequestRequestor = null;
        }
    }

    public DOFConnection.Config getHubConfig()
    {
        return hubConfig;
    }

    private void setHubConfig()
    {
        boolean created = false;
        synchronized (this)
        {
            if (hubConfig == null)
            {
                //@formatter:off
                hubConfig = new DOFConnection.Config
                                    .Builder(DOFConnection.Type.HUB, new DOFGroupAddress(systemData.groupId))
                                    .setName(systemData.groupId.getDataString() + "." + systemData.domainId.getDataString() + "-HubConnection")
                                    .setCredentials(systemData.credentials)
                                    .setTunnelDomains(HubManagerModule.HubsTunnelDomains)
                                    .setMaxReceiveSilence(HubManagerModule.HubsMaxReceiveSilence)
                                    .setMaxSendSilence(HubManagerModule.HubsMaxSendSilence)
                                    .build();
                //@formatter:on
                created = true;
            }
        }
        if (created)
            hubProvideFactory.addHubConfig(this);
    }

    void handleInterfaceAdded(DOFObjectID objectId, DOFInterfaceID interfaceId)
    {
        DOFObject requestor;
        int requestorGracePeriodMinutes = -1;
        // this is the flag that other domainManagers started this sequence, MAX_VALUE == started by activate, not broadcast get
        boolean broadcastGetActivated = (hubProvideFactory.coreDomainGracePeriodMinutes.get() != Integer.MAX_VALUE);
        
        try
        {
            requestor = systemData.system.waitProvider(objectId, interfaceId, hubProvideFactory.commTimeout);
            requestorGracePeriodMinutes = HubRequestInterface.getGracePeriod(requestor, hubProvideFactory.commTimeout);

            synchronized (this)
            {
                if (gracePeriodExpiredFuture != null)
                {
                    gracePeriodExpiredFuture.cancel(true);
                    gracePeriodExpiredFuture = null;
                    return;
                }
                hubRequestRequestor = requestor;
            }
        } catch (Exception e)
        {
            /*
             * Interesting point in time here ... the coredomain broadcast's get may have initiated the startup sequence instead of 
             * a requestor's activate.  The coredomain's get handler will be capturing the smallest value seen from the broadcast
             * and we are going to go with the lowest that has been seen at this time. (comm timeout on get from the requestor).
             * If the requestor's provide and getGracePeriod do not fail, then the state of the other cr/domain managers no longer matters 
             * to this manager as we have a know live requestor.
             */

            // this is the flag that other domainManagers started this sequence, MAX_VALUE == started by activate, not broadcast get
            if (!broadcastGetActivated)
            {
                // this is a fatal timeout, clean up the monitor;
                log.warn("Failed to obtain hubRequestRequestor for getting grace period on {}: " + e, providerId);
                hubProvideFactory.removeHubRequestMonitor(providerId);
                return;
            }
        }
        synchronized (this)
        {
            provider = new HubProvider(hubProvideFactory.coreSystem, providerId, requestorGracePeriodMinutes);
            provider.init();
            provider.requestProvideStarted();
        }
        setHubConfig();
        if (broadcastGetActivated && requestorGracePeriodMinutes == -1)
        {
            GracePeriodExpiredTask gracePeriodExpiredTask = new GracePeriodExpiredTask(this);
            synchronized (this)
            {
                if (gracePeriodFuture != null)
                {
                    gracePeriodFuture.cancel(true);
                    gracePeriodFuture = null;
                }
                int remaining = hubProvideFactory.coreDomainGracePeriodMinutes.get();
                gracePeriodExpiredFuture = hubProvideFactory.timerExecutor.schedule(gracePeriodExpiredTask, remaining, TimeUnit.MINUTES);
                provider.requestProvideStopped(remaining);
            }
        }
    }

    public void handleGracePeriodExpired()
    {
        synchronized (this)
        {
            gracePeriodExpiredFuture = null;
            //            if (requestProvided.get())
            //                return;
            //            gracePeriodExpired.set(true);

            if (provider != null)
            {
                provider.destroy();
                provider = null;
            }
        }
        hubProvideFactory.removeHubConfig(hubConfig);
    }

    /**************************************************************************
     * DOFSystem.QueryOperationListener implementation
     **************************************************************************/

    @Override
    public void interfaceAdded(Query operation, DOFObjectID objectId, DOFInterfaceID interfaceId)
    {
        if (!objectId.equals(providerId) || !interfaceId.equals(HubRequestInterface.IID))
            return;
        //        if (gracePeriodExpired.get())
        //            return;
        //        requestProvided.set(true);
        log.trace("Hub Request Provider Added {}:{}", objectId, interfaceId);
        synchronized (this)
        {
            if (gracePeriodExpiredFuture != null)
            {
                gracePeriodExpiredFuture.cancel(true);
                gracePeriodExpiredFuture = null;
                provider.requestProvideStarted();
            }
            if (provider != null)
                return; // existing state is fine, we are done here.
            // first time, or first time after a hubProvideFactory.remove(monitor)
            gracePeriodFuture = hubProvideFactory.executor.submit(new GetGracePeriodTask(this, objectId, interfaceId));
        }
    }

    @Override
    public void interfaceRemoved(Query operation, DOFObjectID objectID, DOFInterfaceID interfaceID)
    {
        if (!objectID.equals(providerId) || !interfaceID.equals(HubRequestInterface.IID))
            return;
        log.trace("Hub Request Provider Removed {}:{}", objectID, interfaceID);

        //        requestProvided.set(false);
        GracePeriodExpiredTask gracePeriodExpiredTask = new GracePeriodExpiredTask(this);
        synchronized (this)
        {
            if (gracePeriodFuture != null)
            {
                gracePeriodFuture.cancel(true);
                gracePeriodFuture = null;
            }
            gracePeriodExpiredFuture = hubProvideFactory.timerExecutor.schedule(gracePeriodExpiredTask, requestorGracePeriodMinutes.get(), TimeUnit.MINUTES);
            if (provider != null)
                provider.requestProvideStopped(-1);
        }
    }

    @Override
    public void providerRemoved(Query operation, DOFObjectID objectID)
    {
        log.trace("providerRemoved {}", objectID);
    }

    @Override
    public void complete(DOFOperation operation, DOFException exception)
    {
        if (exception == null)
        {
            log.trace("Hub Request Query Complete {}", providerId);
        } else
        {
            log.debug("Hub Request Query Complete {} - Exception: ", providerId, exception);
        }
    }

    /**************************************************************************
     * DOFDomain.StateListener implementation
     **************************************************************************/

    @Override
    public void stateChanged(DOFDomain domain, State state)
    {
        if (state.isConnected())
        {
            synchronized (domainFound)
            {
                domainFound.set(true);
                domainFound.notifyAll();
                log.debug(domain.toString() + " became available");
            }
        }
    }

    @Override
    public void removed(DOFDomain domain, DOFException exception)
    {
    }
}