package com.pslcl.dsp;

public class Configuration {

	public final PlatformConfiguration platformConfig;
	
	public final SystemConfiguration systemConfig;
	
	public Configuration(PlatformConfiguration platformConfig, SystemConfiguration systemConfig) {
		this.platformConfig = platformConfig;
		this.systemConfig = systemConfig;
	}
	
}
