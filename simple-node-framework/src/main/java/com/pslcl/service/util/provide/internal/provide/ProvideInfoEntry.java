/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.provide;

import java.util.concurrent.Future;

import org.opendof.core.oal.DOFOperation;

import com.pslcl.service.util.provide.ProviderInfo;

public class ProvideInfoEntry
{
    public final FactoryWrapper wrapper;
    public final ProviderInfo info;
    private DOFOperation.Provide provide;
    private Future<Void> cancelFuture;
    
    public ProvideInfoEntry(ProviderInfo info)
    {
        this.info = info;
        wrapper = null;
    }
    
    public ProvideInfoEntry(FactoryWrapper wrapper)
    {
        info = null;
        this.wrapper = wrapper;
    }
    
    public synchronized void setProvide(DOFOperation.Provide provide)
    {
        this.provide = provide;
    }
    
    public synchronized DOFOperation.Provide getProvide()
    {
        return provide;
    }
    
    public synchronized void setCancelFuture(Future<Void> future)
    {
        cancelFuture = future;
    }
    
    public synchronized Future<Void> getCancelFuture()
    {
        return cancelFuture;
    }
    
    @Override
    public String toString()
    {
        String rvalue = "info: " + (info == null ? "null" : info.toString()) +
                        " provide: " + (provide == null ? "null" : provide.toString()) +
                        " cancelFuture: " + (cancelFuture == null ? "null" : cancelFuture.toString());
        
        return rvalue;
    }
}
