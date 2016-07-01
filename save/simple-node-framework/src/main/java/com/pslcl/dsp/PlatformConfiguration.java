package com.pslcl.dsp;

public interface PlatformConfiguration {

	/**
	 * Initialize the configuration. This method must be called after instantiation prior to any other method.
	 * Upon return, all details of the configuration must be thoroughly instantiated and be within correct, acceptable limits.
	 * 
	 * @throws Exception Thrown if any configuration values are unspecified or outside acceptable limits.
	 */
	void init() throws Exception;

	/**
	 * Retrieve the base configuration path for the platform.
	 * @return The base configuration path for the platform.
	 */
	String getBaseConfigurationPath();

	/**
	 * Retrieve the base secure configuration path for the platform.
	 * @return The base secure configuration path for the platform.
	 */
	String getBaseSecureConfigurationPath();

}
