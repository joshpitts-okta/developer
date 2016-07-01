package com.pslcl.internal.test.simpleNodeFramework.waveform.service.app;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFNotFoundException;
import org.opendof.core.oal.DOFObject.DefaultProvider;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFRequest.Subscribe;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFUInt8;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.test.simpleNodeFramework.waveform.Waveform;
import com.pslcl.internal.test.simpleNodeFramework.waveform.Waveform.Type;
import com.pslcl.internal.test.simpleNodeFramework.waveform.WaveformInterface;
import com.pslcl.internal.test.simpleNodeFramework.waveform.service.system.WaveformServiceConfigImpl;
import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.GracePeriodProvider;
import com.pslcl.service.util.provide.ProviderInfo;

@SuppressWarnings("javadoc")
public class WaveformServiceProvider extends DefaultProvider implements GracePeriodProvider
{
//    public static final String LoggingServiceOid = "[3:loggingTest@internal.service.opendof.org]";
    
    private final WaveformServiceConfigImpl config;
    private Waveform waveform;

    public WaveformServiceProvider(WaveformServiceConfigImpl config)
    {
        this.config = config;
    }

    public synchronized void setProvideInfo(ProviderInfo providerInfo)
    {
        synchronized (this)
        {
            waveform = new Waveform(Type.Sine, providerInfo.getProvideOperation().getObject(), config.serviceIndex);
            waveform.start();
        }
    }

    public void snfReport()
    {
        LoggerFactory.getLogger(getClass()).info("Hello world");
    }
    
    public void close()
    {
        synchronized (this)
        {
            waveform.close();
        }        
    }
    
    @Override
    public void complete(DOFOperation operation, DOFException exception) 
    {
        close();
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

//    @Override
//    public void invoke(Provide operation, Invoke request, Method method, List<DOFValue> parameters)
//    {
//        short logLevel;
//        String message;
//        DOFObjectID messageID;
//
//        if (method.getInterfaceID().equals(LoggingInterface.ASCII_IID) || method.getInterfaceID().equals(LoggingInterface.UTF_8_IID) || method.getInterfaceID().equals(LoggingInterface.SJIS_IID))
//        {
//            if (method.getItemID() == LoggingInterface.L0G_MESSAGE_ASCII_METHOD_ID || method.getItemID() == LoggingInterface.L0G_MESSAGE_UTF_8_METHOD_ID || method.getItemID() == LoggingInterface.L0G_MESSAGE_SJIS_METHOD_ID)
//            {
//                messageID = (DOFObjectID) parameters.get(0);
//                logLevel = ((DOFUInt8) parameters.get(1)).get();
//                message = ((DOFString) parameters.get(2)).get();
//                logEvent(messageID.toStandardString(), logLevel, message);
//            }
//            request.respond();
//        } else
//        {
//            request.respond(new DOFErrorException(DOFErrorException.NOT_SUPPORTED));
//        }
//    }


    @Override
    public int getGracePeriod(BindingRequest bindingRequest) throws Exception
    {
        return config.gracePeriod;
    }
}
