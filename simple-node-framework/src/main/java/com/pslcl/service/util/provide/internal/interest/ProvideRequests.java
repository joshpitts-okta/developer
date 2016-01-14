/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.interest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendof.core.oal.DOFSystem;

import com.pslcl.service.util.provide.ProvideFactory;
import com.pslcl.service.util.provide.ProvideRequest;

public final class ProvideRequests
{
    public final ProvideFactory factory;
    public final Set<ProvideRequest> provideRequests;
    public volatile List<DOFSystem> systems;

    public ProvideRequests(ProvideFactory factory, List<ProvideRequest> providerRequests)
    {
        if (factory == null || providerRequests == null)
            throw new IllegalArgumentException("factory == null || providerRequests == null");
        this.factory = factory;
        provideRequests = new HashSet<ProvideRequest>(providerRequests);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = prime * ((factory == null) ? 0 : factory.hashCode());
        result = prime * result + ((provideRequests == null) ? 0 : provideRequests.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof ProvideRequests))
            return false;
        ProvideRequests other = (ProvideRequests) obj;
        if (factory == null)
        {
            if (other.factory != null)
                return false;
        } else if (!factory.equals(other.factory))
            return false;
        if (provideRequests == null)
        {
            if (other.provideRequests != null)
                return false;
        } else if (!provideRequests.equals(other.provideRequests))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        //@formatter:off
        return "factory: " + factory.toString() + 
               " provideRequests: " + provideRequests.toString();
        //@formatter:off
    }
}

