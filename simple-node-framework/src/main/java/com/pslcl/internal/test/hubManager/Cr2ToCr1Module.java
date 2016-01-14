package com.pslcl.internal.test.hubManager;

import java.util.Properties;

import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.ConnectionManagerModule;
import com.pslcl.internal.test.simpleNodeFramework.ConnectionManagerModule.ConnectionConfigWithListeners;
import com.pslcl.internal.test.simpleNodeFramework.NodeRunner;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;

@SuppressWarnings("javadoc")
public class Cr2ToCr1Module implements Module
{
    public static final String PropertyFileNameKey = "emitdo.dsp.test.cr2tocr1.properties-file";
    public static final String ModuleNameKey = "emitdo.dsp.test.cr2tocr1.module-name";
    public static final String CommTimeoutKey = "emitdo.dsp.test.cr2tocr1.comm-timeout";
    
    public static final String PropertyFileNameDefault = "cr2tocr1.properties";
    public static final String ModuleNameDefault = "Cr2ToCr1";
    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    
    private final boolean runMe; 
    private final Logger log;
    private StatusTracker statusTracker;
    private String moduleName;
//    private DOFSystem system;
    private ConnectionConfigWithListeners connectionConfig;
    private int commTimeout;
    private ConnectionManagerModule connectionManager;
    
    public Cr2ToCr1Module()
    {
        log = LoggerFactory.getLogger(getClass());
//        String modules = System.getProperty(NodeRunner.ActiveTestModulesKey, null);
//        if(modules == null || !modules.contains(HubManagerTest.CrTwoLongCl))
//            runMe = false;
//        else
            runMe = true;
        log.info(getClass().getName() + (runMe ? " is selected" : " is not selected") + " to executue");
    }
    
    @Override
    public void init(Configuration config) throws Exception
    {
        if(!runMe)
            return;
        StringBuilder sb = new StringBuilder("\n" + getClass().getName() + " init:\n");
        Properties properties = NodeRunner.loadPropertiesFile(config, PropertyFileNameKey, PropertyFileNameDefault, sb, log);
        moduleName = properties.getProperty(ModuleNameKey, ModuleNameDefault);
        sb.append("\t"+ModuleNameKey+"=" + moduleName + "\n");
        commTimeout = Integer.parseInt(properties.getProperty(CommTimeoutKey, CommTimeoutDefault));
        sb.append("\t"+CommTimeoutKey+"=" + commTimeout + "\n");

        String connFile = properties.getProperty(ConnectionManagerModule.ConnectionConfigFileKey, ConnectionManagerModule.ConnectionConfigFileDefault);
        sb.append("\t" +ConnectionManagerModule. ConnectionConfigFileKey + "=" + connFile + "\n");
        connectionConfig = ConnectionManagerModule.getConnConfigFromProperties(config, connFile, sb, log);
        if(connectionConfig == null)
        {
            log.debug(sb.toString());
            throw new Exception("connection host must be specified");

        }
        log.info(sb.toString());
    }

    @Override
    public void start(Node node) throws Exception
    {
        if(!runMe)
            return;
        statusTracker = node.getStatusTracker();
        statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        new StartTask(node).start();
    }

    @Override
    public void stop(Node node) throws Exception
    {
        if(!runMe)
            return;
        log.debug("\n" + getClass().getName() + " stop");

        synchronized (this)
        {
            if(connectionManager != null)
                connectionManager.releaseSystemForConnection(node.getDOF(), connectionConfig);
//            system = null;
            statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        }
    }

    @Override
    public void destroy()
    {
        if(!runMe)
            return;
        log.debug("\n" + getClass().getName() + " destroy");
        if(statusTracker != null)
            statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
    }
    
    private class StartTask extends Thread
    {
        private final SimpleNodeFramework node;
        
        private StartTask(Node node)
        {
            this.node = (SimpleNodeFramework)node;
        }
        
        @Override
        public void run()
        {
            log.debug("\n" + Cr2ToCr1Module.class.getName() + " start");
            try
            {
                connectionManager = null; //(ConnectionManagerModule)node.getStartedModule(ConnectionManagerModule.class.getName(), commTimeout);
                if(connectionManager == null)
                    throw new Exception(ConnectionManagerModule.class.getName() + " is not on the Node stack or did not start");
//                system = connectionManager.getSystemForConnection(node.getDOF(), connectionConfig, commTimeout);
            } catch (Exception e)
            {
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(Cr2ToCr1Module.this, ModuleStatus.Failed);
            }
        }
    }
}