package com.pslcl.internal.test.simpleNodeFramework.waveform.module;

import java.util.ArrayList;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFNotFoundException;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFRequest.Subscribe;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFUInt8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.waveform.Waveform;
import com.pslcl.internal.test.simpleNodeFramework.waveform.Waveform.Type;
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;

@SuppressWarnings("javadoc")
public class WaveformModuleProvider extends DOFObject.DefaultProvider
{
    private final ArrayList<DOFOperation.Provide> operations;
    private final Logger log;
    private final DOFObjectID serviceId;
    private final String creds;
    private Waveform waveform; 
    
    public WaveformModuleProvider(DOFObjectID serviceId, String creds)
    {
        this.serviceId = serviceId;
        this.creds = creds;
        operations = new ArrayList<DOFOperation.Provide>();
        log = LoggerFactory.getLogger(getClass());
    }

    public DOFOperation.Provide beginProvider(DOFObject object)
    {
        DOFOperation.Provide provideOp = object.beginProvide(WaveformInterface.Def, DOF.TIMEOUT_NEVER, this, null);
        operations.add(provideOp);
        waveform = new Waveform(Type.Sine, object, 0);
        waveform.start();
        log.info("providing: " + creds + " " + serviceId.toStandardString() + ":" + WaveformInterface.IID.toStandardString());
        return provideOp;
    }

    @Override
    public void get(DOFOperation.Provide provide, DOFRequest.Get get, DOFInterface.Property property)
    {
        DOFUInt8 rvalue = null;
        switch (property.getItemID())
        {
            case WaveformInterface.Min_ItemId:
                rvalue = new DOFUInt8(waveform.getMinValue());
                break;
            case WaveformInterface.Max_ItemId:
                rvalue = new DOFUInt8(waveform.getMaxValue());
                break;
            case WaveformInterface.Value_ItemId:
                rvalue = new DOFUInt8(waveform.getValue());
                break;
            default:
                get.respond(new DOFNotFoundException("unsupported itemId: " + property.getItemID()));
                return;
        }
        get.respond(rvalue);
    }

    @Override
    public void set(DOFOperation.Provide provide, DOFRequest.Set set, DOFInterface.Property property, DOFValue dofValue)
    {
        short newValue = ((DOFUInt8) dofValue).get();

        switch (property.getItemID())
        {
            case WaveformInterface.Min_ItemId:
                if (newValue > waveform.getMaxValue())
                {
                    set.respond(WaveformInterface.getInvalidException());
                    return;
                }
                short minValue = newValue;
                waveform.setMinValue(minValue);
                if (waveform.getValue() < minValue)
                    waveform.setValue(minValue);
                break;

            case WaveformInterface.Max_ItemId:
                if (newValue < waveform.getMinValue())
                {
                    set.respond(WaveformInterface.getInvalidException());
                    return;
                }

                short maxValue = newValue;
                waveform.setMaxValue(maxValue);
                if (waveform.getValue() > maxValue)
                    waveform.setValue(maxValue);
                break;
            default:
                set.respond(new DOFNotFoundException("unsupported itemId: " + property.getItemID()));
                return;
            }
            set.respond();
    }

    @Override
    public void subscribe(Provide operation, Subscribe request, Property property, int minPeriod)
    {
        request.respond();
    }

    public void close()
    {
        synchronized (this)
        {
            if(waveform != null)
                waveform.close();
            waveform = null;
            if(operations != null)
            {
                for (DOFOperation op : operations)
                    op.cancel();
                operations.clear();
            }
        }
    }
}
