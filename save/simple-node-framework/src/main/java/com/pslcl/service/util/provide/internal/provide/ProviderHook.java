/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.provide;

import java.util.List;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface.Event;
import org.opendof.core.oal.DOFInterface.Method;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObject.Provider;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFRequest.Get;
import org.opendof.core.oal.DOFRequest.Invoke;
import org.opendof.core.oal.DOFRequest.Register;
import org.opendof.core.oal.DOFRequest.Session;
import org.opendof.core.oal.DOFRequest.Subscribe;
import org.opendof.core.oal.DOFValue;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.provide.ProviderInfo;
import com.pslcl.service.util.provide.internal.ProvideMultiplexer;

public class ProviderHook implements Provider
{
    private volatile ProviderInfo providerInfo;
    private volatile Provider provider;
    private final ProvideMultiplexer manager;
    
    public ProviderHook(ProvideMultiplexer manager)
    {
        this.manager = manager;
    }

    public void setProviderInfo(ProviderInfo info)
    {
        providerInfo = info;
        provider = info.getProvideRequest().getProvider();
    }
    
    @Override
    public void complete(DOFOperation operation, DOFException exception)
    {
//        synchronized(manager)
//        {
            try
            {
                provider.complete(operation, exception);
                manager.cleanupProvider(providerInfo);
            }catch(Throwable t)
            {
                LoggerFactory.getLogger(getClass()).error("provider complete cleanup failed", t);
            }
//        }
    }

    @Override
    public void get(Provide operation, Get request, Property property)
    {
        provider.get(operation, request, property);
    }

    @Override
    public void set(Provide operation, DOFRequest.Set request, Property property, DOFValue value)
    {
        provider.set(operation, request, property, value);
    }

    @Override
    public void invoke(Provide operation, Invoke request, Method method, List<DOFValue> parameters)
    {
        provider.invoke(operation, request, method, parameters);
    }

    @Override
    public void subscribe(Provide operation, Subscribe request, Property property, int minPeriod)
    {
        provider.subscribe(operation, request, property, minPeriod);
    }

    @Override
    public void subscribeComplete(Provide operation, Subscribe request, Property property)
    {
        provider.subscribeComplete(operation, request, property);
    }

    @Override
    public void register(Provide operation, Register request, Event event)
    {
        provider.register(operation, request, event);
    }

    @Override
    public void registerComplete(Provide operation, Register request, Event event)
    {
        provider.registerComplete(operation, request, event);
    }

    @Override
    public void session(Provide operation, Session request, DOFObject object, DOFInterfaceID interfaceID, DOFObjectID sessionID, DOFInterfaceID sessionType)
    {
        provider.session(operation, request, object, interfaceID, sessionID, sessionType);
    }

    @Override
    public void sessionComplete(Provide operation, Session request, DOFObject object, DOFInterfaceID interfaceID, DOFObjectID sessionID, DOFInterfaceID sessionType)
    {
        provider.sessionComplete(operation, request, object, interfaceID, sessionID, sessionType);
    }
}
