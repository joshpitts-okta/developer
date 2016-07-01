/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.Callable;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;


public class GetGracePeriodTask implements Callable<Void>
{
    private final HubRequestMonitor monitor;
    
    private final DOFObjectID objectId;
    private final DOFInterfaceID interfaceId;

    public GetGracePeriodTask(HubRequestMonitor monitor, DOFObjectID objectId, DOFInterfaceID interfaceId)
    {
        this.monitor = monitor;
        this.objectId = objectId;
        this.interfaceId = interfaceId;
    }

    @Override
    public Void call() throws Exception
    {
        monitor.handleInterfaceAdded(objectId, interfaceId);
        return null;
    }
}