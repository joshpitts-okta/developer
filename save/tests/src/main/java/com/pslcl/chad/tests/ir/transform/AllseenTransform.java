/*
**  Copyright (c) 2010-2015, Panasonic Corporation.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/
package com.pslcl.chad.tests.ir.transform;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.SAXParserFactory;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xerces.jaxp.SAXParserImpl;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.tools.repository.interfaces.allseen.saxParser.AllseenSaxHandler;
import org.opendof.tools.repository.interfaces.allseen.saxParser.AllseenSaxHandler.AllseenErrorHandler;
import org.opendof.tools.repository.interfaces.allseen.saxParser.node.AllseenInterface.AllseenType;
import org.opendof.tools.repository.interfaces.allseen.saxParser.node.AllseenNode;
import org.opendof.tools.repository.interfaces.opendof.saxParser.InterfaceElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.MetadataElements;
import org.opendof.tools.repository.interfaces.opendof.saxParser.MetadataElements.LanguageString;
import org.opendof.tools.repository.interfaces.opendof.saxParser.event.EventElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.event.EventsElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.exception.ExceptionElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.exception.ExceptionsElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.method.MethodElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.method.MethodsElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.property.PropertiesElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.property.PropertyElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.DofTypeBase;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.TypedefsElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.collection.DofArrayElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.collection.DofStructureElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.collection.FieldData;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofFloat32Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofFloat64Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofInt16Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofInt32Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofInt64Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofInt8Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofUint16Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofUint32Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.numerical.DofUint64Element;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.other.DofBooleanElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.other.DofStringElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.typedef.reference.DofNullableElement;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class AllseenTransform
{
    public AllseenTransform()
    {
    }
    
    private AllseenNode getNodeElement(String xml) throws Exception
    {
        ByteArrayInputStream bais = null;
        byte[] bytes = null;
        System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        SAXParserFactoryImpl spf = (SAXParserFactoryImpl) SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(false);   // can not seem to get cataloging to work
        SAXParserImpl saxParser = (SAXParserImpl)spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new AllseenErrorHandler());
        AllseenSaxHandler handler = new AllseenSaxHandler();
        AllseenSaxHandler.setPublish(false);
        xmlReader.setContentHandler(handler);
        bytes = xml.getBytes();
        bais = new ByteArrayInputStream(bytes);
        InputSource inSource = new InputSource(bais);
        xmlReader.parse(inSource);
        return handler.getNode();
    }
    
    public void doit(String xml) throws Exception
    {
        // read/walk this node and ..
        AllseenNode node = getNodeElement(xml);
        // and convert/create this object graph from it.
        InterfaceElement interfaceData = new InterfaceElement();
        
        //TODO:
        // will not be able to finish the iid until opendof supports string iids
        interfaceData.setIid(DOFInterfaceID.create("[01:{01000055}]"));
        
        interfaceData.setMetadata(getMetadata("Full_Interface"));
        addTypedefs(interfaceData);
        addProperties(interfaceData);
        addMethods(interfaceData);
        addEvents(interfaceData);
        addExceptions(interfaceData);
        
        interfaceData.validate();
        String odxml = interfaceData.export(true);
        LoggerFactory.getLogger(getClass()).info("\nlook here:\n"+ odxml);
    }
    
    private static final int CustomTypeDefItemIdOffset =  25 - AllseenType.values().length;
    private static final int PropertyItemIdOffset =  50 - AllseenType.values().length;
    private static final int MethodItemIdOffset =  75 - AllseenType.values().length;
    private static final int EventItemIdOffset =  100 - AllseenType.values().length;
    private static final int ExceptionItemIdOffset =  125 - AllseenType.values().length;
    
    private static final int ArrayType1ItemId = AllseenType.getMaxOrdinalPlus(CustomTypeDefItemIdOffset);
    private static final int ArrayType2ItemId = AllseenType.getMaxOrdinalPlus(CustomTypeDefItemIdOffset + 1);
    private static final int StructureTypeItemId = AllseenType.getMaxOrdinalPlus(CustomTypeDefItemIdOffset + 2);
    private static final int StructSelfRefTypeItemId = AllseenType.getMaxOrdinalPlus(CustomTypeDefItemIdOffset + 3);
    
    // first call is incrementAndGet, so decrement them by 1
    private static final AtomicInteger propertyItemId = new AtomicInteger(AllseenType.getMaxOrdinalPlus(PropertyItemIdOffset) - 1);
    private static final AtomicInteger methodItemId = new AtomicInteger(AllseenType.getMaxOrdinalPlus(MethodItemIdOffset) - 1);
    private static final AtomicInteger eventItemId = new AtomicInteger(AllseenType.getMaxOrdinalPlus(EventItemIdOffset) - 1);
    private static final AtomicInteger exceptionItemId = new AtomicInteger(AllseenType.getMaxOrdinalPlus(ExceptionItemIdOffset) - 1);
    
    private void addTypedefs(InterfaceElement interfaceData) throws Exception
    {
        TypedefsElement typedefs = new TypedefsElement();
        interfaceData.setTypedefs(typedefs);
        
        //NOTE there is an enum for allseen data types
        AllseenType[] types = AllseenType.values();
        for(int i=0; i < types.length; i++)
        {
            // each of these is redundant in using the type to get the string value to get the type
            // simply done to show the enum's method/values that are available to you.
            AllseenType type = AllseenType.getType(types[i].attrValue);
            addTypedef(interfaceData, type);
        }
        addArrayTypedefs(interfaceData.getTypedefs());
        addStructureTypedef(interfaceData.getTypedefs());
    }
    
    private void addArrayTypedefs(TypedefsElement typedefs)
    {
        MetadataElements metadata = getMetadata("type_String_Array_no_min_10");
        DofArrayElement array = new DofArrayElement(ArrayType1ItemId, AllseenType.String.ordinal(), metadata, null, 10);
        typedefs.addTypeDef(array);
        
        metadata = getMetadata("type_Int32_min1024_2048");
        array = new DofArrayElement(ArrayType2ItemId, AllseenType.Int32.ordinal(), metadata, 1024, 2048);
        typedefs.addTypeDef(array);
    }
    
    private void addStructureTypedef(TypedefsElement typedefs)
    {
        MetadataElements metadata = getMetadata("type_Structure_int_str_array");
        DofStructureElement struct = new DofStructureElement(StructureTypeItemId, metadata);
        metadata = getMetadata("ref_int32");
        FieldData field = new FieldData(AllseenType.Int32.ordinal(),  metadata);
        struct.addField(field);

        metadata = getMetadata("ref_string");
        field = new FieldData(AllseenType.String.ordinal(),  metadata);
        struct.addField(field);

        metadata = getMetadata("ref_array");
        field = new FieldData(ArrayType2ItemId,  metadata);
        struct.addField(field);
        typedefs.addTypeDef(struct);
        
        metadata = getMetadata("type_NullableSelfRef");
        typedefs.addTypeDef(new DofNullableElement(StructSelfRefTypeItemId, StructureTypeItemId, metadata));
    }
    
    private void addTypedef(InterfaceElement interfaceData, AllseenType type) throws Exception
    {
        DofTypeBase doftype = null;
        switch(type)
        {
            //NOTE: using the enum's ordinal value for the item_id gives an easy unique integer
            //      that can be accessed easily/repeatably via the enum constant.
            case Uint8:
                MetadataElements metadata = getMetadata("type_Uint8");
                doftype = new DofInt8Element(type.ordinal(), metadata, null, null, null);
                break;
            case Boolean:
                metadata = getMetadata("type_Boolean");
                doftype = new DofBooleanElement(type.ordinal(), metadata);
                break;
            case Float32:
                metadata = getMetadata("type_Float32");
                doftype = new DofFloat32Element(type.ordinal(), metadata, null, null, null);
                break;
            case Float64:
                metadata = getMetadata("type_Float64");
                doftype = new DofFloat64Element(type.ordinal(), metadata, null, null, null);
                break;
            case Int16:
                Short min = 0;
                Short max = 1000;
                metadata = getMetadata("type_Int16_ranged_miles");
                doftype = new DofInt16Element(type.ordinal(), metadata, min, max, "miles");
                break;
            case Int32:
                metadata = getMetadata("type_Int32");
                doftype = new DofInt32Element(type.ordinal(), metadata, null, null, null);
                break;
            case Int64:
                metadata = getMetadata("type_64");
                doftype = new DofInt64Element(type.ordinal(), metadata, null, null, null);
                break;
            case ObjPath:
                metadata = getMetadata("type_ObjPath");
                doftype = new DofStringElement(type.ordinal(), metadata, 3, 256);
                break;
            case Signature:
                metadata = getMetadata("type_Signature");
                doftype = new DofStringElement(type.ordinal(), metadata, 3, 256);
                break;
            case String:
                metadata = getMetadata("type_String");
                doftype = new DofStringElement(type.ordinal(), metadata, 3, 256);
                break;
            case Uint16:
                metadata = getMetadata("type_Uint16");
                doftype = new DofUint16Element(type.ordinal(), metadata, null, null, null);
                break;
            case Uint32:
                metadata = getMetadata("type_Uint32");
                doftype = new DofUint32Element(type.ordinal(), metadata, null, null, null);
                break;
            case Uint64:
                metadata = getMetadata("type_Uint64");
                doftype = new DofUint64Element(type.ordinal(), metadata, null, null, null);
                break;
            default:
                throw new Exception("unknown type: " + type.name());
        }
        interfaceData.getTypedefs().addTypeDef(doftype);
    }
    
    private void addProperties(InterfaceElement interfaceData)
    {
        PropertiesElement properties = new PropertiesElement();
        interfaceData.setProperties(properties);
        AllseenType[] types = AllseenType.values();
        for(int i=0; i < types.length; i++)
        {
            AllseenType type = types[i];
            int itemId = propertyItemId.incrementAndGet();
            MetadataElements metadata = getMetadata("property_"+type.name());
            PropertyElement property = new PropertyElement(itemId, type.ordinal(), metadata, true, true);
            properties.addProperty(property);
        }
    }
    
    private void addMethods(InterfaceElement interfaceData)
    {
        MethodsElement methods = new MethodsElement();
        interfaceData.setMethods(methods);
        
        MetadataElements metadata = getMetadata("method_empty");
        MethodElement method = new MethodElement(methodItemId.incrementAndGet(), metadata);
        methods.addMethod(method);
        
        metadata = getMetadata("method_inputs");
        method = new MethodElement(methodItemId.incrementAndGet(), metadata);
        metadata = getMetadata("input_arg_float32");
        method.addInput(new FieldData(AllseenType.Float32.ordinal(), metadata));
        metadata = getMetadata("input_arg_float64");
        method.addInput(new FieldData(AllseenType.Float64.ordinal(), metadata));
        methods.addMethod(method);
        
        metadata = getMetadata("method_outputs");
        method = new MethodElement(methodItemId.incrementAndGet(), metadata);
        metadata = getMetadata("output_arg_string");
        method.addOutput(new FieldData(AllseenType.String.ordinal(), metadata));
        metadata = getMetadata("output_arg_int64");
        method.addOutput(new FieldData(AllseenType.Int64.ordinal(), metadata));
        metadata = getMetadata("output_arg_boolean");
        method.addOutput(new FieldData(AllseenType.Boolean.ordinal(), metadata));
        methods.addMethod(method);
        
        metadata = getMetadata("method_inout");
        method = new MethodElement(methodItemId.incrementAndGet(), metadata);
        metadata = getMetadata("inout_in_arg_float32");
        method.addInput(new FieldData(AllseenType.Float32.ordinal(), metadata));
        metadata = getMetadata("inout_in_arg_float64");
        method.addInput(new FieldData(AllseenType.Float64.ordinal(), metadata));
        metadata = getMetadata("inout_out_arg_string");
        method.addOutput(new FieldData(AllseenType.String.ordinal(), metadata));
        metadata = getMetadata("inout_out_arg_int64");
        method.addOutput(new FieldData(AllseenType.Int64.ordinal(), metadata));
        metadata = getMetadata("inout_out_arg_boolean");
        method.addOutput(new FieldData(AllseenType.Boolean.ordinal(), metadata));
        methods.addMethod(method);
    }
    
    private void addEvents(InterfaceElement interfaceData)
    {
        EventsElement events = new EventsElement();
        interfaceData.setEvents(events);
        
        MetadataElements metadata = getMetadata("event_empty");
        EventElement event = new EventElement(eventItemId.incrementAndGet(), metadata);
        events.addEvent(event);
        
        metadata = getMetadata("event_outputs");
        event = new EventElement(eventItemId.incrementAndGet(), metadata);
        metadata = getMetadata("output_arg_string");
        event.addOutput(new FieldData(AllseenType.String.ordinal(), metadata));
        metadata = getMetadata("output_arg_int64");
        event.addOutput(new FieldData(AllseenType.Int64.ordinal(), metadata));
        metadata = getMetadata("output_arg_boolean");
        event.addOutput(new FieldData(AllseenType.Boolean.ordinal(), metadata));
        events.addEvent(event);
    }
    
    private void addExceptions(InterfaceElement interfaceData)
    {
        ExceptionsElement exceptions = new ExceptionsElement();
        interfaceData.setExceptions(exceptions);
        
        MetadataElements metadata = getMetadata("exception_empty");
        ExceptionElement exception = new ExceptionElement(exceptionItemId.incrementAndGet(), metadata);
        exceptions.addException(exception);
        
        metadata = getMetadata("exception_outputs");
        exception = new ExceptionElement(exceptionItemId.incrementAndGet(), metadata);
        metadata = getMetadata("output_arg_string");
        exception.addOutput(new FieldData(AllseenType.String.ordinal(), metadata));
        metadata = getMetadata("output_arg_int64");
        exception.addOutput(new FieldData(AllseenType.Int64.ordinal(), metadata));
        metadata = getMetadata("output_arg_boolean");
        exception.addOutput(new FieldData(AllseenType.Boolean.ordinal(), metadata));
        exceptions.addException(exception);
    }
    
    private MetadataElements getMetadata(String baseValue)
    {
        MetadataElements metadata = new MetadataElements("cn_"+baseValue);
        metadata.addDisplayName(new LanguageString("en", "dn_"+baseValue));
        metadata.addDisplayName(new LanguageString("ja", "ã‚¨ã‚¢ã‚³ãƒ³ãƒ‡ã‚£ã‚·ãƒ§ãƒŠãƒ¼ã®çŠ¶æ…‹ï¼Ž"));
        metadata.addDescription(new LanguageString("en", "desc_"+baseValue));
        metadata.addDescription(new LanguageString("ja", "ã‚¨ã‚¢ã‚³ãƒ³ãƒ‡ã‚£ã‚·ãƒ§ãƒŠãƒ¼ã®çŠ¶æ…‹ï¼Ž"));
        return metadata;
    }
}
