package com.pslcl.internal.test.simpleNodeFramework.waveform.service.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.config.service.EmitDataAccessor;

@SuppressWarnings("javadoc")
public class WaveformServiceDa implements EmitDataAccessor<WaveformServiceConfigImpl>
{
    protected final Logger log;
    
    public WaveformServiceDa()
    {
        log = LoggerFactory.getLogger(getClass());
    }
    
    @Override
    public void init(WaveformServiceConfigImpl config) throws Exception
    {
        log.info("WaveformServiceDa init hit: " + config.getDaDescription());
    }

    @Override
    public void stop() throws Exception
    {
        log.info("WaveformServiceDa stop hit");
    }
}

