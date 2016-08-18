package com.psas.tools.repository.interfaces.mailer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.oal.DOFInterfaceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNLogEntry;

@SuppressWarnings("javadoc")
public class PostAlloc extends Thread
{
    private static final String V1Svn = "/legacy";
    private static final String Publish = "/publish";
    private static final String Working = "/working";
    private static final String MailPublish = IrMailer.SvnDir + Publish; 
    private static final String MailWorking = IrMailer.SvnDir + Working;
    
    private static final String MergedBase = IrMailer.SvnDir + Working; 

    
    private final HashMap<String, List<SVNLogEntry>> pathMap;
    private final HashMap<String, List<SVNLogEntry>> submitterMap;
    private final HashMap<String, List<String>> ownerToPathMap;
    private final HashMap<String, SVNDirEntry> tipDirMap; // path is ksy
    private final Logger log;
    
    //@formatter:off
    public PostAlloc(
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
    
    public void createMailerDirectories() throws Exception
    {
        int totalCount = 0;
        int pacount = 0;
        for (Entry<String, List<String>> ownerElement : ownerToPathMap.entrySet())
        {
            String owner = ownerElement.getKey();
            String basePath = IrMailer.MailDir + owner;
            int count = 0;
            for (String path : ownerElement.getValue())
            {
                String svn = IrMailer.SvnDir + path;
                File dotEmit = new File(path);
                File parent = dotEmit.getParentFile();
                File v1svn = new File(parent.getPath() + V1Svn);
                String siid = getIidFromDotEmit(new File(svn));
                if(PreAlloc.alreadyAllocated(siid))
                {
                    deleteChildrenAndNode(new File(svn).getParentFile());
                    File mergedFile = findMergedFile(svn);
                    deleteChildrenAndNode(mergedFile.getParentFile());
                    ++pacount;
                    continue;
                }
                DOFInterfaceID iid = DOFInterfaceID.create(siid);
                File convertedFile = findConvertedFile(siid);
                boolean working = svn.startsWith(MailWorking); 
                String basep = working ? basePath + Working : basePath + Publish;
                String midPath = "";
                if(working)
                {
                    basep += "/registry_" + iid.getRegistry();
                    midPath = parent.getPath().substring("/working".length());
                }
//                else
//                    midPath = ""; ;parent.getPath().substring("/publish".length());
                basep += midPath;
                createDirs(new File(basep));
                if (convertedFile != null)
                {
                    String fileName = convertedFile.getName();
                    String iidname = findIidName(convertedFile, siid);
                    String mailPath = basep + "/" + iidname;
                    createDirs(new File(mailPath + V1Svn));
                    File mergedFile = findMergedFile(svn);
                    moveFile(mergedFile, new File(mailPath + V1Svn), mergedFile.getName());
                    deleteChildrenAndNode(mergedFile.getParentFile());
                    moveFile(convertedFile, new File(mailPath), fileName);
                    moveFiles(new File(svn).getParentFile(), new File(mailPath + V1Svn));
                } else
                {
                    File mergedFile = findMergedFile(svn);
                    String mailPath = basep + "/" + findIidNameNonConverted(new File(svn), siid);
                    moveFile(mergedFile, new File(mailPath + V1Svn), mergedFile.getName());
                    moveFiles(new File(svn).getParentFile(), new File(mailPath + V1Svn));
                }
                
                SVNDirEntry dirEntry = tipDirMap.get(path);
                if (dirEntry != null)
                {
                    ++count;
                    ++totalCount;
                    //                    tipDirMap.remove(path);
                }
                //                else
                //                    log.info("look here: " + path);
            }
            log.info(owner + " tipDirMap.size: " + tipDirMap.size() + " hits count: " + count);
        }
        log.info("Total count: " + totalCount + pacount);
    }

    private void deleteChildrenAndNode(File node) throws Exception
    {
        File[] files = node.listFiles();
        for(int i=0; i < files.length; i++)
        {
            if(files[i].isDirectory())
                throw new Exception("unexpected directory: " + files[i]);
            boolean ok = files[i].delete();
            if(!ok)
                throw new Exception("failed to delete child: " + files[i]);
        }
        boolean ok = node.delete();
        if(!ok)
            throw new Exception("failed to delete node: " + node);
    }
    
    public static String getIidFromDotEmit(File dotEmit) throws Exception
    {
        byte[] raw = new byte[1024 * 16];
        FileInputStream fis = new FileInputStream(dotEmit);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(raw, 0, raw.length);
        bis.close();
        fis.close();
        String str = new String(raw);
        int idx = str.indexOf("iid=\"[");
        idx += 5;
        str = str.substring(idx);
        idx = str.indexOf(']');
        str = str.substring(0, idx + 1);
        return str;
    }

    private File findConvertedFile(String iid) throws Exception
    {
        File parent = new File(IrMailer.ConvertedPublish);
        for (int j = 0; j < 2; j++)
        {
            File[] children = new File(IrMailer.ConvertedPublish).listFiles();
            if (j == 1)
                children = new File(IrMailer.ConvertedWorking).listFiles();

            for (int i = 0; i < children.length; i++)
            {
                File child = children[i];
                if (children[i].isFile())
                {
                    String ciid = getIidFromDotEmit(child);
                    if (iid.equals(ciid))
                        return child;
                }
            }
        }
        return null;
    }
    
    private File findMergedFile(String svnPath) throws Exception
    {
        String path = svnPath;
        int idx = path.indexOf(".emit");
        path = path.substring(0, idx);
        path += ".xml";
        path = path.substring(IrMailer.SvnDir.length() + 1);
        path = IrMailer.MergedDir + path;
        return new File(path);
    }
    
    private void createDirs(File to) throws Exception
    {
        String path = to.getPath();
        if (!to.exists())
        {
            boolean ok = new File(path).mkdirs();
            if (!ok)
                throw new Exception("failed to mkdirs: " + path);
        }
    }
    
    private void moveFile(File from, File to, String fileName) throws Exception
    {
        createDirs(to);
        String toPath = to.getPath() + "\\" + fileName;
        FileOutputStream fos = new FileOutputStream(toPath);
        Files.copy(from.toPath(), fos);
        fos.close();
    }
    
    private void moveFiles(File from, File to) throws Exception
    {
        if (!to.exists())
        {
            boolean ok = to.mkdirs();
            if (!ok)
                throw new Exception("failed to mkdirs: " + to.getAbsolutePath());
        }
        File[] children = from.listFiles();
        for(int i=0; i < children.length; i++)
        {
            File child = children[i];
            if(child.isDirectory())
                throw new Exception("did not expect a directory here: " + child.getAbsolutePath());
            File toPath = new File(to.getAbsolutePath() + "/" + child.getName());
            FileOutputStream fos = new FileOutputStream(toPath);
            File fromPath = new File(from.getAbsolutePath() + "/" + child.getName());
            Files.copy(fromPath.toPath(), fos);
            fos.close();
            boolean ok = fromPath.delete();
            if (!ok)
            {
                throw new Exception("failed to delete the from child: " + fromPath.getAbsolutePath());
//                log.info("failed to delete the from file: " + fromPath.getAbsolutePath());
            }
        }
        boolean ok = from.delete();
        if (!ok)
            throw new Exception("failed to delete the from node: " + from.getAbsolutePath());
    }

    public static String findIidNameNonConverted(File svn, String iid) throws Exception
    {
        File[] children = svn.getParentFile().listFiles();
        String str = null;
        boolean found = false;
        for (int i = 0; i < children.length; i++)
        {
            File child = children[i];
            if (child.isDirectory())
                throw new Exception("did not expect a directory: " + child.getAbsolutePath());
            BufferedInputStream bis = null;
            byte[] raw = new byte[1024 * 16];
            FileInputStream fis = new FileInputStream(child);
            bis = new BufferedInputStream(fis);
            bis.read(raw, 0, raw.length);
            bis.close();
            fis.close();
            str = new String(raw);
            String tag = "<name xml:lang=\"en\">";
            int idx = str.indexOf(tag);
            if (idx == -1)
                continue;
            found = true;
            idx += tag.length();
            str = str.substring(idx);
            idx = str.indexOf('<');
            str = str.substring(0, idx);
            str += "-" + iid;
            str = str.replace(':', ';');
            return str;
        }
        if(!found)
        {
            String fname = svn.getName();
            int idx = fname.indexOf('.');
            fname = fname.substring(0, idx);
            fname += "-" + iid;
            fname = fname.replace(':', ';');
            return fname;
        }
        return null;
    }

    public static String findIidName(File file, String iid) throws Exception
    {
        BufferedInputStream bis = null;
        byte[] raw = new byte[1024 * 16];
        FileInputStream fis = new FileInputStream(file);
        bis = new BufferedInputStream(fis);
        bis.read(raw, 0, raw.length);
        bis.close();
        fis.close();
        String str = new String(raw);
        String tag = "<md:display-name xml:lang=\"en\">";
        int idx = str.indexOf(tag);
        if (idx == -1)
        {
            tag = "<md:code-name>";
            idx = str.indexOf(tag);
            if (idx == -1)
            {
                String fname = file.getName();
                idx = fname.indexOf('.');
                fname = fname.substring(0, idx);
                return fname;
            }
        }
        idx += tag.length();
        str = str.substring(idx);
        idx = str.indexOf('<');
        str = str.substring(0, idx);
        str += "-" + iid;
        str = str.replace(':', ';');
        return str;
    }
}
