/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import org.opendof.core.oal.DOF;
import org.pslcl.service.status.StatusTracker;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class WrapperNode extends Node
{
    private final String snfDofFdn;
    private final SimpleNodeFramework snf;
    
    public WrapperNode(String fdn) throws Exception
    {
        snf = SimpleNodeFramework.getSnf();
        snfDofFdn = fdn;
    }
    
    
    @Override
    public short getStatus()
    {
        return snf.getStatus();
    }

    @Override
    public float getLoad()
    {
        return snf.getLoad();
    }

    @Override
    public DOF getDOF()
    {
        return ((NodeConfig)snf.getConfig()).getDof(snfDofFdn);
    }

    @Override
    public Configuration getConfig()
    {
        return snf.getConfig();
    }

    @Override
    public StatusTracker getStatusTracker()
    {
        return snf.getStatusTracker();
    }
}
