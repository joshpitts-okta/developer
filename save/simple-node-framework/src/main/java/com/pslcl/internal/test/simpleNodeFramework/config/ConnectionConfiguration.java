/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAuditListener;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.State;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFGroupAddress;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFProtocolNegotiator;
import org.opendof.core.oal.security.DOFPermission;
import org.opendof.core.oal.security.DOFPermissionSet;
import org.opendof.core.transport.inet.InetTransport;

import com.pslcl.chad.app.ConnectionHelper;
import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.internal.test.simpleNodeFramework.ConnectedEvent;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ConnectionConfiguration
{
    private static final AtomicInteger nameIndex = new AtomicInteger(-1);
    
    public static final String ConnectionNameKey = "emitdo.snf.connection.name";
//    public static final String CredentialsKey = "simpleNodeFramework.connmanager.credentials";
    public static final String HostKey = "emitdo.snf.connection.host";
    public static final String GroupAddressKey = "emitdo.snf.connection.group-address";
    public static final String PortKey = "emitdo.snf.connection.port";
    public static final String TypeKey = "emitdo.snf.connection.type";
    public static final String DesireKey = "emitdo.snf.connection.desire";
    public static final String ProtocolNegotiatorKey = "emitdo.snf.connection.protocol-negotiator";
    public static final String DomainsBaseKey = "emitdo.snf.connection.domain"; // add 0..n on end for actual list items
    public static final String TrustedDomainsBaseKey = "emitdo.snf.connection.trusted-domain"; // add 0..n on end for actual list items
    public static final String AuditListenerClassKey = "emitdo.snf.connection.audit-listener";
    public static final String AuditListenerNameKey = "emitdo.snf.connection.audit-name";
    public static final String AuditListenerPeriodKey = "emitdo.snf.connection.audit-period";
    public static final String StreamRequestListenerClassKey = "emitdo.snf.connection.stream-request-listener";
    public static final String TunnelKey = "emitdo.snf.connection.tunnel";
    public static final String MaxSendSilenceKey = "emitdo.snf.connection.max-send-silence";
    public static final String MaxReceiveSilenceKey = "emitdo.snf.connection.max-receive-silence";
    public static final String PermissionsKey = "emitdo.snf.connection.permissions";
    public static final String InitialStartKey = "emitdo.snf.connection.initial-start";

    public static final String FileNameDefault = "connection.properties";
    public static final String ConnectionConfigFileDefault = "sol-dsp-conn.properties";
    public static final String ConnectionNameDefault = "dsp-sol";
//    public static final String CredentialsFileNameDefault = "solAwsDsp.cred";
    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    public static final String PortDefault = "3567";
    public static final String TypeDefault = "stream";      // datagram, datagramStateless, hub, point, group
    public static final String DesireDefault = "secure";    // any, notSecure, secureAny, secureAuthticateOnly
    public static final String ProtocolNegotiatorDefault = "default"; // defaultAs, defaultAsOnly;
    public static final String AuditListenerClassDefault = com.pslcl.dsp.ConnectionAuditor.class.getName();
    public static final String AuditListenerPeriodDefault = Integer.toString(1000 * 60 * 10); // 10 minutes, from the DSP technical specification eng-dsp_ts.xml
    public static final String TunnelDefault = "false";
    public static final String MaxSendSilenceDefault = ""+ DOFConnection.Config.DEFAULT_MAX_SEND_SILENCE;
    public static final String MaxReceiveSilenceDefault = ""+ DOFConnection.Config.DEFAULT_MAX_RECEIVE_SILENCE;
    public static final String InitialStartDefault = "true";
    
    public static ConnectionConfig propertiesToConfig(NodeConfig nodeConfig, CredentialsData credentials) throws Exception
    {
        String msg = "ok";
        try
        {
//            String waitfor = nodeConfig.properties.getProperty(WaitForStartedKey);
//            nodeConfig.toSb(WaitForStartedKey,  "=", waitfor);
//            String waitforFdn = null;
//            Class<?> waitforClass = null;
//            if (waitfor != null)
//            {
//                String[] pair = waitfor.split(",");
//                waitforFdn = pair[0];
//                msg = "Invalid waitfor class name";
//                waitforClass = Class.forName(pair[1]);
//            }
            
            msg = "invalid boolean value for initial start";
            boolean initialStart = Boolean.parseBoolean(nodeConfig.properties.getProperty(InitialStartKey, InitialStartDefault));
            nodeConfig.toSb(InitialStartKey, "=", ""+initialStart);
            
            ConnectionConfig connAddrConfig = new ConnectionConfig(nodeConfig, initialStart);
            
            msg = "The connection host must be configured";
            String host = nodeConfig.properties.getProperty(HostKey); // system prop has priority
            if (host == null)
                throw new Exception(msg);
            nodeConfig.toSb(HostKey,  "=", host);
            String gaddr = nodeConfig.properties.getProperty(GroupAddressKey, null);
            nodeConfig.toSb(GroupAddressKey, "=", gaddr);
            DOFGroupAddress groupAddress = null;
            msg = "invalid group address";
            if (gaddr != null)
                groupAddress = new DOFGroupAddress(Authentication.create(gaddr));
            connAddrConfig.groupAddress = groupAddress;
            
            String name = nodeConfig.properties.getProperty(ConnectionNameKey, ConnectionNameDefault);
            if(name.endsWith("*"))
            {
                name = name.substring(0, name.length() - 1);
                name += ""+nameIndex.incrementAndGet();
            }
            nodeConfig.toSb(ConnectionNameKey, "=", name);

            msg = "invalid integer port value";
            int port = Integer.parseInt(nodeConfig.properties.getProperty(PortKey, PortDefault));
            nodeConfig.toSb(PortKey, "=",  ""+port);

            String type = nodeConfig.properties.getProperty(TypeKey, TypeDefault);
            nodeConfig.toSb(TypeKey, "=", type);
            
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
            
            String desire = nodeConfig.properties.getProperty(DesireKey, DesireDefault);
            nodeConfig.toSb(DesireKey, "=", desire);
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
                
            String negotiator = nodeConfig.properties.getProperty(ProtocolNegotiatorKey, ProtocolNegotiatorDefault);
            nodeConfig.toSb(ProtocolNegotiatorKey, "=", negotiator);
            
            msg = "invalid DOFProtocolNegotiator value";
            DOFProtocolNegotiator protocolNegotiator = null;
            if(negotiator.toLowerCase().equals("default"))
                protocolNegotiator = DOFProtocolNegotiator.createDefault();
            else if(negotiator.toLowerCase().equals("defaultas"))
                protocolNegotiator = DOFProtocolNegotiator.createDefaultAS();
            else if(negotiator.toLowerCase().equals("defaultasonly"))
                protocolNegotiator = DOFProtocolNegotiator.createDefaultASOnly();

            ArrayList<DOFDomain.Config> domains = new ArrayList<DOFDomain.Config>();
            List<Entry<String, String>> entries = NodeConfiguration.getPropertiesForBaseKey(DomainsBaseKey, nodeConfig.properties);
            if(entries.size() > 0)
            {
                msg="invalid domains credential file";
                nodeConfig.toSb("Credentials for Domains:");
                nodeConfig.tabLevel.incrementAndGet();
                for(Entry<String, String> entry : entries)
                {
                    CredentialsData domainCredentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, entry.getValue());
                    domains.add(new DOFDomain.Config.Builder(domainCredentials.credentials).build());
                }
                nodeConfig.tabLevel.decrementAndGet();
            }
            
            ArrayList<Domain> trustedDomains = new ArrayList<Domain>();
            entries = NodeConfiguration.getPropertiesForBaseKey(TrustedDomainsBaseKey, nodeConfig.properties);
            if(entries.size() > 0)
            {
                msg="invalid trusted domains value";
                nodeConfig.toSb("Trusted domains:");
                nodeConfig.tabLevel.incrementAndGet();
                for(Entry<String, String> entry : entries)
                {
                    String value = entry.getValue();
                    nodeConfig.toSb(entry.getKey(), "=", value);
                    
                    Domain oid = Domain.create(DOFObjectID.DOMAIN_BROADCAST);
                    if(!value.equals("broadcast"))
                        oid = Domain.create(value);
                    trustedDomains.add(oid);
                }
                nodeConfig.tabLevel.decrementAndGet();
            }
            
            String auditClass = nodeConfig.properties.getProperty(AuditListenerClassKey);//, AuditListenerClassDefault);
            nodeConfig.toSb(AuditListenerClassKey, "=", auditClass);
            String auditName = nodeConfig.properties.getProperty(AuditListenerNameKey, nodeConfig.systemConfig.getSystemID());
            nodeConfig.toSb(AuditListenerNameKey, "=", auditName);
            int auditPeriod = Integer.parseInt(nodeConfig.properties.getProperty(AuditListenerPeriodKey, AuditListenerPeriodDefault));
            nodeConfig.toSb(AuditListenerPeriodKey + "=" + auditPeriod);

            DOFAuditListener auditor = null;
            if(auditClass != null)
            {
                msg = "invalid DOFAuditListener value";
                Constructor<?> constructor = Class.forName(auditClass).getConstructor(String.class, int.class);
                auditor = (DOFAuditListener) constructor.newInstance(auditName, auditPeriod);
            }

            String streamRequestListenerClass = nodeConfig.properties.getProperty(StreamRequestListenerClassKey, null);
            nodeConfig.toSb(StreamRequestListenerClassKey, "=", streamRequestListenerClass);

            msg = "invalid StreamRequestListener value";
            DOFConnection.StreamRequestListener streamRequestListener = null;
            if (streamRequestListenerClass != null)
                streamRequestListener = (DOFConnection.StreamRequestListener) Class.forName(streamRequestListenerClass).newInstance();
            
            msg = "invalid boolean value for tunnel domains";
            boolean tunnel = Boolean.parseBoolean(nodeConfig.properties.getProperty(TunnelKey, TunnelDefault));
            nodeConfig.toSb(TunnelKey, "=", ""+tunnel);
            
            msg = "invalid int value for maxSendSilence";
            int maxSendSilence = Integer.parseInt(nodeConfig.properties.getProperty(MaxSendSilenceKey, MaxSendSilenceDefault));
            nodeConfig.toSb(MaxSendSilenceKey, "=", ""+maxSendSilence);
            
            msg = "invalid int value for maxReceiveSilence";
            int maxReceiveSilence = Integer.parseInt(nodeConfig.properties.getProperty(MaxReceiveSilenceKey, MaxReceiveSilenceDefault));
            nodeConfig.toSb(MaxReceiveSilenceKey, "=", ""+maxReceiveSilence);
            
            msg = "DOFConnection.Config.Builder.build failed";
            // @formatter:off
            DOFConnection.Config.Builder  builder = new DOFConnection.Config.Builder(
                            dtype, InetTransport.createAddress(host, port))
                                .setCredentials(credentials == null ? null : credentials.credentials)
                                .setSecurityDesire(securityDesire)
                                .setName(name)
                                .setAuditorListener(auditor)
                                .setStreamRequestListener(streamRequestListener)
                                .setProtocolNegotiator(protocolNegotiator)
                                .setTunnelDomains(tunnel)
                                .setMaxSendSilence(maxSendSilence)
                                .setMaxReceiveSilence(maxReceiveSilence);
//            setPermissions(DOFPermissionSet perms)
//            setPermissionsExtendAllowed(boolean isPermissionsExtendAllowed)
//            setTunnelDomains(boolean isTunnelDomains)
//            addDomain(DOFDomain.Config domainConfig)
//            setDomainDiscoveryCredentials(DOFCredentials discoveryCredentials)
//            addTrustedDomains(DOFObjectID.Domain... domains)
//            setBridge(DOFOperation.Bridge.Config bridge)
//            setSendFilter(DOFOperation.Filter sendFilter)
//            setReceiveFilter(DOFOperation.Filter receiveFilter)
//            setTransportConfig(ConnectionConfig config)
            // @formatter:on
            
            for(DOFDomain.Config dconfig : domains)
                builder.addDomain(dconfig);
            if(trustedDomains != null && trustedDomains.size() > 0)
                builder.addTrustedDomains(trustedDomains);
            
            String permissions = nodeConfig.properties.getProperty(PermissionsKey);
            nodeConfig.toSb(PermissionsKey, "=", permissions);
            if(permissions != null)
            {
                DOFPermissionSet permissionSet = null;
                if(permissions.toLowerCase().equals("all"))
                {
                    permissionSet = new DOFPermissionSet.Builder()
                    .addPermission(
                            new DOFPermission.ActAsAny(),
                            new DOFPermission.Provider(),
                            new DOFPermission.Requestor(),
                            new DOFPermission.Binding.Builder(DOFPermission.Binding.ACTION_ALL).setAllAttributesAllowed(true).build(),
                            new DOFPermission.TunnelDomain(DOFPermission.TunnelDomain.ALL))
                    .build();
                }
                msg = "invalid permission set key: " + permissions;
                if(permissionSet == null)
                    throw new Exception(msg);
                builder.setPermissions(permissionSet);
            }

            connAddrConfig.connConfig = builder.build();
            
            nodeConfig.properties.remove(ConnectionNameKey);
            nodeConfig.properties.remove(HostKey);
//            nodeConfig.properties.remove(CredentialsKey);
            nodeConfig.properties.remove(GroupAddressKey);
            nodeConfig.properties.remove(PortKey);
            nodeConfig.properties.remove(TypeKey);
            nodeConfig.properties.remove(DesireKey);
            nodeConfig.properties.remove(ProtocolNegotiatorKey);
            NodeConfiguration.removePropertiesForBaseKey(DomainsBaseKey, nodeConfig.properties);
            NodeConfiguration.removePropertiesForBaseKey(TrustedDomainsBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(AuditListenerClassKey);
            nodeConfig.properties.remove(AuditListenerNameKey);
            nodeConfig.properties.remove(AuditListenerPeriodKey);
            nodeConfig.properties.remove(StreamRequestListenerClassKey);
            nodeConfig.properties.remove(TunnelKey);
            nodeConfig.properties.remove(InitialStartKey);
            nodeConfig.properties.remove(MaxSendSilenceKey);
            nodeConfig.properties.remove(MaxReceiveSilenceKey);
            return connAddrConfig;
        } catch (Exception e)
        {
            nodeConfig.toSb(msg);
            nodeConfig.log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }
    
    public static class ConnectionConfig implements DOFConnection.StateListener
    {
        public final NodeConfig nodeConfig;
        public volatile DOFConnection.Config connConfig;
        public volatile DOFGroupAddress groupAddress;
        public volatile CredentialsData credData;
        private volatile CredConnSysConfig ccsConfig;   
        private ConnectionHelper connHelper;
        public final AtomicBoolean initialStart;
        
        public ConnectionConfig(NodeConfig nodeConfig, boolean initialStart)
        {
            this.nodeConfig = nodeConfig;
            this.initialStart = new AtomicBoolean(initialStart);
        }
        
        public synchronized void disconnect()
        {
            if(connHelper != null)
                connHelper.destroy();
            connHelper = null;
        }
        
        public synchronized boolean isConnected()
        {
            if(connHelper != null)
                return connHelper.isConnected();
            return false;
        }
        
        public synchronized void connection() throws Exception
        {
            initialStart.set(true);
            if(connHelper == null)
            {
//                if(waitForAvailable != null)
//                {
//                    nodeConfig.toSb("connection: ", connConfig.getName(), " waiting for: ", waitForAvailable.getName());
//                    SnfEvent availableEvent = new AvailableEvent(waitForAvailable);
//                    CredConnSysConfig ccsc = nodeConfig.getCredConnSysConfig(waitForStartedCcs);
//                    availableEvent.waitFor(ccsc.commTimeout);
//                }
                
                DOF dof = ccsConfig.subsystem.dofConfig.getDof(); 
                nodeConfig.toSb("starting connection: ", connConfig.getName());
                connHelper = new ConnectionHelper(nodeConfig.statusTracker, dof, connConfig, groupAddress, ccsConfig.commTimeout);
                connHelper.addListener(this);
            }
        }
        
        public void setCredConnSysConfig(CredConnSysConfig config)
        {
            ccsConfig = config;
        }
        
        @Override
        public String toString()
        {
            return "groupAddress: " + (groupAddress == null ? "null" : groupAddress) + " connConfig: " + connConfig.toString() + " credentials: " + (credData == null ? "null" : "given");
        }

        @Override
        public void stateChanged(DOFConnection connection, State state)
        {
            if(state.isConnected())
            {
                SimpleNodeFramework.getSnf().fireEvent(new ConnectedEvent(ccsConfig));
            }
        }

        @Override
        public void removed(DOFConnection connection, DOFException exception)
        {
        }
    }
}
