/*
**  Copyright (c) 2010-2015, Panasonic Corporation.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/
package org.opendof.tools.repository.interfaces.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.opendof.tools.repository.interfaces.opendof.saxParser.InterfaceElement;
import org.opendof.tools.repository.interfaces.opendof.saxParser.OpenDofSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

@SuppressWarnings("javadoc")
public class OpendofSaxParser
{
    private final static String xmlFile = "/ws2/dof-interface-repository/schema-project/opendof/fullinterface.xml";
    
    private final static boolean singleFile = true;  
    private final static boolean publishOnly = false;
    private final static boolean workingOnly = true;
    private final static boolean doExport = true;
    private final static boolean validate = true;
    private final static boolean pretty = true;

    public static final String logBase = "/var/opt/opendof/tools/interfacerepository/log/saxParser";
    public final static Hashtable<String, String> nameSpaces = new Hashtable<String, String>();

    public static final String XsdFileInBaseDir = "/ws2/dof-interface-repository/schema/";
    public static final String NewXmlFileOutBaseDir = "/tmp/irepository/output/new";
    public static final String ConvertedXmlFileOutBaseDir = "/tmp/irepository/converted/";
    public static final String ConvertedXmlFileOutPubishDir = ConvertedXmlFileOutBaseDir + "publish/";
    public static final String ConvertedXmlFileOutWorkingDir = ConvertedXmlFileOutBaseDir + "working/";

    public static final String AtomicPublish = "publish";
    public static final String AtomicWorking = "working";
    public static final String AtomicHtml = "html";
    public static final String Publish = NewXmlFileOutBaseDir + AtomicPublish;
    public static final String Working = NewXmlFileOutBaseDir + AtomicWorking;
    public static final String XsdMainFile = "interface-repository.xsd";
    public static final String XsdMetaFile = "interface-repository-meta.xsd";
    public static final String XsdMainPath = XsdFileInBaseDir + XsdMainFile;
    public static final String XsdMetaPath = XsdFileInBaseDir + XsdMetaFile;
    public static String W3NamespaceLocation = "/ws2/dof-interface-repository/schema-project/xml.xsd";

    public static final Logger log;
    public final static Stack<String> stack = new Stack<String>();

    static
    {
        System.setProperty("log-file-base-name", logBase);
        log = LoggerFactory.getLogger(OpendofSaxParser.class);
        log.info("Logging to: " + logBase);
    }

    //@formatter:off
    private static String[] badInterfaces = new String[]
    {
        // published
        "C:/tmp/irepository/output/publish/air-flow-volume.xml",  //iid="[1:{01000041}] cdata with html
    };
    
    //@formatter:off
    
    public OpendofSaxParser()
    {
    }

    private static String convertToFileURL(String filename)
    {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/')
        {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        return "file:" + path;
    }

    private void exportInterface(String inputFile, InterfaceElement interfaceData, boolean publish) throws Exception
    {
        if(!doExport)
            return;
        String xml = interfaceData.export(pretty);
        log.info("\n\n"+xml+"\n\n");
        File converted = new File(inputFile);
        File convertedFile = new File(ConvertedXmlFileOutPubishDir + converted.getName());
        if(!publish)
            convertedFile = new File(ConvertedXmlFileOutWorkingDir + converted.getName());
        FileOutputStream fos = new FileOutputStream(convertedFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out = new OutputStreamWriter(bos,"UTF-8");
        out.write(xml);
        out.close();
        if(validate)
            validateExport(convertedFile);
    }

    private void validateExport(File xmlFile) throws Exception
    {
        URL schemaFile = new URL(InterfaceElement.InterfaceSchemaLocation);
        Source xmlSource = new StreamSource(xmlFile);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(xmlSource);
    }
    
    private void parseIt(String file, boolean publish) throws Exception
    {
        file = file.replace('\\', '/');
        for(int i=0; i < badInterfaces.length; i++)
        {
            if(badInterfaces[i].equals(file))
                return;
        }
        System.out.print(file + " ");
//        if(++fileCount % 4 == 0)
            System.out.println("");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new LegacyErrorHandler());
        OpenDofSaxHandler handler = new OpenDofSaxHandler();
        xmlReader.setContentHandler(handler);
        xmlReader.parse(convertToFileURL(file));
        exportInterface(file, handler.getInterface(), publish);
    }
     
    private void doit()
    {
        File currentfile = null;
        File file = null;
        File[] files = null;
        try
        {
            if(singleFile)
            {
                currentfile = new File(xmlFile);
                parseIt(xmlFile, false);
                logFoundNamespaces();
                return;
            }
            if(!workingOnly)
            {
                file = new File(Publish);
                files = file.listFiles();
                for(int i=0; i < files.length; i++)
                {
                    currentfile = files[i];
                    parseIt(currentfile.getAbsolutePath(), true);
                }
                if(publishOnly)
                {
                    logFoundNamespaces();
                    return;
                }
            }
            file = new File(Working);
            files = file.listFiles();
            for(int i=0; i < files.length; i++)
            {
                currentfile = files[i];
                parseIt(currentfile.getAbsolutePath(), false);
            }
            logFoundNamespaces();
        } catch (Exception e)
        {
            if(currentfile != null)
                log.error("file: " + currentfile.toString());
            log.error("Failed to traverse the trees", e);
            System.exit(1);
        }
    }

    public void logFoundNamespaces()
    {
        StringBuilder sb = new StringBuilder("\nLegacy Interface Namespaces:");
        for (Entry<String, String> entry : nameSpaces.entrySet())
            sb.append("\n\t" + entry.getValue());
        log.info(sb.toString());
    }
    
    public class LegacyErrorHandler implements ErrorHandler
    {
        @Override
        public void warning(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " warning", ce);
            log.warn(getClass().getSimpleName() + " warning SAXParseException", e);
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " error", ce);
            log.warn(getClass().getSimpleName() + " error SAXParseException", e);
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " fatal", ce);
            log.warn(getClass().getSimpleName() + " fatal SAXParseException", e);
            throw e;
        }
    }
    
    public static void main(String args[])
    {
        new OpendofSaxParser().doit();
    }
}
