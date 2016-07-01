package com.pslcl.dsp;

import org.opendof.core.oal.DOFObjectID;

public interface SystemConfiguration {
	
	/**
	 * Initialize the configuration. This method must be called after instantiation prior to any other method.
	 * Upon return, all details of the configuration must be thoroughly instantiated and be within correct, acceptable limits.
	 * 
	 * @throws Exception Thrown if any configuration values are unspecified or outside acceptable limits.
	 */
	void init(PlatformConfiguration platformConfig) throws Exception;

	/**
	 * Retrieve the configured node ID.
	 * @return The DOFObjectID held by this configuration identifying the node.
	 */
	DOFObjectID getNodeID();
	
	/**
	 * Retrieve the configured system ID.
	 * @return The system ID held by this configuration.
	 */
	String getSystemID();

	/**
	 * Retrieve the configured connection count threshold.
	 * @return The connection count threshold held by this configuration.
	 */
	int getConnectionCountThreshold();
	
	/**
	 * Retrieve the configured threadpool size threshold.
	 * @return The threadpool size threshold held by this configuration.
	 */
	int getThreadpoolSizeThreshold();

}
