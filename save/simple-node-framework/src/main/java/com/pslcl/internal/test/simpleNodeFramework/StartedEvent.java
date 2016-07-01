/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.SubsystemConfiguration.SnfModule;

@SuppressWarnings("javadoc")
public class StartedEvent extends SnfEvent
{
    public StartedEvent(Class<?> clazz)
    {
        super(clazz);
    }
    
    @Override
    public void waitFor(int timeout) throws Exception
    {
        SnfModule module = SimpleNodeFramework.getSnf().getSnfModule(clazz);
        if(module.status == ModuleStatus.Started)
            return;
        super.waitFor(timeout);
    }
}
