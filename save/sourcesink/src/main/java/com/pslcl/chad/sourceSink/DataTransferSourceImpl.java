package com.pslcl.chad.sourceSink;

import java.util.Date;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSystem;
import org.opendof.datatransfer.DuplicateRequestIDException;
import org.opendof.datatransfer.OutOfRangeException;
import org.opendof.datatransfer.OutOfResourcesException;
import org.opendof.datatransfer.StatusLevel;
import org.opendof.datatransfer.StatusListener;
import org.opendof.datatransfer.UnknownDataException;
import org.opendof.datatransfer.source.DataRequestHandler;
import org.opendof.datatransfer.source.DeliveryListener;
import org.opendof.datatransfer.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransferSourceImpl implements DataTransferImpl
{
    private DataTransferType type = DataTransferType.SOURCE;
    Logger log = LoggerFactory.getLogger("com.panasonic.pesdca.platform.battery.compatibilityservice.BatterySourceImpl");
    private Source source;
    private DOFSystem system;
    private DOFObjectID providerID;
    private DOFObjectID instanceID;

    public DataTransferSourceImpl(DOFSystem system, DOFObjectID providerID)
    {
        this.system = system;
        this.providerID = providerID;
        log = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void startSourceProvider()
    {

        Source.Config config = null;
        try
        {

            config = new Source.Config.Builder(system, providerID, new SourceStatusListener(), new SourceDeliveryListener()).setRequestHandler(new SourceRequestHandler()).setThreadpoolSize(10).setInstanceID(Source.Config.Builder.generateInstanceID()).build();
        } catch (Exception e)
        {
            System.out.println("Exception ocurred while creating source config. " + e);
            e.printStackTrace();
        }

        source = Source.create(config);
        instanceID = source.getID();
    }

    @Override
    public void close()
    {
        source.close();

    }

    @Override
    public void run()
    {
        startSourceProvider();
    }

    private class SourceRequestHandler implements DataRequestHandler
    {

        @Override
        public void requestValues(Source source, DOFObjectID sink, long requestID, DOFObjectID deviceID, DOFInterfaceID propertyIID, int propertyItemID, Date start, int duration, int timeout) throws OutOfRangeException, OutOfResourcesException, UnknownDataException, DuplicateRequestIDException, Exception
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void cancelRequest(Source source, DOFObjectID sink, long requestID)
        {
            // TODO Auto-generated method stub

        }

    }

    private class SourceStatusListener implements StatusListener
    {

        @Override
        public void statusChanged(StatusLevel severity, Date timestamp, String message, Exception ex)
        {
            System.out.println("SourceStatusChanged -- severity: " + severity + " timestamp: " + timestamp + " message: " + message + " exception: " + ex);

        }
    }

    private class SourceDeliveryListener implements DeliveryListener
    {

        @Override
        public void eventDelivered(Source source, long eventID, Throwable failure)
        {
            System.out.println("eventDelivered: " + eventID + " failure: " + failure);
        }

        @Override
        public void topologyUpdateDelivered(Source source, long topologyUpdateID, Throwable failure)
        {
            System.out.println("topologyDelivered: " + topologyUpdateID + " failure: " + failure);
        }

        @Override
        public long valueSetDelivered(Source arg0, long arg1, Throwable arg2)
        {
            System.out.println("DataDelivered: " + arg1 + " failure: " + arg2);
            return 0;
        }
    }

    @Override
    public DataTransferType getDataTransferType()
    {
        // TODO Auto-generated method stub
        return type;
    }

    @Override
    public String toString()
    {
        String id;
        if (instanceID == null)
            id = providerID.toStandardString();
        else
            id = instanceID.toStandardString();
        return type.name() + ":" + id + "";
    }
}