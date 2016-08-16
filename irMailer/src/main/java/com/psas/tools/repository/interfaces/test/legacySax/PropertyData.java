package com.psas.tools.repository.interfaces.test.legacySax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;

@SuppressWarnings("javadoc")
public class PropertyData extends LegacySaxHandler implements ChildComplete
{
    public static final String PropertyElement = "property";
    public static final String ReadAttr = "read";
    public static final String WriteAttr = "write";
    
    public int itemId;
    private FieldData typeref;
    private boolean readable;
    private boolean writeable;
    
    //working
    private NameDescriptionData nameDescriptions; // pushed to typeref

    private ChildComplete childComplete;
    
    public PropertyData(int itemId, boolean readable, boolean writeable, ChildComplete childComplete)
    {
        this.itemId = itemId;
        this.readable = readable;
        this.writeable = writeable;
        this.childComplete = childComplete;
    }
    
    public void validate(ContextData context) throws SAXException
    {
//        if(nameDescriptions == null)
//            throw new SAXException("ProprtyData.nameDescriptions is null");
        if(nameDescriptions != null)
            nameDescriptions.validate(false);
        if(itemId < 0 || itemId > 32767)
            throw new SAXException("PropertyData.itemId is less than 0 or greater than 32767");
        if(typeref == null)
            throw new SAXException("PropertyData.typeref is null");
        typeref.validate(context);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if(stack.peek().equals(PropertiesData.MyTag))
        {
            if(NameDescriptionData.oneOfYours(localName))
            {
                if(nameDescriptions == null)
                    nameDescriptions = new NameDescriptionData();
                nameDescriptions.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(localName.equals(ParameterData.MyTagTypeRef))
            {
                if(attributes.getLength() != 1)
                    throw new SAXException(currentPath() + " startElement wrong number of attributes for localName: " + localName);
                typeref = new FieldData(null);
                typeref.typeref = Integer.parseInt(attributes.getValue(0));
                return;
            }
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level: " + localName);
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(isMyTag(PropertiesData.MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if(amIParent(PropertiesData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(localName.equals(PropertiesData.MyTag))
        {
            stack.pop();
            childComplete.setChildComplete(childComplete, this);
            typeref.nameDescriptions = nameDescriptions;
            return;
        }
        String current = stack.peek();
        if(amIParent(PropertiesData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else if(isMyPath(PropertiesData.PropertiesPath) && localName.equals(ParameterData.MyTagTypeRef))
            return; // did not push stack
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }
    
    @Override
    public void setChildComplete(Object listener, Object obj) throws SAXException
    {
        if(nameDescriptions != null)
            ((FieldData)obj).nameDescriptions = nameDescriptions;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        //@formatter:off
        StrH.element(sb, PropertyElement, 
                        new String[]{InterfaceData.ItemIdAttr, DofType.TypeRefAttr, ReadAttr, WriteAttr}, 
                        new String[]{""+itemId, ""+typeref.typeref, ""+readable, ""+writeable}, 
                        null, typeref.nameDescriptions == null, level);
        return StrH.exportNames(sb, PropertyElement, typeref.nameDescriptions, true, level);
        //@formatter:on
//        ++level;
//        typeref.export(sb, level);
//        --level;
//        return StrH.element(sb, PropertyElement, CType.Close, level);
    }
}
