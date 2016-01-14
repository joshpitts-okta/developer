package com.pslcl.service.util.dht;

import com.pslcl.service.util.provide.BindingRequest;

/**
 * This interface is meant to be used for creating a distributed service for DOF Services. An implementation is intended to allow multiple
 * DOF Services to run simultaneously while providing distribution and replication of the workload.
 * This service is used with the <code>ProvideManager</code> to determine how to handle incoming activate requests.
 */
public interface DistributedActivateProvideService
{
	/**
	 * Should the caller consider providing for the given <code>BindingRequest</code>?
	 * <p>
	 * The calling <code>ProvideManager</code> has determined the given <code>BindingRequest</code> contains a <code>DOFInterfaceID</code> for which a
	 * <code>ProvideFactory</code> has expressed interest in seeing and hence could provide for the given interest and/or use it as a trigger to
	 * provide other non-reactive bindings.
	 * </p>
	 * Given the binding request, the <code>DistributedService</code> determines if the node the calling
	 * manager is on should be handling this request.
	 * </p>
	 * 
	 * @param bindingRequest the <code>BindingRequest</code> being questioned. Must not be null.
	 * @return true if the calling manager should handle the request, false otherwise.
	 */
	public boolean shouldIProvide(BindingRequest bindingRequest);

	/**
	 * The calling <code>ProvideManager</code> has canceled an activate request. The <code>DistributedService</code> uses this information to remove
	 * the binding request from its Routing Table.
	 * 
	 * @param bindingRequest the <code>BindingRequest</code> to remove from the Routing Table.
	 */
	public void removeWorkRequest(BindingRequest bindingRequest);
}
