package org.opendof.tools.repository.interfaces.test.legacySax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class FieldData extends LegacySaxHandler
{
    public static final String FieldElement ="field";
//    public static final String TypeRefElement ="typeref";
    public static final String InputElement ="input";
    public static final String OutputElement ="output";
//    public static final String TypeRefAttr = "typeref";
    
    public static final String MyTag = "field";
    private static final String MyTagTypeRef = "typeref";
    public static final String FieldPath = StructureData.StructurePath + "," + MyTag;

    
    public NameDescriptionData nameDescriptions;
    public int typeref;
    
    private ChildComplete childComplete;
    
    public FieldData(ChildComplete childComplete)
    {
        this.childComplete = childComplete;
    }
    
    public void validate(ContextData context) throws SAXException
    {
        //TODO: working fix all the published worked with this test enabled
//      if(nameDescriptions == null)
//          throw new SAXException("FieldData.nameDescriptions is null");
      if(nameDescriptions != null)
        nameDescriptions.validate(false);
        if(typeref < 0 || typeref > 32767)
            throw new SAXException("FieldData.typeref is less than 0 or greater than 32767");
        context.validateTyperef(typeref);
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
        if (localName.equals(MyTag))
        {
            stack.push(localName);
            if(attributes.getLength() != 0)
                throw new SAXException(currentPath() + " did not expect attributes");
            return;
        }
        if(stack.peek().equals(MyTag))
        {
            if(NameDescriptionData.oneOfYours(localName))
            {
                if(nameDescriptions == null)
                    nameDescriptions = new NameDescriptionData();
                nameDescriptions.startElement(uri, localName, qName, attributes);
                return;
            }
            if(localName.equals(MyTagTypeRef))
            {
                if(attributes.getLength() != 1)
                    throw new SAXException(currentPath() + " wrong number of attributes");
                int index = attributes.getIndex("id");
                if(index != 0)
                    throw new SAXException(currentPath() + " wrong index for type ref id");
                typeref = Integer.parseInt(attributes.getValue(index));
                return;
            }
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
        String current = stack.peek();
        if(amIParent(MyTag) && NameDescriptionData.oneOfYours(current))
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
        if(childComplete == null)
            return;  // property currently does not support metadata block
        if(localName.equals(MyTag))
        {
            stack.pop();
            childComplete.setChildComplete(childComplete, this);
            return;
        }
        String current = stack.peek();
        if(amIParent(MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }
    
    public StringBuilder export(StringBuilder sb, String element, int level)
    {
        StrH.element(sb, element, DofType.TypeRefAttr, ""+typeref, null, nameDescriptions == null, level);
        return StrH.exportNames(sb, element, nameDescriptions, true, level);
    }

}
