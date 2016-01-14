/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import java.util.List;

import org.opendof.core.oal.DOFOperation.Provide;

import com.pslcl.service.util.Service;

/**
 * The Provide Factory interface.
 * <p>
 * An interface the application level (<code>Service</code>) must implement to interact with the 
 * Provide manager. This interface declares a factory of <code>ProvideRequest</code> 
 * objects which the <code>ProvideManager</code> requires to begin and manage the 
 * actual provides with.
 * </p> 
 * The implementation will use the <code>BindingRequest</code> supplied by the <code>ProvideManager</code>
 * to determine what <code>Provide</code>s are desired in connection to the triggering request.  Instances 
 * of the <code>Provide</code>'s, their <code>DOFInterface</code>'s and any desired context objects  
 * will need to be provided by the factory.
 * </p>     
 * Typically the <code>Provide</code> implementations will also implement the <code>GracePeriodProvider</code>
 * interface which will allow the provide manager to reduce interest bouncing causing
 * unnecessary creation/shutdown of these <code>ProvideRequests</code> and their subsequent provides.
 * </p>
 * An application (typically a <code>Service</code>) registers <code>ProvideFactory</code>s with the <code>ProvideManager</code> 
 * by calling its addFactory method with an instance of the factory and a list of <code>DOFInterfaceID</code>s
 * the factory is interested in.  Although not typical, an application could have multiple ProvideManagers
 * of which a given factory could be registered with one or more of them.
 * </p> 
 * The supplied <code>Provide</code> objects must be ready to have their implemented interfaces called at any time 
 * prior to returning the list of <code>ProvideRequests</code>.  The <code>ProvideManager</code> can be expected 
 * to call begin provide shortly after returning and then manage the lifecycle of these provides based on ongoing
 * interest in the original <code>BindingRequest</code>'s triggering interest.
 * </p>
 * If the manager this factory has been registered with is going down or the factory has been removed from the manager, 
 * the removed method will be called to notify the factory.
 * 
 * @see ProvideManager
 * @see ProvideRequest
 * @see Provide
 * @see GracePeriodProvider
 * @see BindingRequest
 * @see Service
 */
public interface ProvideFactory
{
    
    /**
     * Get Provide Request Set.
     * </p>
     * When the <code>ProvideManager</code> receives a reactive interest from its 
     * registered <code>DOFSystem.ActivateInterestListener</code> for <code>DOFInterfaceID</code>s 
     * which have been registered with this factory it will call this method to obtain the
     * needed list of <code>ProvideRequest</code>s this factory desires to supply for the 
     * triggering interest.
     * </p>
     * This method will be called on a <code>ProvideManager</code>'s executor thread allowing
     * the implementation to block as needed.
     * @param manager the <code>ProvideManager</code> making the call.  Must not be null.
     * @param request the <code>BindingRequest</code> capturing the reactive interest trigger.  Must not be null.
     * @return list of <code>ProvideRequest</code> objects.  Must not be null, maybe an empty list.
     */
    public List<ProvideRequest> getProvideRequestSet(ProvideManager manager, BindingRequest request);
    
    
    /**
     * Provide is Active.
     * <p>
     * After a <code>List<ProvideRequest</code> has been returned to the <code>ProvideManager</code> from 
     * the <code>getProvideRequestSet</code> method of this interface, the <code>ProvideManager</code> will
     * callback to this method when the <code>beginProvide</code> has been called and the provide is active.
     * @param providerInfo the <code>ProvideInfo</code> associated with the provide that was started.
     */
    public void provideActive(ProviderInfo providerInfo);
    
    /**
     * This factory has been removed from a <code>ProvideManager</code> or the manager is going down.
     * @param manager the manager which this factory has been removed from.  Must not be null.
     */
    public void removed(ProvideManager manager);
}
