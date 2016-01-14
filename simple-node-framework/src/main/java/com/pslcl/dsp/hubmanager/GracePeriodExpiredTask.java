/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.Callable;

public class GracePeriodExpiredTask implements Callable<Void>
{
    private final HubRequestMonitor monitor;
    
    public GracePeriodExpiredTask(HubRequestMonitor monitor)
    {
        this.monitor = monitor;
    }
    
    @Override
    public Void call() throws Exception
    {
        monitor.handleGracePeriodExpired();
        return null;
    }
}
