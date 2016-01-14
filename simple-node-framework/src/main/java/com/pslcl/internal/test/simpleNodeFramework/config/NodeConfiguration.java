/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.pslcl.service.PropertiesFile;
import org.pslcl.service.executor.BlockingExecutor;
import org.pslcl.service.executor.ScheduledExecutor;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTrackerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.CredentialsInfo;
import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.chad.app.StrH;
import com.pslcl.chad.app.StrH.StringPair;
import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.ExecutorConfigData;
import com.pslcl.internal.test.simpleNodeFramework.ExecutorConfigData.ScheduledExecutorConfigData;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.DofConfiguration.DofConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.DofSystemConfiguration.SystemConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.ExecutorConfiguration.ExecutorConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.ScheduledExecutorConfiguration.TimerConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.SubsystemConfiguration.SubsystemConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.SystemBase;

@SuppressWarnings("javadoc")
public class NodeConfiguration
{
    public static final String ExecutorFileNameKey = "emitdo.snf.node.executor-config";
    public static final String TimerFileNameKey = "emitdo.snf.node.timer-executor-config";
    public static final String ConfigBaseKey = "emitdo.snf.config";
    public static final String ConfigJdbKey = "emitdo.snf.config.jdb";
    public static final String ConfigSolKey = "emitdo.snf.config.sol";
    public static final String ConfigQaKey = "emitdo.snf.config.qa";
    
    public static final String SubsystemBaseConfigKey = "emitdo.snf.node.subsystem";
    public static final String FdnKey = "emitdo.snf.node.fdn";
    
    public static final String AppConfigOffsetDefault = "../";

    public static final String ModuleNameDefault = "testModule";
    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    public static final String ServiceIdDefault = "[3:testModule@us.panasonic.com]";
    public static final String CredentialsFileNameDefault = "solAwsDsp.cred";
    public static final String TunnelDefault = "false";

    public static final String DofFileNameDefault = "dof/default-dof.properties";
    public static final String ConnectionFileNameDefault = "connection/sol-dsp-conn.properties";
    public static final String ExecutorFileNameDefault = "executor/default-executor.properties";
    public static final String TimerFileNameDefault = "timer/default-timer.properties";
    
    public static final String ConfigBaseDefault = "/ws/test-helpers/config";
    public static final String ConfigJdbDefault = "/ws/test-helpers/config/jdb";
    public static final String ConfigSolDefault = "/ws/test-helpers/config/qa";
    public static final String ConfigQaDefault = "/ws/test-helpers/config/sol";
    
    public static void loadProperties(NodeConfig nodeConfig, String file) throws Exception
    {
        ConfigBaseType configType = ConfigBaseType.getType(file);
        String path = null;
        switch(configType)
        {
            case base:
                path = nodeConfig.configBase; 
                break;
            case jdb:
                path = nodeConfig.configJdb; 
                break;
            case qa:
                path = nodeConfig.configQa; 
                break;
            case sol:
                path = nodeConfig.configSol; 
                break;
            case unknown:
            default:
                throw new Exception("invalid ConfigBaseType: " + file);
        }
        if (!path.endsWith("/"))
            path += "/";
        
        file = ConfigBaseType.stripType(file);
        if (file.startsWith("/"))
            path += file.substring(1);
        else
            path += file;
        path = path.trim();
        nodeConfig.toSb("path=" + path);
        PropertiesFile.load(nodeConfig.properties, path);
    }

    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Properties properties)
    {
        ArrayList<Entry<String, String>> entries = new ArrayList<Entry<String, String>>();
        Hashtable<Integer, StringPair> orderingMap = new Hashtable<Integer, StringPair>();
        
        int found = 0;
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            int index = 0;
            if (key.startsWith(baseKey))
            {
                ++found;
                char[] chars = key.toCharArray();
                if(Character.isDigit(chars[chars.length-1]))
                {
                    int strIndex = 0;
                    for(int i=chars.length-1; i >=0; i--)
                    {
                        if(!Character.isDigit(chars[i]))
                        {
                            strIndex = i + 1;
                            break;
                        }
                    }
                    index = Integer.parseInt(key.substring(strIndex));
                }
                orderingMap.put(index, new StringPair(entry));
            }
        }
        int i=0;
        int hit = 0;
        do
        {
            StringPair pair = orderingMap.get(i);
            if(pair != null)
            {
                entries.add(pair);
                ++hit;
            }
            ++i;
        }while(hit < found);
        return entries;
    }

    public static void removePropertiesForBaseKey(String baseKey, Properties properties)
    {
        List<String> keys = new ArrayList<String>();
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            if (key.startsWith(baseKey))
                keys.add(key);
        }
        for (String key : keys)
            properties.remove(key);
    }

    public static CredentialsData getCredentialsFromFile(NodeConfig nodeConfig, String credFileName) throws Exception
    {
        String credsPath = nodeConfig.platformConfig.getBaseSecureConfigurationPath() + File.separator + credFileName;
        nodeConfig.toSb("credsPath=", credsPath);
        CredentialsData credentials = null;
        try
        {
            credentials = CredentialsInfo.read(credsPath);
        } catch (Exception e)
        {
            nodeConfig.toSb("failed to read credentials file ", credsPath);
            nodeConfig.log.error(nodeConfig.sb.toString());
            throw e;
        }
        return credentials;
    }

    public static CredentialsData getCredentials(String path, StringBuilder sb, Logger log) throws Exception
    {
        try
        {
            sb.append("\tcredentialsPath=" + path + "\n");
            CredentialsData credentials = CredentialsInfo.read(path);
            sb.append("\t\tcredentials=" + credentials.credentials.toString() + "\n");
            return credentials;
        } catch (Exception e)
        {
            sb.append("\nfailed to load credentials from path " + path + "\n");
            log.error(sb.toString());
            throw e;
        }
    }

    public static class NodeConfig extends Configuration
    {
        public final Node node;
        public final Properties properties;
        public final Logger log;
        public final AtomicInteger tabLevel;
        public final StatusTracker statusTracker;
        public volatile String configBase;
        public volatile String configQa;
        public volatile String configSol;
        public volatile String configJdb;
        public volatile StringBuilder sb;
        public volatile ExecutorService executor;
        public volatile ScheduledExecutorService timer;
        public volatile DofConfig dofConfig; // for non-test modules
        public final List<SubsystemConfig> subsystemConfigs;

        public NodeConfig(Node node, Configuration dspConfig, Properties properties, StringBuilder sb)
        {
            super(dspConfig.platformConfig, dspConfig.systemConfig);
            this.node = node;
            this.sb = sb;
            log = LoggerFactory.getLogger(getClass());
            tabLevel = new AtomicInteger();
            subsystemConfigs = new ArrayList<SubsystemConfig>();
            statusTracker = new StatusTrackerProvider();
            this.properties = properties;
        }

        public void setModuleStatus(Module module, ModuleStatus status)
        {
            if (status == ModuleStatus.Failed)
                ((SimpleNodeFramework) node).failed();
            synchronized (subsystemConfigs)
            {
                for (SubsystemConfig subsystem : subsystemConfigs)
                {
                    if (subsystem.setModuleStatus(module, status))
                        return;
                }
            }
        }

        public void waitForStatus(Module module, ModuleStatus status, int timeout) throws Exception
        {
            synchronized (subsystemConfigs)
            {
                for (SubsystemConfig subsystem : subsystemConfigs)
                {
                    if (subsystem.waitForStatus(module, status, timeout))
                        return;
                }
            }
        }

        public void init() throws Exception
        {
            String msg = "ok";
            try
            {
                configBase = properties.getProperty(ConfigBaseKey, ConfigBaseDefault);
                toSb(ConfigBaseKey, "=", configBase);
                configJdb = properties.getProperty(ConfigJdbKey, ConfigJdbDefault);
                toSb(ConfigJdbKey, "=", configJdb);
                configSol = properties.getProperty(ConfigSolKey, ConfigSolDefault);
                toSb(ConfigSolKey, "=", configSol);
                configQa = properties.getProperty(ConfigQaKey, ConfigQaDefault);
                toSb(ConfigQaKey, "=", configQa);
                
                String executorConfigStr = properties.getProperty(ExecutorFileNameKey);
                if (executorConfigStr != null)
                {
                    loadProperties(this, executorConfigStr);
                    tabLevel.incrementAndGet();
                    ExecutorConfig execConfig = ExecutorConfiguration.propertiesToConfig(this);
                    tabLevel.decrementAndGet();
                    executor = new BlockingExecutor();

                    ExecutorConfigData execConfigData = new ExecutorConfigData(execConfig.corePoolSize, execConfig.threadNamePrefix, statusTracker, execConfig.maxQueueSize, execConfig.allowCoreThreadTimeout, execConfig.keepAliveDelay, execConfig.maxBlockingTime);
                    ((BlockingExecutor) executor).init(execConfigData);
                }

                String timerConfigStr = properties.getProperty(TimerFileNameKey);
                if (timerConfigStr != null)
                {
                    loadProperties(this, timerConfigStr);
                    tabLevel.incrementAndGet();
                    TimerConfig timerConfig = ScheduledExecutorConfiguration.propertiesToConfig(this);
                    tabLevel.decrementAndGet();

                    timer = new ScheduledExecutor();
                    //FIXME: config for timer does not jive
                    ScheduledExecutorConfigData sexecConfig = new ScheduledExecutorConfigData(timerConfig.corePoolSize, true, //timerConfig.allowCoreThreadTimeout,
                                    30000, //timerConfig.keepAliveDelay,
                                    30000); //timerConfig.maxBlockingTime);
                    ((ScheduledExecutor) timer).init(sexecConfig);
                }

                msg = "executor not specified or unable to instantiate";
                if (executor == null)
                    throw new Exception(ExecutorFileNameKey + " not specified");
                
                List<Entry<String, String>> list = getPropertiesForBaseKey(SubsystemBaseConfigKey, properties);
                
                properties.remove(ExecutorFileNameKey);
                properties.remove(TimerFileNameKey);
                properties.remove(ConfigBaseKey);
                properties.remove(ConfigJdbKey);
                properties.remove(ConfigSolKey);
                properties.remove(ConfigQaKey);
                removePropertiesForBaseKey(SubsystemBaseConfigKey, properties);
                
                sb.append("\nModule/Service properties that must be pushed to System.properties:\n");
                String ending = "\tnone\n\n";
                for (Entry<Object, Object> entry : properties.entrySet())
                {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    sb.append("\t" + key + "=" + value + "\n");
                    System.setProperty(key, value);
                    ending = "\n";
                }
                sb.append(ending);
                
                int priority = 1;
                for(Entry<String, String> entry : list)
                {
                    toSb("******** subsystem  ********");
                    toSb(entry.getKey(), "=", entry.getValue());
                    loadProperties(this, entry.getValue());
                    tabLevel.incrementAndGet();
                    subsystemConfigs.add(SubsystemConfiguration.propertiesToConfig(this, priority));
                    tabLevel.decrementAndGet();
                    ++priority;
                }
            } catch (Exception e)
            {
                sb.append("\n" + msg);
                log.error(sb.toString(), e);
                throw e;
            }
        }

        public void start() throws Exception
        {
            tabLevel.set(0);
            sb = new StringBuilder();
            toSb("\nStarting Subsystems:");
            tabLevel.incrementAndGet();
            try
            {
                synchronized (subsystemConfigs)
                {
                    //TODO: lose this later
//                    int[] indices = SubsystemConfig.sortPriority(subsystemConfigs);
//                    for (int i=0; i < indices.length; i++)
//                        indices[i] = i;
                    for (SubsystemConfig ssConfig : subsystemConfigs)
                    {
                        toSb("Starting subsystem: ", ssConfig.name);
                        tabLevel.incrementAndGet();
                        ssConfig.start();
                        tabLevel.decrementAndGet();
                    }
                }
                log.info(sb.toString());
                tabLevel.decrementAndGet();
            } catch (Exception e)
            {
                toSb("start failed: ", e.getClass().getSimpleName(), " ", e.getMessage());
                log.error(sb.toString());
                throw e;
            }
        }

        public void stop() throws Exception
        {
            tabLevel.set(0);
            sb = new StringBuilder();
            toSb("\nStopping Subsystems:");
            tabLevel.incrementAndGet();
            synchronized (subsystemConfigs)
            {
                for(SubsystemConfig subsystem : subsystemConfigs)
                {
                    toSb("stopping subsystem: ", subsystem.name);
                    try
                    {
                        tabLevel.incrementAndGet();
                        subsystem.stop();
                        tabLevel.decrementAndGet();
                    } catch (Exception e)
                    {
                        log.error("stop failed:" + sb.toString(), e);
                    }
                }
            }
            tabLevel.decrementAndGet();
            log.info(sb.toString());
        }

        public void destroy()
        {
            tabLevel.set(0);
            sb = new StringBuilder();
            toSb("\nDestroying Subsystems:");
            tabLevel.incrementAndGet();
            synchronized (subsystemConfigs)
            {
                for(SubsystemConfig subsystem : subsystemConfigs)
                {
                    toSb("destroy subsystem: ", subsystem.name);
                    try
                    {
                        tabLevel.incrementAndGet();
                        subsystem.destroy();
                        tabLevel.decrementAndGet();
                    } catch (Exception e)
                    {
                        toSb("destroy failed: ", e.getClass().getSimpleName(), " ", e.getMessage());
                        log.error("destroy failed");
                    }
                }
                subsystemConfigs.clear();
            }
            // Destroy the Status Tracker
            statusTracker.endStatusProvider();
            tabLevel.decrementAndGet();
            log.debug("Node.destroy() complete");
        }

        
        public int getTimeout(String fdn)
        {
            return getCredConnSysConfig(fdn).commTimeout;
        }
        
        public void waitForCommsUp() throws Exception
        {
            tabLevel.set(1);
            sb = new StringBuilder("\nWaiting for Comms:\n");
            synchronized (subsystemConfigs)
            {
                for (SubsystemConfig subsystem : subsystemConfigs)
                {
                    toSb("waiting for ", subsystem.name);
                    tabLevel.incrementAndGet();
                    subsystem.waitForCommsUp();
                    tabLevel.decrementAndGet();
                }
            }
            toSb("comms ok");
            log.info(sb.toString());
        }
        
        public SystemBase getSystemBase(String subsystem, String moduleName) throws Exception
        {
            return getSubsystemConfig(subsystem).getSystemBase(moduleName);
        }

        public Module getModule(String subsystem, String moduleName) throws Exception
        {
            return getSubsystemConfig(subsystem).getModule(moduleName);
        }

        public void startModule(String subsystem, String moduleName, boolean service) throws Exception
        {
            getSubsystemConfig(subsystem).startModule(moduleName, service);
        }
        
        public void stopModule(String subsystem, String moduleName, boolean service) throws Exception
        {
            getSubsystemConfig(subsystem).stopModule(moduleName, service);
        }
        
        public void startConnection(String fdn, String connName) throws Exception
        {
            getCredConnSysConfig(fdn).connect(connName);
        }
        
        public void stopConnection(String fdn, String connName) throws Exception
        {
            getCredConnSysConfig(fdn).disconnect(connName);
        }
        
        public SubsystemConfig getSubsystemConfig(String fdn)
        {
            String sscName = fdn;
            sscName = sscName.toLowerCase();
            if(fdn.contains("."))
                sscName = StrH.getPenultimateName(fdn, '.');
            for(SubsystemConfig subsystem : subsystemConfigs)
            {
                if(subsystem.name.toLowerCase().equals(sscName))
                    return subsystem;
            }
            return null;
        }
        
        public CredConnSysConfig getCredConnSysConfig(String fdn)
        {
            fdn = fdn.toLowerCase();
            String ccsName = StrH.getAtomicName(fdn, '.');
            SubsystemConfig ssc = getSubsystemConfig(fdn);
            if(ssc != null)
                return ssc.getCredConnSysConfig(ccsName);
            log.error("subsystem fdn atomic name: " + fdn + " not found");
            return null;
        }
        
        public DOF getDof(String fdn)
        {
            SubsystemConfig ssc = getSubsystemConfig(fdn);
            if(ssc != null)
                return ssc.getDof();
            log.error("subsystem fdn name: " + fdn + " not found");
            return null;
        }
        
        public SystemConfig getSystemConfig(String fdn, String name)
        {
            CredConnSysConfig ccsc = getCredConnSysConfig(fdn);
            if(ccsc != null)
                return ccsc.getSystemConfig(name);
            log.error("cred conn system config not found for  fdn: " + fdn);
            return null;
        }
        
        public List<String> getSystemNames(String fdn)
        {
            CredConnSysConfig ccsc = getCredConnSysConfig(fdn);
            if(ccsc != null)
                return ccsc.getSystemNames();
            log.error("cred conn system config not found for  fdn: " + fdn);
            return null;
        }
        
        public void toSb(Object ... values)
        {
            StrH.ttl(sb, tabLevel.get(), values);
        }
    }
    
    public enum ConfigBaseType 
    {
        base, jdb, sol, qa, unknown;
        
        public static ConfigBaseType getType(String path)
        {
            if(!path.startsWith("$"))
                return base;
            String type = path.substring(1, path.indexOf('$', 1));
            if(type.equals(base.name()))
                return base;
            if(type.equals(jdb.name()))
                return jdb;
            if(type.equals(sol.name()))
                return sol;
            if(type.equals(qa.name()))
                return qa;
            return unknown;
        }
        
        public static String stripType(String path)
        {
            if(!path.startsWith("$"))
                return path;
            int index = path.indexOf('$', 1);
            return path.substring(++index);
        }
    }
}
