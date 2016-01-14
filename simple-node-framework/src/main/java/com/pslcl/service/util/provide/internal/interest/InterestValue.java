/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.interest;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProviderInfo;

@SuppressWarnings("javadoc")
public class InterestValue
{
    private ProviderInfo providerInfo;
    public final BindingRequest bindingRequest;
    public final AtomicInteger count;
    private Future<Void> future;
    
    public InterestValue(BindingRequest bindingRequest)
    {
        count = new AtomicInteger(1);
        this.bindingRequest = bindingRequest;
    }
    
    public synchronized void setProviderInfo(ProviderInfo providerInfo)
    {
        this.providerInfo = providerInfo;
    }
    
    public synchronized ProviderInfo getProviderInfo()
    {
        return providerInfo;
    }
    
    public synchronized void setCancelFuture(Future<Void> future)
    {
        this.future = future;
    }
    
    public synchronized Future<Void> getCancelFuture()
    {
        return future;
    }
}
