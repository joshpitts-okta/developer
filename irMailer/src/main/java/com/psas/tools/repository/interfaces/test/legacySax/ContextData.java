package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler.ChildComplete;
import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

@SuppressWarnings("javadoc")
public class ContextData extends LegacySaxHandler implements ChildComplete
{
    public static final String TypeDefsElement = "typedefs";
    
    public static final String MyTag = "context";
    
    private static final String ContextPath = InterfaceData.MyTag + "," + MyTag;
    public static final String TypeDefPath = ContextPath + "," + TypeDefData.MyTag;
    
    
    private final List<TypeDefData> typedefs;
    
    //working variables follow
    private TypeDefData currentTypedef;

    public ContextData()
    {
        typedefs = new ArrayList<TypeDefData>();
    }

    public void validate() throws SAXException
    {
    	// TODO: removed this test
//        if(typedefs.size() < 1)
//            throw new SAXException("Context.typedefs is empty");
        Hashtable<Integer, Integer> uniqueMap = new Hashtable<Integer, Integer>();
        for(TypeDefData tdd : typedefs)
        {
            tdd.validate(this);
            Integer id = tdd.getTypeId();
            if(uniqueMap.get(id) != null)
                throw new SAXException("Context.typedefs not unique: " + id.toString());
            uniqueMap.put(id, id);
        }
    }
    
    public void validateTyperef(int typeref) throws SAXException
    {
        for(TypeDefData tdd : typedefs)
        {
            if(typeref == tdd.getTypeId())
                return;
        }
        throw new SAXException("unknown typeref: " + typeref);
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
            if(attributes.getLength() != 0)
                throw new SAXException(currentPath() + " did not expect attributes");
            return;
        }
        if(stack.peek().equals(MyTag))
        {
            if(TypeDefData.oneOfYours(localName))
            {
                if(currentTypedef == null)
                    currentTypedef = new TypeDefData(this);
                currentTypedef.startElement(uri, localName, qName, attributes);
                return;
            }
            throw new SAXException(currentPath() + " startElement unknown localName for this level" + localName);
        }
        if(isMyPath(TypeDefPath))
        {
            currentTypedef.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(currentPath() + " startElement unknown localName for this level" + localName);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if (isMyTag(MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        if(isMyPath(TypeDefPath))
            currentTypedef.characters(ch, start, length);
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
        if(isMyPath(TypeDefPath))
            currentTypedef.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown current stack localName: " + localName);
    }

    @Override
    public void setChildComplete(Object listener, Object obj) throws SAXException
    {
        typedefs.add(((TypeDefData)obj)); 
        currentTypedef = null;
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        StrH.element(sb, TypeDefsElement, CType.Open, level);
        ++level;
        for(TypeDefData type : typedefs)
            type.export(sb, level);
        --level;
        StrH.element(sb, TypeDefsElement, CType.Close, level);
        return sb;
    }
}
