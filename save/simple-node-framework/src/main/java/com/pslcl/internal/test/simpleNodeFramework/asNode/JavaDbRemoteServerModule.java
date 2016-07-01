/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.asNode;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.domain.DomainAuthenticator;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFAuthenticator;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFSystem;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.JavaDbConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.JavaDbConfiguration.JavaDbConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class JavaDbRemoteServerModule implements Module
{

    public static final String PropertyFileNameKey = "emitdo.snf.as.javadb.server.properties-file";
    public static final String DerbyServerHostKey = "derby.drda.host";
    public static final String DerbyServerPortKey = "derby.drda.portNumber";
    public static final String DerbyServerDbPathKey = "derby.system.home";

    public static final String PropertyFileNameDefault = "pointModule.properties";
    public static final String DerbyServerHostDefault = "localhost";
    public static final String DerbyServerPortDefault = "1527";
    public static final String DerbyServerDbPathDefault = "/tools/sdk-services-1.3.3/oas-secure";

    private final Logger log;

    //    private volatile String host;
    //    private volatile int port;
    //    private String dbPath;
    private volatile NetworkServerControl server;
    private final String moduleName;
    private volatile List<JavaDbConfig> javaDbDomainConfigs;
    private volatile NodeConfig nodeConfig;
    private volatile AuthenticatorRestartingStateListener authenticationRestartingListener;
    private volatile HashMap<DOFObjectID.Domain, JavaDBDomainAuthenticator> domains;
    private volatile String fdn;
    private final Hashtable<Domain, DOFAuthenticator> authenticatorMap;

    private DOFCredentials credentials01;
    private DOFCredentials credentials02;
    private DOFCredentials credentials03;
    private DOFCredentials credentials04;
    private DOFCredentials credentials05;
    private DOFCredentials credentials06;
    private DOFCredentials credentials11;
    private DOFCredentials credentials12;
    private DOFCredentials credentials13;
    private DOFCredentials credentials14;
    private DOFCredentials credentials15;
    private DOFCredentials credentials16;
    private DOFCredentials credentials21;
    private DOFCredentials credentials22;
    private DOFCredentials credentials23;
    private DOFCredentials credentials24;
    private DOFCredentials credentials25;
    private DOFCredentials credentials26;
    private String securityBase;
    
    public JavaDbRemoteServerModule()
    {
        log = LoggerFactory.getLogger(getClass());
        moduleName = StrH.getAtomicName(getClass().getName(), '.');
        domains = new HashMap<DOFObjectID.Domain, JavaDBDomainAuthenticator>();
        authenticatorMap = new Hashtable<Domain, DOFAuthenticator>();
    }

    /* *************************************************************************
     * Daemon implementation
     **************************************************************************/
    @Override
    public void init(Configuration config) throws Exception
    {
        nodeConfig = (NodeConfig) config;
        securityBase = nodeConfig.platformConfig.getBaseSecureConfigurationPath() + File.separator;

        String credentialsFile = securityBase + "d0bffbff.cred";
        securityBase = config.platformConfig.getBaseSecureConfigurationPath() + File.separator;
        credentials01 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d0bffbfe.cred";
        credentials02 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d0manager.cred";
        credentials03 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d0service.cred";
        credentials04 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d0provider.cred";
        credentials05 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d0requestor.cred";
        credentials06 = DOFCredentials.create(credentialsFile);
        
        credentialsFile = securityBase + "d1bffbff.cred";
        credentials11 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d1bffbfe.cred";
        credentials12 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d1manager.cred";
        credentials13 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d1service.cred";
        credentials14 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d1provider.cred";
        credentials15 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d1requestor.cred";
        credentials16 = DOFCredentials.create(credentialsFile);
        
        credentialsFile = securityBase + "d2bffbff.cred";
        credentials21 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d2bffbfe.cred";
        credentials22 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d2manager.cred";
        credentials23 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d2service.cred";
        credentials24 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d2provider.cred";
        credentials25 = DOFCredentials.create(credentialsFile);
        credentialsFile = securityBase + "d2requestor.cred";
        credentials26 = DOFCredentials.create(credentialsFile);
        
        int level = 3;
        String value = nodeConfig.properties.getProperty(PropertyFileNameKey, PropertyFileNameDefault);
        StrH.ttl(nodeConfig.sb, level, PropertyFileNameKey, "=", value);
        NodeConfiguration.loadProperties(nodeConfig, value);

        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
        nodeConfig.toSb(NodeConfiguration.FdnKey, "=", fdn);

        String host = nodeConfig.properties.getProperty(DerbyServerHostKey, DerbyServerHostDefault);
        nodeConfig.sb.append("\t" + DerbyServerHostKey + "=" + host + "\n");
        int port = Integer.parseInt(nodeConfig.properties.getProperty(DerbyServerPortKey, DerbyServerPortDefault));
        nodeConfig.sb.append("\t" + DerbyServerPortKey + "=" + port + "\n");
        String dbPath = nodeConfig.properties.getProperty(DerbyServerDbPathKey, DerbyServerHostDefault);
        nodeConfig.sb.append("\t" + DerbyServerDbPathKey + "=" + dbPath + "\n");

        value = nodeConfig.properties.getProperty(PropertyFileNameKey, PropertyFileNameDefault);
        StrH.ttl(nodeConfig.sb, level, PropertyFileNameKey, "=", value);
        List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(JavaDbConfiguration.PropertiesFileBaseKey, nodeConfig.properties);
        javaDbDomainConfigs = new ArrayList<JavaDbConfig>();
        for (Entry<String, String> entry : list)
        {
            nodeConfig.toSb(entry.getKey(), "=", entry.getValue());
            NodeConfiguration.loadProperties(nodeConfig, entry.getValue());
            nodeConfig.tabLevel.incrementAndGet();
            javaDbDomainConfigs.add(JavaDbConfiguration.propertiesToConfig(nodeConfig));
            nodeConfig.tabLevel.decrementAndGet();
        }

        System.setProperty(DerbyServerDbPathKey, dbPath);
        //TODO: fix localhost only verses the hardcoded wildcard
//        server = new NetworkServerControl(InetAddress.getByName("0.0.0.0"), port);
//        new NsSample().startSample(null);
        server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
        NodeConfiguration.removePropertiesForBaseKey(JavaDbConfiguration.PropertiesFileBaseKey, nodeConfig.properties);
    }

    @Override
    public void start(Node node) throws Exception
    {
        //TODO: hookup to slf4j
        nodeConfig.statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        nodeConfig.executor.execute(new StartTask(node));
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug(getClass().getName() + " stopping on " + node.getDOF());

        for (JavaDBDomainAuthenticator domain : domains.values())
        {
            domain.destroy();
        }
        if (authenticationRestartingListener != null)
            authenticationRestartingListener.cancel();
        
        if (server != null)
        {
            server.shutdown();
            server = null;
        }
        nodeConfig.statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
    }

    @Override
    public void destroy()
    {
        nodeConfig.statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
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
            log.debug("\n" + getName() + " start");
            StringBuilder sb = new StringBuilder("\nHooking up to domains:\n");
            try
            {
                log.info("Starting JavaDb remote server");
                server.start(new PrintWriter(System.out));
                Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
                //TODO: config
                Thread.sleep(1000);
                server.ping();
//                server.trace(true);

                domains = new HashMap<DOFObjectID.Domain, JavaDBDomainAuthenticator>();
                int level = 1;
                authenticationRestartingListener = new AuthenticatorRestartingStateListener();

                DOF dof = nodeConfig.getSubsystemConfig(fdn).getDof();
                for (JavaDbConfig config : javaDbDomainConfigs)
                {
                    config.toStringBuilder(sb, level);
                    //TODO: add min/max pool size to configuration
                    JavaDBDomainAuthenticator javaDdAsStorage = new JavaDBDomainAuthenticator(config.domainId, config.url, config.user, config.password, 1, 10);
                        
                    javaDdAsStorage.start(nodeConfig.getCredConnSysConfig(fdn).commTimeout);

                    DomainAuthenticator da = null;
                    javaDdAsStorage.addStateListener(authenticationRestartingListener);
                    domains.put(config.domainId, javaDdAsStorage);
                    DOFAuthenticator.Config authenticatorConfig = new DOFAuthenticator.Config.Builder(config.domainId, (byte) 17, javaDdAsStorage).build();
                    DOFAuthenticator dofAuthenticator = dof.createAuthenticator(authenticatorConfig);
                    authenticatorMap.put(config.domainId, dofAuthenticator);
                    if(config.credentials != null)
                    {
                        DOFSystem system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(config.credentials.credentials).build(), 2000);
                        system.destroy();
                        dof.resolve(config.credentials.credentials, 2000);
                    }
                }
                node.setModuleStatus(JavaDbRemoteServerModule.this, ModuleStatus.Started);
                log.info(sb.toString());
                
                try
                {
                    DOFSystem system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials01).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials02).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials03).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials04).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials05).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials06).build(), 1000);
                    system.destroy();
                    
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials11).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials12).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials13).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials14).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials15).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials16).build(), 1000);
                    system.destroy();
                    
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials21).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials22).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials23).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials24).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials25).build(), 1000);
                    system.destroy();
                    system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials26).build(), 1000);
                    system.destroy();
                }catch(Exception e)
                {
                    log.error("failed to authenticate:", e);
                }
                
                
            } catch (Exception e)
            {

                log.info(sb.toString());
                log.error("\n" + getClass().getName() + " failed to start", e);
                node.setModuleStatus(JavaDbRemoteServerModule.this, ModuleStatus.Failed);
            }
        }
    }
}    
    
