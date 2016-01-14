package com.pslcl.dsp;

public interface Module {

	/**
	 * Initialize the module.
	 * This is called with elevated privileges and may perform actions (such as reading files) that are otherwise protected.
	 * All other methods in this interface are called with lower privileges.
	 * @param config The Node configuration.
	 * @throws Exception
	 */
	public void init(Configuration config) throws Exception;

    /**
     * Start the module.
     * @param node The Node.
     * @throws Exception
     */
	public void start(Node node) throws Exception;

	/**
	 * Stop the module.
     * @param node The Node.
	 * @throws Exception
	 */
	public void stop(Node node) throws Exception;

	/**
	 * Destroy the module.
	 */
	public void destroy();

}
