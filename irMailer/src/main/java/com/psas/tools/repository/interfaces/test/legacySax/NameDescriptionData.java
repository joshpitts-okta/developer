package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class NameDescriptionData extends LegacySaxHandler
{
    private static final String InterfaceMetaNs = "http://emitdo.pew.com/schema/InterfaceMeta";
    @SuppressWarnings("unused")
    private static final String CodeNamesNs = "http://emitdo.pew.com/schema/CodeNames";
    private static final String XmlLangNs = "http://www.w3.org/XML/1998/namespace";
    private static final String XmlLangQname = "xml:lang";
    
    private static final String MyTag1 = "description";
    private static final String MyTag2 = "name";
    
    private static final String CodeNameTag = "md:code-name";
    private static final String DisplayNameTag = "md:display-name";
    private static final String DescriptionTag = "md:description";
    
    private String codeName;
    private final List<LanguageString> descriptions;
    private final List<LanguageString> displayNames;
    
    //working variables follow
    private StringBuilder cdata;
    private final AtomicBoolean codenameElement;
    private LanguageString langString;
    
    public NameDescriptionData()
    {
        codeName = null;
        descriptions = new ArrayList<LanguageString>();
        displayNames = new ArrayList<LanguageString>();
        codenameElement = new AtomicBoolean(false);
    }
    
    public void validate(boolean codeNameRequired) throws SAXException
    {
        Hashtable<String, String> uniqueMap = new Hashtable<String, String>();
        if(codeName != null && codeName.length() < 1)
            throw new SAXException("codename with empty string");
        if(codeNameRequired)
        {
            if(codeName == null || codeName.length() < 1)
                throw new SAXException("codename is required but no given");
        }
            
        for(LanguageString lang : descriptions)
        {
            lang.validate();
//            if(uniqueMap.get(lang.attrValue) != null)
//                throw new SAXException("description with duplicate language: " + lang.attrValue);
            uniqueMap.put(lang.attrValue, lang.attrValue);
        }
        uniqueMap.clear();
        for(LanguageString lang : displayNames)
        {
            lang.validate();
//            if(uniqueMap.get(lang.attrValue) != null)
//                throw new SAXException("description with duplicate language: " + lang.attrValue);
//            uniqueMap.put(lang.attrValue, lang.attrValue);
        }
        
    }
    
    public static boolean oneOfYours(String element)
    {
        if(MyTag1.equals(element))
            return true;
        return MyTag2.equals(element);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        cdata = new StringBuilder();
        if(localName.equals(MyTag1) || localName.equals(MyTag2))
        {
            stack.push(localName);
            if(MyTag2.equals(localName))
            {
                if(!InterfaceMetaNs.equals(uri))
                {
                    if(attributes.getLength() > 0)
                        throw new SAXException(currentPath() + " startElement codename did not expect any attributes");
                    codenameElement.set(true);
                    return;
                }
            }
            codenameElement.set(false);
            if(!uri.equals(InterfaceMetaNs))
                throw new SAXException(currentPath() + " expect namespace: " + InterfaceMetaNs);
            langString = new LanguageString(attributes);
            langString.currentPath = currentPath();
            if(MyTag1.equals(localName))
            {
                descriptions.add(langString);
                return;
            }
            else if(MyTag2.equals(localName))
            {
                displayNames.add(langString);
                return;
            }
            else
                throw new SAXException(currentPath() + " unexpected localName: " + localName);
        }
        throw new SAXException(currentPath() + " unexpected localName: " + localName);
   }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(isMyTag(MyTag1) || isMyTag(MyTag2))
        {
            cdata.append(new String(ch, start, length));
            return;
        }
        String value = new String(ch, start, length);
        throw new SAXException(currentPath() + " characters unknown current stack: " + value);
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(!localName.equals(MyTag1) && !localName.equals(MyTag2))
            throw new SAXException(currentPath() + " unexpected endElement: " + localName);
        if(codenameElement.get())
        {
            codeName = cdata.toString();
            stack.pop();
            return;
        }
        langString.cdata = StrH.cdataFixup(cdata.toString());
        if(isMyTag(MyTag1) || isMyTag(MyTag2))
        {
            stack.pop();
            return;
        }
        throw new SAXException(currentPath() + " unexpected endElement: " + localName);
    }
    
    private class LanguageString
    {
        public String cdata;
        public final String attrUri;
        public final String attrQname;
        public final String attrValue;
        @SuppressWarnings("unused")
        public String currentPath;
        
        public LanguageString(Attributes attributes) throws SAXException
        {
//            if(attributes.getLength() != 1)
//                throw new SAXException("expected 1 ns attribute got: " + attributes.getLength());
            attrUri = attributes.getURI(0);
            attrQname = attributes.getQName(0);
            attrValue = attributes.getValue(0);
        }
        
        public void validate() throws SAXException
        {
//            if(cdata == null || cdata.length() < 1)
//            {
//                log.info("look here");
//                throw new SAXException("cdata is null or empty string");
//            }
//            if(attrUri == null || attrUri.length() < 1)
//                throw new SAXException("attrUri is null or empty string");
//            if(!attrUri.equals(XmlLangNs))
//                throw new SAXException("attrUri unexpected: " + attrUri);
//            if(attrQname == null || attrQname.length() < 1)
//                throw new SAXException("attrQname is null or empty string");
//            if(!attrQname.equals(XmlLangQname))
//                throw new SAXException("attrQname unexpected: " + attrQname);
//            if(attrValue == null || attrValue.length() < 1)
//                throw new SAXException("attrValue is null or empty string");
        }
        
        @Override
        public String toString()
        {
            return "attrUri: " + attrUri + " attrQname: " + attrQname + " attrValue: " + attrValue + " cdata: " + cdata;
        }
        
        public void export(StringBuilder sb, int level, String tag)
        {
            StrH.element(sb, tag, attrQname, attrValue, cdata, true, level);
        }
    }
    
    public StringBuilder export(StringBuilder sb, int level)
    {
        if(codeName != null && codeName.length() > 0)
            StrH.element(sb, CodeNameTag, codeName, true, level);
        for(LanguageString ls : displayNames)
            ls.export(sb, level, DisplayNameTag);
        for(LanguageString ls : descriptions)
            ls.export(sb, level, DescriptionTag);
        return sb;
    }
}
