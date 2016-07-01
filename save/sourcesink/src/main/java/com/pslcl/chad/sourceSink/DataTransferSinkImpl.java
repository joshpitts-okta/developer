package com.pslcl.chad.sourceSink;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFString;
import org.opendof.datatransfer.DetailRequestStatus;
import org.opendof.datatransfer.DuplicateRequestIDException;
import org.opendof.datatransfer.Event;
import org.opendof.datatransfer.OutOfRangeException;
import org.opendof.datatransfer.OutOfResourcesException;
import org.opendof.datatransfer.SinkNotFoundException;
import org.opendof.datatransfer.SourceNotFoundException;
import org.opendof.datatransfer.StatusLevel;
import org.opendof.datatransfer.StatusListener;
import org.opendof.datatransfer.TopologyInformation;
import org.opendof.datatransfer.UnknownDataException;
import org.opendof.datatransfer.ValueSet;
import org.opendof.datatransfer.ValueSet.Definition.Property;
import org.opendof.datatransfer.sink.EventListener;
import org.opendof.datatransfer.sink.Sink;
import org.opendof.datatransfer.sink.TopologyListener;
import org.opendof.datatransfer.sink.ValueSetListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransferSinkImpl implements DataTransferImpl
{

    private final DataTransferType type = DataTransferType.SINK;

    private DOFObjectID providerID;
    private DOFSystem system;
    private Sink sink;
    private Logger log;
    private StatusListener statusListener = new SinkStatusListener();

    static Integer counter = 0;
    private int delay = 0;
    private DOFObjectID lastSourceID = null;
    private DOFObjectID lastDeviceID;
    private DOFObjectID instanceID = null;
    private DOFInterfaceID lastiid;
    private int lastItemID;

    public DataTransferSinkImpl(DOFSystem system, DOFObjectID providerID)
    {
        this.system = system;
        this.providerID = providerID;
        log = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void startSinkProvider()
    {
        UniqueID.setIncrementing(false);
        long startID = UniqueID.getNewID();
        try
        {
            UniqueID.setUniqueID(startID);
        } catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Sink.Config config = null;
        try
        {
            config = new Sink.Config.Builder(system, providerID, statusListener).setInstanceID(Sink.Config.Builder.generateInstanceID()).setValueSetListener(new SinkDataListener()).setEventListener(new SinkEventListener()).setTopologyListener(new SinkTopologyListener()).setThreadpoolSize(200).build();

        } catch (Exception e)
        {
            System.out.println("Exception ocurred while creating sink config. " + e);
            e.printStackTrace();
        }

        sink = Sink.create(config);
        instanceID = sink.getID();
        UniqueID.setIncrementing(true);
    }

    @Override
    public void close()
    {
        sink.close();

    }

    @Override
    public void run()
    {
        startSinkProvider();
    }

    public void pause()
    {
        try
        {
            Thread.sleep(delay);
        } catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private class SinkStatusListener implements StatusListener
    {

        @Override
        public void statusChanged(StatusLevel severity, Date timestamp, String message, Exception ex)
        {
            System.out.println("SinkStatusChanged -- severity: " + severity + " timestamp: " + timestamp + " message: " + message + " exception: " + ex);

        }
    }

    private class SinkEventListener implements EventListener
    {

        @Override
        public void persistEvent(Sink sink, DOFObjectID sourceID, Event event) throws Exception
        {
            DOFValue[] values = event.getValues();
            String message = "";
            if (values != null)
            {
                try
                {
                    if (values[0].getDOFType().getTypeID() == DOFType.OBJECTID)
                    {
                        message = ((DOFObjectID) values[0]).toStandardString();
                    } else
                    {
                        message = ((DOFString) values[0]).get();
                    }
                } catch (Exception e)
                {

                }
            }

            log.info("Event for Source: " + sourceID + ". ID: " + event.getItemID() + " Message: " + message);
            pause();
        }
    }

    private class SinkDataListener implements ValueSetListener
    {
        AtomicBoolean requestComplete = new AtomicBoolean(true);

        SinkDataListener()
        {
            System.out.println("Constructed: " + getClass());
        }

        /* (non-Javadoc)
         * @see com.panasonic.pesdca.platform.battery.sink.DataListener#persistData(com.panasonic.pesdca.platform.battery.sink.BatteryPlatformSink, com.panasonic.pesdca.platform.battery.common.ValueSet)
         */
        @Override
        public void persistValueSet(Sink sink, DOFObjectID sourceID, ValueSet valueSet) throws Exception
        {
            //System.out.println("persistData for " + valueSet.getDeviceID() + " from: " + sourceID);
            int count;
            synchronized (counter)
            {
                count = ++counter;
            }
            Property[] properties = valueSet.getDefinition().getProperties();
            DOFInterfaceID iid = properties[0].getInterfaceID();
            pause();
            //SwriteToFile(valueSet);
            if (count % 1000 == 0 || lastSourceID == null)
            {
                requestComplete.set(false);
                //long requestnum = UniqueID.getNewID();
                lastSourceID = sourceID.getBase();
                lastDeviceID = valueSet.getDeviceID();
                lastiid = iid;
                lastItemID = properties[0].getItemID();
                //sink.requestValues(sourceID.getBase(), UniqueID.getNewID(), valueSet.getDeviceID(), iid, properties[0].getItemID(), valueSet.getFirstTime(), 0, 120 * 1000);
                System.out.println(new Date().toString() + ": ValueSet Number: " + count + ", First row: " + valueSet.getFirstTime() + ", Last row: " + valueSet.getLastTime() + ", ValueSet size: " + valueSet.getStorageBytes().length + " bytes");
            }
        }

//        private boolean prevRequestComplete()
//        {
//
//            return requestComplete.get();
//        }

        /* (non-Javadoc)
         * @see com.panasonic.pesdca.platform.battery.sink.DataListener#dataComplete(com.panasonic.pesdca.platform.battery.sink.BatteryPlatformSink, long, com.panasonic.pesdca.platform.battery.common.DetailRequestStatus)
         */
        @Override
        public void requestComplete(Sink sink, long requestKey, DetailRequestStatus status) throws Exception
        {
            System.out.println("******************dataComplete: " + requestKey + " Status: " + status + " ******************");
            requestComplete.set(true);
        }

        /* (non-Javadoc)
         * @see com.panasonic.pesdca.platform.battery.sink.DataListener#valueSetsRemaining(org.emitdo.oal.DOFObjectID, long)
         */
        @Override
        public void valueSetsRemaining(Sink sink, DOFObjectID source, long remaining)
        {
            // System.out.println("valueSetsRemaining " + source + " " + remaining);

        }
    }

    public void setDelay(int delay)
    {
        this.delay = delay;

    }

    public void writeToFile(ValueSet valueSet)
    {
        String fileName = "/valuesets/" + UniqueID.getNewID() + ".vs";
        FileOutputStream file = null;
        try
        {
            file = new FileOutputStream(fileName);
            byte[] storage = valueSet.getStorageBytes();
            file.write(storage);
        } catch (IOException ex)
        {
            System.out.println("Failed to write valueset file.  Exception: " + ex.getMessage());
        } finally
        {
            try
            {
                if (file != null)
                {
                    file.flush();
                    file.close();
                }
            } catch (Exception e)
            {
            }
        }

    }

    private class SinkTopologyListener implements TopologyListener
    {

        @Override
        public void persistTopology(Sink arg0, DOFObjectID arg1, TopologyInformation arg2)
        {
            pause();
        }
    }

    public void requestData()
    {
        if (lastSourceID == null)
            return;
        int duration = 7 * 24 * 60 * 60 * 1000;
        long requestnum = UniqueID.getNewID();
        long tenMinAgo = (new Date().getTime() - duration);

        try
        {
            System.out.println("Request values: " + lastSourceID + " " + requestnum + " " + lastDeviceID + " " + new Date(tenMinAgo) + " " + 680 * 1000);
            sink.requestValues(lastSourceID, sink.getID(), requestnum, lastDeviceID, lastiid, lastItemID, new Date(tenMinAgo), duration, 680 * 1000);
        } catch (SourceNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OutOfRangeException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OutOfResourcesException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SinkNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownDataException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DuplicateRequestIDException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("******************requestData: " + requestnum + " ******************");
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

    @Override
    public DataTransferType getDataTransferType()
    {
        return type;
    }

}
