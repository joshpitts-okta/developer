/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.waveform.service.app;

import java.util.ArrayList;
import java.util.List;

import org.opendof.core.oal.DOFInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.system.WaveformServiceConfigImpl;
import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProvideFactory;
import com.pslcl.service.util.provide.ProvideManager;
import com.pslcl.service.util.provide.ProvideRequest;
import com.pslcl.service.util.provide.ProviderInfo;

@SuppressWarnings("javadoc")
public class WaveformProvideFactory implements ProvideFactory
{
    private final WaveformServiceConfigImpl config;
    private final Logger log;
    
    public WaveformProvideFactory(WaveformServiceConfigImpl config)
    {
        this.config = config;
        log = LoggerFactory.getLogger(getClass());
    }
    
    /* *************************************************************************
     * ActivateProvideFactory implementation
     **************************************************************************/

    @Override
    public List<ProvideRequest> getProvideRequestSet(ProvideManager manager, BindingRequest request)
    {
        log.info("getProvideRequestSet called for: " + request.toString());
        List<ProvideRequest> provideRequests = new ArrayList<ProvideRequest>();
        
        WaveformServiceProvider provider = new WaveformServiceProvider(config);
        DOFInterface dofInterface = WaveformInterface.Def;
        provideRequests.add(new ProvideRequest(request, provider, dofInterface, provider));
        return provideRequests;
    }
    
    @Override
    public void removed(ProvideManager manager)
    {
        log.info(toString() + " removed from " + manager.toString());
    }

    public void destroy()
    {
        log.info(toString() + " destroy called");
    }

    @Override
    public void provideActive(ProviderInfo providerInfo)
    {
        ((WaveformServiceProvider)providerInfo.getProvideRequest().getProvider()).setProvideInfo(providerInfo);
    }
}