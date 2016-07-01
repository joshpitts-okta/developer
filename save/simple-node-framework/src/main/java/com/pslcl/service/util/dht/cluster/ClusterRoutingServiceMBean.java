package com.pslcl.service.util.dht.cluster;

public interface ClusterRoutingServiceMBean
{
	/**
	 * Get this node's global ID
	 * 
	 * @return the global ID as a String
	 */
	public String getGlobalID();

	/**
	 * Get the size of the cluster ring
	 * 
	 * @return size of the cluster ring
	 */
	public int getRingSize();

	/**
	 * Get the number of total work requests
	 * 
	 * @return number of work requests
	 */
	public int getAllWorkRequestsSize();

	/**
	 * Get the size of the routing table, including all virtual nodes
	 * 
	 * @return size of routing table
	 */
	public int getRoutingTableSize();

	/**
	 * Get a String representation of the routing table
	 * 
	 * @return the routing table as a String
	 */
	public String getRoutingTableString();

	/**
	 * Get a String representation of the work requests
	 * 
	 * @return the work requests as a String
	 */
	public String getWorkRequestsString();

	/**
	 * Print the routing table to the console
	 */
	public void printRoutingTable();

	/**
	 * Print the work requests to the console
	 */
	public void printWorkRequests();

	/**
	 * Print the work requests for a given node to the console
	 * 
	 * @param nodeID the node in the ring
	 */
	public void printWorkRequests(String nodeID);

	/**
	 * Print the ring to the console
	 */
	public void printRing();
}
