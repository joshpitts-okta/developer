/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAuditListener;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFProtocolNegotiator;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.transport.inet.InetTransport;

import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.chad.app.ServerHelper;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ServerConfiguration
{
    public static final String ServerNameBaseKey = "emitdo.snf.server.name";
    public static final String HostKey = "emitdo.snf.server.host";
    public static final String PortKey = "emitdo.snf.server.port";
    public static final String TunnelKey = "emitdo.snf.server.tunnel";
    public static final String TypeBaseKey = "emitdo.snf.server.type";
    public static final String DesireKey = "emitdo.snf.server.desire";
    public static final String ProtocolNegotiatorKey = "emitdo.snf.server.protocol-negotiator";
    public static final String MaxSendSilenceKey = "emitdo.snf.server.max-send-silence";
    public static final String MaxReceiveSilenceKey = "emitdo.snf.server.max-receive-silence";
    public static final String AuditListenerClassKey = "emitdo.snf.server.audit-listener";
    public static final String AuditListenerPeriodKey = "emitdo.snf.server.audit-period";
    public static final String AuditListenerNameKey = "emitdo.snf.server.audit-name";
    public static final String TrustedDomainsBaseKey = "emitdo.snf.server.trusted-domain"; // add 0..n on end for actual list items
    public static final String DomainDiscoveryCredentialsKey = "emitdo.snf.server.domain-discovery-cred";
    public static final String WildcardCredentialsKey = "emitdo.snf.server.wildcard-cred";
    
    public static final String ServerName1Default = "asStream";
    public static final String ServerName2Default = "asDatagram";
    public static final String HostDefault = "0.0.0.0";
    public static final String PortDefault = "3567";
    public static final String TunneDefault = "false";
    
    public static final String FileNameDefault = "server.properties";
    public static final String Type1Default = "stream";      // datagram, datagramStateless, hub, point, group
    public static final String Type2Default = "datagram";
    public static final String DesireDefault = "secure";    // any, notSecure, secureAny, secureAuthticateOnly
    public static final String ProtocolNegotiatorDefault = "default"; // defaultAs, defaultAsOnly;
    public static final String MaxSendSilenceDefault = ""+ DOFConnection.Config.DEFAULT_MAX_SEND_SILENCE;
    public static final String MaxReceiveSilenceDefault = ""+ DOFConnection.Config.DEFAULT_MAX_RECEIVE_SILENCE;
    public static final String AuditListenerPeriodDefault = Integer.toString(1000 * 60 * 10); // 10 minutes, from the DSP technical specification eng-dsp_ts.xml
    
    public static final String TunnelDefault = "false";
    public static final String CommTimeoutDefault = Integer.toString(1000 * 15);
    
    public static ServerConfig[] propertiesToConfig(NodeConfig nodeConfig, CredentialsData credentials) throws Exception
    {
        String msg = "ok";
        try
        {
            String host = nodeConfig.properties.getProperty(HostKey, HostDefault);
            nodeConfig.toSb(HostKey,  "=", host);
            
            List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(ServerNameBaseKey, nodeConfig.properties);
            nodeConfig.toSb("Server names:");
            nodeConfig.tabLevel.incrementAndGet();
            String[] names = new String[list.size()];
            int i=0;
            for(Entry<String, String> entry : list)
            {
                nodeConfig.toSb(entry.getKey(), "=",  entry.getValue());
                names[i++] = entry.getValue();
            }
            nodeConfig.tabLevel.decrementAndGet();
            
            msg = "invalid integer port value";
            int port = Integer.parseInt(nodeConfig.properties.getProperty(PortKey, PortDefault));
            nodeConfig.toSb(PortKey, "=",  ""+port);

            msg = "invalid boolean tunnel value";
            boolean tunnel = Boolean.parseBoolean(nodeConfig.properties.getProperty(TunnelKey, TunnelDefault));
            nodeConfig.toSb(TunnelKey, "=",  ""+tunnel);

            list = NodeConfiguration.getPropertiesForBaseKey(TypeBaseKey, nodeConfig.properties);
            nodeConfig.toSb("Server types:");
            nodeConfig.tabLevel.incrementAndGet();
            DOFServer.Type[] types = new DOFServer.Type[list.size()];
            i=0;
            for(Entry<String, String> entry : list)
            {
                String type = entry.getValue();
                nodeConfig.toSb(entry.getKey(), "=",  type);
                DOFServer.Type dtype = null;
                if(type.toLowerCase().equals("stream"))
                    dtype = DOFServer.Type.STREAM;
                else if(type.toLowerCase().equals("datagram"))
                    dtype = DOFServer.Type.DATAGRAM;
                types[i++] = dtype;
            }
            nodeConfig.tabLevel.decrementAndGet();
            
            String desire = nodeConfig.properties.getProperty(DesireKey, DesireDefault);
            nodeConfig.toSb(DesireKey, "=", desire);
            SecurityDesire securityDesire = null;
            if(desire.toLowerCase().equals("secure"))
                securityDesire = SecurityDesire.SECURE;
            else if(desire.toLowerCase().equals("secureauthonly"))
                securityDesire = SecurityDesire.SECURE_AUTHENTICATE_ONLY;
            else if(desire.toLowerCase().equals("secureany"))
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
            
            msg = "invalid int value for maxSendSilence";
            int maxSendSilence = Integer.parseInt(nodeConfig.properties.getProperty(MaxSendSilenceKey, MaxSendSilenceDefault));
            nodeConfig.toSb(MaxSendSilenceKey, "=", ""+maxSendSilence);
            
            msg = "invalid int value for maxReceiveSilence";
            int maxReceiveSilence = Integer.parseInt(nodeConfig.properties.getProperty(MaxReceiveSilenceKey, MaxReceiveSilenceDefault));
            nodeConfig.toSb(MaxReceiveSilenceKey, "=", ""+maxReceiveSilence);
            
            
            ArrayList<Domain> trustedDomains = new ArrayList<Domain>();
            List<Entry<String,String>> entries = NodeConfiguration.getPropertiesForBaseKey(TrustedDomainsBaseKey, nodeConfig.properties);
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

            msg = "invalid domain discovery credentials file";
            CredentialsData domainDiscoveryCredentials = null;
            String credFile = nodeConfig.properties.getProperty(DomainDiscoveryCredentialsKey);
            nodeConfig.toSb(DomainDiscoveryCredentialsKey, "=", credFile);
            if(credFile == null)
                domainDiscoveryCredentials = null;
            else
            {
                if(!DofSystemConfiguration.UseCredConnSysCredentialFlag.equals(credFile))
                    domainDiscoveryCredentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, credFile);
                else
                    domainDiscoveryCredentials = credentials;
            }
            
            CredentialsData wildcardCredentials = null;
            credFile = nodeConfig.properties.getProperty(WildcardCredentialsKey);
            nodeConfig.toSb(WildcardCredentialsKey, "=", credFile);
            if(credFile == null)
                wildcardCredentials = null;
            else
            {
                if(!DofSystemConfiguration.UseCredConnSysCredentialFlag.equals(credFile))
                    wildcardCredentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, credFile);
                else
                    wildcardCredentials = credentials;
            }
            
            msg = "DOFConnection.Config.Builder.build failed";
            // @formatter:off
            DOFServer.Config.Builder  builder = new DOFServer.Config.Builder(
                            types[0], InetTransport.createAddress(host, port))
                                .setName(names[0])
                                .setSecurityDesire(securityDesire)
                                .setTunnelDomains(tunnel)
                                .setAuditorListener(auditor)
                                .setProtocolNegotiator(protocolNegotiator)
                                .setMaxSendSilence(maxSendSilence)
                                .setMaxReceiveSilence(maxReceiveSilence);
            
            // @formatter:on
            if(credentials != null)
                builder.addCredentials(credentials.credentials);
            
            if(domainDiscoveryCredentials != null)
                builder.setDomainDiscoveryCredentials(domainDiscoveryCredentials.credentials);
            
            if(wildcardCredentials != null)
                builder.setWildcardCredentials(wildcardCredentials.credentials);
            
            if(trustedDomains != null && trustedDomains.size() > 0)
                builder.addTrustedDomains(trustedDomains);
            
            ServerConfig[] configs = new ServerConfig[types.length];
            for(int j=0; j < configs.length; j++)
                configs[j] = new ServerConfig(nodeConfig, builder.setServerType(types[j]).setName(names[j]).build());
            
            NodeConfiguration.removePropertiesForBaseKey(ServerNameBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(HostKey);
            nodeConfig.properties.remove(PortKey);
            nodeConfig.properties.remove(TunnelKey);
            NodeConfiguration.removePropertiesForBaseKey(TypeBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(DesireKey);
            nodeConfig.properties.remove(ProtocolNegotiatorKey);
            nodeConfig.properties.remove(MaxSendSilenceKey);
            nodeConfig.properties.remove(MaxReceiveSilenceKey);
            nodeConfig.properties.remove(AuditListenerClassKey);
            nodeConfig.properties.remove(AuditListenerNameKey);
            nodeConfig.properties.remove(AuditListenerPeriodKey);
            NodeConfiguration.removePropertiesForBaseKey(TrustedDomainsBaseKey, nodeConfig.properties);
            nodeConfig.properties.remove(DomainDiscoveryCredentialsKey);
            nodeConfig.properties.remove(WildcardCredentialsKey);
            return configs;
        } catch (Exception e)
        {
            nodeConfig.toSb(msg);
            nodeConfig.log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }
    
    public static class ServerConfig
    {
        public final NodeConfig nodeConfig;
        public volatile DOFServer.Config serverConfig;
        private volatile CredConnSysConfig ccsConfig;            
        public ServerHelper serverHelper;
        
        public ServerConfig(NodeConfig nodeConfig, DOFServer.Config config)
        {
            this.nodeConfig = nodeConfig;
            this.serverConfig = config;
        }
        
        public ServerHelper getServer()
        {
            if(serverHelper == null)
            {
                DOF dof = ccsConfig.subsystem.dofConfig.getDof(); 
                nodeConfig.toSb("starting server: ", serverConfig.getName());
                serverHelper = new ServerHelper(nodeConfig.statusTracker, dof, serverConfig, ccsConfig.commTimeout);
            }
            return serverHelper;
        }
        
        public void setCredConnSysConfig(CredConnSysConfig config)
        {
            ccsConfig = config;
        }
        
        @Override
        public String toString()
        {
            return "serverConfig: " + serverConfig.toString();
        }
    }
}
