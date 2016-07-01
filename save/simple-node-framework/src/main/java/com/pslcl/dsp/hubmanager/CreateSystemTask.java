/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.Callable;

import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFSystem;


public class CreateSystemTask implements Callable<Void>//, DOFDomain.StateListener
{
    private final HubRequestMonitor monitor;
//    private boolean completed;

    public CreateSystemTask(HubRequestMonitor monitor)
    {
        this.monitor = monitor;
    }

    @Override
    public Void call() throws Exception
    {
//        DOFObjectID providerId = monitor.getProviderId();
//        Authentication groupId = DOFObjectID.Authentication.create(providerId.getBase());
//        DOFObjectID.Attribute attr = providerId.getAttribute(DOFObjectID.Attribute.GROUP);
//        Domain domainId = DOFObjectID.Domain.create(attr.getValueObjectID());
//        DOFCredentials domainCredentials = DOFCredentials.create(monitor.hubProvideFactory.coreCredentials, domainId);
//        //
//        //@formatter:off
//        DOFSystem.Config domainSystemConfig = new DOFSystem.Config.Builder()
//            .setName(groupId.getDataString() + "." + domainId.getDataString() + "-DOFSystem")
//            .setCredentials(domainCredentials)
//            .setPermissionsExtendAllowed(true)
//            .setTunnelDomains(true).build();
//        //@formatter:on
//
//        DOFDomain.Config domainConfig = new DOFDomain.Config.Builder(domainCredentials).build();
//        DOFDomain serviceDomain = monitor.hubProvideFactory.dof.createDomain(domainConfig);
//        serviceDomain.addStateListener(this);
//        long t0 = System.currentTimeMillis();
//        int timeout = monitor.hubProvideFactory.commTimeout;
//        long to = timeout;
//        synchronized (this)
//        {
//            while (!completed)
//            {
//                try
//                {
//                    wait(to);
//                    if (completed)
//                        break;
//                    long delta = System.currentTimeMillis() - t0;
//                    if (delta >= timeout)
//                        throw new TimeoutException("timed out: " + to + " waiting for Domain listener to report completed");
//                    to = timeout - delta; // spurious wakeup, or wait(to) slightly off System.currentTimeMillis
//                } finally
//                {
//                    serviceDomain.removeStateListener(this);
//                }
//            }
//        }
//        DOFSystem system = monitor.hubProvideFactory.dof.createSystem(domainSystemConfig, timeout);
//        SystemData sdata = new SystemData(groupId, domainId, domainCredentials, system);
        monitor.getSystemAndStartQuery();
        return null;
    }

//    /* *************************************************************************
//     * DOFDomain.StateListener implementation
//     **************************************************************************/
//    @Override
//    public void removed(DOFDomain domain, DOFException exception)
//    {
//    }
//
//    @Override
//    public void stateChanged(DOFDomain domain, DOFDomain.State state)
//    {
//        if (state.isConnected())
//        {
//            synchronized (this)
//            {
//                completed = true;
//                this.notifyAll();
//            }
//        }
//    }

    public static class SystemData
    {
        public final Authentication groupId;
        public final Domain domainId;
        public final DOFCredentials credentials;
        public final DOFSystem system;

        public SystemData(Authentication groupId, Domain domainId, DOFCredentials credentials, DOFSystem system)
        {
            this.groupId = groupId;
            this.domainId = domainId;
            this.credentials = credentials;
            this.system = system;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new 
                            StringBuilder("\nsystem: " + system.getState().getName())
                            .append("\n\tdomainId: " + domainId.toStandardString())
                            .append("\n\tgroupId: " + groupId.toStandardString())
                            .append("\n\tcreds: " + credentials.toString());
            return sb.toString();
        }
    }
}
