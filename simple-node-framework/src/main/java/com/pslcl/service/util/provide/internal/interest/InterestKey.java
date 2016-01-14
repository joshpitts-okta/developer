/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.interest;

import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProvideFactory;

public class InterestKey
{
    public final ProvideFactory factory;
    public final BindingRequest bindingRequest;
    
    public InterestKey(ProvideFactory factory, BindingRequest bindingRequest)
    {
        this.factory = factory;
        this.bindingRequest = bindingRequest;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bindingRequest == null) ? 0 : bindingRequest.hashCode());
        result = prime * result + ((factory == null) ? 0 : factory.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InterestKey other = (InterestKey) obj;
        if (bindingRequest == null)
        {
            if (other.bindingRequest != null)
                return false;
        } else if (!bindingRequest.equals(other.bindingRequest))
            return false;
        if (factory == null)
        {
            if (other.factory != null)
                return false;
        } else if (!factory.equals(other.factory))
            return false;
        return true;
    }
}
