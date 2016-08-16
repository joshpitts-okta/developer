package com.psas.tools.repository.interfaces.test.legacySax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.DofType.DofStructure;
import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;

@SuppressWarnings("javadoc")
public class StructureData extends LegacySaxHandler implements ChildComplete
{
    private static final String MyTag = "structure";
    public static final String StructurePath = ContextData.TypeDefPath + "," + MyTag;
    
    //working variables follow
    private DofStructure structure;
    private FieldData currentField;
    
    public StructureData(DofStructure structure)
    {
        this.structure = structure;
    }
    
    public void validate(ContextData context) throws SAXException
    {
//        if(nameDescriptions == null)
//            throw new SAXException("EventData.nameDescriptions is null");
//        nameDescriptions.validate(true);
//        if(itemId < 0 || itemId > 32767)
//            throw new SAXException("EventDAta.itemId is less than 0 or greater than 32767");
//        for(ParameterData param : outputs)
//            param.validate(context);
    }
    
    public static boolean oneOfYours(String element)
    {
        if(element.equals(MyTag))
            return true;
        return false;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if(stack.peek().equals(MyTag))
        {
            if(localName.equals(FieldData.MyTag))
            {
                if(currentField == null)
                    currentField = new FieldData(this);
                currentField.startElement(uri, localName, qName, attributes);
                return;
            }
        }
        if(isMyPath(FieldData.FieldPath))
        {
            currentField.startElement(uri, localName, qName, attributes);
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
        if(isMyPath(FieldData.FieldPath))
            currentField.characters(ch, start, length);
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
            stack.pop();
            return;
        }
        if(isMyPath(FieldData.FieldPath))
            currentField.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        if(listener != this)
            return;
        structure.fields.add((FieldData)obj);
        currentField = null;
    }
}
