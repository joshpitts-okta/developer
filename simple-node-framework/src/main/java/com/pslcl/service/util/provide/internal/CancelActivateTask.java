/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOFSystem;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.provide.internal.interest.InterestValue;

public class CancelActivateTask implements Callable<Void>
{
    private final DOFSystem system;
    private final InterestValue interestValue;
    private final ExecutorService executor;
    private final AtomicBoolean onTimer;

    public CancelActivateTask(DOFSystem system, InterestValue interestValue, ExecutorService executor)
    {
        this.interestValue = interestValue;
        this.system = system;
        this.executor = executor;
        onTimer = new AtomicBoolean(true);
    }
    
    @Override
    public Void call() throws Exception
    {
        try
        {
            if(onTimer.get())
            {
                onTimer.set(false);
                executor.submit(this);
                return null;
            }
            LoggerFactory.getLogger(getClass()).trace("provide op canceled");
            interestValue.getProviderInfo().getProvideOperation().cancel();
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error("ProvideFactory threw exception on cancelActivate callback", e);
            throw e;
        }
        return null;
    }
}
