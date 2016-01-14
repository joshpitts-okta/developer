package com.pslcl.internal.test.simpleNodeFramework;

import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ModuleWrapper implements Module
{
    public static final String PropertyFileNameBaseKey = "emitdo.snf.wrapper.properties-file";
    public static final String ModulePropertyFileKeyKey = "emitdo.snf.wrapper.properties-file-key";
    public static final String WrappedModuleNameKey = "emitdo.snf.wrapper.module";
    public static final String FdnKey = "emitdo.snf.wrapper.fdn";

    private final Logger log;
    private volatile NodeConfig nodeConfig;
    private final String moduleName;
    private volatile String fdn;
    private volatile Module module;
    private final int index;
    private volatile WrapperNode wrapperNode;

    public ModuleWrapper(String moduleName, int index)
    {
        this.moduleName = moduleName;
        log = LoggerFactory.getLogger(getClass());
        this.index = index;
    }

    
    public Module getWrappedModule()
    {
        return module;
    }
    
    /* *************************************************************************
     * Module interface implementation 
     **************************************************************************/

    @Override
    public void init(Configuration config) throws Exception
    {
        nodeConfig = (NodeConfig) config;
        nodeConfig.toSb(getClass().getName(), " init:");
        nodeConfig.tabLevel.incrementAndGet();
        
        List<Entry<String,String>> list = NodeConfiguration.getPropertiesForBaseKey(PropertyFileNameBaseKey, nodeConfig.properties);
        Entry<String, String> entry = list.get(index);
        
        String value = entry.getValue();
        nodeConfig.toSb(entry.getKey(), "=", value);
        NodeConfiguration.loadProperties(nodeConfig, value);
        
        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);
        wrapperNode = new WrapperNode(fdn);
        
        String wrappedModule = nodeConfig.properties.getProperty(WrappedModuleNameKey);
        nodeConfig.toSb(WrappedModuleNameKey, "=", wrappedModule);
        
        String modulePropertyFileKey = nodeConfig.properties.getProperty(ModulePropertyFileKeyKey);
        nodeConfig.toSb(ModulePropertyFileKeyKey, "=", modulePropertyFileKey);
        if(modulePropertyFileKey != null)
        {
            value = nodeConfig.properties.getProperty(modulePropertyFileKey);
            System.setProperty(modulePropertyFileKey, value);
        }
        
        module = (Module) Class.forName(wrappedModule).newInstance();
        
        //TODO: the following is now base and never cleaned up
//        nodeConfig.properties.remove(PropertyFileNameBaseKey);
        nodeConfig.properties.remove(ModulePropertyFileKeyKey);
        nodeConfig.properties.remove(WrappedModuleNameKey);
        nodeConfig.properties.remove(FdnKey);
        
        try
        {
            module.init(config);
        }catch(Exception e)
        {
            nodeConfig.toSb(moduleName + ".init failed", e);
            throw e;
        }
    }

    @Override
    public void start(Node node) throws Exception
    {
        module.start(wrapperNode);
        nodeConfig.toSb(module.getClass().getName(), " returned from its start");
    }

    @Override
    public void stop(Node node) throws Exception
    {
        nodeConfig.toSb("stopping: " + module.getClass().getName());
        try
        {
            module.stop(wrapperNode);
        }catch(Exception e)
        {
            log.error(moduleName + ".stop failed", e);
        }
        nodeConfig.toSb(module.getClass().getName(), " returned from its stop");
    }

    @Override
    public void destroy()
    {
        nodeConfig.toSb("destroying: " + module.getClass().getName());
        try
        {
            module.destroy();
        }catch(Exception e)
        {
            log.error(moduleName + ".destroy failed", e);
        }
        nodeConfig.toSb(module.getClass().getName(), " returned from its destroy");
    }
}