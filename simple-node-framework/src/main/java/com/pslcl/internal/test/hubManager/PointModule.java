package com.pslcl.internal.test.hubManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
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
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface.WaveformData;

@SuppressWarnings("javadoc")
public class PointModule implements Module//, DOFSystem.QueryOperationListener //, DOFDomain.StateListener
{
    public static final String PropertyFileNameBaseKey = "emitdo.dsp.test.point.properties-file";
    public static final String ProviderIdBaseKey = "emitdo.dsp.test.point.provider-id";
    public static final String SystemNameKey = "emitdo.dsp.test.point.system";

    public static final String PropertyFileNameDefault = "pointModule.properties";

    private final Logger log;
    private final String moduleName;
    private final int index;
    private volatile NodeConfig nodeConfig;
    private volatile String fdn;
    private volatile String systemName;
    private final List<DOFObjectID> providerIds;
    private WaveformRequestHandler waveformRequestHandler;
    private StatusTracker statusTracker;

    public PointModule(String moduleName, int index)
    {
        this.moduleName = moduleName;
        this.index = index;
        log = LoggerFactory.getLogger(getClass());
        providerIds = new ArrayList<DOFObjectID>();
    }

    @Override
    public void init(Configuration config) throws Exception
    {
        nodeConfig = (NodeConfig) config;
        nodeConfig.toSb(getClass().getName(), " init:");
        nodeConfig.tabLevel.incrementAndGet();

        List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(PropertyFileNameBaseKey, nodeConfig.properties);
        Entry<String, String> pfentry = list.get(index);

        String value = pfentry.getValue();
        nodeConfig.toSb(pfentry.getKey(), "=", value);
        NodeConfiguration.loadProperties(nodeConfig, value);

        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);

        systemName = nodeConfig.properties.getProperty(SystemNameKey);
        nodeConfig.toSb(SystemNameKey, "=", value);

        list = NodeConfiguration.getPropertiesForBaseKey(ProviderIdBaseKey, nodeConfig.properties);

        //        nodeConfig.properties.remove(PropertyFileNameKey);
        NodeConfiguration.removePropertiesForBaseKey(ProviderIdBaseKey, nodeConfig.properties);
        nodeConfig.properties.remove(SystemNameKey);
        nodeConfig.properties.remove(NodeConfiguration.FdnKey);

        try
        {
            nodeConfig.toSb("providerIds:");
            nodeConfig.tabLevel.incrementAndGet();
            synchronized (providerIds)
            {
                for (Entry<String, String> entry : list)
                {
                    nodeConfig.toSb(entry.getKey(), "=", entry.getValue());
                    providerIds.add(DOFObjectID.create(entry.getValue()));
                }
            }
            nodeConfig.tabLevel.decrementAndGet();
        } catch (Exception e)
        {
            nodeConfig.toSb("invalid providerId: " + value, e);
            throw e;
        }
    }

    @Override
    public void start(Node node) throws Exception
    {
        synchronized (this)
        {
            this.statusTracker = node.getStatusTracker();
            statusTracker.setStatus(moduleName, Status.Warn);
            waveformRequestHandler = new WaveformRequestHandler(this, node, providerIds);
            nodeConfig.executor.execute(waveformRequestHandler);
        }
    }

    @Override
    public void stop(Node node) throws Exception
    {
        synchronized (this)
        {
            if (statusTracker != null)
                statusTracker.setStatus(moduleName, Status.Warn);
            if (waveformRequestHandler != null)
                waveformRequestHandler.stop();
            waveformRequestHandler = null;
        }
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");
        if (statusTracker != null)
            statusTracker.removeStatus(moduleName);
        nodeConfig.toSb(getClass().getSimpleName() + " Destroyed");
    }

    private class WaveformRequestHandler implements Runnable
    {
        private final PointModule parent;
        private final SimpleNodeFramework node;
        private final List<DOFObjectID> providerIds;
        private final AtomicBoolean stopped;
        private final List<DOFOperation> operations;
        private final List<DOFObject> providers;

        WaveformRequestHandler(PointModule parent, Node node, List<DOFObjectID> providerIds)
        {
            this.parent = parent;
            this.node = (SimpleNodeFramework) node;
            this.providerIds = providerIds;
            stopped = new AtomicBoolean(false);
            providerIds = new ArrayList<DOFObjectID>();
            operations = new ArrayList<DOFOperation>();
            providers = new ArrayList<DOFObject>();
        }

        @Override
        public void run()
        {
            WaveformData waveformData = null;
            StringBuilder sb = new StringBuilder("\nStarting points:");
            try
            {
                SystemConfig systemConfig = nodeConfig.getSystemConfig(fdn, systemName);
                DOFSystem system = systemConfig.getSystem();
                int timeout = nodeConfig.getCredConnSysConfig(fdn).commTimeout;
                synchronized (providerIds)
                {
                    for (DOFObjectID oid : providerIds)
                    {
                        sb.append("\nwaiting for provider: " + oid.toStandardString() + ":" + WaveformInterface.IID.toStandardString());
                        operations.add(system.beginInterest(oid, WaveformInterface.IID, DOFInterestLevel.CONNECT, DOF.TIMEOUT_NEVER, null, null));
                        DOFObject provider = system.waitProvider(oid, WaveformInterface.IID, timeout);
                        providers.add(provider);
                        waveformData = WaveformInterface.getWaveformData(provider, timeout);
                        log.info(waveformData.toString());
                    }
                    statusTracker.setStatus(moduleName, Status.Ok);
                    node.setModuleStatus(parent, ModuleStatus.Started);
                    do
                    {
                        for (DOFObject provider : providers)
                            waveformData = WaveformInterface.getWaveformData(provider, timeout);
                        synchronized (stopped)
                        {
                            stopped.wait(1000);
                        }
                    } while (!stopped.get());
                }
            } catch (Exception e)
            {
                log.error(sb.toString(), e);
                node.setModuleStatus(parent, ModuleStatus.Failed);
            }
        }

        public void stop()
        {
            synchronized (providerIds)
            {
                synchronized (stopped)
                {
                    stopped.set(true);
                    stopped.notifyAll();
                    try
                    {
                        stopped.wait(1);
                    } catch (InterruptedException e)
                    {
                    }
                }
                for (DOFOperation op : operations)
                    op.cancel();
                for (DOFObject provider : providers)
                    provider.destroy();
            }
        }
    }
}