/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.interest;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProvideFactory;
import com.pslcl.service.util.provide.ProvideManager;
import com.pslcl.service.util.provide.ProvideRequest;
import com.pslcl.service.util.provide.ProviderInfo;

@SuppressWarnings("javadoc")
public class TriggerFactory implements ProvideFactory
{
    // need this class for unique factory to create key for interest that no other factories provide for.

    @Override
    public List<ProvideRequest> getProvideRequestSet(ProvideManager manager, BindingRequest request)
    {
        return new ArrayList<ProvideRequest>();
    }

    @Override
    public void removed(ProvideManager manager)
    {
    }

    @Override
    public void provideActive(ProviderInfo providerInfo)
    {
    }
}
