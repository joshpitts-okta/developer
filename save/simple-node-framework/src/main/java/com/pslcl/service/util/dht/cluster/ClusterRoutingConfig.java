package com.pslcl.service.util.dht.cluster;

import org.opendof.core.oal.DOFObjectID;

import com.pslcl.service.util.provide.ProvideManagerConfig;

/**
 * This is an implementation of a custom config object that extends <code>ProvideManagerConfig</code>.
 * This is used together with <code>ClusterRoutingService</code> to configure a node in a cluster.
 */
public class ClusterRoutingConfig extends ProvideManagerConfig
{
	private final DOFObjectID baseServiceID;
	private final DOFObjectID clusterID;
	private final int vnodes;
	private final String guid;
	private final int replicationFactor;
	private final int removeNodePeriod; // milliseconds
	private final int operationTimeout; // milliseconds

	/**
	 * 
	 * @param config
	 * @param baseServiceID the base service id
	 * @param clusterID the id of the ring to join
	 * @param vnodes the number of virtual nodes
	 * @param guid the guid to use for non-random global id
	 * @param operationTimeout operation timeout
	 * @param replicationFactor number of replicas per work
	 * @param removeNodePeriod period to wait when a node drops from the ring before distributing its work
	 */
	public ClusterRoutingConfig(ProvideManagerConfig config, DOFObjectID baseServiceID, DOFObjectID clusterID, int vnodes, String guid,
			int operationTimeout, int replicationFactor, int removeNodePeriod)
	{
		super(config);
		if (baseServiceID == null || clusterID == null || vnodes < 1 || operationTimeout < 1 || replicationFactor < 1 || removeNodePeriod < 1)
			throw new IllegalArgumentException(
					"baseServiceID == null || clusterID == null || vnodes < 1 || operationTimeout < 1 || replicationFactor < 1 || removeNodePeriod < 1");
		this.baseServiceID = baseServiceID;
		this.clusterID = clusterID;
		this.vnodes = vnodes;
		this.guid = guid;
		this.operationTimeout = operationTimeout;
		this.replicationFactor = replicationFactor;
		this.removeNodePeriod = removeNodePeriod;
	}

	/**
	 * @param config
	 * @param baseServiceID the base service id
	 * @param clusterID the id for the ring to join
	 */
	public ClusterRoutingConfig(ProvideManagerConfig config, DOFObjectID baseServiceID, DOFObjectID clusterID)
	{
		super(config);
		if (baseServiceID == null || clusterID == null)
			throw new IllegalArgumentException("baseServiceID == null || clusterID == null");
		this.baseServiceID = baseServiceID;
		this.clusterID = clusterID;
		this.vnodes = 256;
		this.guid = "";
		this.operationTimeout = 30000;
		this.replicationFactor = 1;
		this.removeNodePeriod = 3600000;
	}

	/**
	 * Returns the base service ID, which identifies a single service.
	 * 
	 * @return the base service ID
	 */
	public DOFObjectID getBaseServiceID()
	{
		return baseServiceID;
	}

	/**
	 * Returns the cluster ID, which identifies a unique cluster.
	 * 
	 * @return the cluster ID
	 */
	public DOFObjectID getClusterID()
	{
		return clusterID;
	}

	/**
	 * Returns the number of virtual nodes to configure.
	 * 
	 * @return number of virtual nodes
	 */
	public int getVnodes()
	{
		return vnodes;
	}

	/**
	 * Returns the guid to use to configure a cluster service id. May be null or an empty string.
	 * 
	 * @return the guid string or null
	 */
	public String getGuid()
	{
		return guid;
	}

	/**
	 * Return the operation timeout.
	 * 
	 * @return the operation timeout
	 */
	public int getOperationTimeout()
	{
		return operationTimeout;
	}

	/**
	 * Return the replication factor.
	 * 
	 * @return the replication factor.
	 */
	public int getReplicationFactor()
	{
		return replicationFactor;
	}

	/**
	 * Return the amount of time in milliseconds to wait before removing a dropped node from the ring.
	 * 
	 * @return the period to wait for a dropped node to return to the ring.
	 */
	public int removeNodePeriod()
	{
		return removeNodePeriod;
	}
}
