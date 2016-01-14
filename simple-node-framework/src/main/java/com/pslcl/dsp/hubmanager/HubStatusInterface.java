/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.List;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObject.InvokeOperationListener;
import org.opendof.core.oal.DOFObject.SubscribeOperationListener;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFArray;
import org.opendof.core.oal.value.DOFBoolean;
import org.opendof.core.oal.value.DOFString;
import org.opendof.core.oal.value.DOFStructure;
import org.opendof.core.oal.value.DOFUInt8;

public class HubStatusInterface
{
    public static final DOFInterfaceID IID = DOFInterfaceID.create("[01:{01000041}]");

    public static final int HubState_ItemId = 1;
    public static final int GetHubList_ItemId = 2;

    public static final DOFType ServerStatus_Type = new DOFString.Type(DOFString.UTF_8, 13);
    public static final DOFType ServerCount_Type = DOFUInt8.TYPE;
    public static final DOFType ConnectedCount_Type = DOFUInt8.TYPE;
    public static final DOFType[] HubState_Types = {ServerStatus_Type, ServerCount_Type, ConnectedCount_Type};
    public static final DOFType HubState_Type = new DOFStructure.Type(HubState_Types);
    
    public static final DOFType ServerAddr_Type = new DOFString.Type(DOFString.UTF_8, 32);
    public static final DOFType GroupId_Type = Authentication.TYPE;
    public static final DOFType DomainId_Type = Domain.TYPE;
    public static final DOFType Connected_Type = DOFBoolean.TYPE;

    public static final DOFType[] HubStatusData_Types = {ServerAddr_Type, GroupId_Type, DomainId_Type, Connected_Type};
    public static final DOFType HubStatusData_Type = new DOFStructure.Type(HubStatusData_Types);
    public static final DOFType HubStatusArray_Type = new DOFArray.Type(HubStatusData_Type, 256);
    public static final DOFType[] HubStatusList_Out = new DOFType[]{HubStatusArray_Type};
    
    // @formatter:off
    public static final DOFInterface DEF = new DOFInterface.Builder(IID)
        .addProperty(HubState_ItemId, false, true, HubState_Type)
        .addMethod(GetHubList_ItemId, null, HubStatusList_Out)
        .build();
    // @formatter:on
    
    public static HubState getHubState(DOFObject dobject, int timeout) throws DOFException
    {
        DOFValue value = dobject.get(DEF.getProperty(HubState_ItemId), timeout).get();
        return HubState.fromOalValue(value);
    }
    
    public static HubStatusData[] getHubStatusList(DOFObject dobject, int timeout) throws DOFException
    {
        List<DOFValue> list = dobject.invoke(DEF.getMethod(GetHubList_ItemId), timeout, new DOFValue[0]).get();
        DOFArray array = (DOFArray)list.get(0);
        HubStatusData[] rvalue = new HubStatusData[array.size()];
        for(int i=0; i < rvalue.length; i++)
        {
            DOFStructure struct = (DOFStructure)array.get(i);
            rvalue[i] = new HubStatusData(
                            ((DOFString)struct.getField(0)).get(),
                            (Authentication.create((DOFObjectID)struct.getField(1))),
                            (Domain.create((DOFObjectID)struct.getField(2))),
                            ((DOFBoolean)struct.getField(3)).get());
        }
        return rvalue;
    }
    
    public static void subscribeHubStateChanged(DOFObject dobject, int minPeriod, DOFOperation.Control control, int timeout, SubscribeOperationListener operationListener, Object context)
    {
        dobject.beginSubscribe(DEF.getProperty(HubState_ItemId), minPeriod, control == null ? new DOFOperation.Control() : control, timeout, operationListener, context);
    }
    
    public static void hubStateChanged(DOFObject dobject)
    {
        dobject.changed(DEF.getProperty(HubState_ItemId));
    }
    
    public static DOFOperation.Invoke beginHubStatusList(DOFObject dobject, int timeout, InvokeOperationListener listener, Object context)
    {
        return dobject.beginInvoke(DEF.getMethod(GetHubList_ItemId), timeout, listener, context, new DOFValue[0]);
    }
    
    public static class HubState
    {
        public final ServerStatus status;
        public final short numberOfServers;
        public final short numberOfConnected;
        
        public HubState(ServerStatus status, short numberOfServers, short numberOfConnected)
        {
            this.numberOfConnected = numberOfConnected;
            this.numberOfServers = numberOfServers;
            this.status = status;
        }
        
        public static HubState fromOalValue(DOFValue value)
        {
            DOFStructure struct = (DOFStructure) value;
            return new HubState(
                            ServerStatus.valueOf(((DOFString)struct.getField(0)).get()),
                            ((DOFUInt8)struct.getField(1)).get(),
                            ((DOFUInt8)struct.getField(2)).get());
        }
        
        @Override
        public String toString()
        {
            return status.toString() + " servers: " + numberOfServers + " connected: " + numberOfConnected;
        }
    }
    
    public static class HubStatusData
    {
        public final String serverAddress;
        public final Authentication groupId;
        public final Domain domainId;
        public final boolean connected;
        
        public HubStatusData(String serverAddress, Authentication groupId, Domain domainId, boolean connected)
        {
            this.serverAddress = serverAddress;
            this.groupId = groupId;
            this.domainId = domainId;
            this.connected = connected;
        }
        
        @Override
        public String toString()
        {
            return serverAddress+"."+groupId.toStandardString()+"."+domainId.toStandardString() + ": " + (connected ? "connected" : "not connected");
        }
        
        public static HubStatusData[] fromOalResponse(List<DOFValue> result)
        {
            DOFArray array = (DOFArray)result.get(0);
            HubStatusData[] rvalue = new HubStatusData[array.size()];
            for(int i=0; i < rvalue.length; i++)
            {
                DOFStructure struct = (DOFStructure)array.get(i);
                rvalue[i] = new HubStatusData(
                                ((DOFString)struct.getField(0)).get(),
                                (Authentication.create((DOFObjectID)struct.getField(1))),
                                (Domain.create((DOFObjectID)struct.getField(2))),
                                ((DOFBoolean)struct.getField(3)).get());
            }
            return rvalue;
        }
    }
    
    public enum ServerStatus {NoneConnected, SomeConnected, AllConnected}
}
