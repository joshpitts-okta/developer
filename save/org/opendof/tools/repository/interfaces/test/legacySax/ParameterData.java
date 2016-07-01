package org.opendof.tools.repository.interfaces.test.legacySax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

//TODO: can this be combined with FieldData?
@SuppressWarnings("javadoc")
public class ParameterData extends LegacySaxHandler
{
    public static final String MyTagInput = "input";
    public static final String MyTagOutput = "output";
    public static final String MyTagTypeRef = "typeref";
    public static final String MyTagParameter = "parameter";

    private int typeref;
    private NameDescriptionData nameDescription;
    
    //working variables follow
    private ChildComplete childComplete;
    public ParameterType type;
    
    public ParameterData(ChildComplete childComplete)
    {
        this.childComplete = childComplete;
    }

    public void validate(ContextData context) throws SAXException
    {
//        if(nameDescription == null)
//            throw new SAXException("ParameterData.nameDescriptions is null");
        //TODO: PDSPThroughputTestInterface did not have codenames on these
        if(nameDescription != null)
            nameDescription.validate(false);
        if(typeref < 0 || typeref > 32767)
            throw new SAXException("ParameterData.typeref is less than 0 or greater than 32767");
        context.validateTyperef(typeref);
    }
    
    public static boolean oneOfYours(String element)
    {
        if (MyTagInput.equals(element))
            return true;
        if (MyTagOutput.equals(element))
            return true;
        if (MyTagParameter.equals(element))
            return true;
        return MyTagTypeRef.equals(element);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (localName.equals(MyTagInput) || localName.equals(MyTagOutput) || localName.equals(MyTagParameter))
        {
            stack.push(localName);
            if (attributes.getLength() != 0)
                throw new SAXException(currentPath() + " startElement did not expect attributes");
            if(localName.equals(MyTagInput))
                type = ParameterType.Inputs;
            else if(localName.equals(MyTagOutput))
                type = ParameterType.Outputs;
            else if(localName.equals(MyTagParameter))
                type = ParameterType.Parameters;
            else
                throw new SAXException(currentPath() + " startElement unexpected: " + localName);
            return;
        }
        String current = stack.peek();
        if (current.equals(MyTagInput) || current.equals(MyTagOutput)|| current.equals(MyTagParameter))
        {
            if (NameDescriptionData.oneOfYours(localName))
            {
                if(nameDescription == null)
                    nameDescription = new NameDescriptionData();
                nameDescription.startElement(uri, localName, qName, attributes);
                return;
            }else
            if (localName.equals(MyTagTypeRef))
            {
                if (attributes.getLength() != 1)
                    throw new SAXException(currentPath() + "."+ MyTagTypeRef + " startElement expected 1 attribute");
                int index = attributes.getIndex("id");
                if (index != 0)
                    throw new SAXException(currentPath() + "." + MyTagTypeRef + " startElement unexpected attribute index");
                typeref = Integer.parseInt(attributes.getValue(index));
                return;
            }
            throw new SAXException(currentPath() + " startElement unknown local name for this level: " + localName);
        }

        if ((amIParent(MyTagInput) || amIParent(MyTagOutput)) && NameDescriptionData.oneOfYours(localName))
        {
            if (nameDescription != null)
                nameDescription = new NameDescriptionData();
            nameDescription.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown local name for this level" + localName);
    }

    private boolean amITheParent()
    {
        if(amIParent(MyTagInput))
            return true;
        if(amIParent(MyTagOutput))
            return true;
        return amIParent(MyTagParameter);
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if (isMyTag(MyTagInput) || isMyTag(MyTagOutput) || isMyTag(MyTagTypeRef))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if (amITheParent() && NameDescriptionData.oneOfYours(current))
            nameDescription.characters(ch, start, length);
        else
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unknown current stack: " + value);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(localName.equals(MyTagTypeRef))
            return;
        if (isMyTag(MyTagInput) || isMyTag(MyTagOutput)|| isMyTag(MyTagParameter))
        {
            stack.pop();
            childComplete.setChildComplete(childComplete, this);
            return;
        }
        String current = stack.peek();
        if (amITheParent() && NameDescriptionData.oneOfYours(current))
           nameDescription.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }
 
    public StringBuilder export(StringBuilder sb, String element, int level)
    {
        StrH.element(sb, element, DofType.TypeRefAttr, ""+typeref, null, nameDescription == null, level);
        return StrH.exportNames(sb, element, nameDescription, true, level);
    }

    public enum ParameterType{Inputs, Outputs, Parameters}
}
