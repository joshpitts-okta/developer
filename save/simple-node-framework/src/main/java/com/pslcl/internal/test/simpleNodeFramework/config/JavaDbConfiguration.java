package com.pslcl.internal.test.simpleNodeFramework.config;

import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Domain;

import com.pslcl.chad.app.CredentialsInfo.CredentialsData;
import com.pslcl.chad.app.StrH;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class JavaDbConfiguration
{
    public static final String PropertiesFileBaseKey = "emitdo.snf.as.javadb.domain.properties-file";
    public static final String DomainIdKey = "emitdo.snf.as.javadb.domain-id";
    public static final String DbUrlKey = "emitdo.snf.as.javadb.url";
    public static final String UserKey = "emitdo.snf.as.javadb.user";
    public static final String PasswordKey = "emitdo.snf.as.javadb.password";
    public static final String Key = "emitdo.snf.as.javadb.password";
    public static final String TestCredentialsKey="emitdo.snf.as.javadb.test-credentials";

    public static final String DomainIdDefault = "[6:tech-services.emit-networking.org]";
    public static final String DbUrlDefault = "jdbc:derby://localhost:1527/tsas";
    public static final String UserDefault = "auth";
    public static final String PasswordDefault = "auth";
    public static final String TestCredentialsDefault="d0manager.cred";
    
    public static final String PropertiesFileBaseDefault = "../../../as/javadb/techServicesCore.properties";
    
    public static JavaDbConfig propertiesToConfig(NodeConfig nodeConfig) throws Exception
    {
        String msg ="ok";
        try
        {
            msg ="invalid domain id";
            String value = nodeConfig.properties.getProperty(DomainIdKey, DomainIdDefault);
            nodeConfig.toSb(DomainIdKey, "=", value);
            Domain domain = Domain.create(value);
            
            String url = nodeConfig.properties.getProperty(DbUrlKey, DbUrlDefault);
            nodeConfig.toSb(DbUrlKey, "=", url);
            
            String user = nodeConfig.properties.getProperty(UserKey, UserDefault);
            nodeConfig.toSb(UserKey, "=", user);
            
            String password = nodeConfig.properties.getProperty(PasswordKey, PasswordDefault);
            nodeConfig.toSb(PasswordKey,  "=", password);
            
            msg = "invalid credentials file";
            String credFile = nodeConfig.properties.getProperty(TestCredentialsKey);
            nodeConfig.toSb(TestCredentialsKey, "=", credFile);
            CredentialsData credentials = null;
            if(credFile != null)
                credentials = NodeConfiguration.getCredentialsFromFile(nodeConfig, credFile);
            
            nodeConfig.properties.remove(DomainIdKey);
            nodeConfig.properties.remove(DbUrlKey);
            nodeConfig.properties.remove(UserKey);
            nodeConfig.properties.remove(PasswordKey);
            nodeConfig.properties.remove(TestCredentialsKey);
            
            return new JavaDbConfig(domain, url, user, password, credentials);
        }catch(Exception e)
        {
            nodeConfig.sb.append(msg);
            nodeConfig.log.error(nodeConfig.sb.toString(),e);
            throw e;
        }
    }
    
    public static class JavaDbConfig
    {    
        public final DOFObjectID.Domain domainId;
        public final String url;
        public final String user;
        public final String password;
        public final CredentialsData credentials;
        
        public JavaDbConfig(
                        Domain domainId,
                        String url,
                        String user,
                        String password,
                        CredentialsData credentials)
        {
            this.domainId = domainId;
            this.url = url;
            this.user = user;
            this.password = password;
            this.credentials = credentials;
        }
        
        public void toStringBuilder(StringBuilder sb, int level)
        {
            
            StrH.ttl(sb, level, "JavaDbConfig:");
            ++level;
            StrH.ttl(sb, level, domainId.toStandardString());
            StrH.ttl(sb, level, url);
            StrH.ttl(sb, level, user);
            StrH.ttl(sb, level, password);
        }
    }
}
