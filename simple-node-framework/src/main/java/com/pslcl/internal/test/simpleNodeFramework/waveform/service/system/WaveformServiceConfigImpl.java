/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.waveform.service.system;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.pslcl.service.status.StatusTracker;

import com.pslcl.service.util.provide.ProvideManager;

@SuppressWarnings("javadoc")
public class WaveformServiceConfigImpl implements WaveformServiceConfig<WaveformServiceConfigImpl>
{
    private final StatusTracker statusTracker;
    private final ExecutorService executor;
    private final ScheduledExecutorService timer;
    private final String moduleName;
    private final ProvideManager provideManager;
    private final int maxSessions;
    public final int gracePeriod;
    public final int serviceIndex;
    
    public WaveformServiceConfigImpl(
                    String moduleName, 
                    ExecutorService executor,
                    ScheduledExecutorService timer,
                    StatusTracker statusTracker,
                    ProvideManager provideManager,
                    int maxSessions,
                    int gracePeriod,
                    int serviceIndex) throws Exception
    {
        this.moduleName = moduleName;
        this.executor = executor;
        this.timer = timer;
        this.statusTracker = statusTracker;
        this.provideManager = provideManager;
        this.maxSessions = maxSessions;
        this.gracePeriod = gracePeriod;
        this.serviceIndex = serviceIndex;
    }
    
    @Override
    public ExecutorService getExecutor()
    {
        return executor;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor()
    {
        return timer;
    }
    
    @Override
    public StatusTracker getStatusTracker()
    {
        return statusTracker;
    }

    @Override
    public String getModuleName()
    {
        return moduleName;
    }

    @Override
    public WaveformServiceDa getDataAccessor()
    {
        //TODO: see if you can property narrow <T> to the da's config
        return null;
    }
    
    public String getDaDescription()
    {
        return "the DataAccessor Description";
    }
    
    public ProvideManager getProvideManager()
    {
        return provideManager;
    }
    
    public int getMaxSessions()
    {
        return maxSessions;
    }
}
