package com.pslcl.dsp;

import org.opendof.core.oal.DOFObjectID;

public interface UniqueAuthenticatorIDResolver {

	/**
	 * Determine a unique authenticator ID for a DOFAuthenticator instance for the given domain.
	 * This is a number between 0 and 63, inclusive, and must be unique to an instance of a DOFAuthenticator.
	 * This method will be called periodically (once per minute) for each domain ID & node ID combination.
	 * It is expected that the returned identifier will not change frequently.
	 * @param domainID The domain ID.
	 * @param nodeID The node ID requesting the number.
	 * @throws Exception if the authenticator ID for the given domain ID and node ID could not be determined.
	 * @return A unique authenticator ID between 0 and 63 for a DOFAuthenticator instance for the given domain.
	 */
	public byte getAuthenticatorID(DOFObjectID.Domain domainID, DOFObjectID nodeID) throws Exception;
	
	/**
	 * Release an authenticator ID previously obtained.
	 * No action taken if the ID is already no longer assigned.
	 * @param domainID The domain ID.
	 * @param nodeID The node ID that requested the number.
	 * @param authenticatorID The authenticator ID obtained previously.
	 */
	public void releaseAuthenticatorID(DOFObjectID.Domain domainID, DOFObjectID nodeID, byte authenticatorID);

}
