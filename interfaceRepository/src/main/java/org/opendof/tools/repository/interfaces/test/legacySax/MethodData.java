package org.opendof.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.List;

import org.opendof.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import org.opendof.tools.repository.interfaces.test.legacySax.StrH.CType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class MethodData extends LegacySaxHandler implements ChildComplete
{
    public static final String MethodElement = "method";
    public static final String InputsElement = "inputs";
    public static final String OutputsElement = "outputs";
    
    private static final String InputsPath = MethodsData.MethodsPath + "," + ParameterData.MyTagInput;
    private static final String OutputsPath = MethodsData.MethodsPath + "," + ParameterData.MyTagOutput;
    private static final String TypeRefPath = MethodsData.MethodsPath + "," + ParameterData.MyTagTypeRef;
    
    private NameDescriptionData nameDescriptions;
    private final List<ParameterData> inputs;
    private final List<ParameterData> outputs;
    public int itemId;
    
    private ParameterData currentParameter;
    
    public MethodData(int itemId)
    {
        this.itemId = itemId;
        inputs = new ArrayList<ParameterData>();
        outputs = new ArrayList<ParameterData>();
    }
    
    public void validate(ContextData context) throws SAXException
    {
        //TODO: working fix all the published worked with this test enabled
//      if(nameDescriptions == null)
//          throw new SAXException("MethodData.nameDescriptions is null");
      if(nameDescriptions != null)
        nameDescriptions.validate(false);
        if(itemId < 0 || itemId > 32767)
            throw new SAXException("ExceptionData.itemId is less than 0 or greater than 32767");
        for(ParameterData param : inputs)
            param.validate(context);
        for(ParameterData param : outputs)
            param.validate(context);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if(stack.peek().equals(MethodsData.MyTag))
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
        if(isMyPath(InputsPath) || isMyPath(OutputsPath) || isMyPath(TypeRefPath))
        {
            currentParameter.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level: " + localName);
    }
    
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(isMyTag(MethodsData.MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if(amIParent(MethodsData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.characters(ch, start, length);
        else if(isMyPath(InputsPath) || isMyPath(OutputsPath) || isMyPath(TypeRefPath))
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
        if(localName.equals(MethodsData.MyTag))
        {
            stack.pop();
            return;
        }
        String current = stack.peek();
        if(amIParent(MethodsData.MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else if(isMyPath(InputsPath) || isMyPath(OutputsPath) || isMyPath(TypeRefPath))
            currentParameter.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj)
    {
        ParameterData parameter = (ParameterData)obj;
        if(parameter.type == ParameterData.ParameterType.Inputs)
            inputs.add(parameter);
        else
            outputs.add(parameter);
        currentParameter = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        //@formatter:off
        StrH.element(
            sb, MethodElement, InterfaceData.ItemIdAttr, ""+itemId, null, 
            nameDescriptions == null && inputs.size() == 0 && outputs.size() == 0, level);
        //@formatter:on
        StrH.exportNames(sb, null, nameDescriptions, true, level);
        if(inputs.size() > 0)
        {
            ++level;
            StrH.element(sb, InputsElement, CType.Open, level);
            ++level;
            for(ParameterData param : inputs)
                param.export(sb, FieldData.InputElement, level);
            --level;
            StrH.element(sb, InputsElement, CType.Close, level);
            --level;
        }
        
        if(outputs.size() > 0)
        {
            ++level;
            StrH.element(sb, OutputsElement, CType.Open, level);
            ++level;
            for(ParameterData param : outputs)
                param.export(sb, FieldData.OutputElement, level);
            --level;
            StrH.element(sb, OutputsElement, CType.Close, level);
            --level;
        }
        return StrH.element(sb, MethodElement, CType.Close, level);
    }
}
