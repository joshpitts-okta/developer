/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFSecurityScope;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.security.DOFPermissionSet;

import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.chad.app.StrH;
import com.pslcl.internal.test.simpleNodeFramework.config.ConnectionConfiguration.ConnectionConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;


@SuppressWarnings("javadoc")
public class DofSystemConfiguration
{
    public static final String SystemNameKey = "emitdo.dof.system.name";
    public static final String CredentialFileKey = "emitdo.dof.system.credentials";
    public static final String ExtendAllowedKey = "emitdo.dof.system.extend";
    public static final String TunnelKey = "emitdo.dof.system.tunnel";
    public static final String ViaCoreKey = "emit.dof.system.via-ccs-core";
    
    public static final String UseCredConnSysCredentialFlag = "ccs";

    public static final String SystemNameDefault = "null";
    public static final String ExtendAllowedDefault = "true";
    public static final String TunnelDefault = "false";
//    public static final String DofDefault = "d0-dof.properties";
//    public static final String CredentialFileDefault = "d0bffbff.cred";

    public static SystemConfig propertiesToConfig(NodeConfig nodeConfig, CredentialsData credentials) throws Exception
    {
        return propertiesToConfig(nodeConfig, credentials, null, null, null, null);
    }
    
    public static SystemConfig propertiesToConfig(
                    NodeConfig nodeConfig,
                    CredentialsData credentials,
                    DOFOperation.Filter sendFilter,
                    DOFOperation.Filter receiveFilter,
                    DOFPermissionSet permissions,
                    DOFSecurityScope scope) throws Exception
    {
        String msg = "ok";
        try
        {
//            msg = "Problem with credential file name";
//            String file = nodeConfig.properties.getProperty(CredentialFileKey);
//            nodeConfig.toSb(CredentialFileKey, "=", (file == null ? "null" : file));
//            CredentialsData credentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, file);
            
            String name = nodeConfig.properties.getProperty(SystemNameKey); // don't default, if null, don't set
            nodeConfig.toSb(SystemNameKey, "=", (name == null ? "null" : name));
            
            msg = "invalid boolean value for extend allowed";
            boolean extendAllowed = Boolean.parseBoolean(nodeConfig.properties.getProperty(ExtendAllowedKey, ExtendAllowedDefault));
            nodeConfig.toSb(ExtendAllowedKey, "=", ""+extendAllowed);

            msg = "invalid boolean value for tunnel";
            boolean tunnel = Boolean.parseBoolean(nodeConfig.properties.getProperty(TunnelKey, TunnelDefault));
            nodeConfig.toSb(TunnelKey, "=", ""+tunnel);

            msg = "invalid credentials file";
            String credFile = nodeConfig.properties.getProperty(CredentialFileKey);
            nodeConfig.toSb(CredentialFileKey, "=", credFile);
            if(credFile == null)
                credentials = null;
            else
            if(!UseCredConnSysCredentialFlag.equals(credFile))
                credentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, credFile);

            msg = "DOFSystem.Builder.build failed";
            DOFSystem.Config.Builder builder = new DOFSystem.Config.Builder()
                .setPermissionsExtendAllowed(extendAllowed)
                .setTunnelDomains(tunnel);
            if(name != null)
                builder.setName(name);
            if(credentials != null)
                builder.setCredentials(credentials.credentials);
            if(sendFilter != null)
                builder.setSendFilter(sendFilter);
            if(receiveFilter != null)
                builder.setReceiveFilter(receiveFilter);
            if(permissions != null)
                builder.setPermissions(permissions);
            if(scope != null)
                builder.setRemoteDomain(scope);
            
            nodeConfig.properties.remove(SystemNameKey);
//            nodeConfig.properties.remove(CredentialFileKey);
            nodeConfig.properties.remove(ExtendAllowedKey);
            nodeConfig.properties.remove(TunnelKey);
//            nodeConfig.properties.remove(DofKey);
            return new SystemConfig(nodeConfig, builder.build(), credentials);
        } catch (Exception e)
        {
            nodeConfig.toSb("\n", msg);
            nodeConfig.log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }
    
    public static class SystemConfig 
    {
        public volatile DOF dof;
        public volatile DOFSystem system;
        public volatile CredConnSysConfig ccsc;
        public final DOFSystem.Config config;
        public final CredentialsData credentials;
        private final AtomicBoolean dofset;
        public final NodeConfig nodeConfig;
        
        
        public SystemConfig(NodeConfig nodeConfig, DOFSystem.Config config, CredentialsData credentials)
        {
            this.config = config;
            this.credentials = credentials;
            dofset = new AtomicBoolean(false);
            this.nodeConfig = nodeConfig;
        }
        
        public DOFSystem getSystem() throws Exception
        {
            if(system == null)
                system = dof.createSystem(config, ccsc.commTimeout);
            return system;
            
            //@formatter:off
//            DOFPermissionSet coreSystemPermissions = new 
//                            DOFPermissionSet.Builder()
//                            .addPermission(new DOFPermission.ActAsAny(), new DOFPermission.Provider(), new DOFPermission.Requestor(), 
//                                new DOFPermission.Binding.Builder(DOFPermission.Binding.ACTION_ALL)
//                                .setAllAttributesAllowed(true)
//                                .build(), new DOFPermission.TunnelDomain(DOFPermission.TunnelDomain.ALL)).build();
//            DOFSystem.Config coreSystemConfig = new 
//                                DOFSystem.Config.Builder()
//                                .setCredentials(credentials)
//                                .setTunnelDomains(true)
//                                .setPermissions(coreSystemPermissions)
//                                .setPermissionsExtendAllowed(true)
//                                .build();
            //@formatter:on
//            system = DynamicSystemFactory.getSystemFuture(nodeConfig.executor, dof, config, ccsc.commTimeout).get();
//            return system;
            
        }

        public void setDofandCcsc(DOF dof, CredConnSysConfig ccsc)
        {
            this.dof = dof;
            this.ccsc = ccsc;
            dofset.set(true);
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((config == null) ? 0 : config.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SystemConfig other = (SystemConfig) obj;
            if (config == null)
            {
                if (other.config != null)
                    return false;
            } else if (!config.equals(other.config))
                return false;
            return true;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            toString(sb, 0);
            return sb.toString();
        }
        
        public StringBuilder toString(StringBuilder sb, int level)
        {
            if(dofset.get())
            {
                StrH.ttl(sb, level, "DOFSystem subsystem: ", ccsc.subsystem.name);
                ++level;
                StrH.ttl(sb, level, "dof: " + dof.getState().getName());
                StrH.ttl(sb, level, "credConnSys: ", ccsc.name);
                StrH.ttl(sb, level, "conn creds: ", ccsc.credConfig.credentials);
                StrH.ttl(sb, level, "connections:");
                ++level;
                boolean found = false;
                for(ConnectionConfig config : ccsc.connConfigs)
                {
                    StrH.ttl(sb, level, config.connConfig.getName());
                    found = true;
                }
                if(!found)
                    StrH.ttl(sb, level, "none");
                --level;                    
                StrH.ttl(sb, level, "system: ");
                ++level;
                try
                {
                    StrH.ttl(sb, level, "name: ", getSystem().getState().getName());
                    StrH.ttl(sb, level, "creds: ", credentials.credentials);
                }catch(Exception e)
                {
                    StrH.ttl(sb, level, "creds: ", credentials);
                    ++level;
                    StrH.ttl(sb, level, "failed to obtain DOFSystem");
                }
                // @formatter:on
                return sb;
            }
            StrH.ttl(sb, level, "system:");
            ++level;
            StrH.ttl(sb, level, "creds: ", credentials.credentials);
            try
            {
                StrH.ttl(sb, level, "name: ", getSystem().getState().getName());
                StrH.ttl(sb, level, "creds: ", credentials.credentials);
            }catch(Exception e)
            {
                StrH.ttl(sb, level, "creds: ", credentials.credentials);
                ++level;
                StrH.ttl(sb, level, "failed to obtain DOFSystem");
            }
            return sb;
        }
    }
}
