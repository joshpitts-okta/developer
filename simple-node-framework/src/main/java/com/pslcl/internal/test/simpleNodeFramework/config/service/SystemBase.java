/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.config.service;

import org.opendof.core.oal.DOF;
import org.pslcl.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public abstract class SystemBase
{
    protected final Logger log;
    protected final String moduleName;
    protected final int serviceIndex;
    protected volatile NodeConfig nodeConfig;
    protected volatile SimpleNodeFramework snf;
    protected volatile DOF dof;
    protected String serviceName;
    
    public SystemBase(String moduleName, int index)
    {
        this.moduleName = moduleName;
        this.serviceIndex = index;
        log = LoggerFactory.getLogger(getClass());
    }

    public void init(NodeConfig config) throws Exception
    {
        this.nodeConfig = config;
    }

    public void setDof(DOF dof)
    {
        this.dof = dof;
    }
    
    public void start(SimpleNodeFramework snf) throws Exception
    {
        this.snf = snf;
    }
    
    public abstract Service<?> getService();
    public abstract void stop() throws Exception;
    public abstract void destroy();
}