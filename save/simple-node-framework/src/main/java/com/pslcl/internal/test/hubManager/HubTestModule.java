package com.pslcl.internal.test.hubManager;

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

@SuppressWarnings("javadoc")
public class HubTestModule implements Module
{
    public static final String PropertyFileNameKey = "emitdo.dsp.test.hubtest.properties-file";

    private final Logger log;
    private final String moduleName;
    private volatile NodeConfig nodeConfig;
    private StatusTracker statusTracker;
    private StartTask hubRequestHandler;

    public HubTestModule()
    {
        this(HubTestModule.class.getSimpleName(), 0);
    }

    public HubTestModule(String moduleName, int index)
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
        nodeConfig.tabLevel.set(3);
        nodeConfig.sb.append("\n");
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
                "hubmanager0", "hubManagerModule0", 
                "hubmanager0", "waveformModule0", 
                "hubmanager1", "hubManagerModule1", 
                "hubmanager1", "waveformModule1", 
                "hubrequestor", "HubRequestingModule", 
                "wfRequestorD1", "wfRequestorD1", 
                "wfRequestorD2", "wfRequestorD2", 
                "wfRequestorD3", "wfRequestorD3", 
            };
        //@formatter:on

        private static final int hubManager0 = 0;
        private static final int waveformProvider0 = 2;
        private static final int hubManager1 = 4;
        private static final int waveformProvider1 = 6;
        private static final int hubRequestor = 8;
        private static final int wfRequestorD1 = 10;
        private static final int wfRequestorD2 = 12;
        private static final int wfRequestorD3 = 14;
        
        private static final String hubManager0Ccsc = "hubmanager0.crtoAsCrtoCrSvr0";
        private static final String hubManager1Ccsc = "hubmanager1.crtoAsSvr1";
        private static final String hubRequestorCcsc = "hubrequestor.hubRequestorD1";
        private static final String wfRequestorD1Ccsc = "wfRequestorD1.wfRequestorD1";
        private static final String wfRequestorD2Ccsc = "wfRequestorD2.wfRequestorD2";
        private static final String wfRequestorD3Ccsc = "wfRequestorD3.wfRequestorD3";

        private final HubTestModule parent;
        private final SimpleNodeFramework node;
        private final NodeConfig nodeConfig;
        private final Logger log;

        private StartTask(HubTestModule parent, Node node, NodeConfig nodeConfig)
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
                nodeConfig.waitForCommsUp();
//                startModule(hubManager1, hubManager1Ccsc);
                startModule(hubManager0, hubManager0Ccsc);
                startModule(waveformProvider0, hubManager0Ccsc);
                startModule(waveformProvider1, hubManager1Ccsc);
                startModule(hubRequestor, hubRequestorCcsc);
                startModule(hubManager1, hubManager1Ccsc);
//                startConnection(wfRequestorD1Ccsc, "pointToCrD1");
//                startConnection(wfRequestorD2Ccsc, "pointToCrD2");
//                startConnection(wfRequestorD3Ccsc, "pointToCrD3");
                startModule(wfRequestorD1, wfRequestorD1Ccsc);
                startModule(wfRequestorD2, wfRequestorD2Ccsc);
                startModule(wfRequestorD3, wfRequestorD3Ccsc);
                //                StrH.ttl(sb, level, "fdn: ", fdn);
                //                ccsc = nodeConfig.getCredConnSysConfig(fdn);
                //                ConnectedEvent connEvent = new ConnectedEvent(ccsc);
                //                //                connEvent.waitFor(ccsc.commTimeout);
                //                SnfEvent startedEvent = new StartedEvent(HubManagerModule.class);
                //                //                startedEvent.waitFor(ccsc.commTimeout);
                //                Thread.sleep(100); // give hubManager time for its startup thread to execute
                //                HubActivateInterestListener listener = new HubActivateInterestListener();
                //                StrH.ttl(sb, level, "groupPairs:");
                //                ++level;
                //                log.debug(sb.toString());
                log.info("Hub Manager Test passed");
                SimpleNodeFramework.getSnf().stop();
                SimpleNodeFramework.getSnf().destroy();
            } catch (Exception e)
            {
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }

        private void startModule(int index, String ccsc)
        {
            String subsystem = modulePairs[index];
            String module = modulePairs[index + 1];
            try
            {
                long t0 = System.currentTimeMillis();
                int to = nodeConfig.getTimeout(ccsc);
                log.info("\nHubTestModule starting subsystem: " + subsystem + " module: " + module);
                nodeConfig.startModule(subsystem, module, false);
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

        @SuppressWarnings("unused")
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