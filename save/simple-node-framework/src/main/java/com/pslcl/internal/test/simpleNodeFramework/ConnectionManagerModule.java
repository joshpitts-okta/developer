/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.internal.core.SharedConnection.ConnectOperation;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAddress;
import org.opendof.core.oal.DOFAuditListener;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.StateListener;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFGroupAddress;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFProtocolNegotiator;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.PropertiesFile;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;

@SuppressWarnings("javadoc")
public class ConnectionManagerModule implements Module
{
    public static final String ConnectionManagerModuleLongCl = "connectionmanager-module";
    
    public static final String FileNameKey = "simpleNodeFramework.connmanager.properties-file";
    public static final String ModuleNameKey = "simpleNodeFramework.connmanager.module-name";
    public static final String ConnectionConfigFileKey = "simpleNodeFramework.connmanager.connection-config-file";
    public static final String ConnectionNameKey = "simpleNodeFramework.connmanager.name";
    public static final String CredentialsFileNameKey = "simpleNodeFramework.connmanager.credentials";
    public static final String HostKey = "simpleNodeFramework.connmanager.host";
    public static final String GroupAddressKey = "simpleNodeFramework.connmanager.group-address";
    public static final String PortKey = "simpleNodeFramework.connmanager.port";
    public static final String TypeKey = "simpleNodeFramework.connmanager.type";
    public static final String DesireKey = "simpleNodeFramework.connmanager.desire";
    public static final String ProtocolNegotiatorKey = "simpleNodeFramework.connmanager.protocol-negotiator";
    public static final String DomainsBaseKey = "simpleNodeFramework.connmanager.domain"; // add 0..n on end for actual list items
    public static final String CommTimeoutKey = "simpleNodeFramework.connmanager.comm-timeout";
    public static final String AuditListenerClassKey = "simpleNodeFramework.connmanager.audit-listener";
    public static final String AuditListenerNameKey = "simpleNodeFramework.connmanager.audit-name";
    public static final String AuditListenerPeriodKey = "simpleNodeFramework.connmanager.audit-period";
    public static final String StreamRequestListenerClassKey = "simpleNodeFramework.connmanager.stream-request-listener";

    public static final String FileNameDefault = "connmanager.properties";
    public static final String ConnectionConfigFileDefault = "conn-config.properties";
    public static final String ConnectionNameDefault = "PDSP Connection";
    public static final String CredentialsFileNameDefault = "dsp.cred";
    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    public static final String PortDefault = "3567";
    public static final String TypeDefault = "stream";      // datagram, datagramStateless, hub, point, group
    public static final String DesireDefault = "secure";    // any, notSecure, secureAny, secureAuthticateOnly
    public static final String ProtocolNegotiatorDefault = "default"; // defaultAs, defaultAsOnly;
    public static final String AuditListenerClassDefault = com.pslcl.dsp.ConnectionAuditor.class.getName();
    public static final String AuditListenerPeriodDefault = Integer.toString(1000 * 60 * 10); // 10 minutes, from the DSP technical specification eng-dsp_ts.xml
    public static final String ModuleNameDefault = "ConnectionManager";

    private final Logger log;
    private final AtomicBoolean startedBefore;
    private final boolean runMe; 
    private final Hashtable<ConnectionConfigKey, EstablishedConnection> establishedConnections;

    private StatusTracker statusTracker;
    private String moduleName;
    private ConnectionConfigWithListeners myStartupConnectionConfig;
    private int commTimeout;


    public ConnectionManagerModule()
    {
        log = LoggerFactory.getLogger(getClass());
        startedBefore = new AtomicBoolean(false);
        establishedConnections = new Hashtable<ConnectionConfigKey, EstablishedConnection>();
        String modules = System.getProperty(NodeRunner.ActiveTestModulesKey, null);
        if(modules == null || !modules.contains(ConnectionManagerModuleLongCl))
            runMe = false;
        else
            runMe = true;
        log.info(getClass().getName() + (runMe ? " is selected" : " is not selected") + " to executue");
    }

    public static ConnectionConfigWithListeners getConnConfigFromProperties(Configuration nodeConfig, String connFile, StringBuilder sb, Logger log) throws Exception
    {
        Properties properties = new Properties();
        String path = nodeConfig.platformConfig.getBaseConfigurationPath() + "/" + connFile;
        sb.append("\tconnectionConfigPath=" + path + "\n");
        try
        {
            PropertiesFile.load(properties, path);
        } catch (FileNotFoundException fnfe)
        {
            sb.append("\t\tconnectionConfigPath not found, using default connection configuration values\n");
        } catch (IOException e)
        {
            sb.append("failed to read config file" + path);
            log.error(sb.toString());
            throw e;
        }
        String host = System.getProperty(HostKey, null); // system prop has priority
        if (host == null)
        {
            host = properties.getProperty(HostKey, null);
            if (host == null)
            {
                String msg = "The connection host must be configured if a connection is desired";
                sb.append("\t" + msg + "\n");
                return null;
            }
        }
        sb.append("\t" + HostKey + "=" + host + "\n");
        String gaddr = properties.getProperty(GroupAddressKey, null);
        sb.append("\t" + GroupAddressKey + "=" + gaddr + "\n");
        DOFGroupAddress groupAddress = null;
        if (gaddr != null)
            groupAddress = new DOFGroupAddress(Authentication.create(gaddr));

        String name = properties.getProperty(ConnectionNameKey, ConnectionNameDefault);
        sb.append("\t" + ConnectionNameKey + "=" + name + "\n");

        int port = Integer.parseInt(properties.getProperty(PortKey, PortDefault));
        sb.append("\t" + PortKey + "=" + port + "\n");

        String type = properties.getProperty(TypeKey, TypeDefault);
        sb.append("\t" + TypeKey + "=" + type + "\n");
        
        DOFConnection.Type dtype = null;
        if(type.toLowerCase().equals("stream"))
            dtype = DOFConnection.Type.STREAM;
        else if(type.toLowerCase().equals("datagram"))
            dtype = DOFConnection.Type.DATAGRAM;
        else if(type.toLowerCase().equals("datagramstateless"))
            dtype = DOFConnection.Type.DATAGRAM_STATELESS;
        else if(type.toLowerCase().equals("point"))
            dtype = DOFConnection.Type.POINT;
        else if(type.toLowerCase().equals("hub"))
            dtype = DOFConnection.Type.HUB;
        else if(type.toLowerCase().equals("group"))
            dtype = DOFConnection.Type.GROUP;
        
        String desire = properties.getProperty(DesireKey, DesireDefault);
        sb.append("\t" + DesireKey + "=" + desire + "\n");
        SecurityDesire securityDesire = null;
        if(desire.toLowerCase().equals("secure"))
            securityDesire = SecurityDesire.SECURE;
        else if(desire.toLowerCase().equals("secureAuthOnly"))
            securityDesire = SecurityDesire.SECURE_AUTHENTICATE_ONLY;
        else if(desire.toLowerCase().equals("secureAny"))
            securityDesire = SecurityDesire.SECURE_ANY;
        else if(desire.toLowerCase().equals("any"))
            securityDesire = SecurityDesire.ANY;
        else if(desire.toLowerCase().equals("notsecure"))
            securityDesire = SecurityDesire.NOT_SECURE;
        
        DOFCredentials credentials = null;
        // caller can use CredentialsFileNameDefault above, but not_secure connections don't need it.  avoid confusion of showing/setting the default in these cases
        String credFileName = properties.getProperty(CredentialsFileNameKey, null);
        sb.append("\tConnection Credentials:\n");
        if(credFileName != null && credFileName.length() > 0)
        {
            credentials = getCredentialsFromFile(nodeConfig, credFileName, sb, log);
            sb.append("\t\t" + credentials.toString() + "\n");
        }else
            sb.append("\t\tnone\n");
            
        String negotiator = properties.getProperty(ProtocolNegotiatorKey, ProtocolNegotiatorDefault);
        sb.append("\t" + ProtocolNegotiatorKey + "=" + negotiator + "\n");
        
        DOFProtocolNegotiator protocolNegotiator = null;
        if(negotiator.toLowerCase().equals("default"))
            protocolNegotiator = DOFProtocolNegotiator.createDefault();
        else if(negotiator.toLowerCase().equals("defaultas"))
            protocolNegotiator = DOFProtocolNegotiator.createDefaultAS();
        else if(negotiator.toLowerCase().equals("defaultasonly"))
            protocolNegotiator = DOFProtocolNegotiator.createDefaultASOnly();

        ArrayList<DOFDomain.Config> domains = new ArrayList<DOFDomain.Config>();
        List<Entry<String, String>> entries = getPropertiesForBaseKey(DomainsBaseKey, properties);
        if(entries.size() > 0)
        {
            sb.append("\tCredentials for Domains:\n");
            for(Entry<String, String> entry : entries)
            {
                credentials = getCredentialsFromFile(nodeConfig, entry.getValue(), sb, log);
                domains.add(new DOFDomain.Config.Builder(credentials).build());
            }
        }
        
        //TODO: decide if these listeners should be considered in key
        String auditClass = properties.getProperty(AuditListenerClassKey, AuditListenerClassDefault);
        sb.append("\t" + AuditListenerClassKey + "=" + auditClass + "\n");
        String auditName = properties.getProperty(AuditListenerNameKey, nodeConfig.systemConfig.getSystemID());
        sb.append("\t" + AuditListenerNameKey + "=" + auditName + "\n");
        int auditPeriod = Integer.parseInt(properties.getProperty(AuditListenerPeriodKey, AuditListenerPeriodDefault));
        sb.append("\t" + AuditListenerPeriodKey + "=" + auditPeriod + "\n");

        Constructor<?> constructor = Class.forName(auditClass).getConstructor(String.class, int.class);
        DOFAuditListener auditor = (DOFAuditListener) constructor.newInstance(auditName, auditPeriod);

        String streamRequestListenerClass = properties.getProperty(StreamRequestListenerClassKey, null);
        sb.append("\t" + StreamRequestListenerClassKey + "=" + streamRequestListenerClass + "\n");

        DOFConnection.StreamRequestListener streamRequestListener = null;
        if (streamRequestListenerClass != null)
            streamRequestListener = (DOFConnection.StreamRequestListener) Class.forName(streamRequestListenerClass).newInstance();
        
        // @formatter:off
        DOFConnection.Config.Builder  builder = new DOFConnection.Config.Builder(
                        dtype, InetTransport.createAddress(host, port))
                            .setCredentials(credentials)
                            .setSecurityDesire(securityDesire)
                            .setName(name)
                            .setAuditorListener(auditor)
                            .setStreamRequestListener(streamRequestListener)
                            .setProtocolNegotiator(protocolNegotiator);
        // @formatter:on
        for(DOFDomain.Config dconfig : domains)
            builder.addDomain(dconfig);
        return new ConnectionConfigWithListeners(builder.build(), auditor, streamRequestListener, groupAddress);
    }

    @SuppressWarnings("null")
    public DOFSystem getSystemForConnection(DOF dof, ConnectionConfigWithListeners config, int timeout) throws Exception
    {
        ConnectionConfigKey configKey = new ConnectionConfigKey(dof, config.config);
        EstablishedConnection estConnection = null;
        synchronized (establishedConnections)
        {
            estConnection = establishedConnections.get(configKey);
        }
        if (estConnection != null)
        {
            synchronized (estConnection)
            {
                long t0 = 0;
                while (estConnection.system == null)
                {
                    try
                    {
                        estConnection.wait(50);
                        t0 += 50;
                        if (t0 >= timeout)
                            throw new Exception("did not obtain the domain resolution before timeout");
                    } catch (InterruptedException e)
                    {
                        log.warn("unexpected wakeup", e);
                    }
                }
                return estConnection.system;
            }
        }

        // @formatter:off
        DOFConnection connection = null;
        if (config.config.getConnectionType().equals(DOFConnection.Type.POINT))
        {
            boolean use61 = false;
            if(use61)
            {
//                connection = new BuilderPoint(config.groupAddress, config.config.getCredentials())
//                                    .setName(config.config.getName())
//                                    .setAuditorListener(config.auditListener)
//                                    .setCredentials(config.config.getCredentials())
//                                    .setMaxSendSilence(config.config.getMaxSendSilence())
//                                    .setStreamRequestListener(config.streamListener)
//                                    .createConnection(dof, config.config.getAddress());
            }else
            {
                final DOFConnection.Config connConfigDatagram = new DOFConnection.Config.Builder(
                                                                        DOFConnection.Type.DATAGRAM, config.config.getAddress())
                                                                            .setName(config.config.getName() + " Point_UnicastConnection")
                                                                            .setAuditorListener(config.auditListener)
                                                                            .build();
                final DOFConnection connDatagram = dof.createConnection(connConfigDatagram);
    
                final DOFConnection.Config.Builder builder = new DOFConnection.Config.Builder(
                                DOFConnection.Type.POINT, config.groupAddress)
                                    .setName(config.config.getName() + " Point_GroupConnection")
                                    .setCredentials(config.config.getCredentials())
                                    .setSecurityDesire(SecurityDesire.SECURE)
                                    .setMaxReceiveSilence(config.config.getMaxReceiveSilence())
                                    .setStreamRequestListener(config.streamListener)
                                    .setAuditorListener(config.auditListener);
                // @formatter:on
                final DOFConnection.Config connConfigPoint = builder.build();
                connection = connDatagram.createConnection(connConfigPoint);

                log.info("UniGroup$Point.connect: nodeid=" + moduleName + " connecting related " + connDatagram.getState().getConnectionType() + " on " + dof.getState().getName() + " to " + connDatagram.getState().getAddress());
                log.info("UniGroup$Point.connect: nodeid=" + dof + " connecting " + connection.getState().getConnectionType() + " on " + dof.getState().getName() + " to " + connection.getState().getAddress() + " with cred.identity=" + config.config.getCredentials().getIdentity().getDataString());
            }
        } else
            connection = dof.createConnection(config.config);

        estConnection = new EstablishedConnection(connection, config);
        estConnection.consumers.incrementAndGet();
        ConnectOperationListener operationListener = new ConnectOperationListener(estConnection);
        // start servers
        Long beginStart = System.currentTimeMillis();

//if(config.config.getConnectionType() == DOFConnection.Type.POINT)
//{
//    try
//    {
//        connection.connect(5000);
//    }catch(Throwable e)
//    {
//        log.error("point blocking connect failed", e);
//        throw new Exception(e);
//    }
//}else
        connection.beginConnect(timeout, operationListener, beginStart);

        if(config.config.getCredentials() != null)
        {
            DOFDomain.Config domainConfig = new DOFDomain.Config.Builder(config.config.getCredentials()).build();
            DOFDomain serviceDomain = dof.createDomain(domainConfig);
    
            DomainResolvedListener listener = new DomainResolvedListener(dof, estConnection, timeout);
            serviceDomain.addStateListener(listener);
            synchronized (estConnection)
            {
                long t0 = 0;
                while (estConnection.system == null)
                {
                    try
                    {
                        estConnection.wait(50);
                        t0 += 50;
                        if (t0 >= timeout)
                            throw new Exception("did not obtain the domain resolution before timeout");
                    } catch (InterruptedException e)
                    {
                        log.warn("unexpected wakeup", e);
                    }
                }
            }
            synchronized (establishedConnections)
            {
                establishedConnections.put(configKey, estConnection);
            }
        }else
        {
            estConnection.system = dof.createSystem();
        }
        return estConnection.system;
    }

    public void releaseSystemForConnection(DOF dof, ConnectionConfigWithListeners config)
    {
        ConnectionConfigKey configKey = new ConnectionConfigKey(dof, config.config);
        EstablishedConnection estConnection = null;
        synchronized (establishedConnections)
        {
            estConnection = establishedConnections.get(configKey);
        }
        if (estConnection == null)
            return;
        estConnection.decrementConsumer(configKey);
    }

    /* *************************************************************************
     * Daemon implementation
     **************************************************************************/
    @Override
    public void init(Configuration config) throws Exception
    {
        if(!runMe)
            return;
        StringBuilder sb = new StringBuilder("\n" + getClass().getName() + " init:\n");
        Properties properties = NodeRunner.loadPropertiesFile(config, SimpleNodeFramework.PropertyPathKey, SimpleNodeFramework.propertyPathDefault, sb, log);
        moduleName = properties.getProperty(ModuleNameKey, ModuleNameDefault);
        sb.append("\t" + ModuleNameKey + "=" + moduleName + "\n");
        commTimeout = Integer.parseInt(properties.getProperty(CommTimeoutKey, CommTimeoutDefault));
        sb.append("\tconnectionStartupTimeout=" + commTimeout + "\n");

        String connFile = properties.getProperty(ConnectionConfigFileKey, ConnectionConfigFileDefault);
        sb.append("\t" + ConnectionConfigFileKey + "=" + connFile + "\n");

        // host has no default, if not given, the connection manager will not create a connection at startup.
        myStartupConnectionConfig = getConnConfigFromProperties(config, connFile, sb, log);

        log.info(sb.toString());
    }

    @Override
    public void start(Node node) throws Exception
    {
        if(!runMe)
            return;
        statusTracker = node.getStatusTracker();
        statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        new StartTask(node).start();
    }

    @Override
    public void stop(Node node) throws Exception
    {
        if(!runMe)
            return;
        log.debug(getClass().getName() + " stopping on " + node.getDOF());

        //TODO: stop/restart needs to be fleshed out - current impl is not dealing with asynchronous listener callbacks.
        synchronized (establishedConnections)
        {
            for (Entry<ConnectionConfigKey, EstablishedConnection> entry : establishedConnections.entrySet())
                entry.getValue().stop();
        }
        statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
    }

    @Override
    public void destroy()
    {
        if(!runMe)
            return;
        synchronized (establishedConnections)
        {
            for (Entry<ConnectionConfigKey, EstablishedConnection> entry : establishedConnections.entrySet())
                entry.getValue().destroy();
        }
        if(statusTracker != null)
            statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
        statusTracker = null;
    }

    /* *************************************************************************
     * Inner classes
     **************************************************************************/

    private class ConnectOperationListener implements DOFConnection.ConnectOperationListener
    {
        private final EstablishedConnection establishedConnection;

        ConnectOperationListener(EstablishedConnection establishedConnection)
        {
            this.establishedConnection = establishedConnection;
        }

        @Override
        public void complete(DOFOperation operation, DOFException exception)
        {
            // here for conn.beginConnect() complete, whether successful or not
            DOFConnection conn = ((ConnectOperation) operation).getConnection();
            long msSinceBegin = System.currentTimeMillis() - (Long) operation.getContext();
            log.debug("ConnectionManager connection beginConnect() for " + conn.getState().getName() + " completed: is " + (conn.isConnected() ? "" : "NOT ") + "connected (" + msSinceBegin + "ms to connect)");
            if (exception != null)
                log.debug("beginConnect() for " + conn.getState().getName() + " sees exception message : " + exception.getMessage(), exception);
            ReconnectingStateListener reconnectingStateListener = new ReconnectingStateListener();
            reconnectingStateListener.setMinimumDelay(commTimeout);
            reconnectingStateListener.setMaximumDelay(commTimeout * 2);
            log.debug("ConnectionManager.ConnectOperationListener starting ReconnectingStateListener minDelay: " + commTimeout + " maxDelay: " + (commTimeout * 2));
            establishedConnection.addStateListener(reconnectingStateListener);
            ConnectionStateListener connStateListener = new ConnectionStateListener();
            establishedConnection.addStateListener(connStateListener);
            conn.addStateListener(reconnectingStateListener); // auto restart at a schedule
            conn.addStateListener(connStateListener);
        }
    }

    private class ConnectionStateListener implements DOFConnection.StateListener
    {
        @Override
        public void stateChanged(DOFConnection conn, DOFConnection.State state)
        {
            log.debug(conn.getState().getName() + " state change: now " + (conn.isConnected() ? "" : "NOT ") + "connected");
            statusTracker.setStatus(conn.getState().getName(), (conn.isConnected() ? StatusTracker.Status.Ok : StatusTracker.Status.Warn));
        }

        @Override
        public void removed(DOFConnection conn, DOFException exception)
        {
            statusTracker.setStatus(conn.getState().getName(), StatusTracker.Status.Warn);
        }
    }

    /**
     * Capture a minimal set any given Connection configuration parameters that
     * would be required to maintain security and matching of requested
     * connections with existing connections.
     */
    private class ConnectionConfigKey
    {
        final DOF dof;
        final DOFAddress address;
        final DOFConnection.Type type;
        // final DOF.SecurityDesire securityDesire;
        final DOFCredentials credentials;

        public ConnectionConfigKey(DOF dof, DOFConnection.Config config)
        {
            this.dof = dof;
            address = config.getAddress();
            type = config.getConnectionType();
            // securityDesire = config.getSecurityDesire();
            credentials = config.getCredentials();
            // config.getBridge();
            // config.getSendFilter();
            // config.getReceiveFilter();
            // config.getPermissions();
            // config.getDomainDiscoveryCredentials();
            // config.getTrustedDomains();
            // config.getDomains();
            // config.getMaxSendSilence();
            // config.getMaxReceiveSilence();
            // config.getProtocolNegotiator();
            // config.getTransportConfig();
            // config.getName();
            // config.getStreamRequestListener();
            // config.isPermissionsExtendAllowed();
            // config.isTunnelDomains();
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((address == null) ? 0 : address.hashCode());
            result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            String dofName = dof.getState().getName();
            result = prime * result + ((dofName == null) ? 0 : dofName.hashCode()); 
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof ConnectionConfigKey))
                return false;
            ConnectionConfigKey other = (ConnectionConfigKey) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (dof == null)
            {
                if (other.dof != null)
                    return false;
            } else if (!dof.getState().getName().equals(other.dof.getState().getName()))
                return false;
            if (address == null)
            {
                if (other.address != null)
                    return false;
            } else if (!address.equals(other.address))
                return false;
            if (credentials == null)
            {
                if (other.credentials != null)
                    return false;
            } else if (!credentials.equals(other.credentials))
                return false;
            if (type != other.type)
                return false;
            return true;
        }

        public ConnectionManagerModule getOuterType()
        {
            return ConnectionManagerModule.this;
        }
    }

    private class EstablishedConnection
    {
        final DOFConnection connection;
        final ConnectionConfigWithListeners config;
        private DOFSystem system;
        final AtomicInteger consumers;
        final List<StateListener> stateListeners;

        EstablishedConnection(DOFConnection connection, ConnectionConfigWithListeners config)
        {
            this.connection = connection;
            this.config = config;
            consumers = new AtomicInteger(0);
            stateListeners = new ArrayList<StateListener>();
        }

        void addStateListener(StateListener listener)
        {
            stateListeners.add(listener);
        }

        void decrementConsumer(ConnectionConfigKey key)
        {
            int count = consumers.decrementAndGet();
            if (count == 0)
            {
                destroy();
                statusTracker.removeStatus(config.config.getName());
                synchronized (establishedConnections)
                {
                    establishedConnections.remove(key);
                }
            }
        }

        void restart() throws DOFException
        {
            connection.connect(commTimeout);
        }

        void stop()
        {
            connection.disconnect();
        }

        void destroy()
        {
            for (StateListener l : stateListeners)
                connection.removeStateListener(l);
            connection.destroy();
            if (system != null)
                system.destroy();
        }
    }

    private class DomainResolvedListener implements DOFDomain.StateListener
    {
        private final DOF dof;
        private final EstablishedConnection estConnection;
        private final int timeout;

        DomainResolvedListener(DOF dof, EstablishedConnection estConn, int timeout)
        {
            estConnection = estConn;
            this.timeout = timeout;
            this.dof = dof;
        }

        /* *************************************************************************
         * DOFDomain.StateListener implementation
         **************************************************************************/
        @Override
        public void removed(DOFDomain domain, DOFException exception)
        {
            if (log.isDebugEnabled())
            {
                String msg = getClass().getName() + " domain removed: " + domain.toString();
                if (exception == null)
                    log.debug(msg);
                else
                    log.debug(msg, exception);
            }
        }

        @Override
        public void stateChanged(DOFDomain domain, DOFDomain.State state)
        {
            log.debug(getClass().getName() + " domain state change: " + state.toString());
            if (state.isConnected())
            {
                try
                {
                    if (estConnection.system == null)
                    {
                        // @formatter:off
                        DOFSystem.Config systemConfig = new DOFSystem.Config.Builder()
                                    .setCredentials(estConnection.config.config.getCredentials())
                                    .setTunnelDomains(true).build();
                        // @formatter:on

                        DOFSystem system = dof.createSystem(systemConfig, timeout);
                        log.debug("ConnectionManager creating system for domain: " + domain.toString() + " identity: " + estConnection.config.config.getCredentials().getIdentity().toStandardString());
                        synchronized (estConnection)
                        {
                            estConnection.system = system;
                            estConnection.notifyAll();
                        }
                    } else
                    {
                        log.warn(estConnection.config.toString() + " already had a system associated with it");
                    }
                } catch (Exception e)
                {
                    log.error("Unable to create DOFSystem", e);
                }
            }
        }
    }

    public static DOFCredentials getCredentialsFromFile(Configuration nodeConfig, String credFileName, StringBuilder sb, Logger log) throws Exception
    {
        String credsPath = nodeConfig.platformConfig.getBaseSecureConfigurationPath() + File.separator + credFileName;
        sb.append("\t\tcredsPath=" + credsPath + "\n");

        DOFCredentials credentials = null;
        try
        {
            credentials = DOFCredentials.create(credsPath);
        } catch (Exception e)
        {
            sb.append("failed to read credentials file " + credsPath);
            log.error(sb.toString());
            throw e;
        }
        return credentials;
    }
    
    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Properties properties)
    {
        ArrayList<Entry<String,String>> entries = new ArrayList<Entry<String,String>>();
        for(Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String)entry.getKey();
            if(key.contains(baseKey))
                entries.add(new StringPair(entry));
        }
        return entries;
    }
    
    private static class StringPair implements Entry<String, String>
    {
        private final String key;
        private String value;
        
        public StringPair(Entry<Object, Object> entry)
        {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
        }
        
        @Override
        public String setValue(String value)
        {
            String old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String getValue()
        {
            return value;
        }
        
        @Override
        public String toString()
        {
            return key+"="+value; 
        }
    }
    
    public static class ConnectionConfigWithListeners
    {
        public final DOFConnection.Config config;
        public final DOFAuditListener auditListener;
        public final DOFConnection.StreamRequestListener streamListener;
        public final DOFGroupAddress groupAddress;

        public ConnectionConfigWithListeners(DOFConnection.Config config, DOFAuditListener auditListener, DOFConnection.StreamRequestListener streamListener, DOFGroupAddress groupAddress)
        {
            this.config = config;
            this.auditListener = auditListener;
            this.streamListener = streamListener;
            this.groupAddress = groupAddress;
        }
    }
    
    private class StartTask extends Thread
    {
        private final SimpleNodeFramework node;
        
        private StartTask(Node node)
        {
            this.node = (SimpleNodeFramework)node;
        }
        
        @Override
        public void run()
        {
            log.debug(ConnectionManagerModule.class.getName() + " starting on " + node.getDOF());
            try
            {
                if (myStartupConnectionConfig == null)
                    return;

                if (startedBefore.get())
                {
                    synchronized (establishedConnections)
                    {
                        for (Entry<ConnectionConfigKey, EstablishedConnection> entry : establishedConnections.entrySet())
                            entry.getValue().restart();
                    }
                } else
                {
                    getSystemForConnection(node.getDOF(), myStartupConnectionConfig, commTimeout);
                }
                startedBefore.set(true);
                statusTracker.setStatus(moduleName, StatusTracker.Status.Ok);
            } catch (Exception e)
            {
                log.error("start failed", e);
                node.setModuleStatus(ConnectionManagerModule.this, ModuleStatus.Failed); //startFatal(ConnectionManagerModule.this, e);
            }
        }
    }
}
