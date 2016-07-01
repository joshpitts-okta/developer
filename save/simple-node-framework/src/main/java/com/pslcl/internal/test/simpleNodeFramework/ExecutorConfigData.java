/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import org.pslcl.service.executor.BlockingExecutorConfig;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTrackerProvider;

@SuppressWarnings("javadoc")
public class ExecutorConfigData implements BlockingExecutorConfig
{
    public static final String ExecutorStatusSubsystemName = "Executor-status";
    public static final String ScheduledExecutorStatusSubsystemName = "Scheduled-executor-status";
    public static final String ScheduledExecutorStatusThreadPrefix = "sexec-";

    public static final int DefaultCorePoolSize = 2;
    public static final String DefaultThreadNamePrefix = "";
    public static final StatusTracker DefaultStatusTracker = new StatusTrackerProvider(); 
    public static final int DefaultMaximumPoolSize = DefaultCorePoolSize;
    public static final boolean DefaultAllowCoreThreadTimeout = true; 
    public static final int DefaultKeepAliveTime = 1000 * 15; 
    public static final int DefaultMaximumBlockingTime = 1000 * 60;
    
    private final int corePoolSize;
    private final String threadNamePrefix;
    private final StatusTracker statusTracker;
    private final int maximumPoolSize;
    private final boolean allowCoreThreadTimeout;
    private final int keepAliveTime;
    private final int maximumBlockingTime;

    public ExecutorConfigData()
    {
        corePoolSize = DefaultCorePoolSize;
        threadNamePrefix = DefaultThreadNamePrefix; 
        statusTracker = DefaultStatusTracker;
        maximumPoolSize = DefaultMaximumPoolSize;
        allowCoreThreadTimeout = DefaultAllowCoreThreadTimeout; 
        keepAliveTime = DefaultKeepAliveTime;
        maximumBlockingTime = DefaultMaximumBlockingTime;
    }

    public ExecutorConfigData(int corePoolSize, String threadNamePrefix, StatusTracker statusTracker, int maximumPoolSize, boolean allowCoreThreadTimeout, int keepAliveTime, int maximumBlockingTime)
    {
        this.corePoolSize = corePoolSize;
        this.threadNamePrefix = threadNamePrefix;
        StatusTracker st = DefaultStatusTracker;
        if(statusTracker != null)
            st = statusTracker;
        this.statusTracker = st;
        this.maximumPoolSize = maximumPoolSize;
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
        this.keepAliveTime = keepAliveTime;
        this.maximumBlockingTime = maximumBlockingTime;
    }

    @Override
    public int getCorePoolSize()
    {
        return corePoolSize;
    }

    @Override
    public String getThreadNamePrefix()
    {
        return threadNamePrefix;
    }

    @Override
    public StatusTracker getStatusTracker()
    {
        return statusTracker;
    }

    @Override
    public String getStatusSubsystemName()
    {
        return ExecutorStatusSubsystemName;
    }

    @Override
    public int getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    @Override
    public boolean isAllowCoreThreadTimeout()
    {
        return allowCoreThreadTimeout;
    }

    @Override
    public int getKeepAliveTime()
    {
        return keepAliveTime;
    }

    @Override
    public int getMaximumBlockingTime()
    {
        return maximumBlockingTime;
    }

    public static class ScheduledExecutorConfigData extends ExecutorConfigData
    {
        public ScheduledExecutorConfigData()
        {
            super();
        }
        
        public ScheduledExecutorConfigData(int corePoolSize, boolean allowCoreThreadTimeout, int keepAliveTime, int maximumBlockingTime)
        {
            super(corePoolSize, ScheduledExecutorStatusThreadPrefix, null, corePoolSize, allowCoreThreadTimeout, keepAliveTime, maximumBlockingTime);
        }
        
        @Override
        public String getStatusSubsystemName()
        {
            return ScheduledExecutorStatusSubsystemName;
        }
    }
}
