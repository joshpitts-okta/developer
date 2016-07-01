/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("javadoc")
public class SnfEvent
{
    protected final String name;
    protected final Class<?> clazz;
    
    public SnfEvent(Class<?> clazz)
    {
        this.clazz = clazz;
        name = clazz.getName();
    }
    
    public boolean isClass(Class<?> clazz)
    {
        return clazz.equals(this.clazz);
    }
    
    public boolean isClass(String className)
    {
        return clazz.getName().equals(className);
    }
    
    public void waitFor(int timeout) throws Exception
    {
        waitFor(getClass().getName(), clazz, timeout);
    }
    
    private void waitFor(final String eventClassName, final Class<?> clazz, int timeout) throws Exception
    {
        final AtomicBoolean fired = new AtomicBoolean(false);
        SnfEventListener listener = new SnfEventListener()
        {
            @Override
            public void fired(SnfEvent event)
            {
                if(!eventClassName.equals(event.getClass().getName()))
                    return;
                if(!event.isClass(clazz))
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
                if(fired.get())
                    return;
                if(System.currentTimeMillis() - t0 >= timeout)
                    throw new Exception(clazz.getName() + " failed to start within: " + timeout + " ms");
            }
        }while(true);
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SnfEvent))
            return false;
        SnfEvent other = (SnfEvent) obj;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
