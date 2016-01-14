/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFSystem;
import org.pslcl.service.status.StatusTracker;

import com.pslcl.service.util.dht.cluster.ClusterRoutingService;


/**
 * ProvideManager configuration data class.
 * </p>
 * Provides all possible configuration parameters available to the ProvideManager.
 * This class provides an immutable data object.
 */
public class ProvideManagerConfig
{
    private final DOF dof;
    private final DOFSystem system;
    private final DOFCredentials credentials;
    private final int authTimeout;
    private final List<Domain> domains;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final ProvideManager manager;
    private final StatusTracker statusTracker;
    private ClusterRoutingService dhtService;

    /**
     * Provide Manager Config data class.
     * @param manager the ProvideManager initialized with this config.
     * @param dof the dof for the manager to use.  Must not be null.
     * @param system the tunneling enabled <code>DOFSystem</code> to be used by the <code>ProvideManager</code> and <code>DHTService</code>.
     * @param credentials the <code>DOFCredentials</code> to be used for creating <code>DOFSystem</code>s in the specific domains. 
     * @param authTimeout the authentication timeout used by the manager. 
     * @param domains the list of cross domains that the manager should allow.  May be null or empty.
     * @param executor the executor service the manager will use.  Must not be null.
     * @param scheduledExecutor the scheduled executor service the manager will use.  Must not be null.
     * @param statusTracker the application wide <code>StatusTracker</code>. Must not be null.
     * @throws IllegalArgumentException if any of the input parameters are invalid.
     */
    public ProvideManagerConfig(ProvideManager manager, DOF dof, DOFSystem system, DOFCredentials credentials, int authTimeout, List<Domain> domains, ExecutorService executor, ScheduledExecutorService scheduledExecutor, StatusTracker statusTracker)
    {
        if (manager == null || dof == null || system == null || credentials == null || executor == null || scheduledExecutor == null)
            throw new IllegalArgumentException("manager == null || dof == null || systemConfig == null || credentials == null || executor == null || scheduledExecutor == null");
        this.manager = manager;
        this.dof = dof;
        this.system = system;
        this.credentials = credentials;
        this.authTimeout = authTimeout;
        this.domains = domains;
        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.statusTracker = statusTracker;
    }
    
    protected ProvideManagerConfig(ProvideManagerConfig config)
    {
        this.manager = config.manager;
        this.dof = config.dof;
        this.system = config.system;
        this.credentials = config.credentials;
        this.authTimeout = config.authTimeout;
        this.domains = config.domains;
        this.executor = config.executor;
        this.scheduledExecutor = config.scheduledExecutor;
        this.statusTracker = config.statusTracker;
    }


    /**
     * The <code>ProvideManager</code> initialized with this configuration;
     * @return the <code>ProvideManager</code>.  Will not be null.
     */
    public ProvideManager getManager()
	{
		return manager;
	}

	/**
     * Return the DOF.
     * @return the <code>DOF</code> Will not return null.
     */
    public DOF getDof()
    {
        return dof;
    }

    /**
     * Return the <code>DOFSystem</code>
     * @return the <code>DOFSystem</code>. Will not return null.
     */
    public DOFSystem getSystem()
    {
        return system;
    }

    /**
     * Return the <code>DOFCredentials</code>.
     * @return the <code>DOFCredentials</code>. Will not return null.
     */
    public DOFCredentials getCredentials()
    {
        return credentials;
    }

    /**
     * Return the authentication timeout.
     * @return the authentication timeout.
     */
    public int getAuthTimeout()
    {
        return authTimeout;
    }

    /**
     * Return the list of trusted domains.
     * @return the list of trusted domains.  Will not return null, may return an empty set.
     */
    public List<Domain> getDomains()
    {
        return domains;
    }

    /**
     * Return the Executor.
     * @return the ExecutorService.  Will not return null.
     */
    public ExecutorService getExecutor()
    {
        return executor;
    }

    /**
     * Return the Scheduled Executor Service.
     * @return the ScheduledExecutorService.  Will not return null.
     */
    public ScheduledExecutorService getScheduledExecutor()
    {
        return scheduledExecutor;
    }
    
    /**
     * Return the DHT Service.
     * @return the <code>ClusterRoutingService</code>.  May be null if not enabled.
     */
    public synchronized ClusterRoutingService getDhtService()
    {
        return dhtService;
    }
    
    /**
     * Set the the DHT Service.
     * @param the <code>ClusterRoutingService</code> to use.  May be null if not enabled.
     */
    public synchronized void setDhtService(ClusterRoutingService dhtService)
    {
        this.dhtService = dhtService;
    }
    
    /**
     * Return the Status Tracker.
     * @return the applications <code>StatusTracker</code>.
     */
    public StatusTracker getStatusTracker()
    {
        return statusTracker;
    }
}