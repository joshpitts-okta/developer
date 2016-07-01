/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.internal.test.simpleNodeFramework.config.ConnectionConfiguration.ConnectionConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.DofSystemConfiguration.SystemConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.ServerConfiguration.ServerConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.SubsystemConfiguration.SubsystemConfig;

@SuppressWarnings("javadoc")
public class CredConnSysConfiguration
{
    public static final String NameKey = "emitdo.snf.subsystem.ccs.name";
    public static final String CredentialsKey = "emitdo.snf.subsystem.ccs.credentials";
    public static final String ConnectionConfigBaseKey = "emitdo.snf.subsystem.ccs.connection";
    public static final String ServerConfigKey = "emitdo.snf.subsystem.ccs.server";
    public static final String SystemConfigBaseKey = "emitdo.snf.subsystem.ccs.system";
    public static final String CommTimeoutKey = "emitdo.snf.subsystem.ccs.comm-timeout";

    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    public static final AtomicInteger nameIndex = new AtomicInteger(-1);
    public static CredConnSysConfig propertiesToConfig(NodeConfig nodeConfig, int priority) throws Exception
    {
        String msg = "ok";
        try
        {
            msg = NameKey + " must be given";
            String name = nodeConfig.properties.getProperty(NameKey);
            if (name == null)
                throw new Exception(msg);
            if(name.endsWith("*"))
            {
                name = name.substring(0, name.length() - 1);
                name += ""+nameIndex.incrementAndGet();
            }
            nodeConfig.toSb(NameKey, "=", name);
            
            String file = nodeConfig.properties.getProperty(CredentialsKey, null);
            nodeConfig.toSb(CredentialsKey, "=", file);
            CredentialsData credentials = null;
            if (file != null)
            {
                msg = "invalid credentials file: " + file;
                nodeConfig.tabLevel.incrementAndGet();
                credentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, file);
                nodeConfig.tabLevel.decrementAndGet();
            }

            nodeConfig.toSb("********  connections ********");
            List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(ConnectionConfigBaseKey, nodeConfig.properties);
            nodeConfig.toSb("Connections:");
            List<ConnectionConfig> connConfigs = new ArrayList<ConnectionConfig>();
            nodeConfig.tabLevel.incrementAndGet();
            boolean found = false;
            for (Entry<String, String> entry : list)
            {
                file = entry.getValue();
                nodeConfig.toSb(entry.getKey(), "=", file);
                NodeConfiguration.loadProperties(nodeConfig, file);
                nodeConfig.tabLevel.incrementAndGet();
                connConfigs.add(ConnectionConfiguration.propertiesToConfig(nodeConfig, credentials));
                nodeConfig.tabLevel.decrementAndGet();
                found = true;
            }
            if (!found)
                nodeConfig.toSb("none");
            nodeConfig.tabLevel.decrementAndGet();

            nodeConfig.toSb("********  servers ********");
            file = nodeConfig.properties.getProperty(ServerConfigKey);
            nodeConfig.toSb(ServerConfigKey, "=", file);
            ServerConfig[] serverConfigs = null;
            if (file != null)
            {
                NodeConfiguration.loadProperties(nodeConfig, file);
                nodeConfig.tabLevel.incrementAndGet();
                serverConfigs = ServerConfiguration.propertiesToConfig(nodeConfig, credentials);
                nodeConfig.tabLevel.decrementAndGet();
            }

            List<Entry<String, String>> systemList = NodeConfiguration.getPropertiesForBaseKey(SystemConfigBaseKey, nodeConfig.properties);
            List<SystemConfig> systemConfigs = new ArrayList<SystemConfig>();
            msg = "duplicate DOFSystem.Config in configuration";
            nodeConfig.toSb("********  DOFSystems ********");
            for (Entry<String, String> entry : systemList)
            {
                nodeConfig.toSb(entry.getKey(), "=", entry.getValue());
                nodeConfig.tabLevel.incrementAndGet();
                NodeConfiguration.loadProperties(nodeConfig, entry.getValue());
                SystemConfig systemConfig = DofSystemConfiguration.propertiesToConfig(nodeConfig, credentials);
                nodeConfig.tabLevel.decrementAndGet();
                systemConfigs.add(systemConfig);
            }

            msg = "invalid integer timeout value";
            int commTimeout = Integer.parseInt(nodeConfig.properties.getProperty(CommTimeoutKey, CommTimeoutDefault));
            nodeConfig.toSb(CommTimeoutKey, "=", "" + commTimeout);

            nodeConfig.properties.remove(NameKey);
            nodeConfig.properties.remove(CredentialsKey);
            NodeConfiguration.removePropertiesForBaseKey(ConnectionConfigBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(ServerConfigKey);
            NodeConfiguration.removePropertiesForBaseKey(SystemConfigBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(CommTimeoutKey);

            return new CredConnSysConfig(name, credentials, connConfigs, serverConfigs, systemConfigs, commTimeout, priority);
        } catch (Exception e)
        {
            nodeConfig.toSb(msg);
            throw e;
        }
    }

    public static class CredConnSysConfig
    {
        public final String name;
        public volatile SubsystemConfig subsystem;
        public final CredentialsData credConfig;
        public final List<ConnectionConfig> connConfigs;
        public final List<SystemConfig> systemConfigs;
        public final List<ServerConfig> serverConfigs;
        public final int commTimeout;
        private final int priority;

        public CredConnSysConfig(String name, CredentialsData credConfig, List<ConnectionConfig> connConfigs, ServerConfig[] serverConfigs, List<SystemConfig> systemConfigs, int commTimeout, int priority)
        {
            this.name = name;
            this.credConfig = credConfig;
            this.connConfigs = connConfigs;
            this.commTimeout = commTimeout;
            this.systemConfigs = systemConfigs;
            this.priority = priority;
            this.serverConfigs = new ArrayList<ServerConfig>();
            if(serverConfigs != null)
            {
                for(int i=0; i < serverConfigs.length; i++)
                {
                    this.serverConfigs.add(serverConfigs[i]);
                    serverConfigs[i].setCredConnSysConfig(this);
                }
            }
            for (ConnectionConfig config : connConfigs)
                config.setCredConnSysConfig(this);
        }

        public void waitForCommsUp() throws Exception
        {
            long t0 = System.currentTimeMillis();
            Hashtable<String, Boolean> serverStates = new Hashtable<String, Boolean>();
            Hashtable<String, Boolean> connStates = new Hashtable<String, Boolean>();
            String current = null;
            String currentType;
            boolean ok;
            do
            {
                ok = true;
                currentType = "server";
                for (ServerConfig config : serverConfigs)
                {
                    current = config.serverConfig.getName();
                    if(!config.serverHelper.isStarted())
                    {
                        ok = false;
                        break;
                    }
                    serverStates.put(current, true);
                }
                if(!ok)
                    continue;
                currentType = "connection";
                for (ConnectionConfig config : connConfigs)
                {
                    if(!config.initialStart.get())
                        continue;
                    current = config.connConfig.getName();
                    if(!config.isConnected())
                    {
                        ok = false;
                        break;
                    }
                    connStates.put(current, true);
                }
                if(ok)
                {
                    for(Entry<String, Boolean> entry: serverStates.entrySet())
                        subsystem.nodeConfig.toSb("waiting for server ", entry.getKey());
                    for(Entry<String, Boolean> entry: connStates.entrySet())
                        subsystem.nodeConfig.toSb("waiting for connection ", entry.getKey());
                    return;
                }
                if(System.currentTimeMillis() - t0 >= commTimeout)
                {
                    for(Entry<String, Boolean> entry: serverStates.entrySet())
                        subsystem.nodeConfig.toSb("waiting for server ", entry.getKey());
                    for(Entry<String, Boolean> entry: connStates.entrySet())
                        subsystem.nodeConfig.toSb("waiting for connection ", entry.getKey());
                    subsystem.nodeConfig.toSb("\t failed waiting for ", currentType, " ", current);
                    LoggerFactory.getLogger(getClass()).error(subsystem.nodeConfig.sb.toString());
                    throw new Exception("timed out waiting for ccsc: " + name + " connections and servers to connect/start");
                }
                synchronized(this)
                {
                    wait(50);
                }
            } while (true);
        }

        public boolean isConnected(String name) throws Exception
        {
            return findConnection(name).isConnected();
        }

        public boolean isStarted(String name) throws Exception
        {
            return findServer(name).serverHelper.isStarted();
        }

        public void connect(String connName) throws Exception
        {
            findConnection(connName).connection();
        }
        
        public void disconnect(String connName) throws Exception
        {
            findConnection(connName).disconnect();
        }
        
        public void startServer(String serverName) throws Exception
        {
            findServer(serverName).getServer();
        }
        
        public void stopServer(String serverName) throws Exception
        {
            findServer(serverName).serverHelper.destroy();
        }
        
        public ServerConfig findServer(String serverName) throws Exception
        {
            synchronized (serverConfigs)
            {
                for (ServerConfig config : serverConfigs)
                {
                    if (serverName.equals(config.serverConfig.getName()))
                        return config;
                }
            }
            throw new Exception("serverName: " + serverName + " not found in ccsc: " + name);
        }
        
        public ConnectionConfig findConnection(String connName) throws Exception
        {
            synchronized (connConfigs)
            {
                for (ConnectionConfig config : connConfigs)
                {
                    if (connName.equals(config.connConfig.getName()))
                        return config;
                }
            }
            throw new Exception("connectionName: " + connName + " not found in ccsc: " + name);
        }
        
        public void start() throws Exception
        {
            for (ServerConfig config : serverConfigs)
                config.getServer();
            for (ConnectionConfig config : connConfigs)
            {
                if(config.initialStart.get())
                    config.connection();
            }
        }

        public void setSubsystem(SubsystemConfig subsystem)
        {
            this.subsystem = subsystem;
            for (SystemConfig config : systemConfigs)
                config.setDofandCcsc(subsystem.getDof(), this);
        }

        public SystemConfig getSystemConfig(String name)
        {
            for (SystemConfig config : systemConfigs)
            {
                if (config.config.getName().equals(name))
                    return config;
            }
            return null;
        }

        public List<String> getSystemNames()
        {
            List<String> list = new ArrayList<String>();
            for (SystemConfig config : systemConfigs)
                list.add(config.config.getName());
            return list;
        }

        @Override
        public String toString()
        {
            return "name: " + name + " priority: " + priority;
        }
    }
}
