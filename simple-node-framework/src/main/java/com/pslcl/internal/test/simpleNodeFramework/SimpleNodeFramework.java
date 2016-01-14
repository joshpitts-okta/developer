/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.opendof.core.oal.DOF;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.ClassInfo;
import org.pslcl.service.PropertiesFile;
import org.pslcl.service.status.StatusTracker;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.ConnectionAuditor;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.dsp.PlatformConfiguration;
import com.pslcl.dsp.SystemConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.SubsystemConfiguration.SnfModule;

@SuppressWarnings("javadoc")
public class SimpleNodeFramework extends Node
{
    public static final String CoreDofKey = "emitdo.snf.dof-Fdn";
    public static final String PropertyPathKey = "emitdo.snf.properties-path";
    public static final String PlatformConfigKey = "emitdo.snf.dsp.platform-config";
    public static final String DspSystemConfigKey = "emitdo.snf.dsp.system-config";
    public static final String BaseSecurityPathKey = "emitdo.snf.base-security-path";
    public static final String BaseConfigPathKey = "emitdo.snf.base-config-path";
    public static final String RoutingKey = "emitdo.snf.routing";
    public static final String NodeIdKey = "emitdo.snf.node-id";
    public static final String SystemIdKey = "emitdo.snf.system-id";
    public static final String ConnectionsThresholdKey = "emitdo.snf.connections-threshold";
    public static final String ThreadpoolThresholdKey = "emitdo.snf.threadpools-threshold";
    public static final String CrServerPortKey = "emitdo.snf.cr.server-port";
    public static final String CrServerCredentialsKey = "emitdo.snf.cr.server-credentials";
    public static final String AsServerPortKey = "emitdo.snf.as.server-port";

    public static String baseConfigurationPathDefault = "/ws/test/config/apps";
    public static String baseSecureConfigurationPathDefault = "/ws/test/config/secure";
    public static final String propertyFilenameDefault = "snf.properties";
    public static final String propertyPathDefault = baseConfigurationPathDefault + "/" + propertyFilenameDefault;

    //    private static final int DEFAULT_CONNECTION_AUDIT_PERIOD = 1000 * 60 * 10; // 10 minutes, from the DSP technical specification eng-dsp_ts.xml

    //    public static final String SystemConfigBaseKey = "simpleNodeFramework.node.system-config";
    //    public static final String ModuleBaseKey = "simpleNodeFramework.node.module";
    //    public static final String ServiceBaseKey = "simpleNodeFramework.node.service";
    //    public static final String CredentialsKey = "simpleNodeFramework.node.credentials";
    //    public static final String DofConfigBaseKey = "simpleNodeFramework.dof.config-file";
    //    public static final String NodeDofConfigKey = "simpleNodeFramework.node.dof-config";
    //
    //
    //    public static String baseConfigurationPathDefault = "/etc/opt/enc/dsp";

    public static final String PlatformConfigDefault = "org.emitdo.internal.test.simpleNodeFramework.FileConfiguration";
    public static final String DspSystemConfigDefault = "org.emitdo.internal.test.simpleNodeFramework.FileConfiguration";
    //    public static final String RoutingDefault = "org.emitdo.internal.test.simpleNodeFramework.RandomIDResolver";

    private static SimpleNodeFramework snf;
    
    private final List<SnfEventListener> eventListeners;
    private final Hashtable<String, List<SnfEvent>> firedEvents; // <Event type class name, list of fired events for that event type>
    
    private final List<SnfModule> snfModules;
    private volatile NodeConfig nodeConfig;
    private volatile boolean isRouterNode = false; // default is no
    private volatile DOF dof = null;
    private volatile String dofFdn;
    
    //    private volatile ConnectionAuditor connectionAuditor;
    //    private final List<SystemBase> services;

    private SimpleNodeFramework() throws Exception
    {
        super();
        eventListeners = new ArrayList<SnfEventListener>();
        snfModules = new ArrayList<SnfModule>();
        firedEvents = new Hashtable<String, List<SnfEvent>>();

        StringBuilder sb = new StringBuilder("\nSimpleNodeFramework constructor\n");
        sb.append("\t" + ClassInfo.getInfo(System.class).toString() + "\n");
        sb.append("\t" + ClassInfo.getInfo(this.getClass()).toString() + "\n");
        sb.append("\t" + ClassInfo.getInfo(DOF.class).toString() + "\n");
        sb.append("\t" + ClassInfo.getInfo(InetTransport.class).toString() + "\n");
        //        services = new ArrayList<SystemBase>();
        //        modules = new ArrayList<SnfModule>();
        logger.debug(sb.toString());
    }

    
    public static SimpleNodeFramework getSimpleNodeFramework() throws Exception
    {
        if(snf == null)
            snf = new SimpleNodeFramework(); 
        return snf;
    }
    
    public static SimpleNodeFramework getSnf()
    {
        return snf;
    }
    
    public void addListener(SnfEventListener listener, Class<?> eventType)
    {
        synchronized (eventListeners)
        {
            List<SnfEvent> list = firedEvents.get(eventType.getName());
            if(list == null)
            {
                list = new ArrayList<SnfEvent>();
                firedEvents.put(eventType.getName(), list);
            }
            eventListeners.add(listener);
            for(SnfEvent event : list)
            {
                for (int i=eventListeners.size()-1; i >= 0; i--)
                    eventListeners.get(i).fired(event);
            }
        }
    }

    public void removeListener(SnfEventListener listener)
    {
        synchronized (eventListeners)
        {
            eventListeners.remove(listener);
        }
    }
    
    public void fireEvent(SnfEvent event)
    {
        synchronized (eventListeners)
        {
            
            List<SnfEvent> list = firedEvents.get(event.getClass().getName());
            if(list == null)
            {
                list = new ArrayList<SnfEvent>();
                firedEvents.put(event.getClass().getName(), list);
            }
            boolean found = false;
            for(SnfEvent evt : list)
            {
                if(evt.equals(event))
                {
                    found = true;
                    break;
                }
            }
            if(!found)
                list.add(event);
            
            for (int i=eventListeners.size()-1; i >= 0; i--)
                eventListeners.get(i).fired(event);
        }
    }

    public void addSnfModule(SnfModule module)
    {
        synchronized (snfModules)
        {
            snfModules.add(module);
        }
    }
    
    public SnfModule getSnfModule(Module module)
    {
        synchronized (snfModules)
        {
            for(SnfModule snfModule : snfModules)
            {
                if(snfModule.module.getClass().getName().equals(module.getClass().getName()))
                    return snfModule;
            }
        }
        return null;
    }
    
    public SnfModule getSnfModule(Class<?> clazz)
    {
        synchronized (snfModules)
        {
            for(SnfModule snfModule : snfModules)
            {
                if(snfModule.module.getClass().equals(clazz))
                    return snfModule;
            }
        }
        return null;
    }
    
    @Override
    public void init(DaemonContext context) throws DaemonInitException, Exception
    {
        String msg = null;
        StringBuilder sb = new StringBuilder("\nSimpleNodeFramework init:\n");
        try
        {
            String value = System.getProperty(PropertyPathKey, propertyPathDefault);
            sb.append("\tconfiguration file: " + value + "\n");
            Properties properties = new Properties();
            PropertiesFile.load(properties, value);

            msg = "invalid PlatformConfiguration class name";
            value = properties.getProperty(PlatformConfigKey, PlatformConfigDefault);
            sb.append("\t" + PlatformConfigKey + "=" + value + "\n");
            PlatformConfiguration platformConfig = (PlatformConfiguration) Class.forName(value).newInstance();
            platformConfig.init();

            msg = "invalid SystemConfiguration class name";
            value = properties.getProperty(DspSystemConfigKey, DspSystemConfigDefault);
            sb.append("\t" + DspSystemConfigKey + "=" + value + "\n");
            SystemConfiguration systemConfig = (SystemConfiguration) Class.forName(value).newInstance();
            systemConfig.init(platformConfig);

            dofFdn = properties.getProperty(CoreDofKey);
            sb.append("\t" + CoreDofKey + "=" + dofFdn + "\n");

            properties.remove(PropertyPathKey);
            properties.remove(PlatformConfigKey);
            properties.remove(DspSystemConfigKey);
            properties.remove(BaseSecurityPathKey);
            properties.remove(BaseConfigPathKey);
            properties.remove(RoutingKey);
            properties.remove(NodeIdKey);
            properties.remove(SystemIdKey);
            properties.remove(ConnectionsThresholdKey);
            properties.remove(ThreadpoolThresholdKey);
            properties.remove(CrServerPortKey);
            properties.remove(CrServerCredentialsKey);
            properties.remove(AsServerPortKey);
            properties.remove(CoreDofKey);

            Configuration config = new Configuration(platformConfig, systemConfig);
            nodeConfig = new NodeConfig(this, config, properties, sb);
            nodeConfig.tabLevel.set(1);
            nodeConfig.init();
            nodeConfig.tabLevel.decrementAndGet();

            // Setup JMX monitoring capability.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName("org.emitdo.dsp:type=Node"));

        } catch (Exception e)
        {
            sb.append(msg);
            logger.error(sb.toString(), e);
            failed();
            throw e;
        }
        logger.info(sb.toString());
    }

    /**
     * Start the service.
     * Create the connection to the cloud and attach the ReconnectingStateListener to ensure the connection will reconnect when needed.
     */
    @Override
    public void start() throws Exception
    {
        logger.info("Starting DSP Node.");
        nodeConfig.start();
        logger.info("DSP Node Running" + (isRouterNode ? ", DOF routes" : ""));
    }

    //FIXME: cleanup SystemManager and services
    @Override
    public void stop() throws Exception
    {
        logger.info("Stopping DSP Node.");
        nodeConfig.stop();
    }

    @Override
    public void destroy()
    {
        nodeConfig.destroy();
        logger.info("DSP Node Terminated.");
    }

    @Override
    public short getStatus()
    {
        return (short)statusTracker.getStatus().ordinal();
    }

    @Override
    public float getLoad()
    {
        float load = 0.0f;
        synchronized (this)
        {
            if (dof != null)
            {
                // TODO Is there a better measurement of load than this?
                double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                double memLoad = (double) memUse.getUsed() / memUse.getMax();
                if (memLoad > cpuLoad)
                    return (float) memLoad;
                return (float) cpuLoad;
            }
        }
        return load;
    }

    @Override
    public DOF getDOF()
    {
        throw new RuntimeException("Use the ModuleWrapper class for native Modules");
    }

    @Override
    public Configuration getConfig()
    {
        return nodeConfig;
    }

    @Override
    public StatusTracker getStatusTracker()
    {
        return statusTracker;
    }

    @Override
    public ConnectionAuditor getConnectionAuditor()
    {
        return null;
//        return nodeConfig.connectionAuditor;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        String msg = "FATAL ERROR: Uncaught exception in thread " + thread;
        logger.error(msg, ex);
        // Since the state is unknown at this point, we may not be able to perform a graceful exit.
        System.exit(1); // forces termination of all threads in the JVM
    }

    public void waitForStatus(Module module, ModuleStatus status, int timeout) throws Exception
    {
        nodeConfig.waitForStatus(module, status, timeout);
    }

    public void setModuleStatus(Module module, ModuleStatus status)
    {
        nodeConfig.setModuleStatus(module, status);
    }

    //    public void startFatal(Module module, Exception exception)
    //    {
    //        logger.error(module.getClass().getName() + " failed to start", exception);
    //        try
    //        {
    //            stop();
    //        } catch (Exception e)
    //        {
    //            logger.error("Node.startFatal call to stop failed (calling destroy anyway)", e);
    //        }
    //        destroy();
    //    }

    public void failed()
    {
        try
        {
            stop();
        } catch (Exception e)
        {
            logger.error("failed to stop cleanly", e);
        }
        try
        {
            destroy();
        } catch (Exception e)
        {
            logger.error("failed to destroy cleanly", e);
        }
        System.exit(1);
    }

    //    private class CheckStateTask extends TimerTask
    //    {
    //        private ModuleStatus expectedStatus;
    //
    //        private CheckStateTask(ModuleStatus status)
    //        {
    //            expectedStatus = status;
    //        }
    //
    //        @Override
    //        public void run()
    //        {
    //            for (SnfModule module : modules)
    //            {
    //                if (module.status != expectedStatus)
    //                {
    //                    logger.error(module.module.getClass().getSimpleName() + " should be at state: " + expectedStatus + " but is at: " + module.status);
    //                    failed();
    //                }
    //            }
    //        }
    //    }

    public enum ModuleStatus
    {
        Initing, Starting, Started, Stopping, Stopped, Destroying, Destroyed, Failed
    }

}
