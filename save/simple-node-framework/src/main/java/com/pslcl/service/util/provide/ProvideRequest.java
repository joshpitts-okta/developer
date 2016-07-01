/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFObject.Provider;
import org.opendof.core.oal.DOFOperation.Provide;

import com.pslcl.service.util.Service;

/**
 * The Provide Request.
 * <p>
 * A concrete immutable object the application level (<code>ProvideFactory</code>) will provide 
 * the <code>ProvideManager</code> with the needed information for it to begin and 
 * manage <code>Provide</code> implementations for the application.
 * </p> 
 * A <code>ProvideManager</code> will supply the <code>ProvideFactory</code> with the needed
 * <code>BindingRequest</code> generated from a reactive interest being seen for a <code>DOFInterfaceID</code>
 * which the factory originally registered with the manager for.  The factory will then determine what 
 * provides, if any, that it will request based on this reactive triggering interest.  A set of <code>ProvideRequest</code> 
 * instances of the <code>Provide</code>'s, their <code>DOFInterface</code>'s and any desired context objects  
 * the factory desires the manager to begin and manage provides for are then returned to the <code>ProvideManager</code>.
 * </p>     
 * Typically the <code>Provide</code> implementations will also implement the <code>GracePeriodProvider</code>
 * interface which will allow the provide manager to reduce interest bouncing causing
 * unnecessary creation/shutdown of these <code>ProvideRequests</code> and their subsequent provides.
 * </p>
 * The supplied <code>Provide</code> objects must be ready to have their implemented interfaces called at any time 
 * prior to returning the list of <code>ProvideRequests</code>.  The <code>ProvideManager</code> can be expected 
 * to call begin provide shortly after returning and then manage the lifecycle of these provides based on ongoing
 * interest in the original <code>BindingRequest</code>'s triggering interest.
 * 
 * @see ProvideManager#addFactory(ProvideFactory, java.util.Set)
 * @see ProvideFactory#getProvideRequestSet(ProvideManager, BindingRequest)
 * @see Provide
 * @see GracePeriodProvider
 * @see BindingRequest
 * @see Service
 */
public final class ProvideRequest
{
    private final BindingRequest bindingRequest;
    private final DOFInterface dofInterface;
    private final Provider provider;
    private final Object context;

    /**
     * Provide Request constructor.
     * @param bindingRequest the <code>BindingRequest</code> received from the <code>ProvideManager</code>. 
     *  Must not be null.
     * @param provider the <code>Provide</code> implementation object which the <code>ProvideManager</code> 
     * is requested to begin and manage.  Must not be null.
     * @param dofInterface the <code>DOFInterface</code> the <code>ProvideManager</code> will require to
     * begin the provide with.  Must not be null.
     * @param context a custom context object which the application may desire to associate with this 
     * <code>ProvideRequest</code>.  May be null.
     * @throws IllegalArgumentException if bindingRequest or provider is null.
     */
    public ProvideRequest(BindingRequest bindingRequest, Provider provider, DOFInterface dofInterface, Object context)
    {
        if (bindingRequest == null || provider == null)
            throw new IllegalArgumentException("bindingRequest == null || provider == null");
        this.bindingRequest = bindingRequest;
        this.provider = provider;
        this.dofInterface = dofInterface;
        this.context = context;
    }

    /**
     * Get the Context object.
     * @return the applications custom context object given for this <code>ProvideRequest</code>.  May return null.  
     */
    public Object getContext()
    {
        return context;
    }
    
    /**
     * Get the DOFInterface object.
     * @return the <code>DOFInterface</code> given for this <code>ProvideRequest</code>.  Will never return null.  
     */
    public DOFInterface getDofInterface()
    {
        return dofInterface;
    }
    
    /**
     * Get the BindingRequest object.
     * @return the <code>BindingRequest</code> given for this <code>ProvideRequest</code>.  Will never return null.  
     */
    public BindingRequest getBindingRequest()
    {
        return bindingRequest;
    }
    
    /**
     * Get the Provider associated with this binding request.
     * @return the <code>Provider</code> given for this <code>ProvideRequest</code>.  Will never return null.  
     */
    public Provider getProvider()
    {
        return provider;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = prime * ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((dofInterface == null) ? 0 : dofInterface.hashCode());
        result = prime * result + ((bindingRequest == null) ? 0 : bindingRequest.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof ProvideRequest))
            return false;
        ProvideRequest other = (ProvideRequest) obj;
        if (dofInterface == null)
        {
            if (other.dofInterface != null)
                return false;
        } else if (!dofInterface.equals(other.dofInterface))
            return false;
        if (provider == null)
        {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (bindingRequest == null)
        {
            if (other.bindingRequest != null)
                return false;
        } else if (!bindingRequest.equals(other.bindingRequest))
            return false;
        if (context == null)
        {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        //@formatter:off
        return "bindingRequest: " + bindingRequest.toString() + 
               " provider: " + provider.toString() +
               " dofInterface: " + dofInterface.toString() + 
               " context: " + (context == null ? "null" : context.toString());
        //@formatter:off
    }
}

