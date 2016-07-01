/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal;

import java.util.List;
import java.util.concurrent.Callable;

import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProvideFactory;
import com.pslcl.service.util.provide.ProvideManager;
import com.pslcl.service.util.provide.ProvideRequest;
import com.pslcl.service.util.provide.internal.interest.ProvideRequests;

public class GetFactoryProvideRequests implements Callable<ProvideRequests>
{
    private final ProvideManager manager;
    private final ProvideFactory provideFactory;
    private final BindingRequest bindingRequest;
    
    public GetFactoryProvideRequests(ProvideManager manager, ProvideFactory provideFactory, BindingRequest bindingRequest)
    {
        this.manager = manager;
        this.provideFactory = provideFactory;
        this.bindingRequest = bindingRequest;
    }
    
    @Override
    public ProvideRequests call() throws Exception
    {
        List<ProvideRequest> provideRequests = provideFactory.getProvideRequestSet(manager, bindingRequest);
        if(provideRequests == null || provideRequests.size() == 0)
            return null;
        return new ProvideRequests(provideFactory, provideRequests);
    }
}
