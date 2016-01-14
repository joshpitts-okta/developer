package com.pslcl.internal.test.simpleNodeFramework.asNode;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.RestartingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAuthenticator;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFProtocolNegotiator;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.oal.DOFServer.State;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;
import com.pslcl.internal.test.simpleNodeFramework.FileConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class ConfigurableAuthServer implements Module
{
    private static final int SERVER_MAX_RECEIVE_SILENCE_MS = 1000 * 60; // 60 seconds, from design document dsp.xml
    private static final int SERVER_MAX_SEND_SILENCE_MS = SERVER_MAX_RECEIVE_SILENCE_MS * 9 / 10; // 90%
    private static final long HEALTH_CHECK_PERIOD = 15 * 1000; // The frequency at which health checks are made.

    private final Logger logger = LoggerFactory.getLogger(ConfigurableAuthServer.class);
    private NodeConfig nodeConfig;
    private DOF dof;
    private DOFServer authServerStream;
    private DOFServer authServerUDP;
    private RestartingStateListener restartingStateListener = null;
    private DOFServer.StateListener statusStateListener = null;
    private StatusTracker statusTracker;
    private Timer timer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String fdn;

    @Override
    public void init(Configuration config) throws Exception
    {
        this.nodeConfig = (NodeConfig) config;
        fdn = nodeConfig.properties.getProperty(NodeConfiguration.FdnKey);
    }

    @Override
    public void start(Node node) throws Exception
    {
        String address = "0.0.0.0";
        logger.info("AuthServer starting on " + nodeConfig.systemConfig.getNodeID());

        //Make sure servers are destroyed if start has been called before.
        if (authServerStream != null)
            authServerStream.destroy();
        if (authServerUDP != null)
            authServerUDP.destroy();

        this.dof = nodeConfig.getDof(fdn);
        this.statusTracker = node.getStatusTracker();
        updateStatus();

        //@formatter:off
		DOFServer.Config.Builder scBuilder = new DOFServer.Config.Builder(DOFServer.Type.STREAM, InetTransport.createAddress(address, ((FileConfiguration)nodeConfig.systemConfig).getAsServerPort()))
            .setMaxReceiveSilence(SERVER_MAX_RECEIVE_SILENCE_MS)
            .setMaxSendSilence(SERVER_MAX_SEND_SILENCE_MS)
			.setSecurityDesire(SecurityDesire.NOT_SECURE)
			.setProtocolNegotiator(DOFProtocolNegotiator.createDefaultASOnly())
			.setAuditorListener(node.getConnectionAuditor());
        //@formatter:on
        DOFServer.Config serverConfigStream = scBuilder.setServerType(DOFServer.Type.STREAM).setName("Stream Auth Server - " + address).build();
        DOFServer.Config serverConfigDatagram = scBuilder.setServerType(DOFServer.Type.DATAGRAM).setName("Datagram Auth Server - " + address).build();
        authServerStream = dof.createServer(serverConfigStream);
        authServerUDP = dof.createServer(serverConfigDatagram);
        logger.info("\nAuthServer stream: " + serverConfigStream.getName() + " and udp: " + serverConfigDatagram.getName() + " started on dof: " + dof.getState().getName());
        restartingStateListener = new RestartingStateListener();
        restartingStateListener.setMinimumDelay(1000);
        restartingStateListener.setMaximumDelay(30000);
        statusStateListener = new ServerStateListener();
        authServerStream.addStateListener(statusStateListener);
        authServerUDP.addStateListener(statusStateListener);

        timer = new Timer();
        timer.scheduleAtFixedRate(new HealthCheck(), 0L, HEALTH_CHECK_PERIOD);
    }

    @Override
    public void stop(Node node) throws Exception
    {
        logger.info("AuthServer stopping on " + nodeConfig.systemConfig.getNodeID());
        timer.cancel();
        synchronized (timer)
        {
            timer = null;
            if (authServerStream != null)
            {
                authServerStream.removeStateListener(statusStateListener);
                authServerStream.removeStateListener(restartingStateListener);
            }
            if (authServerUDP != null)
            {
                authServerUDP.removeStateListener(statusStateListener);
                authServerUDP.removeStateListener(restartingStateListener);
            }
            running.set(false);
        }
        if (restartingStateListener != null)
            restartingStateListener.cancel();
    }

    @Override
    public void destroy()
    {
        if (authServerStream != null)
            authServerStream.destroy();
        if (authServerUDP != null)
            authServerUDP.destroy();
    }

    private void updateStatus()
    {
        Status updatedStatus = StatusTracker.Status.Ok;
        if (authServerStream == null || !authServerStream.isStarted())
            updatedStatus = StatusTracker.Status.Warn;
        if (authServerUDP == null || !authServerUDP.isStarted())
            updatedStatus = StatusTracker.Status.Warn;
        statusTracker.setStatus("AuthServer", updatedStatus);

        logger.trace("AuthServer.updateStatus() computes 'AuthServer' status: " + updatedStatus);
        logger.trace("AuthServer.updateStatus() sees consolidated status now as " + statusTracker.getStatus());
    }

    private class ServerStateListener implements DOFServer.StateListener
    {

        @Override
        public void stateChanged(DOFServer server, State state)
        {
            logger.debug(server.getState().getName() + " state change: now " + (server.isStarted() ? "" : "NOT ") + "started");
            updateStatus();
        }

        @Override
        public void removed(DOFServer server, DOFException exception)
        {
            statusTracker.removeStatus("AuthServer");
        }
    }

    private class HealthCheck extends TimerTask
    {

        @Override
        public void run()
        {
            synchronized (timer)
            {
                if (timer != null)
                {
                    boolean shouldRun = false;
                    for (DOFAuthenticator auth : dof.getAuthenticators())
                    {
                        if (auth.getState().isAvailable())
                        {
                            shouldRun = true;
                            break;
                        }
                    }
                    if (shouldRun && running.compareAndSet(false, true))
                    {
                        // start both servers
                        authServerStream.addStateListener(restartingStateListener);
                        authServerUDP.addStateListener(restartingStateListener);
                    }
                    if (!shouldRun && running.compareAndSet(true, false))
                    {
                        authServerStream.removeStateListener(restartingStateListener);
                        authServerUDP.removeStateListener(restartingStateListener);
                    }
                }
            }
        }
    }
}
