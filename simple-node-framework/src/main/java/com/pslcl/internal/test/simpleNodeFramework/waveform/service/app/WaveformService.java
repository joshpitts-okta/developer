/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.waveform.service.app;

import java.util.HashSet;
import java.util.Set;

import org.opendof.core.oal.DOFInterfaceID;
import org.pslcl.service.Service;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.system.WaveformServiceConfigImpl;

@SuppressWarnings("javadoc")
public class WaveformService implements Service<WaveformServiceConfigImpl> 
{
    private final Logger log;
    
    private volatile WaveformServiceConfigImpl config;
    private volatile WaveformProvideFactory provideFactory;

    public WaveformService()
    {
        log = LoggerFactory.getLogger(getClass());
    }
    
    @Override
    public void init(final WaveformServiceConfigImpl config) throws Exception
    {
        log.info("WaveformService init hit");
        this.config = config;
        config.getStatusTracker().setStatus(config.getModuleName(), StatusTracker.Status.Warn);
        provideFactory = new WaveformProvideFactory(config);
        Set<DOFInterfaceID> iids = new HashSet<DOFInterfaceID>();
        iids.add(WaveformInterface.IID);
        provideFactory = new WaveformProvideFactory(config);
        config.getProvideManager().addFactory(provideFactory, iids);
        config.getStatusTracker().setStatus(config.getModuleName(), StatusTracker.Status.Ok);
        log.debug("ok");
    }
    
    @Override
    public void destroy()
    {
        log.info("WaveformService close hit");
        provideFactory.destroy();
        config.getStatusTracker().removeStatus(config.getModuleName());
    }
}