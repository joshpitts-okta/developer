package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

@SuppressWarnings("javadoc")
public class ExceptionsData extends LegacySaxHandler implements ChildComplete
{
    public static final String ExceptionsElement = "exceptions";
    
    public static final String MyTag = "exception";
    public static final String ExceptionsPath = InterfaceData.MyTag + "," + MyTag;
    
    private List<ExceptionData> exceptions;
    private ExceptionData currentException;
    
    public ExceptionsData()
    {
        exceptions = new ArrayList<ExceptionData>();
    }
    
    public void uniqueItemId(Hashtable<Integer, Integer> uniqueMap) throws SAXException
    {
        for(ExceptionData data : exceptions)
        {
            Integer itemId = data.itemId;
            if(uniqueMap.get(itemId) != null)
                throw new SAXException("ItemId is not unique: " + itemId);
            uniqueMap.put(itemId, itemId);
        }
    }
    
    public void validate(ContextData context) throws SAXException
    {
        for(ExceptionData ex : exceptions)
            ex.validate(context);
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
            int index = attributes.getIndex("", "id");
            if(index != 0)
                throw new SAXException(currentPath() + " could not find exception itemid");
            int id = Integer.parseInt(attributes.getValue(index));
            currentException = new ExceptionData(id);
            return;
        }
        if(isMyPath(ExceptionsPath))
        {
            currentException.startElement(uri, localName, qName, attributes);
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
        if(isMyPath(ExceptionsPath))
            currentException.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(localName.equals(MyTag))
        {
            exceptions.add(currentException);
            stack.pop();
            return;
        }
        if(isMyPath(ExceptionsPath))
            currentException.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        exceptions.add((ExceptionData)obj);
        currentException = null;
    }

    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, ExceptionsElement, CType.Open, level);
        ++level;
        for(ExceptionData exception : exceptions)
            exception.export(sb, level);
        --level;
        return StrH.element(sb, ExceptionsElement, CType.Close, level);
    }
}
