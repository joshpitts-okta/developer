/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.waveform.module;

import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSystem;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.DofSystemConfiguration.SystemConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class WaveformModule implements Module
{
    public static final String PropertyFileNameKey = "emitdo.snf.waveform.properties-file";
    public static final String ServiceIdKey = "emitdo.snf.waveform.serviceId";
    public static final String SystemNameKey = "emitdo.snf.waveform.system";

    public static final String PropertyFileNameDefault = "waveform.properties";

    private final Logger log;
    private final String moduleName;
    private final int index;
    private volatile NodeConfig nodeConfig;
    private volatile String fdn;
    private volatile String[] systems;
    private volatile DOFObjectID[] serviceOids;

    private StatusTracker statusTracker;
    private DOFObject[] waveformObjects;
    private WaveformModuleProvider[] waveformProviders;

    public WaveformModule(String moduleName, int index)
    {
        this.moduleName = moduleName;
        this.index = index;
        log = LoggerFactory.getLogger(getClass());
    }

    public void snfSequencingExample()
    {
        log.info("hello world");
    }

    /* *************************************************************************
     * Module implementation
     **************************************************************************/
    @Override
    public void init(Configuration config) throws Exception
    {
        String msg = "ok";
        try
        {
            nodeConfig = (NodeConfig) config;
            nodeConfig.tabLevel.set(3);
            nodeConfig.sb.append("\n");
            nodeConfig.toSb(getClass().getName(), " init:");
            nodeConfig.tabLevel.incrementAndGet();

            List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(PropertyFileNameKey, nodeConfig.properties);
            Entry<String, String> entry = list.get(index);
            String value = entry.getValue();
            nodeConfig.toSb(entry.getKey(), "=", value);
            NodeConfiguration.loadProperties(nodeConfig, value);

            fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
            nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);

            String systemNames = nodeConfig.properties.getProperty(SystemNameKey);
            nodeConfig.toSb(SystemNameKey, "=", systemNames);
            systems = systemNames.split(" ");

            String services = nodeConfig.properties.getProperty(ServiceIdKey);
            nodeConfig.toSb(ServiceIdKey, "=", services);
            String[] serviceIds = services.split(" ");
            serviceOids = new DOFObjectID[serviceIds.length];
            int i = 0;
            for (String sid : serviceIds)
            {
                msg = "invalid service oid: " + sid;
                serviceOids[i++] = DOFObjectID.create(sid);
            }

            nodeConfig.properties.remove(PropertyFileNameKey);
            nodeConfig.properties.remove(ServiceIdKey);
            nodeConfig.properties.remove(SystemNameKey);
            nodeConfig.properties.remove(NodeConfiguration.FdnKey);
        } catch (Exception e)
        {
            nodeConfig.toSb(msg);
            log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }

    @Override
    public void start(Node node) throws Exception
    {
        synchronized (this)
        {
            statusTracker = node.getStatusTracker();
            statusTracker.setStatus(moduleName, Status.Warn);
            NodeConfig nodeConfig = (NodeConfig) node.getConfig();
            nodeConfig.executor.submit(new WaveformStartTask(node));
        }
    }

    @Override
    public void stop(Node node) throws Exception
    {
        synchronized (this)
        {
            log.debug("\n" + getClass().getName() + " stop");
            if (waveformProviders != null)
            {
                for (int i = 0; i < waveformProviders.length; i++)
                {
                    if (waveformProviders[i] != null)
                        waveformProviders[i].close();
                }
            }
            if (waveformObjects != null)
            {
                for (int i = 0; i < waveformObjects.length; i++)
                {
                    if (waveformObjects[i] != null)
                        waveformObjects[i].destroy();
                }
            }
            if (statusTracker != null)
                statusTracker.setStatus(moduleName, Status.Warn);
        }
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");

        if (statusTracker != null)
            statusTracker.removeStatus(moduleName);
        statusTracker = null;
    }

    private class WaveformStartTask implements Runnable //, InterestOperationListener, ActivateInterestListener
    {
        private final SimpleNodeFramework node;

        //        private volatile CredConnSysConfig ccsc;

        private WaveformStartTask(Node node)
        {
            this.node = (SimpleNodeFramework) node;
        }

        @Override
        public void run()
        {
            StringBuilder sb = new StringBuilder("\nWaveformStartTask run:");
            try
            {
                waveformProviders = new WaveformModuleProvider[systems.length];
                waveformObjects = new DOFObject[systems.length];
                for (int i = 0; i < systems.length; i++)
                {
                    SystemConfig systemConfig = nodeConfig.getSystemConfig(fdn, systems[i]);
                    if (systemConfig == null)
                        log.error("Invalid ccsc fdn: " + fdn + " and/or system name: " + systems[i]);
                    else
                    {
                        DOFSystem system = systemConfig.getSystem();
                        String creds = systemConfig.credentials.toString();
                        waveformProviders[i] = new WaveformModuleProvider(serviceOids[i], creds);
                        waveformObjects[i] = system.createObject(DOFObjectID.create(serviceOids[i]));
                        waveformProviders[i].beginProvider(waveformObjects[i]);
                    }
                }
                statusTracker.setStatus(moduleName, Status.Ok);
                node.setModuleStatus(WaveformModule.this, ModuleStatus.Started);
                log.debug(sb.toString());
            } catch (Exception e)
            {
                log.debug(sb.toString());
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(WaveformModule.this, ModuleStatus.Failed);
            }
        }
    }
}
