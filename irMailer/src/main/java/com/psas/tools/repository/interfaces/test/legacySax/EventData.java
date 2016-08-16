package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

@SuppressWarnings("javadoc")
public class EventData extends LegacySaxHandler implements ChildComplete
{
    public static final String EventElement = "event";
    
    private static final String ParametersPath = EventsData.EventsPath + "," + ParameterData.MyTagParameter;
    private static final String TypeRefPath = EventsData.EventsPath + "," + ParameterData.MyTagTypeRef;
    
    private NameDescriptionData nameDescriptions;
    private final List<ParameterData> outputs;
    public int itemId;
    
    //working variables follow
    private ParameterData currentParameter;
    
    public EventData(int itemId)
    {
        this.itemId = itemId;
        outputs = new ArrayList<ParameterData>();
    }
    
    public void validate(ContextData context) throws SAXException
    {
        //TODO: working fix all the published worked with this test enabled
//      if(nameDescriptions == null)
//          throw new SAXException("EventData.nameDescriptions is null");
      if(nameDescriptions != null)
        nameDescriptions.validate(false);
        if(itemId < 0 || itemId > 32767)
            throw new SAXException("EventData.itemId is less than 0 or greater than 32767");
        for(ParameterData param : outputs)
            param.validate(context);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if(stack.peek().equals(EventsData.MyTag))
        {
            if(NameDescriptionData.oneOfYours(localName))
            {
                if(nameDescriptions == null)
                    nameDescriptions = new NameDescriptionData();
                nameDescriptions.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(ParameterData.oneOfYours(localName))
            {
                if(currentParameter == null)
                    currentParameter = new ParameterData(this);
                currentParameter.startElement(uri, localName, qName, attributes);
                return;
            }
        }
        if(isMyPath(ParametersPath)|| isMyPath(TypeRefPath))
        {
            currentParameter.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level: " + localName);
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(isMyTag(EventsData.MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if(amIParent(EventsData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.characters(ch, start, length);
        else if(isMyPath(ParametersPath) || isMyPath(TypeRefPath))
            currentParameter.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(localName.equals(EventsData.MyTag))
        {
            stack.pop();
            return;
        }
        String current = stack.peek();
        if(amIParent(EventsData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else if(isMyPath(ParametersPath) || isMyPath(TypeRefPath))
            currentParameter.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        outputs.add((ParameterData)obj);
        currentParameter = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, EventElement, InterfaceData.ItemIdAttr, ""+itemId, null, nameDescriptions == null && outputs.size() == 0, level);
        StrH.exportNames(sb, null, nameDescriptions, true, level);
        if(outputs.size() > 0)
        {
            ++level;
            StrH.element(sb, MethodData.OutputsElement, CType.Open, level);
            ++level;
            for(ParameterData param : outputs)
                param.export(sb, FieldData.OutputElement, level);
            --level;
            StrH.element(sb, MethodData.OutputsElement, CType.Close, level);
            --level;
        }
        return StrH.element(sb, EventElement, CType.Close, level);
    }
}
