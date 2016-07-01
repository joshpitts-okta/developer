package org.opendof.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.opendof.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import org.opendof.tools.repository.interfaces.test.legacySax.StrH.CType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class EventsData extends LegacySaxHandler implements ChildComplete
{
    public static final String EventsElement = "events";
    
    public static final String MyTag = "event";
    public static final String EventsPath = InterfaceData.MyTag + "," + MyTag;
    
    private List<EventData> events;
    private EventData currentEvent;
    
    public EventsData()
    {
        events = new ArrayList<EventData>();
    }
    
    public void uniqueItemId(Hashtable<Integer, Integer> uniqueMap) throws SAXException
    {
        for(EventData data : events)
        {
            Integer itemId = data.itemId;
            if(uniqueMap.get(itemId) != null)
                throw new SAXException("ItemId is not unique: " + itemId);
            uniqueMap.put(itemId, itemId);
        }
    }
    
    public void validate(ContextData context) throws SAXException
    {
        for(EventData event : events)
            event.validate(context);
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
                throw new SAXException(currentPath() + " could not find event itemid");
            int id = Integer.parseInt(attributes.getValue(index));
            currentEvent = new EventData(id);
            return;
        }
        if(isMyPath(EventsPath))
        {
            currentEvent.startElement(uri, localName, qName, attributes);
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
        if(isMyPath(EventsPath))
            currentEvent.characters(ch, start, length);
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
            events.add(currentEvent);
            stack.pop();
            return;
        }
        if(isMyPath(EventsPath))
            currentEvent.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        events.add((EventData)obj);
        currentEvent = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, EventsElement, CType.Open, level);
        ++level;
        for(EventData event : events)
            event.export(sb, level);
        --level;
        return StrH.element(sb, EventsElement, CType.Close, level);
    }
}
