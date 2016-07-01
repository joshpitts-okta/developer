/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ExecutorConfiguration
{
    public static final String CorePoolSizeKey = "emitdo.service-utils.executor.core-size";
    public static final String MaximumQueueSizeKey = "emitdo.service-utils.executor.max-queue-size";
    public static final String MaxBlockingTimeKey = "emitdo.service-utils.executor.max-blocking-time";
    public static final String ThreadNamePrefixKey = "emitdo.service-utils.executor.thread-name";
    public static final String KeepAliveDelayKey = "emitdo.service-utils.executor.keep-alive-delay";
    public static final String AllowCoreThreadTimeoutKey = "emitdo.service-utils.executor.core-timeout";
    public static final String StatusNameKey = "emitdo.service-utils.executor.status-name";

    public static final String CorePoolSizeDefault = "8";
    public static final String MaximumQueueSizeDefault = "128";
    public static final String MaxBlockingTimeDefault = "120000";
    public static final String ThreadNamePrefixDefault = "EmitBlockingExecutor";
    public static final String KeepAliveDelayDefault = "120000";
    public static final String AllowCoreThreadTimeoutDefault = "true";
    public static final String StatusNameDefault = "EMITBlockingExecutor";
    
    public static ExecutorConfig propertiesToConfig(NodeConfig nodeConfig) throws Exception
    {
        String msg ="ok";
        try
        {
            String value = nodeConfig.properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            nodeConfig.toSb(CorePoolSizeKey, "=", value);
            msg = "invalid corePoolSize value";
            int corePoolSize = Integer.parseInt(value);
            value = nodeConfig.properties.getProperty(MaximumQueueSizeKey, MaximumQueueSizeDefault);
            nodeConfig.toSb(MaximumQueueSizeKey, "=", value);
            msg = "invalid maxQueueSize value";
            int maxQueueSize = Integer.parseInt(value);
            value = nodeConfig.properties.getProperty(MaxBlockingTimeKey, MaxBlockingTimeDefault);
            nodeConfig.toSb(MaxBlockingTimeKey, "=", value);
            msg = "invalid maxBlockingTime value";
            int maxBlockingTime = Integer.parseInt(value);
            String threadName = nodeConfig.properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            nodeConfig.toSb(ThreadNamePrefixKey, "=", value);
            value = nodeConfig.properties.getProperty(KeepAliveDelayKey, KeepAliveDelayDefault);
            nodeConfig.toSb(KeepAliveDelayKey, "=", value);
            msg = "invalid keepAliveDelay value";
            int keepAliveDelay = Integer.parseInt(value);
            value = nodeConfig.properties.getProperty(AllowCoreThreadTimeoutKey, AllowCoreThreadTimeoutDefault);
            nodeConfig.toSb(AllowCoreThreadTimeoutKey, "=", value);
            msg = "invalid allowCoreThreadTimeout value";
            boolean allowCoreThreadTimeout = Boolean.parseBoolean(value);
            
            String statusName = nodeConfig.properties.getProperty(StatusNameKey, StatusNameDefault);
            nodeConfig.toSb(StatusNameKey, "=", statusName);

            nodeConfig.properties.remove(CorePoolSizeKey);
            nodeConfig.properties.remove(MaximumQueueSizeKey);
            nodeConfig.properties.remove(MaxBlockingTimeKey);
            nodeConfig.properties.remove(ThreadNamePrefixKey);
            nodeConfig.properties.remove(KeepAliveDelayKey);
            nodeConfig.properties.remove(AllowCoreThreadTimeoutKey);
            nodeConfig.properties.remove(StatusNameKey);
            
            msg = "EmitBlockingExecutor builder failed";
            return new ExecutorConfig(corePoolSize, maxQueueSize, maxBlockingTime, threadName, keepAliveDelay, allowCoreThreadTimeout, statusName);
        }catch(Exception e)
        {
            nodeConfig.toSb(msg);
//            nodeConfig.log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }
    
    public static class ExecutorConfig
    {    
        public final int corePoolSize;
        public final int maxQueueSize;
        public final int maxBlockingTime;
        public final String threadNamePrefix;
        public final int keepAliveDelay;
        public final boolean allowCoreThreadTimeout;
        public final String statusName;

        public ExecutorConfig(
                        int corePoolSize,
                        int maxQueueSize,
                        int maxBlockingTime,
                        String threadNamePrefix,
                        int keepAliveDelay,
                        boolean allowCoreThreadTimeout,
                        String statusName)
        {
            this.corePoolSize = corePoolSize;
            this.maxQueueSize = maxQueueSize;
            this.maxBlockingTime = maxBlockingTime;
            this.threadNamePrefix = threadNamePrefix;
            this.keepAliveDelay = keepAliveDelay;
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
            this.statusName = statusName;
        }
    }
}
