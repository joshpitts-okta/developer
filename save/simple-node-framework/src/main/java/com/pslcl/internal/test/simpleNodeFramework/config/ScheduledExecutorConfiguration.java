/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ScheduledExecutorConfiguration
{
    public static final String CorePoolSizeKey = "emitdo.service-utils.scheduled-executor.core-size";
    public static final String ThreadNamePrefixKey = "emitdo.service-utils.scheduled-executor.thread-name";
    public static final String StatusNameKey = "emitdo.service-utils.scheduled-executor.status-name";

    public static final String CorePoolSizeDefault = "2";
    public static final String ThreadNamePrefixDefault = "EmitScheduledExecutor";
    public static final String StatusNameDefault = "EmitScheduledExecutor";

    public static TimerConfig propertiesToConfig(NodeConfig nodeConfig) throws Exception
    {
        String msg ="ok";
        try
        {
            String value = nodeConfig.properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            nodeConfig.toSb(CorePoolSizeKey, "=", value);
            msg = "invalid corePoolSize value";
            int corePoolSize = Integer.parseInt(value);
            String threadName = nodeConfig.properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            nodeConfig.toSb(ThreadNamePrefixKey, "=", value);
            String statusName = nodeConfig.properties.getProperty(StatusNameKey, StatusNameDefault);
            nodeConfig.toSb(StatusNameKey, "=", statusName);
            
            nodeConfig.properties.remove(CorePoolSizeKey);
            nodeConfig.properties.remove(ThreadNamePrefixKey);
            nodeConfig.properties.remove(StatusNameKey);
            
            return new TimerConfig(corePoolSize, threadName, statusName);
        }catch(Exception e)
        {
            nodeConfig.toSb(msg);
//            nodeConfig.log.error(nodeConfig.sb.toString(),e);
            throw e;
        }
    }
    
    public static class TimerConfig
    {    
        public final int corePoolSize;
        public final String threadNamePrefix;
        public final String statusName;

        public TimerConfig(int corePoolSize, String threadNamePrefix, String statusName)
        {
            this.corePoolSize = corePoolSize;
            this.threadNamePrefix = threadNamePrefix;
            this.statusName = statusName;
        }
    }
}
