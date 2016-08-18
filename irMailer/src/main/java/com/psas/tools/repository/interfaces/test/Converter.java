package com.psas.tools.repository.interfaces.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.opendof.tools.repository.interfaces.core.CoreController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ccc.tools.TabToLevel;
import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxParser;

@SuppressWarnings("javadoc")
public class Converter
{
    public static final String ConverterGroup = "pslcl";
    public static final String ConverterCreator = "Chad Adams";

    public static final boolean ToFile = true;
    public static final boolean ToDb = false;
    private final static boolean publishOnly = false;
    private final static boolean workingOnly = false;
    //    public static final boolean fromConverted = true;
    //    public static final boolean validateAll = true;

    public static final String XmlFileInBaseDir = "c:/share/ir/svn/";
    public static final String XmlFileOutBaseDir = "/share/ir/output/";
    public static final String AtomicPublish = "publish";
    public static final String AtomicWorking = "working";
    public static final String AtomicHtml = "html";
    public static final String Publish = XmlFileInBaseDir + AtomicPublish;
    public static final String Working = XmlFileInBaseDir + AtomicWorking;

    public static final String ConvertedXmlFileOutBaseDir = "/share/ir/production/";
    public static final String ConvertedXmlFileOutPubishDir = ConvertedXmlFileOutBaseDir + "publish/";
    public static final String ConvertedXmlFileOutWorkingDir = ConvertedXmlFileOutBaseDir + "working/";

    private final DocumentBuilderFactory dbf;
    private final Logger log;
    private final CoreController controller;

    public Converter(CoreController controller)
    {
        log = LoggerFactory.getLogger(getClass());
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(true);
        this.controller = controller;
    }

    /* ************************************************************************
     * This is Bryant's legacy Merge code
    **************************************************************************/

    private void merge(Document emit, Document patch) throws Exception
    {
        //get the root element
        Element docEle = patch.getDocumentElement();

        NodeList nl = docEle.getElementsByTagNameNS("http://emitdo.pew.com/schema/RepositoryMeta", "patch");
        if (nl != null && nl.getLength() > 0)
        {
            for (int i = 0; i < nl.getLength(); i++)
            {
                //get the apply element
                Element el = (Element) nl.item(i);

                // Get the target
                // This is other part of the XPath *HACK* where the fake namespace is inserted.
                String target = el.getAttribute("target");
                target = target.replaceAll("/enum", "/g:enum");
                target = target.replaceAll("/([a-zA-Z][^:])", "/f:$1");
                XPathFactory factory = XPathFactory.newInstance();
                XPath xpath = factory.newXPath();
                xpath.setNamespaceContext(new MyNamespaceContext());

                try
                {
                    XPathExpression expr = xpath.compile(target);
                    Object result = expr.evaluate(emit, XPathConstants.NODESET);
                    NodeList nodes = (NodeList) result;
                    Node N = nodes.item(0);

                    NodeList nl2 = el.getChildNodes();
                    if (N != null && nl2 != null && nl2.getLength() > 0)
                    {
                        for (int j = 0; j < nl2.getLength(); j++)
                        {
                            Node IM = emit.importNode(nl2.item(j), true);
                            if (IM != null)
                                N.insertBefore(IM, N.getFirstChild());
                        }
                    }
                } catch (Exception e)
                {
                    throw e;
                }
            }
        }
    }

    private void documentToFile(Document doc, File file) throws Exception
    {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        DOMSource source = new DOMSource(doc);
        if (!file.exists())
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file, false);
        StreamResult result = new StreamResult(fos);
        transformer.transform(source, result);
        fos.close();
    }

    @SuppressWarnings("unused")
    private String documentToString(Document doc) throws Exception
    {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);
        String xml = baos.toString("utf-8");
        baos.close();
        return xml;
    }

    /**
     * This goes with TestType.Merge
     * This is a *HACK* to get around the fact that XPath operations do not have a way of using
     * a default namespace. Since we really don't want to qualify the XPath inputs, this gets
     * around it by allowing us to put "f:" in front of the XPath Qnames.
     */
    static public class MyNamespaceContext implements NamespaceContext
    {
        @Override
        public String getNamespaceURI(String prefix)
        {
            if (prefix.compareTo("f") == 0)
                return "http://emitdo.pew.com/schema/Interface";
            return "http://emitdo.pew.com/schema/Enums";
        }

        @Override
        public String getPrefix(String namespaceURI)
        {
            if (namespaceURI.compareTo("http://emitdo.pew.com/schema/Interface") == 0)
                return "f";
            return "g";
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Iterator getPrefixes(String namespaceURI)
        {
            return null;
        }
    }

    /* ************************************************************************
     * Chad added this section to utilized Byrant's code above.
     * To use this:
     *      1. do a full svn checkout to some working folder.
     *      2. modify XmlFileInBaseDir at top of this file to point to your checkout base.
     *      3. modify XmlFileOutBaseDir at top of this file to point to when you want the merged files to go
     *      The following reentrant traversal will then walk the whole svn check out and merge them.
    **************************************************************************/

    private Hashtable<String, AtomicInteger> namesmap = new Hashtable<String, AtomicInteger>();

    private boolean traverse(File file, boolean publish, int level, TabToLevel format, boolean writeOutputFiles) throws Exception
    {
        if (file.isDirectory())
        {
            if (file.getName().equals("sanyo_rm"))
                log.info("look here");
//        	if(file.getName().equals("sanyo_rm"))
//        		return false;
            File[] children = file.listFiles();
            for (File child : children)
            {
                if (child.isDirectory())
                    format.ttl(removeBase(child, publish), ":");
                if (traverse(child, publish, ++level, format, writeOutputFiles))
                    break;
                --level;
            }
            return false;
        }
        String path = file.getAbsolutePath();
        int index = path.lastIndexOf('\\');
        path = path.substring(0, index);
        File[] files = new File(path).listFiles();
//        if (!publish)
        if (publish | !publish)
        {
            File emitFile = null;
            List<File> metaFiles = new ArrayList<File>();
            for (File child : files)
            {
                if (child.isDirectory())
                    throw new Exception("unexpected directory" + child.getAbsolutePath());
                if (child.getName().toLowerCase().endsWith(".emit"))
                {
                    if (emitFile != null)
                        throw new Exception("two .emit: " + child.getAbsolutePath());
                    emitFile = child;
                    format.ttl(child.getName());
                } else if (child.getName().toLowerCase().endsWith(".meta"))
                {
                    metaFiles.add(child);
                    format.ttl(child.getName());
                }
                else if (child.getName().toLowerCase().endsWith(".png"))
                {
                    format.ttl("WARNING other file type found: ", child.getName());
                } 
                else if (child.getName().toLowerCase().endsWith(".xml"))
                {
                    format.ttl("WARNING other file type found: ", child.getName());
                } 
                else
                {
                    format.ttl("WARNING ", child.getName());
                    throw new Exception("unexpected file type: " + child.getAbsolutePath());
                }
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document emitDoc = db.parse(new InputSource(new FileInputStream(emitFile)));
            Collections.sort(metaFiles);
            for (File meta : metaFiles)
            {
                FileInputStream fis = new FileInputStream(meta);
                Document patch = db.parse(fis);
                merge(emitDoc, patch);
            }
            if (writeOutputFiles)
            {
                String emitPath = emitFile.getPath();
                emitPath = emitPath.replace('\\', '/');
                emitPath = emitPath.substring(XmlFileInBaseDir.length());
                int len = publish ? AtomicPublish.length() : AtomicWorking.length();
                emitPath = emitPath.substring(len);
                String mergedFileName = XmlFileOutBaseDir + (publish ? AtomicPublish : AtomicWorking) + emitPath;
//                String mergedFileName = XmlFileOutBaseDir + (publish ? AtomicPublish : AtomicWorking) + "/" + emitFile.getName();
                mergedFileName = mergedFileName.substring(0, mergedFileName.indexOf(".emit"));
                AtomicInteger count = namesmap.get(mergedFileName);
                if (count == null)
                {
                    count = new AtomicInteger(0);
                    namesmap.put(mergedFileName, count);
                }
                int c = count.incrementAndGet();
                if (c > 1)
                    mergedFileName += "" + c;
                mergedFileName += ".xml";
                format.ttl(mergedFileName);
                File mergedFile = new File(mergedFileName);
                documentToFile(emitDoc, mergedFile);
//                if(!emitFile.delete())
//                    log.error("failed to delete: " + emitFile.getPath());
            }
            // was considering using the merged old schema originally            
            //            if(writeOutputFiles)
            //            {
            //                RequestData request = new RequestData(null, ConverterCreator, null, null, null, "opendof", false, documentToString(emitDoc));
            //                controller.addInterface(ConverterUser, null, documentToString(emitDoc), false);
            //            }
            return true;
        }
        // must be working

        return true;
    }

    private String removeBase(File file, boolean publish)
    {
        String path = file.getAbsolutePath();
        path = path.replace('\\', '/');
        String base = Publish;
        if (!publish)
            base = Working;
        int size = base.length() + 3; // for c:/
        path = path.substring(size);
        return path;
    }

    /* ************************************************************************
     *  This starts the convert section where after running the above merge
     *  you convert the merged output files from above to the new xml schema.
     *  The following bases from top of this file are of interest here.
     *  
     *  XmlFileOutBaseDir ...       where the merge dropped its output
     *  ConvertedXmlFileOutBaseDir  where you want the merged converted to new schema output to.
     *  
     *  I decided to do this conversion with the LegacySaxParser instead of xsl
     *  which duplicates these bases  See the constants at the top of that class and modify them to
     *  match yours here.
    **************************************************************************/

    private void doConvert(String catalogPath) throws Exception
    {
        new LegacySaxParser().doit(catalogPath);
    }

    /* ************************************************************************
     *  This starts the upload section where after running the above merge
     *  and convert, you upload the new xml schema files to the database.
     *  
     *  The following base from top of this file are of interest here.
     *      ConvertedXmlFileOutBaseDir  
    **************************************************************************/

    public static String fileToString(File file) throws Exception
    {
        BufferedReader bufferedReader = null;
        StringBuilder sb = new StringBuilder();
        try
        {
            FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null)
                sb.append(line).append("\n");
        } finally
        {
            if (bufferedReader != null)
                try
                {
                    bufferedReader.close();
                } catch (IOException e)
                {
                    LoggerFactory.getLogger(Converter.class).warn("failed to close bufferedReader cleanly", e);
                }
        }
        return sb.toString();
    }

    //    String[] fixups = new String[]
    //    {
    //        "DataSnapshotConfig",
    //        "TopologyInfo",
    //        "DataManager",
    //        "TopologyUpdate",
    //        "DataSource",
    //        "DataSnapshot",
    //        "DataSink",
    //    };

    String[] fixups = new String[] {
                    //        "GPSLocation",
                    //        "Impact",
                    //        "LocationHistory",
                    //        "Lockable",
                    //        "Weight",
                    //    	"TemporaryCredentials",	
                    "toaster-suggested", };

    //    private void doUpload() throws Exception
    //    {
    //        File currentfile = null;
    //        File file = new File(ConvertedXmlFileOutWorkingDir);
    //        boolean published = false;
    //        File[] files = file.listFiles();
    //        for(int i=0; i < files.length; i++)
    //        {
    //        	currentfile = files[i];
    //        	if(fixups.length > 0)
    //        	{
    //        		boolean found = false;
    //        		for(int j=0; j < fixups.length; j++)
    //        		{
    //        			if(currentfile.getAbsolutePath().contains(fixups[j]))
    //        			{
    //        				found = true;
    //        				break;
    //        			}
    //        		}
    //        		if (!found)
    //        			continue;
    //        	}
    //            String xml = fileToString(currentfile);
    //            InterfaceRequest iface = new InterfaceRequest(null, "1", "opendof");
    //            RequestData request = new RequestData(null, null, ConverterGroup, iface);
    //            log.debug("\nadding file: " + currentfile.getAbsolutePath());
    //            controller.addInterface(request);
    //        }
    //    	if(fixups.length > 0)
    //    	{
    //            controller.destroy();
    //    		return;
    //    	}
    //
    //
    //        
    //        file = new File(ConvertedXmlFileOutWorkingDir);
    //        files = file.listFiles();
    //        for(int i=0; i < files.length; i++)
    //        {
    //        	currentfile = files[i];
    //            String xml = fileToString(currentfile);
    //            String repoType = "opendof";
    //            if(currentfile.getAbsolutePath().contains("org.allseen") || currentfile.getAbsolutePath().contains("org.freedesktop"))
    //            	repoType = "allseen";
    //            	
    //            InterfaceRequest iface = new InterfaceRequest(null, repoType, false, xml);
    //            RequestData request = new RequestData(null, ConverterGroup, null, null, iface);
    //            log.debug("\nadding file: " + currentfile.getAbsolutePath());
    //            controller.addInterface(request);
    //        }
    //        controller.destroy();
    //    }

    public void doMerge() throws Exception
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\nPublish:\n");
        try
        {
            traverse(new File(Publish), true, 1, format, ToFile);
            format.ttl("\nPublished complete\n");

            if (publishOnly)
            {
                log.info(format.toString());
                return;
            }
            format.ttl("Working:\n");
            traverse(new File(Working), false, 1, format, ToFile);
            log.info(format.toString());
        } catch (Exception e)
        {
            log.error(format.toString(), e);
        }
    }

    public void run(TestType type, Properties properties) throws Exception
    {
        switch (type)
        {
            case Convert:
                String catalogPath = "./config/opendof.xml";
                if (properties != null)
                    catalogPath = properties.getProperty(LegacySaxParser.XsdCatalogFileKey, "./config/opendof.xml");
                doConvert(catalogPath);
                break;
            case Merge:
                doMerge();
                break;
            case Upload:
                //                doUpload();
                break;
            default:
                throw new Exception("unknown TestType: " + type.name());
        }
    }

    public enum TestType
    {
        Merge, Convert, Upload
    }
}
