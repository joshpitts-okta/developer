/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import java.util.concurrent.atomic.AtomicBoolean;

import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;

@SuppressWarnings("javadoc")
public class ConnectedEvent extends SnfEvent
{
    private final CredConnSysConfig ccsConfig;

    public ConnectedEvent(CredConnSysConfig ccsConfig)
    {
        super(ConnectedEvent.class);
        this.ccsConfig = ccsConfig;
    }

    public void waitFor(String name, int timeout) throws Exception
    {
        if(ccsConfig.isConnected(name))
            return;
        final AtomicBoolean fired = new AtomicBoolean(false);
        SnfEventListener listener = new SnfEventListener()
        {
            @Override
            public void fired(SnfEvent event)
            {
                if (!(event instanceof ConnectedEvent))
                    return;
                fired.set(true);
                SimpleNodeFramework.getSnf().removeListener(this);
            }
        };
        SimpleNodeFramework.getSnf().addListener(listener, getClass());
        long t0 = System.currentTimeMillis();
        do
        {
            synchronized (this)
            {
                try
                {
                    wait(100);
                } catch (InterruptedException e)
                {
                }
                if (fired.get())
                    return;
                if (System.currentTimeMillis() - t0 >= timeout)
                    throw new Exception(clazz.getName() + " failed to start within: " + timeout + " ms");
            }
        } while (true);
    }
}
