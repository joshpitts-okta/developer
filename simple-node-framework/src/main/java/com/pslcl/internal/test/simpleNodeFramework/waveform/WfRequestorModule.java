package com.pslcl.internal.test.simpleNodeFramework.waveform;

import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFSubscription;
import org.opendof.core.oal.DOFSubscription.State;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFSystem.InterestOperationListener;
import org.opendof.core.oal.DOFValue;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface.WaveformData;

@SuppressWarnings("javadoc")
public class WfRequestorModule implements Module
{
    public static final String PropertyFileNameKey = "emitdo.snf.waveform.requestor.properties-file";
    public static final String PropertyFileNameDefault = "waveformRequestor.properties";
    public static final String WfRequestorProvideOidKey="org.snf.waveform.requestor.provide-oid";
    
    public static final String WfRequestorProvideOidDefault="[3:waveformD1p0@internal.service.opendof.org]";
    
    private final Logger log;
    private final int index;
    private final String moduleName;
    // guarded by this
    private WfRequestHandler wfRequestHandler;
    private StatusTracker statusTracker;
    
    // set only single called init method
    private volatile Hashtable<DOFObjectID, ProvideInfo> provideInfos;
    private volatile NodeConfig nodeConfig;
    private volatile String subSystemName;
    
    public WfRequestorModule()
    {
        this(WfRequestorModule.class.getSimpleName(), 0);
    }
    
    public WfRequestorModule(String moduleName, int index)
    {
        this.moduleName = moduleName;
        log = LoggerFactory.getLogger(getClass());
        this.index = index;
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
        
        List<Entry<String, String>> configList = NodeConfiguration.getPropertiesForBaseKey(PropertyFileNameKey, nodeConfig.properties);
        Entry<String, String> configPair = configList.get(index);
        String value = nodeConfig.properties.getProperty(configPair.getKey(), PropertyFileNameDefault);
        nodeConfig.toSb(PropertyFileNameKey, "=", value);
        NodeConfiguration.loadProperties(nodeConfig, value);

        String fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);
        subSystemName = StrH.getAtomicName(fdn, '.');
        String pen = StrH.getPenultimateName(fdn, '.');
        if(pen != null)
            subSystemName = pen;

        provideInfos = new Hashtable<DOFObjectID, ProvideInfo>();
        configList = NodeConfiguration.getPropertiesForBaseKey(WfRequestorProvideOidKey, nodeConfig.properties);
        for(Entry<String, String> entry : configList)
        {
            DOFObjectID oid = DOFObjectID.create(entry.getValue());
            provideInfos.put(oid, new ProvideInfo(oid));
        }
        if(provideInfos.size() == 0)
        {
            DOFObjectID oid = DOFObjectID.create(WfRequestorProvideOidDefault);
            provideInfos.put(oid, new ProvideInfo(oid));
        }
        
        if(configList.size() == 0)
            
        nodeConfig.tabLevel.decrementAndGet();
//        nodeConfig.properties.remove(configPair.getKey());
    }

    @Override
    public void start(Node node) throws Exception
    {
        synchronized (this)
        {
            statusTracker = node.getStatusTracker();
            statusTracker.setStatus(moduleName, Status.Warn);
            wfRequestHandler = new WfRequestHandler(node);
            new Thread(wfRequestHandler).start();
        }
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug("\n" + getClass().getName() + " stop");

        synchronized (this)
        {
            if(wfRequestHandler != null)
                wfRequestHandler.close();
            wfRequestHandler = null;
            if(statusTracker != null)
                statusTracker.setStatus(moduleName, Status.Warn);
            statusTracker = null;
        }
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");
        if (statusTracker != null)
            statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
        if (wfRequestHandler != null)
            wfRequestHandler.close();
        wfRequestHandler = null;
    }

    private class WfRequestHandler implements Runnable, InterestOperationListener, DOFSubscription.Listener
    {
        final AtomicBoolean goingDown;
        private final SimpleNodeFramework node;

        private WfRequestHandler(Node node)
        {
            this.node = (SimpleNodeFramework) node;
            goingDown = new AtomicBoolean(false);
        }

        @Override
        public void run()
        {
            StringBuilder sb = new StringBuilder("\nWfRequestHandler" + index + "  running\n");
            int level = 1;
            try
            {
                CredConnSysConfig ccs = nodeConfig.getCredConnSysConfig("apmRequestorD1.wfRequestorD1");
                DOFSystem system = ccs.getSystemConfig("d1requestor").getSystem();
                StrH.ttl(sb, level, "Activate Interests:");
                ++level;
                synchronized (this)
                {
                    for(Entry<DOFObjectID, ProvideInfo> entry: provideInfos.entrySet())
                    {
                        ProvideInfo pi = entry.getValue();
                        StrH.ttl(sb, level, pi.oid.toStandardString());
                        pi.setInterestOp(system.beginInterest(pi.oid, WaveformInterface.IID, DOFInterestLevel.ACTIVATE, DOF.TIMEOUT_NEVER, this, pi));
                        pi.setProvider(system.waitProvider(pi.oid, WaveformInterface.IID, ccs.commTimeout));
                        pi.getWaveformData(ccs.commTimeout);
                        system.createSubscription(pi.oid, WaveformInterface.getValueProperty(), 0, this);
                        CheckSubscribeTask checkSubscribeTask = new CheckSubscribeTask(pi, goingDown);
                        nodeConfig.timer.schedule(checkSubscribeTask, 1000, TimeUnit.MILLISECONDS);
                    }
                }
                --level;
                StrH.ttl(sb, level, "all activate provides obtained");
                log.info(sb.toString());
                statusTracker.setStatus(moduleName, Status.Ok);
                node.setModuleStatus(WfRequestorModule.this, ModuleStatus.Started);
                synchronized(goingDown)
                {
                    goingDown.wait();
                    log.info("WfRequestHandler" + index + "  shutdown");
                }
                statusTracker.setStatus(moduleName, Status.Warn);
                node.setModuleStatus(WfRequestorModule.this, ModuleStatus.Stopped);
            } catch (Exception e)
            {
                log.debug(sb.toString());
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(WfRequestorModule.this, ModuleStatus.Failed);
                close();
            }
        }

        public void close()
        {
            for(Entry<DOFObjectID, ProvideInfo> entry: provideInfos.entrySet())
                entry.getValue().close();
            
            synchronized (goingDown)
            {
                goingDown.set(true);
                goingDown.notifyAll();
            }
        }

        @Override
        public void complete(DOFOperation operation, DOFException exception)
        {
            if(exception != null)
                log.error("Interest operation failed: ", exception);
        }

        @Override
        public void propertyChanged(DOFSubscription subscription, DOFObjectID providerId, DOFValue value)
        {
            ProvideInfo pi = provideInfos.get(providerId);
            if(pi == null)
            {
                log.error("unexpected providerID on subscribe callback: " + providerId.toStandardString());
                return;
            }
            pi.setCurrentValue(value.toString());
        }

        @Override
        public void stateChanged(DOFSubscription subscription, State state)
        {
            log.info(moduleName + ": stateChanged: " + state.toString());
        }

        @Override
        public void removed(DOFSubscription subscription, DOFException exception)
        {
            if(exception != null)
                log.error("subscription removed with failure: ", exception);
            else
                log.info("subscription removed");
        }
    }
    
    private class ProvideInfo
    {
        public final DOFObjectID oid;
        public final AtomicInteger subscribeCallbacks;
        // guarded by this
        private DOFOperation.Interest interestOp;
        private DOFObject provider;
        private WaveformData waveformData;
        private String currentValue;
        
        private ProvideInfo(DOFObjectID oid)
        {
            this.oid = oid;
            subscribeCallbacks = new AtomicInteger(0);
        }
        
        private synchronized void setInterestOp(DOFOperation.Interest interestOp)
        {
            this.interestOp = interestOp;
        }
        
        private synchronized void setProvider(DOFObject provider)
        {
            this.provider = provider;
        }

        private void getWaveformData(int timeout) throws DOFException
        {
            waveformData = WaveformInterface.getWaveformData(provider, timeout);
            log.info(waveformData.toString());
        }
        
        private void setCurrentValue(String value)
        {
            subscribeCallbacks.incrementAndGet();
            currentValue = value;
        }

        private void close()
        {
            synchronized (this)
            {
                if(provider != null)
                    provider.destroy();
                if(interestOp != null)
                    interestOp.cancel();
                provider = null;
                interestOp = null;
            }
        }
        
        @Override
        public String toString()
        {
            return oid.toStandardString() + " value: " + currentValue + " " + waveformData.toString();
        }
    }
    
    private class CheckSubscribeTask implements Runnable
    {
        private final ProvideInfo pi;
        private final AtomicBoolean goingDown;
        private int lastCount;
        
        private CheckSubscribeTask(ProvideInfo pi, AtomicBoolean goingDown)
        {
            this.pi = pi;
            this.goingDown = goingDown;
            lastCount = pi.subscribeCallbacks.get();
        }
        
        @Override
        public void run()
        {
            synchronized (this)
            {
                int count = pi.subscribeCallbacks.get(); 
                if(count == lastCount)
                    log.error("subscribes stalled: " + pi.toString());
                lastCount = count;
                if(!goingDown.get())
                    nodeConfig.timer.schedule(this, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }
}