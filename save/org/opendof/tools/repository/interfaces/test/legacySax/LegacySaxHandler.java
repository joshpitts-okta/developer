package org.opendof.tools.repository.interfaces.test.legacySax;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("javadoc")
public class LegacySaxHandler extends DefaultHandler
{
    protected final Logger log;
    public final static Stack<String> stack = new Stack<String>();

    private InterfaceData interfaceData;

    public LegacySaxHandler()
    {
        log = LoggerFactory.getLogger(LegacySaxHandler.class);
    }

    public InterfaceData getInterface()
    {
        return interfaceData;
    }
    
    public boolean isMyTag(String tag)
    {
        return stack.peek().equals(tag);
    }

    public boolean amIParent(String parent)
    {
        int size = stack.size();
        if (size < 2)
            return false;
        if (stack.get(size - 2).equals(parent))
            return true;
        return false;
    }

    public boolean isMyPath(String path)
    {
        String[] elements = path.split(",");
        int size = stack.size();
        if(size < elements.length)
            return false;
        for (int i = 0; i < elements.length; i++)
        {
            if (!stack.get(i).equals(elements[i]))
                return false;
        }
        return true;
    }

    public String currentPath()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stack.size(); i++)
        {
            if(i != 0)
                sb.append(".");
            sb.append(stack.get(i));
        }
        return sb.toString();
    }
    
    public String pathToString(String path)
    {
        StringBuilder sb = new StringBuilder();
        String[] elements = path.split(",");
        for (int i = 0; i < elements.length; i++)
        {
            if(i != 0)
                sb.append(".");
            sb.append(elements[i]);
        }
        return sb.toString();
    }
    
    @Override
    public void startDocument() throws SAXException
    {
        interfaceData = new InterfaceData();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        LegacySaxParser.nameSpaces.put(uri, uri);
        interfaceData.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        interfaceData.endElement(uri, localName, qName);
    }

    @Override
    public void endDocument() throws SAXException
    {
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        String value = new String(ch, start, length).trim();
        if (value.length() == 0)
            return; // ignore white space
        interfaceData.characters(ch, start, length);
    }

    @Override
    public void skippedEntity(String name) throws SAXException
    {
        throw new SAXException("skippedEntity: " + name);
    }

    public static void logAttrs(Attributes attributes, StringBuilder sb)
    {
        for (int i = 0; i < attributes.getLength(); i++)
        {
            sb.append("\n\tindex: " + i);
            sb.append("\n\t\turi: " + attributes.getURI(i));
            sb.append("\n\t\tlocalName: " + attributes.getLocalName(i));
            sb.append("\n\t\tqname: " + attributes.getQName(i));
            sb.append("\n\t\ttype: " + attributes.getType(i));
            sb.append("\n\t\tvalue: " + attributes.getValue(i));
            //            public int getIndex (String uri, String localName);
            //            public int getIndex (String qName);
            //            public abstract String getType (String uri, String localName);
            //            public abstract String getType (String qName);
            //            public abstract String getValue (String uri, String localName);
            //            public abstract String getValue (String qName);
        }
    }


    public interface ChildComplete
    {
        public void setChildComplete(Object listener, Object value) throws SAXException;
    }
}
