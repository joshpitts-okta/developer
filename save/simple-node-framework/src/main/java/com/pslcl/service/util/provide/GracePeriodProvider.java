/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import org.opendof.core.oal.DOFObject.Provider;


/**
 * The Grace period Provider Interface.
 * </p>
 * This interface provides the <code>ProvideManager</code> a means of 
 * obtaining the services desired grace period for a given <code>Provide</code>
 * implementation.
 * </p>
 * The implementation of this interface will typically extend <code>DOFObject.DefaultProvider</code>
 * to meet the <code>Provider</code> interface being complemented here. 
 * @see ProvideManager 
 * @see ProvideFactory
 */
public interface GracePeriodProvider extends Provider
{
    /**
     * Get grace period.
     * </p>
     * Return the grace period that should be allowed a reactive EMIT Object if it's 
     * interest has been lost before the <code>ProvideManager</code> drops 
     * the provide.
     * @param bindingRequest The bindingRequest that is being canceled.  Must not be null
     * @return The number of milliseconds to delay before this provide will be torn 
     * down.  Must not be negative.
     * @throws Exception if this EMIT provide object could not obtain the gracePeriod.
     */
    public int getGracePeriod(BindingRequest bindingRequest) throws Exception;
}
