/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.waveform.service.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.pslcl.service.Service;

import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.SystemBase;
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.app.WaveformService;
import com.pslcl.service.util.dht.cluster.ClusterRoutingConfig;
import com.pslcl.service.util.dht.cluster.ClusterRoutingService;
import com.pslcl.service.util.provide.ProvideManager;
import com.pslcl.service.util.provide.ProvideManagerConfig;

@SuppressWarnings("javadoc")
public class WaveformSystem extends SystemBase
{
    private volatile ProvideManager provideManager;
    private volatile Service<WaveformServiceConfigImpl> service;
    private volatile boolean enableDht;
    private volatile DOFObjectID dhtBaseOid;
    private ClusterRoutingService clusterService;

    public WaveformSystem(String moduleName, int index)
    {
        super(moduleName, index);
    }

    @Override
    public void init(NodeConfig nodeConfig) throws Exception
    {
        super.init(nodeConfig);
        service = new WaveformService();
        
        List<Entry<String,String>> dhtPropertyFiles = NodeConfiguration.getPropertiesForBaseKey(WaveformInterface.DhtPropertiesFileKey, nodeConfig.properties);
        String dhtPropertiesFile = dhtPropertyFiles.get(serviceIndex).getValue();
        nodeConfig.toSb(WaveformInterface.DhtPropertiesFileKey,"=",dhtPropertiesFile);
        if(dhtPropertiesFile != null)
        {
            NodeConfiguration.loadProperties(nodeConfig, dhtPropertiesFile);
        }else
            throw new Exception(WaveformInterface.DhtPropertiesFileKey + " must be declared");
        
        String value = nodeConfig.properties.getProperty(WaveformInterface.EnableDhtKey, WaveformInterface.EnableDhtDefault);
        enableDht = Boolean.parseBoolean(value);
        nodeConfig.toSb(WaveformInterface.EnableDhtKey,"="+enableDht);
        if(!enableDht)
            return;

        String baseOid = nodeConfig.properties.getProperty(WaveformInterface.DhtBaseOidKey);
        nodeConfig.toSb(WaveformInterface.DhtBaseOidKey,"=",baseOid);
        if(baseOid == null)
            throw new Exception(WaveformInterface.DhtBaseOidKey + " must be declared");
        dhtBaseOid = DOFObjectID.create(baseOid);
    }

    @Override
    public void start(SimpleNodeFramework snf) throws Exception
    {
        super.start(snf);
        try
        {
            CredConnSysConfig ccs = nodeConfig.getCredConnSysConfig("apm" + serviceIndex + ".coretocr" + serviceIndex);

            provideManager = new ProvideManager();            
            ProvideManagerConfig pmconfig = new ProvideManagerConfig(provideManager, dof, dof.createSystem(ccs.systemConfigs.get(0).config, ccs.commTimeout), ccs.credConfig.credentials, ccs.commTimeout, new ArrayList<Domain>(), nodeConfig.executor, nodeConfig.timer, nodeConfig.statusTracker);
            if (enableDht)
            {
            	ClusterRoutingConfig clusterconfig = null; //new ClusterRoutingConfig(pmconfig, dhtBaseOid, DOFObjectID.create("[3:someclusterId@plscl.org"));
            	clusterService = new ClusterRoutingService();
            	clusterService.init(clusterconfig);
            	pmconfig.setDhtService(clusterService);
            }
            provideManager.init(pmconfig);
            WaveformServiceConfigImpl wsconfig = new WaveformServiceConfigImpl(moduleName, nodeConfig.executor, nodeConfig.timer, nodeConfig.statusTracker, provideManager, 0, 1000 * 60 * 2, serviceIndex);
            service.init(wsconfig);
        } catch (Exception e)
        {
            log.error("start failed", e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception
    {
        try
        {
        	if (clusterService != null)
        		clusterService.destroy();
        	clusterService = null;
            if (service != null)
                service.destroy();
            service = null;
            if (provideManager != null)
                provideManager.destroy();
            provideManager = null;
        } catch (Exception e)
        {
            log.error("stop cleanup failed", e);
            throw e;
        }
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public Service<WaveformServiceConfigImpl> getService()
    {
        return service;
    }
}