/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

/**
 * The Outstanding Interest Handler interface.
 * <p>
 * An application can register an <code>OutstandingInterestHandler</code> with the 
 * <code>ProvideManager.setOutstandingInterestHandler</code> method.
 * </p>
 * It is possible that a reactive interest is seen which was been registered for by one or more 
 * <code>ProvideFactory</code>s and yet none of the registered factories supply any <code>ProvideRequest</code>s
 * at the time of the interest.  It is also possible that a given <code>ProvideRequest</code> fails when the 
 * <code>ProvideManager</code> attempts the begin provide on it.  In these cases, a registered <code>OutstandingInterestHandler</code>
 * will be called to notify the application of these conditions.
 * </p>
 * The application may have a means of resolving why no <code>ProvideRequest</code>s were given or why the begin provide failed.
 * The <code>ProvideManager.retryActivateRequest</code> method can then be called by the application to retry the request.
 * 
 * @see ProvideManager#setOutstandingInterestHandler(OutstandingInterestHandler)   
 * @see ProvideManager#retryActivateRequest(BindingRequest)   
 * @see ProvideFactory#getProvideRequestSet(ProvideManager, BindingRequest)
 */
public interface OutstandingInterestHandler
{
    /**
     * Outstanding Interest.
     * <p>
     * Notification that no <code>Provide</code> was given for a registered <code>DOFInterfaceID</code>
     * @param bindingRequest the <code>BindingRequest</code> that is missing a provide.  Will never be null.
     */
    public void outstandingInterest(BindingRequest bindingRequest);
    
    /**
     * Begin Provide Failed.
     * <p>
     * Notification that the begin provide failed.
     * @param bindingRequest the <code>BindingRequest</code> that is missing a provide.  Will never be null.
     * @param exception the exception thrown trying to execute the begin provide.  Will never be null.
     */
    public void beginProvideFailed(BindingRequest bindingRequest, Exception exception);
}
