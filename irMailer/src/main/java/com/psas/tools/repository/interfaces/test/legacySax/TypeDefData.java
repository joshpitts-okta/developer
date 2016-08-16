package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;

@SuppressWarnings("javadoc")
public class TypeDefData extends LegacySaxHandler implements ChildComplete
{
    public static final String MyTag = "typedef";
    public static final String MyTagMax = "maxInclusive";
    public static final String MyTagMin = "minInclusive";
    public static final String MyTagUnit = "unit";
    public static final String MyTagEnum = "enum";

//    public static final String TypeDefTag = "typedef";
//    public static final String TypeIdAttr = "typeid";

    
    private int typeId;
    private NameDescriptionData nameDescriptions;
    private DofType type;
    private String unit;
    private List<String>enums;
    
    //working variables follow
    private StringBuilder cdata;
    private String min;
    private String max;
    private ChildComplete childComplete;
    
    public TypeDefData(ChildComplete childComplete)
    {
        this.childComplete = childComplete;
        enums = new ArrayList<String>();
    }
    
    public int getTypeId()
    {
        return typeId;
    }
    
    public void validate(ContextData context) throws SAXException
    {
        //TODO: working fix all the published worked with this test enabled
//      if(nameDescriptions == null)
//          throw new SAXException("TypeDefData.nameDescriptions is null");
      if(nameDescriptions != null)
        nameDescriptions.validate(false);
        if(typeId < 0 || typeId > 32767)
            throw new SAXException("TypeDefData.typeId is less than 0 or greater than 32767");
        if(type == null)
            throw new SAXException("TypeDefData.type is null");
        type.currentType.validate(context);
        if(unit != null && unit.length() == 0)
            throw new SAXException("TypeDefData.unit is empty string");
        for(String value : enums)
        {
            if(value != null && value.length() == 0)
                throw new SAXException("TypeDefData.enum is empty string");
        }
    }
    
    public static boolean oneOfYours(String element)
    {
        return MyTag.equals(element);
    }

//    public static boolean downYourNode(String element)
//    {
//        if (oneOfYours(element))
//            return true;
//        if (NameDescriptionData.downYourNode(element))
//            return true;
//        return DofType.oneOfYours(element);
//    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (localName.equals(MyTag))
        {
            stack.push(localName);
            if(attributes.getLength() != 1)
                throw new SAXException(currentPath() + " wrong number of attributes");
            int index = attributes.getIndex("", "id");
            if (index != 0)
                throw new SAXException(currentPath() + " could not find TypeDef.id");
            typeId = Integer.parseInt(attributes.getValue(index));
            return;
        }
        else if (stack.peek().equals(MyTag))
        {
            if (NameDescriptionData.oneOfYours(localName))
            {
                if (nameDescriptions == null)
                    nameDescriptions = new NameDescriptionData();
                nameDescriptions.startElement(uri, localName, qName, attributes);
                return;
            } else if (DofType.oneOfYours(localName))
            {
                type = new DofType();
                type.setChildComplete(this);
                type.hasEnums(enums.size() > 0);
                type.startElement(uri, localName, qName, attributes);
                return;
            }
            else if(FieldData.oneOfYours(localName))
            {
                type.startElement(uri, localName, qName, attributes);
                return;
            }
            else if (MyTagMin.equals(localName) || MyTagMax.equals(localName) || MyTagUnit.equals(localName))
            {
                cdata = new StringBuilder();
                stack.push(localName);
                return; // cdata only
            }
            else if(MyTagEnum.equals(localName))
            {
                if(attributes.getLength() != 1)
                    throw new SAXException(currentPath() + " enum wrong number of attributes");
                int index = attributes.getIndex("name");
                if(index != 0)
                    throw new SAXException(currentPath() + " enum did not find name attribute");
                enums.add(attributes.getValue(index));
                stack.push(localName);
                return;
            }
        }
        else if (DofType.oneOfYours(localName))
        {
            type.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(FieldData.oneOfYours(localName))
        {
            type.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(isMyPath(FieldData.FieldPath))
        {
            type.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level: " + localName);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if (isMyTag(MyTagMin) || isMyTag(MyTagMax) || isMyTag(MyTagUnit))
        {
            cdata.append(new String(ch, start, length));
            return;
        }
        String current = stack.peek();
        if (amIParent(MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.characters(ch, start, length);
        else if (amIParent(MyTag) && DofType.oneOfYours(current))
            type.characters(ch, start, length);
        else if(isMyPath(FieldData.FieldPath))
            type.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (localName.equals(MyTag) || localName.equals(MyTagMin) || localName.equals(MyTagMax) || localName.equals(MyTagUnit) || localName.equals(MyTagEnum))
        {
            stack.pop();
            if(localName.equals(MyTag))
                childComplete.setChildComplete(childComplete, this);
            else if (localName.equals(MyTagMin))
            {
                min = cdata.toString();
                if(min.length() == 0)
                    throw new SAXException(currentPath() + " min is empty string");
            }
            else if (localName.equals(MyTagMax))
            {
                max = cdata.toString();
                if(max.length() == 0)
                    throw new SAXException(currentPath() + " max is empty string");
            }
            else if (localName.equals(MyTagUnit))
            {
                unit = cdata.toString();
                if(unit.length() == 0)
                    throw new SAXException(currentPath() + " unit is empty string");
            }
            return;
        }
        String current = stack.peek();
        if (amIParent(MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else if (amIParent(MyTag) && DofType.oneOfYours(current))
            type.endElement(uri, localName, qName);
        else if(isMyPath(FieldData.FieldPath))
            type.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj) throws SAXException
    {
        if(min == null && max == null)
            return;
//        if(min == null)
//            throw new SAXException(currentPath() + " setChildComplete min is null with max given");
//        if(max == null)
//            throw new SAXException(currentPath() + " setChildComplete max is null with min given");
        ((DofType)obj).currentType.setRange(min, max);
        ((DofType)obj).currentType.setUnit(unit);
//        ((DofType)obj).currentType.setEnums(enums);
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        type.currentType.export(sb, level, ""+typeId, nameDescriptions);
        return sb;
    }
}
