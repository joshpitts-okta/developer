package com.psas.tools.repository.interfaces.test.legacySax;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

@SuppressWarnings("javadoc")
public class DofType extends LegacySaxHandler
{
    public static final String UnitElement= "unit";
    
    public static final String MyTagTypeRef = "typeref";
    
//    public final static String TypeRefTag = "typeref";
    public final static String LengthAttr = "length";
    public final static String MinLengthAttr = "min-length";
    public final static String MinAttr = "min";
    public final static String MaxAttr = "max";
    public static final String TypeIdAttr = "type-id";
    public static final String TypeRefAttr = "type-ref";

    public DofType currentType;
    private StructureData structure;
    private ChildComplete childComplete;
    private boolean hasEnums;
    
    //@formatter:off
    public static final String MyTags[] = new String[]
    {
        "string",
        "boolean",
        "uint8",
        "uint16",
        "uint32",
        "uint64",
        "int8",
        "int16",
        "int32",
        "int64",
        "float32",
        "float64",
        "datetime",
        "array",
        "guid",
        "blob",
        "structure",
        "reference",
        "nullable",
        "iid",
        "oid",
    };
    //@formatter:on
    
    public DofType()
    {
    }
    
    public void hasEnums(boolean value)
    {
        hasEnums = value;
    }
    
    public void setChildComplete(ChildComplete cc)
    {
       childComplete = cc;
    }
    
    public static boolean oneOfYours(String element)
    {
        for(int i=0; i < MyTags.length; i++)
        {
            if(MyTags[i].equals(element))
                return true;
        }
        if(element.equals(MyTagTypeRef))
            return true;
        return false;
    }
    
    public DofType instantiate(String tag) throws SAXException
    {
        if(hasEnums)
        {
            log.error("has enums fixme"); // AirConditioner2/4
        }
        String clazzName = DofType.class.getName();
        try
        {
            clazzName = clazzName.substring(0, clazzName.lastIndexOf('.')+1);
            String upperFirstChar = tag.substring(0,1);
            upperFirstChar = upperFirstChar.toUpperCase();
            String atomic = "DofType$Dof" + upperFirstChar + tag.substring(1);
            clazzName += atomic;
            return (DofType) Class.forName(clazzName).newInstance();
        }catch(Exception e)
        {
            throw new SAXException("could not instantiate DofType: " + clazzName);
        }
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (oneOfYours(localName))
        {
            if(!localName.equals(MyTagTypeRef) && !localName.equals(FieldData.MyTag))
            {
                stack.push(localName);
                currentType = instantiate(localName);
                currentType.setAttributes(attributes);
                if(currentType instanceof DofStructure)
                    structure = new StructureData((DofStructure)currentType);
                return;
            }
            if(localName.equals(FieldData.MyTag))
            {
                structure.startElement(uri, localName, qName, attributes);
                return;
            }
        }
        if(FieldData.oneOfYours(localName))
        {
            structure.startElement(uri, localName, qName, attributes);
            return;
        }
        if(isMyPath(FieldData.FieldPath))
        {
            structure.startElement(uri, localName, qName, attributes);
            return;
        }
        
        else if(localName.equals(MyTagTypeRef))
        {
            if(!(currentType instanceof DofNullable) && !(currentType instanceof DofArray))
                throw new SAXException(currentPath() + "." + MyTagTypeRef + " given on non-DofNullable");
            if(attributes.getLength() != 1)
                throw new SAXException(currentPath() + "." + MyTagTypeRef + " wrong number of attributes given");
            int index = attributes.getIndex("id");
            if(index != 0)
                throw new SAXException(currentPath() + "." + MyTagTypeRef + " id not found");
            if(currentType instanceof DofNullable)
                ((DofNullable)currentType).reference = Integer.parseInt(attributes.getValue(index));
            else
                ((DofArray)currentType).reference = Integer.parseInt(attributes.getValue(index));
            return;
        }
        else if(isMyPath(FieldData.FieldPath))
        {
            structure.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " DofType startElement unknown local name for this level");
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if (isMyTag(MyTagTypeRef))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + "." + MyTagTypeRef + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if(oneOfYours(current))
            currentType.characters(ch, start, length);
        else if(isMyPath(FieldData.FieldPath))
            structure.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (localName.equals(MyTagTypeRef))
            return;     // was never pushed on stack
        String current = stack.peek();
        for(int i=0; i < MyTags.length; i++)
        {
            if(current.equals(MyTags[i]))
            {
                stack.pop();
                childComplete.setChildComplete(childComplete, this);
                return;
            }
        }
        if(oneOfYours(current))
            currentType.endElement(uri, localName, qName);
        else if(isMyPath(FieldData.FieldPath))
            structure.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " DofType endElement unknown current stack localName: " + localName);
    }

    protected void setAttributes(Attributes attributes) throws SAXException
    {
        if(attributes.getLength() != 0)
            throw new SAXException(currentPath() + " did not expect any attributes");
    }
    
    protected void setRange(String min, String max) throws SAXException
    {
        throw new SAXException(currentPath() + " does not support range");
    }
    
    protected void setUnit(String unit) throws SAXException
    {
        throw new SAXException(currentPath() + " does not support unit");
    }
    
    public void validate(ContextData context) throws SAXException
    {
    }

    public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
    {
        throw new RuntimeException("needs override");
    }
    
    public StringBuilder exportNumerical(StringBuilder sb, String tag, String typeId, Object min, Object max, String unit, NameDescriptionData names, int level)
    {
        if(min == null)
            StrH.element(sb, tag, TypeIdAttr, typeId, null, names == null, level);
        else
            StrH.element(sb, tag, new String[]{TypeIdAttr, MinAttr, MaxAttr}, new String[]{typeId, min.toString(), max.toString()}, null, names == null, level);
        StrH.exportNames(sb, tag, names, unit == null, level);
        if(unit != null)
        {
            ++level;
            StrH.element(sb, UnitElement, (String)null, (String)null, unit, true, level);
            --level;
            StrH.element(sb, tag, CType.Close, level);
        }
        return sb; 
    }
    public static class DofString extends DofType
    {
        private static String StringElement = "string";
        private static String EncodingAttr = "encoding";
        
        public int length;
        public int encoding;
        public DofString(){}
        @Override
        protected void setAttributes(Attributes attributes) throws SAXException
        {
            int size = attributes.getLength();
            if(size < 2)
                throw new SAXException("DofString with less than two attributes");
            int index = attributes.getIndex("length");
            if(index != 0 && index != 1)
                throw new SAXException(currentPath() + " DofString two params but wrong index");
            length = Integer.parseInt(attributes.getValue(index));
            index = attributes.getIndex("encoding");
            if(index != 0 && index != 1)
                throw new SAXException("DofString two params but wrong index");
            encoding = Integer.parseInt(attributes.getValue(index));
            if(encoding < 3 || encoding > 32767)
                throw new SAXException("DofString encoding out of range: " + encoding);
        }
        
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, StringElement, new String[]{TypeIdAttr, EncodingAttr, LengthAttr}, new String[]{typeId, ""+encoding, ""+length}, null, names == null, level);
            return StrH.exportNames(sb, StringElement, names, true, level);
        }
    }
    
    public static class DofBoolean extends DofType
    {
        private static String BooleanElement = "boolean";
        
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, BooleanElement, TypeIdAttr, typeId, null, names == null, level);
            return StrH.exportNames(sb, BooleanElement, names, true, level);
        }
    }

    public static class DofUint8 extends DofType
    {
        private static String UInt8Element = "uint8";
        
        public Short min;
        public Short max;
        public String unit;
        public DofUint8(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = 0;
            else
                this.min = Short.parseShort(min);
            if(max == null)
                this.max = (Byte.MAX_VALUE * 2) + 1;
            else
                this.max = Short.parseShort(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, UInt8Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofUint16 extends DofType
    {
        private static String UInt16Element = "uint16";
        
        public Integer min;
        public Integer max;
        public String unit;
        public DofUint16(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = 0;
            else
                this.min = Integer.parseInt(min);
            if(max == null)
                this.max = (Short.MAX_VALUE * 2) + 1;
            else
                this.max = Integer.parseInt(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, UInt16Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofUint32 extends DofType
    {
        private static String UInt32Element = "uint32";
        
        public Long min;
        public Long max;
        public String unit;
        public DofUint32(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = 0L;
            else
                this.min = Long.parseLong(min);
            if(max == null)
            {
                //TODO: is this ok?
                this.max = (Integer.MAX_VALUE * 2) + 1L;
            }
            else
                this.max = Long.parseLong(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, UInt32Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofUint64 extends DofType
    {
        private static String UInt64Element = "uint64";
        
        public BigInteger min;
        public BigInteger max;
        public String unit;
        public DofUint64(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = new BigInteger("0");
            else
                this.min = new BigInteger(min);
            if(max == null)
            {
                //TODO: is this ok?
                this.max = new BigInteger("18446744073709441615");
            }
            else
                this.max = new BigInteger(max);
            if(this.min != null && this.min.doubleValue() > this.max.doubleValue())
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, UInt64Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofInt8 extends DofType
    {
        private static String Int8Element = "int8";
        
        public Byte min;
        public Byte max;
        public String unit;
        public DofInt8(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Byte.MIN_VALUE;
            else
                this.min = Byte.parseByte(min);
            if(max == null)
                this.max = Byte.MAX_VALUE;
            else
                this.max = Byte.parseByte(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Int8Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofInt16 extends DofType
    {
        private static String Int16Element = "int16";
        
        public Short min;
        public Short max;
        public String unit;
        public DofInt16(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Short.MIN_VALUE;
            else
                this.min = Short.parseShort(min);
            if(max == null)
                this.max = Short.MAX_VALUE;
            else
                this.max =Short.parseShort(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Int16Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofInt32 extends DofType
    {
        private static String Int32Element = "int32";
        
        public Integer min;
        public Integer max;
        public String unit;
        public DofInt32(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Integer.MIN_VALUE;
            else
                this.min = Integer.parseInt(min);
            if(max == null)
                this.max = Integer.MAX_VALUE;
            else
                this.max = Integer.parseInt(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Int32Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofInt64 extends DofType
    {
        private static String Int64Element = "int64";
        
        public Long min;
        public Long max;
        public String unit;
        public DofInt64(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Long.MIN_VALUE;
            else
                this.min = Long.parseLong(min);
            if(max == null)
                this.max = Long.MAX_VALUE;
            else
                this.max =Long.parseLong(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Int64Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofFloat32 extends DofType
    {
        private static String Float32Element = "float32";
        
        public Float min;
        public Float max;
        public String unit;
        public DofFloat32(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Float.MIN_VALUE;
            else
                this.min = Float.parseFloat(min);
            if(max == null)
                this.max = Float.MAX_VALUE;
            else
                this.max = Float.parseFloat(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Float32Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofFloat64 extends DofType
    {
        private static String Float64Element = "float64";
        
        public Double min;
        public Double max;
        public String unit;
        public DofFloat64(){}
        @Override
        protected void setRange(String min, String max) throws SAXException
        {
            if(min == null)
                this.min = Double.MIN_VALUE;
            else
                this.min = Double.parseDouble(min);
            if(max == null)
                this.max = Double.MAX_VALUE;
            else
                this.max = Double.parseDouble(max);
            if(this.min != null && this.min > this.max)
                throw new SAXException(getClass().getSimpleName() + " min > max");
        }
        @Override
        protected void setUnit(String unit) throws SAXException
        {
            this.unit = unit;
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            return exportNumerical(sb, Float64Element, typeId, min, max, unit, names, level);
        }
    }
    public static class DofDatetime extends DofType
    {
        private static String DatetimeElement = "datetime";
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, DatetimeElement, TypeIdAttr, typeId, null, names == null, level);
            return StrH.exportNames(sb, DatetimeElement, names, true, level);
        }
    }
    public static class DofArray extends DofType
    {
        private static String ArrayElement = "array";
        
        public Integer minLength;
        public int length;
        public int reference = -1;
        public DofArray(){}
        @Override
        protected void setAttributes(Attributes attributes) throws SAXException
        {
            int size = attributes.getLength();
            if(size < 1)
                throw new SAXException("DofArray with no attributes");
            if(size > 2)
                throw new SAXException("DofArray with too many attributes");
            int index = attributes.getIndex("length");
            if(size == 1 && index != 0)
                throw new SAXException("DofArray one param but wrong index");
            length = Integer.parseInt(attributes.getValue(index));
//            minLength = length;
            if(size == 2)
            {
                index = attributes.getIndex("min-length");
                if(index != 0 && index != 1)
                    throw new SAXException("DofArray two params but wrong index");
                minLength = Integer.parseInt(attributes.getValue(index));
            }
        }
        @Override
        public void validate(ContextData context) throws SAXException
        {
            if(reference < 0)
                throw new SAXException(getClass().getSimpleName() + " reference was not set");
            if(minLength != null && minLength > length)
                throw new SAXException(getClass().getSimpleName() + " minLength > length");
            context.validateTyperef(reference);
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            if(minLength != null)
                //@formatter:off
                StrH.element(sb, ArrayElement, 
                                new String[]{TypeIdAttr, TypeRefAttr, MinLengthAttr, LengthAttr}, 
                                new String[]{typeId, ""+reference, minLength.toString(), ""+length}, 
                                null, names == null, level);
                //@formatter:on
            else
                StrH.element(sb, ArrayElement, new String[]{TypeIdAttr, TypeRefAttr, LengthAttr}, new String[]{typeId, ""+reference, ""+length}, null, names == null, level);
            if(names != null)
            {
                ++level;
                names.export(sb, level);
//            FieldData field = new FieldData(null);
//            field.nameDescriptions = names;
//            field.typeref = reference;
//            field.export(sb, level);
                --level;
            }
            return StrH.element(sb, ArrayElement, CType.Close, level);
        }
    }
    public static class DofGuid extends DofType
    {
        private static String GuidElement = "guid";
        
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, GuidElement, TypeIdAttr, typeId, null, names == null, level);
            return StrH.exportNames(sb, GuidElement, names, true, level);
        }
    }
    public static class DofBlob extends DofType
    {
        private static String BlobElement = "blob";
        
        public Integer minLength;
        public int length;
        public DofBlob(){}
        @Override
        protected void setAttributes(Attributes attributes) throws SAXException
        {
            int size = attributes.getLength();
            if(size < 1)
                throw new SAXException("DofBlob with no attributes");
            if(size > 2)
                throw new SAXException("DofBlob with too many attributes");
            int index = attributes.getIndex("length");
            if(size == 1 && index != 0)
                throw new SAXException("DofBlob one param but wrong index");
            length = Integer.parseInt(attributes.getValue(index));
//            minLength = length;
            if(size == 2)
            {
                index = attributes.getIndex("min-length");
                if(index != 0 && index != 1)
                    throw new SAXException("DofBlob two params but wrong index");
                minLength = Integer.parseInt(attributes.getValue(index));
            }
        }
        @Override
        public void validate(ContextData context) throws SAXException
        {
            if(minLength != null && minLength > length)
                throw new SAXException(getClass().getSimpleName() + " minLength > length");
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            if(minLength != null)
                StrH.element(sb, BlobElement, new String[]{TypeIdAttr, MinLengthAttr, LengthAttr}, new String[]{typeId, minLength.toString(), ""+length}, null, names == null, level);
            else
                StrH.element(sb, BlobElement, new String[]{TypeIdAttr, LengthAttr}, new String[]{typeId, ""+length}, null, names == null, level);
            return StrH.exportNames(sb, BlobElement, names, true, level);
        }
    }
    public static class DofStructure extends DofType
    {
        private static String StructureElement = "structure";
        
        public final List<FieldData> fields;
        public DofStructure()
        {
            fields = new ArrayList<FieldData>();
        }
        @Override
        public void validate(ContextData context) throws SAXException
        {
            if(fields.size() == 0)
                throw new SAXException(getClass().getSimpleName() + " fields is emtpy");
            for(FieldData field : fields)
                field.validate(context);
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, StructureElement, TypeIdAttr, typeId, null, names == null, level);
            if(names != null)
            {
                ++level;
                names.export(sb, level);
            }
            for(FieldData field : fields)
                field.export(sb, FieldData.FieldElement, level);
            --level;
            StrH.element(sb, StructureElement, CType.Close, level);
            return sb;
        }
    }
    public static class DofReference extends DofType
    {
        private static String ReferenceElement = "reference";
        
        public int reference = -1;
        public DofReference(){}
        @Override
        public void validate(ContextData context) throws SAXException
        {
            if(reference < 0)
                throw new SAXException(getClass().getSimpleName() + " reference was not set");
            context.validateTyperef(reference);
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, ReferenceElement, 
                            new String[]{TypeIdAttr, TypeRefAttr}, 
                            new String[]{typeId, ""+reference}, 
                            null, names == null, level);
            if(names != null)
            {
                ++level;
//            FieldData field = new FieldData(null);
//            field.nameDescriptions = names;
//            field.typeref = reference;
//            field.export(sb, level);
                --level;
                names.export(sb, level);
            }
            return StrH.element(sb, ReferenceElement, CType.Close, level);
        }
    }
    public static class DofNullable extends DofType
    {
        private static String NullableElement = "nullable";
        
        public int reference = -1;
        public DofNullable(){}
        @Override
        public void validate(ContextData context) throws SAXException
        {
            if(reference < 0)
                throw new SAXException(getClass().getSimpleName() + " reference was not set");
            context.validateTyperef(reference);
        }
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, NullableElement, 
                            new String[]{TypeIdAttr, TypeRefAttr}, 
                            new String[]{typeId, ""+reference}, 
                            null, names == null, level);
            if(names != null)
            {
                ++level;
//                FieldData field = new FieldData(null);
//                field.nameDescriptions = names;
//                field.typeref = reference;
//                field.export(sb, level);
                names.export(sb, level);
                --level;
            }
            return StrH.element(sb, NullableElement, CType.Close, level);
        }
    }
    public static class DofIid extends DofType
    {
        private static String IidElement = "iid";
        
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, IidElement, TypeIdAttr, typeId, null, names == null, level);
            return StrH.exportNames(sb, IidElement, names, true, level);
        }
    }
    public static class DofOid extends DofType
    {
        public DofOid()
        {
        }
        private static String OidElement = "oid";
        @Override
        public StringBuilder export(StringBuilder sb, int level, String typeId, NameDescriptionData names)
        {
            StrH.element(sb, OidElement, TypeIdAttr, typeId, null, names == null, level);
            return StrH.exportNames(sb, OidElement, names, true, level);
        }
    }
}
