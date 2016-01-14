/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import org.opendof.core.RestartingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.oal.DOFServer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.serviceUtility.status.StatusTracker;

@SuppressWarnings("javadoc")
public class ServerHelper implements DOFServer.StateListener
{
    public static final String ConnectionStatus = "ConnectionState";

    private final Logger log;
    private final DOFServer server;
    private RestartingStateListener restartingStateListener = null;
    private volatile StatusTracker statusTracker;

    public ServerHelper(StatusTracker statusTracker, DOF dof, DOFServer.Config serverConfig, int timeout)
    {
        log = LoggerFactory.getLogger(getClass());
        this.statusTracker = statusTracker;
        server = dof.createServer(serverConfig);
        
        restartingStateListener = new RestartingStateListener();
        restartingStateListener.setMinimumDelay(1000);
        restartingStateListener.setMaximumDelay(30000);
//        server.addStateListener(restartingStateListener);
        server.addStateListener(this);
        server.beginStart(timeout);
    }

    public boolean isStarted()
    {
        return server.isStarted();
    }

    public void addListener(DOFServer.StateListener listener)
    {
        server.addStateListener(listener);
    }

    public void destroy()
    {
        restartingStateListener.cancel();
        server.removeStateListener(restartingStateListener);
        server.removeStateListener(this);
        server.stop();
        server.destroy();
    }

    /* *************************************************************************
     * DOFServer.StateListener implementation
     **************************************************************************/
    @Override
    public void removed(DOFServer server, DOFException exception)
    {
        statusTracker.removeStatus(server.getState().getName());
    }

    @Override
    public void stateChanged(DOFServer server, State state)
    {
        //Called when the status of the connectToCloud DOFConnection changes
        String msg = " is started";
        if (state.isStarted())
            statusTracker.setStatus(server.getState().getName(), StatusTracker.Status.Ok);
        else
        {
            msg = " is stopped";
            statusTracker.setStatus(server.getState().getName(), StatusTracker.Status.Warn);
        }
        log.info("\nserver " + server.getState().getName() + msg);
    }
}
