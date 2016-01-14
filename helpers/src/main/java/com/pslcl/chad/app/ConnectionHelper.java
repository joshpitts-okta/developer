/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.Config.BuilderPoint;
import org.opendof.core.oal.DOFConnection.State;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFGroupAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.serviceUtility.status.StatusConnectionStateListener;
import com.pslcl.chad.app.serviceUtility.status.StatusTracker;

@SuppressWarnings("javadoc")
public class ConnectionHelper implements DOFConnection.StateListener
{
    public static final String ConnectionStatus = "ConnectionState";

    private final Logger log;
    private final DOFConnection connection;
    private final ReconnectingStateListener reconnector;
    private final StatusConnectionStateListener statusListener;
    private volatile StatusTracker statusTracker;

    public ConnectionHelper(StatusTracker statusTracker, DOF dof, DOFConnection.Config connConfig, int timeout)
    {
        this(statusTracker, dof, connConfig, null, timeout);
    }

    public ConnectionHelper(StatusTracker statusTracker, DOF dof, DOFConnection.Config connConfig, DOFGroupAddress groupAddress, int timeout)
    {
        log = LoggerFactory.getLogger(getClass());
        this.statusTracker = statusTracker;
        DOFConnection conn = null;
        if (connConfig.getConnectionType().equals(DOFConnection.Type.POINT))
        {
            boolean use61 = true;
            if (use61)
            {
                // @formatter:off
                conn = new BuilderPoint(groupAddress, connConfig.getCredentials())
                .setName(connConfig.getName())
                .setAuditorListener(connConfig.getAuditListener())
                .setCredentials(connConfig.getCredentials())
                .setMaxSendSilence(connConfig.getMaxSendSilence())
                .setStreamRequestListener(connConfig.getStreamRequestListener())
                .createConnection(dof, connConfig.getAddress());
                // @formatter:on
            } else
            {
                // @formatter:off
                final DOFConnection.Config connConfigDatagram = new DOFConnection.Config.Builder(
                                                                        DOFConnection.Type.DATAGRAM, connConfig.getAddress())
                                                                        .setName(connConfig.getName() + " Point_UnicastConnection")
                                                                        .setAuditorListener(connConfig.getAuditListener())
                                                                        .setProtocolNegotiator(connConfig.getProtocolNegotiator())
                                                                        .build();
                // @formatter:on
                final DOFConnection connDatagram = dof.createConnection(connConfigDatagram);

                // @formatter:off
                final DOFConnection.Config.Builder builder = new DOFConnection.Config.Builder(
                                DOFConnection.Type.POINT, groupAddress)
                                .setName(connConfig.getName() + " Point_GroupConnection")
                                .setCredentials(connConfig.getCredentials())
                                .setSecurityDesire(SecurityDesire.SECURE)
                                .setMaxReceiveSilence(connConfig.getMaxReceiveSilence())
                                .setStreamRequestListener(connConfig.getStreamRequestListener())
                                .setAuditorListener(connConfig.getAuditListener());
                                                        
                    // @formatter:on
                final DOFConnection.Config connConfigPoint = builder.build();
                conn = connDatagram.createConnection(connConfigPoint);

                log.info("UniGroup$Point.connect: nodeid=" + conn.getState().getName() + " connecting related " + connDatagram.getState().getConnectionType() + " on " + dof.getState().getName() + " to " + connDatagram.getState().getAddress());
                log.info("UniGroup$Point.connect: nodeid=" + dof + " connecting " + conn.getState().getConnectionType() + " on " + dof.getState().getName() + " to " + conn.getState().getAddress() + " with cred.identity=" + connConfig.getCredentials().getIdentity().getDataString());
            }
        } else
            conn = dof.createConnection(connConfig);

        connection = conn;
        connection.addStateListener(this);
        reconnector = new ReconnectingStateListener();
        connection.addStateListener(reconnector);
        statusListener = new StatusConnectionStateListener();
        statusListener.init(statusTracker, ConnectionStatus);
        connection.addStateListener(statusListener);
        connection.beginConnect(timeout, null, null);
    }

    public boolean isConnected()
    {
        return connection.isConnected();
    }

    public void addListener(DOFConnection.StateListener listener)
    {
        connection.addStateListener(listener);
    }

    public void destroy()
    {
        reconnector.cancel();
        connection.disconnect();
        connection.destroy();
    }

    /* *************************************************************************
     * DOFConnection.StateListener implementation
     **************************************************************************/
    @Override
    public void removed(DOFConnection connection, DOFException exception)
    {
        statusTracker.removeStatus(connection.getState().getName());
    }

    @Override
    public void stateChanged(DOFConnection connection, State state)
    {
        //Called when the status of the connectToCloud DOFConnection changes
        String msg = " is connected";
        if (state.isConnected())
            statusTracker.setStatus(connection.getState().getName(), StatusTracker.Status.Ok);
        else
        {
            msg = " is disconnected";
            statusTracker.setStatus(connection.getState().getName(), StatusTracker.Status.Warn);
        }
        log.info("\nconnection: " + connection.getState().getName() + msg);
    }
}
