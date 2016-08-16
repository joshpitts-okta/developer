package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

//TODO: if this passes review ... drop this and use FieldData
@SuppressWarnings("javadoc")
public class PropertiesData extends LegacySaxHandler implements ChildComplete
{
    public static final String PropertiesElement = "properties";
    
    public static final String MyTag = "property";
    public static final String PropertiesPath = InterfaceData.MyTag + "," + MyTag;
    
    private List<PropertyData> properties;
    private PropertyData currentProperty;
    
    public PropertiesData()
    {
        properties = new ArrayList<PropertyData>();
    }
    
    public void uniqueItemId(Hashtable<Integer, Integer> uniqueMap) throws SAXException
    {
        for(PropertyData data : properties)
        {
            Integer itemId = data.itemId;
            if(uniqueMap.get(itemId) != null)
                throw new SAXException("ItemId is not unique: " + itemId);
            uniqueMap.put(itemId, itemId);
        }
    }

    public void validate(ContextData context) throws SAXException
    {
        for(PropertyData prop : properties)
            prop.validate(context);
    }
    
    public static boolean oneOfYours(String element)
    {
        return MyTag.equals(element);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (localName.equals(MyTag))
        {
            stack.push(localName);
            if(attributes.getLength() != 3)
                throw new SAXException(currentPath() + " expected 3 attibutes");
            int index = attributes.getIndex("", "id");
            if(index < 0)
                throw new SAXException(currentPath() + " could not find property itemid");
            int id = Integer.parseInt(attributes.getValue(index));
            index = attributes.getIndex("", "readable");
            if(index < 0)
                throw new SAXException(currentPath() + " could not find property readable");
            boolean readable = Boolean.parseBoolean(attributes.getValue(index));
            index = attributes.getIndex("", "writeable");
            if(index < 0)
                throw new SAXException(currentPath() + " could not find property writeable");
            boolean writable = Boolean.parseBoolean(attributes.getValue(index));
            currentProperty = new PropertyData(id, readable, writable, this);
            return;
        }
        if(isMyPath(PropertiesPath))
        {
            currentProperty.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level: " + localName);
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(isMyTag(MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        if(isMyPath(PropertiesPath))
            currentProperty.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(isMyPath(PropertiesPath))
            currentProperty.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        properties.add((PropertyData)obj);
        currentProperty = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, PropertiesElement, CType.Open, level);
        ++level;
        for(PropertyData prop : properties)
            prop.export(sb, level);
        --level;
        return StrH.element(sb, PropertiesElement, CType.Close, level);
    }
}
