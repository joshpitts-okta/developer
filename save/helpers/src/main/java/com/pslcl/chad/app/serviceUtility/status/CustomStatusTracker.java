/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app.serviceUtility.status;




/**
 * A configurable application status tracking interface.
 * </p>
 * This interface extends the <code>StatusTracker</code> interface with an init method
 * which takes a custom configuration object.  The configuration object is typically 
 * declared and typed by the <code>Service <code>T</code> implementation. 
 * @param <T> the custom <code>Service</code> configuration implementation object .
 * @see Service
 */
public interface CustomStatusTracker<T> extends StatusTracker
{
    /**
     * Initialize the status tracker with the given custom configuration object. 
     * @param config the custom configuration object.  Can be declared <code>&ltVoid&gt</code>.
     */
    public void init(T config);
}
