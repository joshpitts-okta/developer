package com.psas.tools.repository.interfaces.test.legacySax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOFInterfaceID;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.psas.tools.repository.interfaces.test.legacySax.StrH.CType;

@SuppressWarnings("javadoc")
public class InterfaceData extends LegacySaxHandler
{
    public static String InterfaceNamespace = "http://opendof.org/schema/interface-repository";
    public static String InterfaceMetaNamespace = "http://opendof.org/schema/interface-repository-meta";
    public static String InterfaceSchemaLocation = "http://opendof.org/schema/interface-repository.xsd";
    public static String W3SchemaNamespace = "http://www.w3.org/2001/XMLSchema-instance";
    public static String W3Namespace = "http://www.w3.org/XML/1998/namespace";
    public static String W3NamespaceLocation = "http://www.w3.org/2001/xml.xsd";
    
    public static final String InterfaceElement = "interface";
//    public static final String VersionAttr = "version";
    public static final String Version = "1.0.0";
    public static final String IidAttr = "iid";
    public static String Header1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; 
    //@formatter:off
    public static String Header2 = 
                    "<"+
                    InterfaceElement +
                    " xmlns=\"" + InterfaceNamespace + "\"" +
                    " xmlns:md=\"" + InterfaceMetaNamespace + "\"" +
                    " xmlns:xsi=\"" + W3SchemaNamespace + "\"" +
                    " xsi:schemaLocation=\"" + 
                    InterfaceNamespace + " " + InterfaceSchemaLocation + " " +
                    "\"" +
//                    " " + VersionAttr + "=\"" + Version + "\"" +
                    " " + IidAttr + "=\"";
    //@formatter:on
    
    public static final String ExternalsElement = "externals";
    public static final String ParenteElement = "parent";
    public static final String SiblingsElement = "siblings";
    public static final String ExternalElement = "external";
    public static final String SiblingElement = "sibling";
    
    public static final String ItemIdAttr = "item-id";
    public static final String UrlAttr = "url";

    public static final String MyTag = "interface";
    private static final String MyTagSibling = "sibling";
    private static final String MyTagParent = "parent";
    private static final String MyTagExternal = "external";
    
    private static final String InterfacePath = MyTag;
    private static final String ContextPath = MyTag + "," + ContextData.MyTag;
    private static final String PropertiesPath = MyTag + "," + PropertiesData.MyTag;
    private static final String MethodsPath = MyTag + "," + MethodsData.MyTag;
    private static final String EventsPath = MyTag + "," + EventsData.MyTag;
    private static final String ExceptionsPath = MyTag + "," + ExceptionsData.MyTag;
    private DOFInterfaceID iid;
    private NameDescriptionData nameDescriptions;
    private ContextData context;
    private PropertiesData properties;
    private MethodsData methods;
    private EventsData events;
    private ExceptionsData exceptions;
    private final List<Sibling> siblings;
    private final List<Sibling> externals;
    private List<Sibling> parent;
    
    private Sibling currentSibling;
    private StringBuilder cdata;
    public AtomicBoolean registry2 = new AtomicBoolean(false);
    
    public InterfaceData()
    {
        siblings = new ArrayList<Sibling>();
        externals = new ArrayList<Sibling>();
        parent = new ArrayList<Sibling>();
    }
    
    public void validate() throws SAXException
    {
        if(stack.size() != 0)
            throw new SAXException("Interface complete but stack is not zero");
        for(Sibling sib : siblings)
            sib.validate();
        for(Sibling sib : externals)
            sib.validate();
        if(parent.size() != 0)
        {
            if(parent.size() > 1)
                throw new SAXException("Interface more than one parent found");
            parent.get(0).validate();
        }
        //TODO: working fix all the published worked with this test enabled
//        if(nameDescriptions == null)
//            throw new SAXException("Interface.nameDescriptions is null");
        if(nameDescriptions != null)
            nameDescriptions.validate(false);
        if(context == null)
            throw new SAXException("Interface.context is null");
        context.validate();
        Hashtable<Integer, Integer> uniqueMap = new Hashtable<Integer, Integer>();
        if(properties != null)
        {
            properties.validate(context);
            properties.uniqueItemId(uniqueMap);
        }
        if(methods != null)
        {
            methods.validate(context);
            methods.uniqueItemId(uniqueMap);
        }
        if(events != null)
        {
            events.validate(context);
            events.uniqueItemId(uniqueMap);
        }
        if(exceptions != null)
        {
            exceptions.validate(context);
            exceptions.uniqueItemId(uniqueMap);
        }
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if(registry2.get())
            return;
        if(stack.size() == 0)
        {
            stack.push(localName);
            if(!isMyTag(MyTag))
                throw new SAXException("did not find element: " + MyTag);
            int index = attributes.getIndex("", "iid");
            if(index == -1)
                throw new SAXException("could not find interface.iid");
            iid = DOFInterfaceID.create(attributes.getValue(index));
            if(iid.getRegistry() == 2)
            {
                log.warn(iid.toStandardString() + " registry 2 skipping for now");
                if(!registry2.get())
                    log.info("\n\nregistry 2\n\n");
                registry2.set(true);
            }
            return;
        }
        if(stack.peek().equals(MyTag))
        {
            if(localName.equals(MyTagSibling) || localName.equals(MyTagParent)|| localName.equals(MyTagExternal))
            {
                if(attributes.getLength() != 1)
                    throw new SAXException(currentPath() + " sibling wrong number of attributes");
                int index = attributes.getIndex("url");
                if(index != 0)
                    throw new SAXException(currentPath() + " sibling did not find url attribute");
                currentSibling = new Sibling(attributes.getValue(index));
                cdata = new StringBuilder();
                stack.push(localName);
                return;
            }
            
            if(NameDescriptionData.oneOfYours(localName))
            {
                if(nameDescriptions == null)
                    nameDescriptions = new NameDescriptionData();
                nameDescriptions.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(ContextData.oneOfYours(localName))
            {
                if(context == null)
                {
                    context = new ContextData();
                    context.startElement(uri, localName, qName, attributes);
                    return;
                }
                throw new SAXException(pathToString(InterfacePath) + " startElement two context elements ");
            }else
            if(PropertiesData.oneOfYours(localName))
            {
                if(properties == null)
                    properties = new PropertiesData();
                properties.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(MethodsData.oneOfYours(localName))
            {
                if(methods == null)
                    methods = new MethodsData();
                methods.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(EventsData.oneOfYours(localName))
            {
                if(events == null)
                    events = new EventsData();
                events.startElement(uri, localName, qName, attributes);
                return;
            }else
            if(ExceptionsData.oneOfYours(localName))
            {
                if(exceptions == null)
                    exceptions = new ExceptionsData();
                exceptions.startElement(uri, localName, qName, attributes);
                return;
            }else
                throw new SAXException(pathToString(InterfacePath) + " startElement unknown localName for this level: " + localName);
        }
        if(isMyPath(ContextPath))
        {
            context.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(isMyPath(PropertiesPath))
        {
            properties.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(isMyPath(MethodsPath))
        {
            methods.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(isMyPath(EventsPath))
        {
            events.startElement(uri, localName, qName, attributes);
            return;
        }
        else if(isMyPath(ExceptionsPath))
        {
            exceptions.startElement(uri, localName, qName, attributes);
            return;
        }
        throw new SAXException(pathToString(InterfacePath) + " startElement unknown localName for this level: " + localName);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if(registry2.get())
            return;
        if(isMyTag(MyTag))
        {
            String value = new String(ch, start, length);
            throw new SAXException(currentPath() + " characters unexpected: " + value);
        }
        String current = stack.peek();
        if(amIParent(MyTag) && (current.equals(MyTagSibling) || current.equals(MyTagParent)|| current.equals(MyTagExternal)))
        {
            String value = new String(ch, start, length);
            cdata.append(value);
            return;
        }
        else if(amIParent(MyTag) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.characters(ch, start, length);
        else if(isMyPath(ContextPath))
            context.characters(ch, start, length);
        else if(isMyPath(PropertiesPath))
            properties.characters(ch, start, length);
        else if(isMyPath(MethodsPath))
            methods.characters(ch, start, length);
        else if(isMyPath(EventsPath))
            events.characters(ch, start, length);
        else if(isMyPath(ExceptionsPath))
            exceptions.characters(ch, start, length);
        else
            throw new SAXException(currentPath() + " characters unknown current stack: " + current);
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if(registry2.get())
        {
            stack.clear();
            return;
        }
        if(localName.equals(MyTag))
        {
            stack.pop();
            validate();
            return;
        }
        if(amIParent(MyTag) && (localName.equals(MyTagSibling) || localName.equals(MyTagParent)|| localName.equals(MyTagExternal)))
        {
            stack.pop();
            currentSibling.name = cdata.toString();
            if(localName.equals(MyTagSibling))
                siblings.add(currentSibling);
            else if(localName.equals(MyTagParent))
                parent.add(currentSibling);
            else
                externals.add(currentSibling);
            currentSibling = null;
            return;
        }
        String current = stack.peek();
        if(amIParent(InterfacePath) && NameDescriptionData.oneOfYours(current))
            nameDescriptions.endElement(uri, localName, qName);
        else if(isMyPath(ContextPath))
            context.endElement(uri, localName, qName);
        else if(isMyPath(PropertiesPath))
            properties.endElement(uri, localName, qName);
        else if(isMyPath(MethodsPath))
            methods.endElement(uri, localName, qName);
        else if(isMyPath(EventsPath))
            events.endElement(uri, localName, qName);
        else if(isMyPath(ExceptionsPath))
            exceptions.endElement(uri, localName, qName);
        else
            throw new SAXException(currentPath() + " endElement unknown stack path for localName: " + localName);
    }

    public class Sibling
    {
        public final String url;
        public String name;
        
        private Sibling(String url)
        {
            this.url = url;
        }
        
        public void validate() throws SAXException
        {
            if(url == null || url.length() == 0)
                throw new SAXException("Sibling.url is null or empty");
            if(name == null || name.length() == 0)
                throw new SAXException("Sibling.name is null or empty");
        }
    }

    public String export(boolean pretty)
    {
        StrH.pretty = pretty;
        int level = 0;
        StringBuilder sb = new StringBuilder(Header1);
        if(pretty)
            sb.append("\n");
        sb.append(Header2);
        sb.append(iid.toStandardString()).append("\"");
        StrH.closer(sb, null, StrH.CType.Open, true);
        ++level;
        if(nameDescriptions != null)
            nameDescriptions.export(sb, level);
        if(context != null)
            context.export(sb, level);
        if(properties != null)
            properties.export(sb, level);
        if(methods != null)
            methods.export(sb, level);
        if(events != null)
            events.export(sb, level);
        if(exceptions != null)
            exceptions.export(sb, level);
        if(externals.size() > 0)
        {
            StrH.element(sb, ExternalsElement, CType.Open, level);
            ++level;
            for(Sibling sibling : externals)
                StrH.element(sb, ExternalElement, UrlAttr, sibling.url, sibling.name, true, level);
            --level;
            StrH.element(sb, ExternalsElement, CType.Close, level);
        }
        if(parent.size() > 0)
            StrH.element(sb, ParenteElement, IidAttr, parent.get(0).url, parent.get(0).name, true, level);
        
        if(siblings.size() > 0)
        {
            StrH.element(sb, SiblingsElement, CType.Open, level);
            ++level;
            for(Sibling sibling : siblings)
                StrH.element(sb, SiblingElement, IidAttr, sibling.url, sibling.name, true, level);
            --level;
            StrH.element(sb, SiblingsElement, CType.Close, level);
        }
        --level;
        StrH.element(sb, InterfaceElement, CType.Close, level);
        return sb.toString();
    }
}
