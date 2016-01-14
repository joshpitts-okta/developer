/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFSystem;


/**
 * A factory for creating <code>DOFSystem</code> objects.
 * </p>
 * This class wraps a <code>Callable</code> task which implements the <code>DOFDomain.StateListener</code>
 * and guarantees that authentication is complete for a requested <code>DOFSystem</code> before creating and 
 * returning that system to the caller.
 * This class can be used to cover the following cases:
 * <ul>
 * <li>Need a system in the domain of the credentials</li>
 * <li>Need a system in the domain of a <code>DOFSecurityScope</code></li>
 * <li>Need a system in a given domain</li>
 * </ul>
 */
public class DynamicSystemFactory
{
    /**
     * Get a <code>Future</code> that returns the desired <code>DOFSystem</code>.
     * </p>
     * To get a system in the credentials domain set the credentials and desired 
     * tunneling in the <code>DOFSystem.Config.Builder</code> used to create the
     * given systemConfig.
     * </p>
     * To get a system in a given <code>DOFSecurityScope</code> set the scope
     * in the <code>DOFSystem.Config.Builder.setRemoteDomain</code> method and
     * set the parent credentials and desired tunneling in the builder used to 
     * create the given systemConfig.
     * @param executor the executor service to use to obtain the future.  Must not be null.
     * @param dof the dof to create the system with.  Must not be null.
     * @param systemConfig the <code>DOFSystem.Config</code> to create the system with.  Must not be null.
     * @param timeout time to allow for doing authentication checks and obtaining 
     * the system.
     * @return a Future who's get method will provide the desired system.
     * @throws IllegalArgumentException if any of the input parameters are null.
     */
    public static Future<DOFSystem> getSystemFuture(ExecutorService executor, DOF dof, DOFSystem.Config systemConfig, int timeout)
    {
        if(executor == null || dof == null || systemConfig == null)
            throw new IllegalArgumentException("executor == null || dof == null || systemConfig == null");
        return executor.submit(new GetSystemTask(dof, systemConfig, timeout));
    }

    /**
     * ****************************************************************************
     * Inner classes
     * *****************************************************************************.
     */

    //    @ThreadSafe
    private static class GetSystemTask implements Callable<DOFSystem>, DOFDomain.StateListener
    {
        private final DOF dof;
        private final DOFSystem.Config systemConfig;
        private final int timeout;
        //        @GuardedBy("this")
        private boolean completed;

        private GetSystemTask(DOF dof, DOFSystem.Config systemConfig, int timeout)
        {
            this.dof = dof;
            this.systemConfig = systemConfig;
            this.timeout = timeout;
        }

        @Override
        public DOFSystem call() throws Exception
        {
            DOFCredentials credentials = systemConfig.getCredentials();
            if(credentials == null)
            {
                if(systemConfig.getRemoteDomain() != null)
                    throw new Exception("Scope set but no credentials given");
                return dof.createSystem(systemConfig, timeout);
            }
            DOFDomain.Config domainConfig = new DOFDomain.Config.Builder(systemConfig.getCredentials()).build();
            DOFDomain serviceDomain = dof.createDomain(domainConfig);
            serviceDomain.addStateListener(this);
            long t0 = System.currentTimeMillis();
            long to = timeout;
            synchronized (this)
            {
                while (!completed)
                {
                    try
                    {
                        wait(to);
                        if (completed)
                            break;
                        long delta = System.currentTimeMillis() - t0;
                        if (delta >= timeout)
                            throw new TimeoutException("timed out: " + timeout + " waiting for listener to report completed");
                        to = timeout - delta; // spurious wakeup, or wait(to) slightly off System.currentTimeMillis
                    } finally
                    {
                        serviceDomain.removeStateListener(this);
                    }
                }
            }
            return dof.createSystem(systemConfig, timeout);
        }

        /* *************************************************************************
         * DOFDomain.StateListener implementation
         **************************************************************************/
        @Override
        public void removed(DOFDomain domain, DOFException exception)
        {
        }

        @Override
        public void stateChanged(DOFDomain domain, DOFDomain.State state)
        {
            if (state.isConnected())
            {
                synchronized (this)
                {
                    completed = true;
                    this.notifyAll();
                }
            }
        }
    }
}
