package com.pslcl.internal.test.simpleNodeFramework.crNode;

import java.io.File;

import org.opendof.core.RestartingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.AvailableEvent;
import com.pslcl.internal.test.simpleNodeFramework.FileConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ConfigurableWanServer implements Module
{
    private static final int SERVER_START_TIMEOUT = 1000 * 5;
    private static final int SERVER_MAX_RECEIVE_SILENCE_MS = 1000 * 60; // 60 seconds, from design document dsp.xml
    private static final int SERVER_MAX_SEND_SILENCE_MS = SERVER_MAX_RECEIVE_SILENCE_MS * 9 / 10; // 90%

    private final Logger log = LoggerFactory.getLogger(ConfigurableWanServer.class);

    private DOF dof;
    private DOFServer emitServerStream;
    private DOFServer emitServerUDP;
    private RestartingStateListener restartingStateListener = null;
    private DOFServer.StateListener statusStateListener = null;
    private StatusTracker statusTracker;
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
    private FileConfiguration config;
    private NodeConfig nodeConfig;
    private volatile String fdn;
    private volatile String securityBase;

    public ConfigurableWanServer()
    {
        System.out.println("look here");
    }
    
    @Override
    public void init(Configuration config) throws Exception
    {
        nodeConfig = (NodeConfig)config;
        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);

        this.config = (FileConfiguration)config.systemConfig;
        securityBase = config.platformConfig.getBaseSecureConfigurationPath() + File.separator;

        String credentialsFile = securityBase + this.config.getCrServerCredentialsFile();
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
    }

    @Override
    public void start(Node node) throws Exception
    {
        String address = "0.0.0.0";
        log.debug("WANServer starting on " + nodeConfig.systemConfig.getNodeID());

        //Make sure servers are destroyed if start has been called before.
        if (emitServerStream != null)
            emitServerStream.destroy();
        if (emitServerUDP != null)
            emitServerUDP.destroy();

        this.dof = nodeConfig.getDof(fdn);
        this.statusTracker = node.getStatusTracker();

        //@formatter:off
        DOFServer.Config.Builder scBuilder = new DOFServer.Config.Builder(DOFServer.Type.STREAM, InetTransport.createAddress(address, config.getCrServerPort()))
            .setSecurityDesire(SecurityDesire.SECURE_ANY)
            .addCredentials(credentials01)
            .setTunnelDomains(true)
            .setMaxReceiveSilence(SERVER_MAX_RECEIVE_SILENCE_MS)
            .setMaxSendSilence(SERVER_MAX_SEND_SILENCE_MS)
            .addTrustedDomains(DOFObjectID.DOMAIN_BROADCAST) // Temporary work-around for blasting TRP to connected devices...
            .setAuditorListener(node.getConnectionAuditor())
            .setDomainDiscoveryCredentials(credentials01)
            .setWildcardCredentials(credentials01);
        
        //@formatter:on
        DOFServer.Config serverConfigStream = scBuilder.setServerType(DOFServer.Type.STREAM).setName("Stream EMIT Server - " + address).build();
        DOFServer.Config serverConfigDatagram = scBuilder.setServerType(DOFServer.Type.DATAGRAM).setName("Datagram EMIT Server - " + address).build();
        emitServerStream = dof.createServer(serverConfigStream);
        emitServerUDP = dof.createServer(serverConfigDatagram);
        restartingStateListener = new RestartingStateListener();
        restartingStateListener.setMinimumDelay(1000);
        restartingStateListener.setMaximumDelay(30000);
        statusStateListener = new ServerStateListener();

        // start both servers
        StartOperationListener startOperationListener = new StartOperationListener();
        emitServerStream.beginStart(SERVER_START_TIMEOUT, startOperationListener, emitServerStream);
        emitServerUDP.beginStart(SERVER_START_TIMEOUT, startOperationListener, emitServerUDP);
        log.info("\nWanServers stream: " + serverConfigStream.getName() + " and udp: " + serverConfigDatagram.getName() + " started on dof: " + dof.getState().getName() + " with creds: " +credentials01);
        
        StartupTask task = new StartupTask();
        task.start();
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug("WANServer stopping on " + node.getDOF());
        if (restartingStateListener != null)
            restartingStateListener.cancel();
        if (emitServerStream != null)
            emitServerStream.stop();
        if (emitServerUDP != null)
            emitServerUDP.stop();
    }

    @Override
    public void destroy()
    {
        if (emitServerStream != null)
        {
            emitServerStream.destroy();
            emitServerStream = null;
        }
        if (emitServerUDP != null)
        {
            emitServerUDP.destroy();
            emitServerUDP = null;
        }
    }

    private class StartOperationListener implements DOFServer.StartOperationListener
    {
        @Override
        public void complete(DOFOperation operation, DOFException exception)
        {
            // here for conn.beginConnect() complete, whether successful or not
            DOFServer serv = (DOFServer) operation.getContext();
            log.debug("server beginStart() for " + serv.getState().getName() + " completes: is " + (serv.isStarted() ? "" : "NOT ") + "started");
            if (exception != null)
                log.debug("beginStart() for " + serv.getState().getName() + " sees exception message : " + exception.getMessage());
            serv.addStateListener(restartingStateListener); // auto restart at a schedule
            serv.addStateListener(statusStateListener);
        }
    }

    private class ServerStateListener implements DOFServer.StateListener
    {

        @Override
        public void stateChanged(DOFServer server, DOFServer.State state)
        {
            log.debug(server.getState().getName() + " state change: now " + (server.isStarted() ? "" : "NOT ") + "started");
            statusTracker.setStatus(server.getState().getName(), (server.isStarted() ? StatusTracker.Status.Ok : StatusTracker.Status.Warn));
            if(server.isStarted())
                SimpleNodeFramework.getSnf().fireEvent(new AvailableEvent(ConfigurableWanServer.class));
        }

        @Override
        public void removed(DOFServer server, DOFException exception)
        {
            statusTracker.removeStatus(server.getState().getName());
        }
    }
    
    private class StartupTask extends Thread
    {
        @Override
        public void run()
        {
            try
            {
//                SubsystemConfig subSystemConfig = nodeConfig.getSubsystemConfig(fdn);
//                String name = StrH.getAtomicName(fdn, '.');
//                CredConnSysConfig ccsConfig = subSystemConfig.getCredConnSysConfig(name);
//                DOFCredentials creds = ccsConfig.credConfig.credentials;
                DOFSystem system = nodeConfig.getCredConnSysConfig(fdn).getSystemConfig("d0bffbff").getSystem();
                system.destroy();
                dof.resolve(credentials01, 2000);
//                log.info("authenticated ok: " + creds.toString());
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials02).build(), 1000);
                system.destroy();
                dof.resolve(credentials02, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials03).build(), 1000);
                system.destroy();
                dof.resolve(credentials03, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials04).build(), 1000);
                system.destroy();
                dof.resolve(credentials04, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials05).build(), 1000);
                system.destroy();
                dof.resolve(credentials05, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials06).build(), 1000);
                system.destroy();
                dof.resolve(credentials06, 2000);
                
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials11).build(), 1000);
                system.destroy();
                dof.resolve(credentials11, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials12).build(), 1000);
                system.destroy();
                dof.resolve(credentials12, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials13).build(), 1000);
                system.destroy();
                dof.resolve(credentials13, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials14).build(), 1000);
                system.destroy();
                dof.resolve(credentials14, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials15).build(), 1000);
                system.destroy();
                dof.resolve(credentials15, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials16).build(), 1000);
                system.destroy();
                dof.resolve(credentials16, 2000);
                
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials21).build(), 1000);
                system.destroy();
                dof.resolve(credentials21, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials22).build(), 1000);
                system.destroy();
                dof.resolve(credentials22, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials23).build(), 1000);
                system.destroy();
                dof.resolve(credentials23, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials24).build(), 1000);
                system.destroy();
                dof.resolve(credentials24, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials25).build(), 1000);
                system.destroy();
                dof.resolve(credentials25, 2000);
                system = dof.createSystem(new DOFSystem.Config.Builder().setCredentials(credentials26).build(), 1000);
                system.destroy();
                dof.resolve(credentials26, 2000);
            }catch(Exception e)
            {
                log.error("failed to authenticate:", e);
                SimpleNodeFramework.getSnf().setModuleStatus(ConfigurableWanServer.this, ModuleStatus.Failed);
            }
        }
    }
}
