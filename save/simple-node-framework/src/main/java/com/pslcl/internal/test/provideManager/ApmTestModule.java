package com.pslcl.internal.test.provideManager;

import org.pslcl.service.Service;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.SystemBase;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.app.WaveformService;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.system.WaveformServiceConfigImpl;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.system.WaveformSystem;

@SuppressWarnings("javadoc")
public class ApmTestModule implements Module
{
    public static final String PropertyFileNameKey = "emitdo.apm.test.properties-file";

    private final Logger log;
    private final String moduleName;
    private volatile NodeConfig nodeConfig;
    private StatusTracker statusTracker;
    private StartTask hubRequestHandler;

    public ApmTestModule()
    {
        this(ApmTestModule.class.getSimpleName(), 0);
    }

    public ApmTestModule(String moduleName, int index)
    {
        this.moduleName = moduleName;
        log = LoggerFactory.getLogger(getClass());
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

        //        String value = nodeConfig.properties.getProperty(PropertyFileNameKey, PropertyFileNameDefault);
        //        nodeConfig.toSb(PropertyFileNameKey, "=", value);
        //        NodeConfiguration.loadProperties(nodeConfig, value);

        //        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        //        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);

        nodeConfig.properties.remove(PropertyFileNameKey);
        //        nodeConfig.properties.remove(NodeConfiguration.FdnKey);
    }

    @Override
    public void start(Node node) throws Exception
    {
        synchronized (this)
        {
            statusTracker = node.getStatusTracker();
            statusTracker.setStatus(moduleName, Status.Warn);
            hubRequestHandler = new StartTask(this, node, nodeConfig);
            new Thread(hubRequestHandler).start();
        }
        nodeConfig.toSb(getClass().getSimpleName() + " Start task started");
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug("\n" + getClass().getName() + " stop");
        synchronized (this)
        {
            if (hubRequestHandler != null)
                hubRequestHandler.close();
            hubRequestHandler = null;
            if (statusTracker != null)
                statusTracker.setStatus(moduleName, Status.Warn);
        }
        nodeConfig.toSb(getClass().getSimpleName() + " Stopped");
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");
        synchronized (this)
        {
            if (statusTracker != null)
                statusTracker.removeStatus(moduleName);
            if (hubRequestHandler != null)
                hubRequestHandler.close();
            hubRequestHandler = null;
        }
        nodeConfig.toSb(getClass().getSimpleName() + " Destroyed");
    }

    private static class StartTask implements Runnable
    {
        /*
         * dnbffbff    core/super user
         * dnManager   all oid/iid in the domain
         * dnProvider  HubManager/HubRequestingModule IID permissioned
         * dnRequestor hubdomain permissioned qa thru-put and waveform
         * dnService   hubdomain permissioned qa thru-put and waveform
         */
        
        //@formatter:off
        private static final String[] modulePairs = 
            new String[] 
            { 
                "apm0", "waveformService0",
                "apm1", "waveformService1",
                "apm2", "waveformService2",
                "apmRequestorD1", "WfRequestorModule0", 
                "apmRequestorD1", "WfRequestorModule1", 
                "apmRequestorD1", "WfRequestorModule2", 
            };
        //@formatter:on

        private static final int wfServiceNode0 = 0;
        private static final int wfServiceNode1 = 2;
        private static final int wfServiceNode2 = 4;
        private static final int wfRequestorDomain10 = 6;
        private static final int wfRequestorDomain11 = 8;
        private static final int wfRequestorDomain12 = 10;
        
        private static final String wfService0 = "apm0.coretocr0";
        private static final String wfService1 = "apm1.coretocr1";
        private static final String wfService2 = "apm2.coretocr2";
        private static final String wfRequestorD1 = "apmRequestorD1.wfRequestorD1";

        private final ApmTestModule parent;
        private final SimpleNodeFramework node;
        private final NodeConfig nodeConfig;
        private final Logger log;

        private StartTask(ApmTestModule parent, Node node, NodeConfig nodeConfig)
        {
            this.parent = parent;
            this.node = (SimpleNodeFramework) node;
            this.nodeConfig = nodeConfig;
            log = LoggerFactory.getLogger(getClass());
        }

        @Override
        public void run()
        {
            node.setModuleStatus(parent, ModuleStatus.Started);
            try
            {
//                startConnection(wfRequestorD1, "streamToCr0"); if initial-start == false in connection configuration
                nodeConfig.waitForCommsUp();
                
                startModule(wfServiceNode0, wfService0, true);
                startModule(wfServiceNode1, wfService1, true);
                startModule(wfServiceNode2, wfService2, true);
//                Thread.sleep(1000); // give the dht ring time to establish before introducing any reactive provide requests
                startModule(wfRequestorDomain10, wfRequestorD1, false); // will fire up the three[3:waveformD1p0/1/2@internal.service.opendof.org]
                startModule(wfRequestorDomain11, wfRequestorD1, false);
                startModule(wfRequestorDomain12, wfRequestorD1, false);
                
                stopModule(wfRequestorDomain10, wfRequestorD1, false);
                stopModule(wfRequestorDomain11, wfRequestorD1, false);
                stopModule(wfRequestorDomain12, wfRequestorD1, false);
                
                SystemBase systemBase = nodeConfig.getSystemBase("apm0", "waveformService0");
                WaveformSystem wfsystem = (WaveformSystem)systemBase;
                Service<WaveformServiceConfigImpl> service = wfsystem.getService();
                WaveformService wfservice = (WaveformService)service;
//                WaveformServiceProvider provider = wfservice.getProvider();
                
                systemBase = nodeConfig.getSystemBase("apm1", "waveformService1");
                systemBase = nodeConfig.getSystemBase("apm2", "waveformService2");

                log.info("Global Hash Test passed");
                SimpleNodeFramework.getSnf().stop();
                SimpleNodeFramework.getSnf().destroy();
            } catch (Exception e)
            {
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }

        private void startModule(int index, String ccsc, boolean service)
        {
            String subsystem = modulePairs[index];
            String module = modulePairs[index + 1];
            try
            {
                long t0 = System.currentTimeMillis();
                int to = nodeConfig.getTimeout(ccsc);
                log.info("\nHubTestModule starting subsystem: " + subsystem + " module: " + module);
                nodeConfig.startModule(subsystem, module, service);
                do
                {
                    if (nodeConfig.statusTracker.getStatus(module) == Status.Ok)
                        return;
                    synchronized(this)
                    {
                        wait(50);
                    }
                    if(System.currentTimeMillis() - t0 >= to)
                        throw new Exception("start on module: " + module + " did not set status ok within " + to +"ms");
                }while(true);
            } catch (Exception e)
            {
                log.error("Failed to start subsystem: " + subsystem + " module: " + module, e);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }

        private void stopModule(int index, String ccsc, boolean service)
        {
            String subsystem = modulePairs[index];
            String module = modulePairs[index + 1];
            try
            {
                long t0 = System.currentTimeMillis();
                int to = nodeConfig.getTimeout(ccsc);
                log.info("\nHubTestModule stopping subsystem: " + subsystem + " module: " + module);
                nodeConfig.stopModule(subsystem, module, service);
                do
                {
                    if (nodeConfig.statusTracker.getStatus(module) == Status.Warn)
                        return;
                    synchronized(this)
                    {
                        wait(50);
                    }
                    if(System.currentTimeMillis() - t0 >= to)
                        throw new Exception("stop on module: " + module + " did not set status warn within " + to +"ms");
                }while(true);
            } catch (Exception e)
            {
                log.error("Failed to stop subsystem: " + subsystem + " module: " + module, e);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }
        
        // try starting out with a method per TS use case
        private void multipleProvideManagersTest()
        {
        }
        
        private void singleProvideFactoryRegisteredWithMultipleProvideManagers()
        {
        }
        
        private void multipleUniqueProvideFactoryOverlappingIids()
        {
        }
        
        private void removeAddProvideFactoryInRunningSystem()
        {
        }
        
        private void reactiveProvidesInMulitpleDomains()
        {
        }
        
        private void checkGracePeriod()
        {
        }
        
        private void checkOustandingActivateRequestsHandler()
        {
            
        }
        
        private void startConnection(String fdn, String connName)
        {
            try
            {
                long t0 = System.currentTimeMillis();
                int to = nodeConfig.getTimeout(fdn);
                log.info("\nHubTestModule starting connection: " + fdn + " connection: " + connName);
                nodeConfig.startConnection(fdn, connName);
                do
                {
                    if (nodeConfig.statusTracker.getStatus(connName) == Status.Ok)
                        return;
                    synchronized(this)
                    {
                        wait(50);
                    }
                    if(System.currentTimeMillis() - t0 >= to)
                        throw new Exception("start on connection: " + connName + " did not set status ok within " + to +"ms");
                }while(true);
            } catch (Exception e)
            {
                log.error("Failed to start connection: " + fdn + " connection: " + connName);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }

        public void close()
        {
        }
    }
}