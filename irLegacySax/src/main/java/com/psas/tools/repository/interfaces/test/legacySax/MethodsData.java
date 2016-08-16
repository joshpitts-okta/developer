package org.opendof.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.opendof.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import org.opendof.tools.repository.interfaces.test.legacySax.StrH.CType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class MethodsData extends LegacySaxHandler implements ChildComplete
{
    public static final String MethodsElement = "methods";
    
    public static final String MyTag = "method";
    public static final String MethodsPath = InterfaceData.MyTag + "," + MyTag;
    
    private List<MethodData> methods;
    private MethodData currentMethod;
    
    public MethodsData()
    {
        methods = new ArrayList<MethodData>();
    }
    
    public void uniqueItemId(Hashtable<Integer, Integer> uniqueMap) throws SAXException
    {
        for(MethodData data : methods)
        {
            Integer itemId = data.itemId;
            if(uniqueMap.get(itemId) != null)
                throw new SAXException("ItemId is not unique: " + itemId);
            uniqueMap.put(itemId, itemId);
        }
    }

    public void validate(ContextData context) throws SAXException
    {
        for(MethodData method : methods)
            method.validate(context);
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
                throw new SAXException(currentPath() + " could not find method itemid");
            int id = Integer.parseInt(attributes.getValue(index));
            currentMethod = new MethodData(id);
            return;
        }
        if(isMyPath(MethodsPath))
        {
            currentMethod.startElement(uri, localName, qName, attributes);
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
        if(isMyPath(MethodsPath))
            currentMethod.characters(ch, start, length);
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
            methods.add(currentMethod);
            stack.pop();
            return;
        }
        if(isMyPath(MethodsPath))
            currentMethod.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        methods.add((MethodData)obj);
        currentMethod = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, MethodsElement, CType.Open, level);
        ++level;
        for(MethodData method : methods)
            method.export(sb, level);
        --level;
        return StrH.element(sb, MethodsElement, CType.Close, level);
    }
}
