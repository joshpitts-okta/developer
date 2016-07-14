/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Class TimerHelper.
 */
public class TimerHelper extends TimerTask
{
    private static Timer timer;
    private final AtomicBoolean monitor;
    private final AtomicBoolean errorFlag;
    
    /**
     * Instantiates a new timer helper.
     *
     * @param monitor the monitor
     * @param errorFlag the error flag
     */
    public TimerHelper(AtomicBoolean monitor, AtomicBoolean errorFlag)
    {
        this.monitor = monitor;
        this.errorFlag = errorFlag;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        synchronized(monitor)
        {
            monitor.set(true);
            errorFlag.set(true);
            monitor.notifyAll();
        }
    }
    
    // this timer can be used for any TimerTask if you are just looking to share an existing timer
    /**
     * Schedule task.
     *
     * @param task the task
     * @param delay the delay
     */
    public static void scheduleTask(TimerTask task, long delay)
    {
        if(timer == null)
            timer = new Timer();
        timer.schedule(task, delay);
    }
    
    // this timer can be used for any TimerTask that needs fixed repeated delays
    /**
     * Schedule task.
     *
     * @param task the task
     * @param delay the delay
     * @param period the period
     */
    public static void scheduleTask(TimerTask task, long delay, long period)
    {
        if(timer == null)
            timer = new Timer();
        timer.schedule(task, delay, period);
    }
}

