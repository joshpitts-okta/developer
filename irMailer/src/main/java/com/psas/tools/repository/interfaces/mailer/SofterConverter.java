package com.psas.tools.repository.interfaces.mailer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.opendof.core.oal.DOFInterfaceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.xml.sax.XMLReader;

import com.ccc.tools.TabToLevel;
import com.psas.tools.repository.interfaces.test.legacySax.InterfaceData;
import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxHandler;
import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxParser;
import com.psas.tools.repository.interfaces.test.legacySax.LegacySaxParser.LegacyErrorHandler;

@SuppressWarnings("javadoc")
public class SofterConverter extends Thread
{
    private final HashMap<String, List<SVNLogEntry>> pathMap;
    private final HashMap<String, List<SVNLogEntry>> submitterMap;
    private final HashMap<String, List<String>> ownerToPathMap;
    private final HashMap<String, SVNDirEntry> tipDirMap; // path is ksy
    private final Logger log;

    //@formatter:off
    public SofterConverter(
            HashMap<String, List<SVNLogEntry>> pathMap,
            HashMap<String, List<SVNLogEntry>> submitterMap,
            HashMap<String, List<String>> ownerToPathMap,
            HashMap<String, SVNDirEntry> tipDirMap)
    //@formatter:on
    {
        this.pathMap = pathMap;
        this.submitterMap = submitterMap;
        this.ownerToPathMap = ownerToPathMap;
        this.tipDirMap = tipDirMap;
        log = LoggerFactory.getLogger(getClass());
    }

    public void fnSize() throws Exception
    {
        File file = new File(IrMailer.MailDir);
        TabToLevel format = new TabToLevel();
        format.ttl("\nFile name length check:");
        try
        {
            traverseForSize(file, format);
        } catch (Exception e)
        {
            format.ttl("\nERROR: ", e.getClass().getName(), " ", e.getMessage());
            log.info(format.toString());
            throw e;
        }
        log.info(format.toString());
    }
    
    public void convert() throws Exception
    {
        List<String> paths = new ArrayList<>();
        File file = new File(IrMailer.MergedDir);
        TabToLevel format = new TabToLevel();
        format.ttl("\nMerged structure:");
        try
        {
            traverse(file, format, paths);
        } catch (Exception e)
        {
            format.ttl("\nERROR: ", e.getClass().getName(), " ", e.getMessage());
            log.info(format.toString());
            throw e;
        }
        log.info(format.toString());
        mergedToV2(paths);
    }

    private void mergedToV2(List<String> paths) throws Exception
    {
        int count = 0;
        for (String path : paths)
        {
            log.info("parsing: " + path);
            int pathLength = path.length();
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setErrorHandler(new LegacyErrorHandler());
            LegacySaxHandler handler = new LegacySaxHandler();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(LegacySaxParser.convertToFileURL(path));
            String output = "\\" + path.substring(IrMailer.MergedDir.length());
            int outputLength = output.length();
            File mergedFile = new File(path);
            boolean publish = mergedFile.getPath().contains("\\publish\\");
            String siid = PostAlloc.getIidFromDotEmit(mergedFile);
            DOFInterfaceID iid = DOFInterfaceID.create(siid);
            int idx = output.lastIndexOf('.');
            output = output.substring(0, idx);
            output += ".emit";
//            output = IrMailer.SvnDir + "/" + output;
//            File f = new File(output);
//            output = f.getPath();
            String owner = PreAlloc.getOwnerFromPath(output, ownerToPathMap);
            output = IrMailer.SvnDir + output;
            String iidName = PostAlloc.findIidNameNonConverted(new File(output), siid);
            output = output.substring(IrMailer.SvnDir.length());
            idx = output.lastIndexOf('.');
            output = output.substring(0, idx);
            output += ".xml";
            String fileName = new File(output).getName();
            String midPath = "";
            if(publish)
            {
                output = IrMailer.MailDir + owner + "/publish/" + iidName + "/";
                File outParent = new File(output);
                if(!outParent.exists())
                    throw new Exception("path does not exist: " + outParent);
                output = outParent.getPath() + "\\" + fileName;
            }else
            {
                File outParent = new File(output).getParentFile();
                String working = "/working";
                idx = working.length();
                output = output.substring(idx);
                midPath = outParent.getPath().substring("/working".length());
               
                output = IrMailer.MailDir + owner + "/working/registry_" + iid.getRegistry() + midPath + "/" +iidName;
                outParent = new File(output);
                if(!outParent.exists())
                    throw new Exception("path does not exist: " + outParent);
                output = outParent.getPath() + "\\" + fileName;
            }
            exportInterface(output, handler.getInterface());
            ++count;
        }
        log.info("converted " + count + " files");
    }

    private void exportInterface(String output, InterfaceData interfaceData) throws Exception
    {
        String xml = interfaceData.export(true);
        log.info("\n\n"+xml+"\n\n");
        File outFile = new File(output);
        FileOutputStream fos = new FileOutputStream(outFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out = new OutputStreamWriter(bos,"UTF-8");
        out.write(xml);
        out.close();
        log.info("last good: " + output);
    }

    
    
    private void traverse(File file, TabToLevel format, List<String> paths) throws Exception
    {
        if (file.getPath().contains("33"))
            log.info("look here");
        format.ttl(file.getPath());
        File[] children = file.listFiles();
        if (children != null)
        {
            format.inc();
            for (int i = 0; i < children.length; i++)
                traverse(children[i], format, paths);
            format.dec();
            return;
        }
        if (file.isDirectory())
        {
            // empty folder
            boolean ok = file.delete();
            if (!ok)
                throw new Exception("failed to remove empty directory: " + file.getPath());
            return;
        }
        paths.add(file.getPath());
    }
    
    private void traverseForSize(File file, TabToLevel format) throws Exception
    {
        int length = file.getAbsolutePath().length();
        if(length > 255)
            log.info(file.getAbsolutePath() + " is " + length);
        format.ttl(file.getPath());
        File[] children = file.listFiles();
        if (children != null)
        {
            format.inc();
            for (int i = 0; i < children.length; i++)
                traverseForSize(children[i], format);
            format.dec();
            return;
        }
    }
}
