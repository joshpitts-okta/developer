/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import org.opendof.core.oal.DOFOperation;


/**
 * Provider Info
 * <p>
 * <code>ProvideInfo</code> contains the information for which a <code>ProvideManager</code> has
 * obtained <code>ProvideRequests</code> from a <code>ProvideFactory</code> and then successfully
 * done a begin provide on.
 * </p>
 * The <code>ProvideManager</code> has several search methods which allow the application to 
 * obtain immutable instances of this class.  Those searches will typically require a 
 * <code>BindingRequest</code> which will then return these objects allowing for the application
 * to obtain the <code>DOFOperation.Provide</code> obtain to then be able to manually control
 * the provide outside of the <code>ProvideManager</code>.
 * </p>
 * Note that for a <code>Provide</code> implementation to support subscriptions they will need to
 * obtain the <code>DOFObject</code> from the provideOperation captured in this class.
 * 
 * @see ProvideManager
 * @see ProvideFactory
 * @see BindingRequest
 */
public final class ProviderInfo
{
    private final ProvideRequest provideRequest;
    private final DOFOperation.Provide provideOperation;
    private final ProvideFactory factory;

    /**
     * Provider Info constructor
     * 
     * @param factory the <code>ProvideFactory</code> which requested this provide.  Must not be null.
     * @param provideRequest the <code>ProvideRequest</code> given from the factory. Must not be null.
     * @param provideOperation the <code>DOFOperation.Provide</code> for the provide. Must not be null.
     * @throws IllegalArgumentException if factory, provideRequest or provideOperation is null.
     */
    public ProviderInfo(ProvideFactory factory, ProvideRequest provideRequest, DOFOperation.Provide provideOperation)
    {
        if (factory == null || provideRequest == null || provideOperation == null)
            throw new IllegalArgumentException("factory == null || provideRequest == null || provideOperation == null");
        this.factory = factory;
        this.provideRequest = provideRequest;
        this.provideOperation = provideOperation;
    }

    /**
     * Get Factory
     * @return the <code>ProvideFactory</code> that requested this provide.  Will never return null.
     */
    public ProvideFactory getFactory()
    {
        return factory;
    }
    
    /**
     * Get Provide Request
     * @return the <code>ProvideRequest</code> associated with this provide.  Will never return null.
     */
    public ProvideRequest getProvideRequest()
    {
        return provideRequest;
    }
    
    /**
     * Get the Provide Operation.
     * @return the provide operation associated with this provide.  Will never return null.  
     */
    public DOFOperation.Provide getProvideOperation()
    {
        return provideOperation;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = prime * ((provideOperation == null) ? 0 : provideOperation.hashCode());
        result = prime * result + ((provideRequest == null) ? 0 : provideRequest.hashCode());
        result = prime * result + ((factory == null) ? 0 : factory.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof ProviderInfo))
            return false;
        ProviderInfo other = (ProviderInfo) obj;
        if (provideOperation == null)
        {
            if (other.provideOperation != null)
                return false;
        } else if (!provideOperation.equals(other.provideOperation))
            return false;
        if (provideRequest == null)
        {
            if (other.provideRequest != null)
                return false;
        } else if (!provideRequest.equals(other.provideRequest))
            return false;
        if (factory == null)
        {
            if (other.factory != null)
                return false;
        } else if (!factory.equals(other.factory))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        //@formatter:off
        return "provideRequest: " + provideRequest.toString() + 
               " provideOperation: " + provideOperation.toString() + 
               " factory: " + factory.toString();
        //@formatter:off
    }
}

