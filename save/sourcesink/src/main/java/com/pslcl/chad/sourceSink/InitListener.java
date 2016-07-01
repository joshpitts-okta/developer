package com.pslcl.chad.sourceSink;

/**
 * Callback used to indicate initialization.
 * @author dethington
 *
 */
public interface InitListener {
	/**
	 * Called when object {@link InitListener} has been passed to is initialized.
	 * Indicates that objects and methods that depend on the initialized object can 
	 * now be used.
	 */
	public boolean isInitialized();

	public void lostConnection();

	public void reconnected();
}
