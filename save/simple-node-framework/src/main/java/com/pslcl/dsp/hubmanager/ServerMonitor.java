/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.internal.core.SharedConnection.ConnectOperation;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.ConnectOperationListener;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.oal.DOFServer.State;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFArray;
import org.opendof.core.oal.value.DOFBoolean;
import org.opendof.core.oal.value.DOFString;
import org.opendof.core.oal.value.DOFStructure;
import org.opendof.core.oal.value.DOFUInt8;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.hubmanager.HubStatusInterface.ServerStatus;

public class ServerMonitor implements ConnectOperationListener
{
    public final static int ReconnectingStateListenerMinDelay = 5000;
    public final static int ReconnectingStateListenerMaxDelay = 30000;

    private final Logger log;
    private final int pollRate;
    private final int loadBalancerPort;
    private final Hashtable<String, ServerListener> serverListeners;
    private final Hashtable<DOFConnection.Config, ServerStatusData> hubProviders;
    private final Hashtable<String, List<HubConnection>> hubs;
    private final int commTimeout;
    
    //volatile, the following are effectively immutable. Guard for visibility on reads being called in a different thread than setters.   
    private volatile StatusTracker statusTracker;
    private volatile ServerStatusData globalStatus;
    private volatile ScheduledFuture<?> poller;
    
    public ServerMonitor(int loadBalancerPort, int pollRate, int commTimeout)
    {
        log = LoggerFactory.getLogger(getClass());
        this.pollRate = pollRate;
        this.loadBalancerPort = loadBalancerPort;
        serverListeners = new Hashtable<String, ServerListener>();
        hubs = new Hashtable<String, List<HubConnection>>();
        hubProviders = new Hashtable<DOFConnection.Config, ServerStatusData>();
        this.commTimeout = commTimeout;
    }
    
    public void setHubProvideFactory(HubProvideFactory hubProvideFactory)
    {
        globalStatus = new ServerStatusData(hubProvideFactory);
    }
    
    /**
     * Obtain the ServerStatus enum, number of servers and number of connected hubs for the given hub configuration.
     * @param hubConfig if null return all known server/hubs, otherwise only return a list of servers for the given configuration.
     * @return the HubStatusInterface.IID's HubState_Type structure DOFValue.
     */
    public DOFValue getProviderHubState(DOFConnection.Config hubConfig)
    {
        synchronized (hubs)
        {
            if(hubConfig != null)
                return hubProviders.get(hubConfig).toOalResponse();
            return globalStatus.toOalResponse();
        }
    }
    
    /**
     * Obtain a list of [server address, groupId, domainId and connected flag] for the given hub configuration.
     * @param hubConfig if null return all known hubs, otherwise only return a list of servers for the given configuration.
     * @return the HubStatusInterface.IID's HubStatusList_Out structure in the required EMIT.invoke's output parameters DOFValue array.
     */
    public DOFValue[] getProviderHubList(DOFConnection.Config hubConfig)
    {
        ArrayList<HubConnection> hubsOfInterest = new ArrayList<HubConnection>();
        synchronized (hubs)
        {
            for(Entry<String, List<HubConnection>> entry : hubs.entrySet())
            {
                List<HubConnection> list = entry.getValue();
                if(list != null)
                {
                    for(HubConnection hub : list)
                    {
                        if(hubConfig == null)
                            hubsOfInterest.add(hub);
                        else if(hub.hubConfig.equals(hubConfig))
                            hubsOfInterest.add(hub);
                    }
                }
            }
        }
        DOFValue[] structs = new DOFValue[hubsOfInterest.size()];
        for(int i=0; i < structs.length; i++)
        {
            HubConnection hub = hubsOfInterest.get(i);
            DOFValue[] fields = new DOFValue[4];
            byte[] serverAddr = hub.server.getState().getAddress().toString().getBytes();
            fields[0] = new DOFString((short)DOFString.UTF_8, serverAddr.length, serverAddr);
            Authentication groupId = Authentication.create(hub.hub.getState().getAddress().toString());
            fields[1] = groupId;
            fields[2] = hub.hubConfig.getCredentials().getDomainID();
            fields[3] = new DOFBoolean(hub.hub.isConnected());
            structs[i] = new DOFStructure(fields);
        }
        DOFValue[] rvalue = new DOFValue[1];
        rvalue[0] = new DOFArray(structs); 
        return rvalue;
    }
    
    /**
     * Are Hubs connected on all servers for this hubConfig?
     * @param hubConfig the hubConfig to check.
     * @return true if all available servers have a Hub for the given configuration connected, false otherwise.
     */
    public boolean isConnected(DOFConnection.Config hubConfig)
    {
        boolean connected = true;
        synchronized (hubs)
        {
            boolean found = false;
            for(Entry<String, List<HubConnection>> entry : hubs.entrySet())
            {
                List<HubConnection> list = entry.getValue();
                if(list != null)
                {
                    for(HubConnection hub : list)
                    {
                        if(hub.hubConfig.equals(hubConfig))
                        {
                            if(connected) // if ever set false, don't allow for reset back to true
                                connected = hub.hub.isConnected();
                            found = true;
                        }
                    }
                }
            }
            if(!found)
                connected = false;
        }
        return connected;
    }
    
    /**
     * This DSP CR node has been notified of Hubs on a different DSP CR node.
     * <p>see if the hub is already registered for this node, adding it if not.
     * @param hubConfig the hub to be added if it does not already exist.
     */
    public void addHubConfig(DOFConnection.Config hubConfig)
    {
        synchronized (hubs)
        {
            if(hubProviders.get(hubConfig) != null)
                return;
            addHubConfig(null, hubConfig);
        }
    }
    
    /**
     * A new provide has been activated.
     * <p>create a hub on all known datagram servers and
     * cache the new hub connections in this class.
     * @param requestMonitor the monitor controlling the hubs activation/deactivation
     */
    public void addHubConfig(HubRequestMonitor requestMonitor)
    {
        addHubConfig(requestMonitor, requestMonitor.getHubConfig());
    }
    
    /**
     * A new provide has been activated.
     * <p>create a hub on all known datagram servers and
     * cache the new hub connections in this class.
     * @param provider the activate provider adding the config
     */
    private void addHubConfig(HubRequestMonitor requestMonitor, DOFConnection.Config hubConfig)
    {
        log.debug("ServerMonitor.addHubConfig " + hubConfig.getAddress().toString() + "." + hubConfig.getCredentials().getDomainID().toString());
        synchronized (hubs)
        {
            ServerStatusData serverStatusData = new ServerStatusData(requestMonitor, globalStatus);
            hubProviders.put(hubConfig, serverStatusData);
            for(Entry<String, ServerListener> entry : serverListeners.entrySet())
            {
                DOFServer server = entry.getValue().server;
                DOFConnection.Config config = getNewNamedConfig(server, hubConfig);
                DOFConnection hub = server.createConnection(config);
                List<HubConnection> list = hubs.get(server.getState().getAddress().toString());
                if(list == null)
                {
                    list = new ArrayList<HubConnection>();
                    hubs.put(server.getState().getAddress().toString(), list);
                }
                serverStatusData.adjustNumberOfServers(1);
                HubConnection hubData = new HubConnection(server, hub, hubConfig); 
                list.add(hubData);
                hub.beginConnect(commTimeout, this, hubData);
            }
        }
    }
    
    /**
     * Interest in a HubProvider has dropped.
     * <p>destroy all hubs that have been created for this hub configuration.
     * @param hubConfig the configuration to be removed.
     */
    public void removeHubConfig(DOFConnection.Config hubConfig)
    {
        log.debug("ServerMonitor.removeHubConfig called by provider " + hubConfig.getAddress().toString() + "." + hubConfig.getCredentials().getDomainID().toString());
        synchronized (hubs)
        {
            for(Entry<String, List<HubConnection>> entry : hubs.entrySet())
            {
                List<HubConnection> list = entry.getValue();
                if(list != null)
                {
                    for(HubConnection hub : list)
                    {
                        if(hub.hubConfig.equals(hubConfig))
                        {
                            hub.hub.destroy();
                            list.remove(hub);
                            hubProviders.remove(hubConfig);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * A given provider registered hub configuration can have multiple 
     * instances on different servers and hence needs name modification
     * to avoid unique name violation exceptions.
     * <p>server-address.group-address.group-domain is used as the name
     * @param server
     * @param hubConfig
     * @return
     */
    private DOFConnection.Config getNewNamedConfig(DOFServer server, DOFConnection.Config hubConfig)
    {
        DOFConnection.Config.Builder builder = new DOFConnection.Config.Builder(hubConfig);
        
        return builder.setName(
                   server.getState().getAddress().toString() + "." + 
                   hubConfig.getAddress().toString() +"." + 
                   hubConfig.getCredentials().getDomainID().toStandardString())
                   .build();
    }

    /** 
     * A new datagram server has been discovered.
     * <p>startup all known hub configurations on this new server.
     * @param server the newly discovered server
     */
    protected void addHubs(DOFServer server)
    {
        log.debug("ServerMonitor.addHubs called by server " + server.getState().getAddress());
        synchronized (hubs)
        {
            List<HubConnection> list = hubs.get(server.getState().getAddress().toString());
            if(list == null)
            {
                list = new ArrayList<HubConnection>();
                hubs.put(server.getState().getAddress().toString(), list);
            }
            for(Entry<DOFConnection.Config, ServerStatusData> entry : hubProviders.entrySet())
            {
                DOFConnection.Config hconfig = getNewNamedConfig(server, entry.getKey());
                DOFConnection hub = server.createConnection(hconfig);
                HubConnection hubConn = new HubConnection(server, hub, entry.getKey()); 
                list.add(hubConn); 
                entry.getValue().adjustNumberOfServers(1);
                hub.beginConnect(commTimeout, this, hubConn);
           }
        }
    }

    /**
     * A datagram server has dropped.
     * <p>Destroy all hubs started up on the lost server.
     * @param server server that dropped.
     */
    protected void dropHubs(DOFServer server)
    {
        log.debug("ServerMonitor.dropHubs called by server " + server.getState().getAddress());
        synchronized (hubs)
        {
            List<HubConnection> list = hubs.get(server.getState().getAddress().toString());
            if(list == null || list.size() == 0)
                return;
            for(HubConnection hub : list)
            {
                hubProviders.get(hub.hubConfig).adjustNumberOfServers(-1);
                hub.destroy();
            }
            list.clear();
            hubs.remove(server.getState().getAddress().toString());
        }
    }
    
    public void start(DOF dof, StatusTracker statusTracker, ScheduledExecutorService timerExecutor)
    {
        this.statusTracker = statusTracker;
        poller = timerExecutor.scheduleAtFixedRate(new ServerPollTask(this, dof), 0, pollRate, TimeUnit.MILLISECONDS);
    }
    
    public void stop()
    {
        if(poller != null)
            poller.cancel(true);
        synchronized (hubs)
        {
            for(Entry<String, ServerListener> entry : serverListeners.entrySet())
                dropHubs(entry.getValue().server);
            serverListeners.clear();
        }
    }
    
    public void destroy()
    {
        synchronized (hubs)
        {
            serverListeners.clear();
            hubProviders.clear();
            hubs.clear();
        }
    }
    
    /**************************************************************************
     * Inner Classes 
    **************************************************************************/     
    
    private class ServerPollTask implements Runnable
    {
        private final ServerMonitor serverMonitor;
        private final DOF dof;
        
        private ServerPollTask(ServerMonitor serverMonitor, DOF dof)
        {
            this.serverMonitor = serverMonitor;
            this.dof = dof;
        }
        
        @Override
        public void run()
        {
            Collection<DOFServer> servers = dof.getRuntime().getServers();
            for(DOFServer server : servers)
            {
                DOFServer.State state = server.getState();
                if(state.getServerType() != DOFServer.Type.DATAGRAM)
                    continue;
                if(server.getRelatedServer() != null)
                    continue;
                
                Object address = state.getAddress().getAddress();
                if(!(address instanceof InetSocketAddress))
                    continue;
                if(((InetSocketAddress)address).getPort() != loadBalancerPort)
                    continue;
                synchronized (hubs)
                {
                    if(serverListeners.get(server.getState().getAddress().toString()) == null)
                    {
                        serverListeners.put(server.getState().getAddress().toString(), new ServerListener(serverMonitor, server));
                        log.info("ServerMonitor discovered a datagram server " + server.getState().getAddress().toString() + " on port " + InetTransport.DEFAULT_UNICAST_PORT + " with no related server");
                    }
                }
            }
        }
    }
    
    
    private class ServerListener implements DOFServer.StateListener
    {
        private final ServerMonitor serverMonitor;
        private final DOFServer server;
        
        private ServerListener(ServerMonitor serverMonitor, DOFServer server)
        {
            this.serverMonitor = serverMonitor;
            this.server = server;
            server.addStateListener(this);
        }
        
        @Override
        public void stateChanged(DOFServer server, State state)
        {
            log.debug("ServerMonitor.ServerListener server " + server.getState().getAddress() + " changed state to " + (state.isStarted() ? "started" : "stopped"));
            if(state.isStarted())
                serverMonitor.addHubs(server);
            else
               serverMonitor.dropHubs(server);
        }

        @Override
        public void removed(DOFServer server, DOFException exception)
        {
            log.debug("ServerMonitor.ServerListener server " + server.getState().getAddress() + " listener removed");
            serverMonitor.dropHubs(server);
            synchronized (hubs)
            {
                serverListeners.remove(server.getState().getAddress().toString());
            }
        }
    }

    /**************************************************************************
     * ConnectOperationListener implementation 
    **************************************************************************/     
    
    @Override
    public void complete(DOFOperation operation, DOFException exception)
    {
        DOFConnection conn = ((ConnectOperation)operation).getConnection();
        HubConnection hub = (HubConnection)operation.getContext();
        String msg = "ServerMonitor hub beginConnect completed for " + hub.toString(); 
        log.info(msg + " connected: " + (conn.isConnected() ? "true" : "false"));
//        synchronized (hubs)
//        {
//            hubProviders.get(hub.hubConfig).adjustNumberOfConnected(conn.isConnected() ? -1 : 1);  // addListener will cause this to equal out
//        }
        if (exception != null)
            log.error(msg + " failed", exception);
        hub.addListeners();
    }    
    
    private class HubConnection
    {
        private final DOFServer server;
        private final DOFConnection hub;
        private final DOFConnection.Config hubConfig;
        private final ReconnectingStateListener reconnectingListener;
        private final ConnectionStateListener stateListener;
        
        private HubConnection(DOFServer server, DOFConnection hub, DOFConnection.Config hubConfig)
        {
            this.server = server;
            this.hub = hub;
            this.hubConfig = hubConfig;
            reconnectingListener = new ReconnectingStateListener();
            reconnectingListener.setMinimumDelay(ReconnectingStateListenerMinDelay);
            reconnectingListener.setMaximumDelay(ReconnectingStateListenerMaxDelay);
            stateListener = new ConnectionStateListener(this);
        }
        
        private void addListeners()
        {
            hub.addStateListener(reconnectingListener);
            hub.addStateListener(stateListener);
        }
        
        private void destroy()
        {
            hub.removeStateListener(reconnectingListener);
            hub.removeStateListener(stateListener);
            hub.destroy();
        }
        
        @Override
        public String toString()
        {
            return server.getState().getAddress().toString()+"."+hub.getState().getAddress().toString()+"."+hub.getState().getCredentials().getDomainID().toString();
        }
    }
    
    private class ConnectionStateListener implements DOFConnection.StateListener
    {
        private final HubConnection hub;
        
        private ConnectionStateListener(HubConnection hub)
        {
            this.hub = hub;
        }
        
        @Override
        public void stateChanged(DOFConnection connection, DOFConnection.State state)
        {
            log.info(connection.getState().getName() + " state change: now " + (connection.isConnected() ? "" : "NOT ") + "connected");
            statusTracker.setStatus(hub.toString(), (connection.isConnected() ? StatusTracker.Status.Ok : StatusTracker.Status.Warn));
            synchronized (hubs)
            {
                hubProviders.get(hub.hubConfig).adjustNumberOfConnected(connection.isConnected() ? 1 : -1);
            }
        }

        @Override
        public void removed(DOFConnection connection, DOFException exception)
        {
            statusTracker.removeStatus(hub.toString());
        }
    }
    
//    private class ProviderData
//    {
//        private final HubProvider provider;
//        private final ServerStatusData statusData;
//        
//        private ProviderData(HubProvider provider)
//        {
//            this.provider = provider;
//            statusData = new ServerStatusData(provider);
//        }
//    }
    
    public static class ServerStatusData
    {
        public final ServerStatusData globalStatus;
        public final HubRequestMonitor requestMonitor;
        public final HubProvideFactory hubProvideFactory;
        public final AtomicInteger numberOfServers;
        public final AtomicInteger numberOfConnected;
        public ServerStatus status;
        
        private ServerStatusData(HubRequestMonitor requestMonitor, ServerStatusData globalStatus)
        {
            this.requestMonitor = requestMonitor;
            hubProvideFactory = null;
            this.globalStatus = globalStatus;
            this.status = ServerStatus.NoneConnected;
            this.numberOfConnected = new AtomicInteger(0);
            this.numberOfServers = new AtomicInteger(0);
        }
        
        private ServerStatusData(HubProvideFactory hubProvideFactory)
        {
            this.requestMonitor = null;
            this.hubProvideFactory = hubProvideFactory;
            this.globalStatus = null;
            this.status = ServerStatus.NoneConnected;
            this.numberOfConnected = new AtomicInteger(0);
            this.numberOfServers = new AtomicInteger(0);
        }
        
        public synchronized void adjustNumberOfServers(int delta)
        {
            numberOfServers.addAndGet(delta);
            if(globalStatus != null)
                globalStatus.adjustNumberOfServers(delta);
            adjustStatus();
        }
        
        public synchronized void adjustNumberOfConnected(int delta)
        {
            numberOfConnected.addAndGet(delta);
            if(globalStatus != null)
                globalStatus.adjustNumberOfConnected(delta);
            adjustStatus();
        }
        
        public synchronized ServerStatus getStatus()
        {
            return status;
        }
        
        private synchronized void adjustStatus()
        {
            if(numberOfConnected.get() == 0)
                status = ServerStatus.NoneConnected;
            else if(numberOfServers.get() == numberOfConnected.get())
                status = ServerStatus.AllConnected;
            else
                status = ServerStatus.SomeConnected;
//            if(provider != null)
//                provider.statusChanged(this);
            if(globalStatus != null)
            {
                globalStatus.adjustStatus();
//                globalStatus.hubProvideFactory.statusChanged(globalStatus);
            }
        }
        
        public DOFValue toOalResponse()
        {
            DOFValue[] fields = new DOFValue[3];
            byte[] rawStatus = status.name().getBytes();
            fields[0] = new DOFString((short)DOFString.UTF_8, rawStatus.length, rawStatus);
            fields[1] = new DOFUInt8((short)numberOfServers.get());
            fields[2] = new DOFUInt8((short)numberOfConnected.get());
            return new DOFStructure(fields); 
        }
    }
}
 

