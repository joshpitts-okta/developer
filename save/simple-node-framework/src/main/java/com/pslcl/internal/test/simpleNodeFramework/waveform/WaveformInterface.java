package com.pslcl.internal.test.simpleNodeFramework.waveform;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFProviderException;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.value.DOFUInt8;


@SuppressWarnings("javadoc")
public class WaveformInterface
{
    public static final String DhtPropertiesFileKey="org.snf.waveform.dht.config-properties-file";
    public static final String EnableDhtKey="org.snf.waveform.dht.enable";
    public static final String DhtBaseOidKey = "org.emitdo.service.util.dht.base-oid";
    public static final String EnableDhtDefault="false";
    
    public static final DOFInterfaceID IID = DOFInterfaceID.create("[1:{01000001}]");

    public static final short Value_ItemId = 1;
    public static final short Min_ItemId = 2;
    public static final short Max_ItemId = 3;
    public static final short Invalid_ItemId = 4;
    
    public static final DOFType Value = DOFUInt8.TYPE;

    public static final DOFInterface Def = new DOFInterface.Builder( IID )
        .addProperty(Value_ItemId, false, true, Value)
        .addProperty(Min_ItemId, true, true, Value)
        .addProperty(Max_ItemId, true, true, Value)
        .addException(Invalid_ItemId)
        .build();
    
    public static Property getValueProperty()
    {
        return Def.getProperty(Value_ItemId);
    }
    
    public static Property getMinProperty()
    {
        return Def.getProperty(Min_ItemId);
    }
    
    public static Property getMaxProperty()
    {
        return Def.getProperty(Max_ItemId);
    }
    
    public static short getValue(DOFObject dobject, int timeout) throws DOFException
    {
        DOFUInt8 value =  (DOFUInt8)dobject.get(Def.getProperty(Value_ItemId), timeout).get();
        return value.get();
    }
    
    public static short getMin(DOFObject dobject, int timeout) throws DOFException
    {
        DOFUInt8 value =  (DOFUInt8)dobject.get(Def.getProperty(Min_ItemId), timeout).get();
        return value.get();
    }
    
    public static short getMax(DOFObject dobject, int timeout) throws DOFException
    {
        DOFUInt8 value =  (DOFUInt8)dobject.get(Def.getProperty(Max_ItemId), timeout).get();
        return value.get();
    }
    
    public static WaveformData getWaveformData(DOFObject dobject, int timeout) throws DOFException
    {
        int to = timeout/3;
        return new WaveformData(getValue(dobject, to), getMin(dobject, to), getMax(dobject, to));
    }
    
    public static DOFProviderException getInvalidException()
    {
        return new DOFProviderException(Def.getException(Invalid_ItemId));
    }

    public static class WaveformData
    {
        public final short value;
        public final short min;
        public final short max;
        
        public WaveformData(short value, short min, short max)
        {
            this.value = value;
            this.min = min;
            this.max = max;
        }
        
        @Override
        public String toString()
        {
            return "value: " + value + " min: " + min + " max: " + max;
        }
    }
}
