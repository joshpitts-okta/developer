package com.psas.tools.repository.interfaces.mailer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.ccc.tools.TabToLevel;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

@SuppressWarnings("javadoc")
public class IrMailer extends Thread
{
    public static final String LogFilePathKey = "ccc.tools.log-file-path";
    public static final String LogFileBaseKey = "ccc.tools.log-file-base";

    private static final String IrUrl = "https://interfaces.emit-networking.org/files";
    private static final String RootDir = "/share/ir/";
    public static final String MailDir = RootDir + "mail/";
    public static final String SvnDir = RootDir + "svn";
    private static final String ConvertedDir = RootDir + "production/converted/";
    public static final String ConvertedPublish = ConvertedDir + "publish/";
    public static final String ConvertedWorking = ConvertedDir + "working/";
    private static final String DeployedPublish = RootDir + "production/publish/";
    private static final String DeployedWorking = RootDir + "production/working/";
    public static final String MergedDir = RootDir + "output/";
    public static final String MergedPublish = MergedDir + "publish/";
    public static final String MergedWorking = MergedDir + "working/";
    
    
    private static final Logger log;

    private final HashMap<String, List<SVNLogEntry>> pathMap;
    private final HashMap<String, List<SVNLogEntry>> submitterMap;
    private final HashMap<String, List<String>> ownerToPathMap;
    private final HashMap<String, SVNDirEntry> tipDirMap;
    private final PreAlloc preAlloc;
    private final PostAlloc postAlloc;
    private final SofterConverter softerConverter;
    
    public IrMailer(@SuppressWarnings("unused") String[] args)
    {
        pathMap = new HashMap<>();
        submitterMap = new HashMap<>();
        ownerToPathMap = new HashMap<>();
        List<String> list = new ArrayList<>();
        ownerToPathMap.put("aaron.mauchley@us.panasonic.com", list);
        list = new ArrayList<>();
        ownerToPathMap.put("jason.reber@us.panasonic.com", list);
        list = new ArrayList<>();
        ownerToPathMap.put("joseph.clark@us.panasonic.com", list);
        list = new ArrayList<>();
        ownerToPathMap.put("murakami.shuji@jp.panasonic.com", list);
        tipDirMap = new HashMap<>();
        preAlloc = new PreAlloc(pathMap, submitterMap, ownerToPathMap, tipDirMap);
        postAlloc = new PostAlloc(pathMap, submitterMap, ownerToPathMap, tipDirMap);
        softerConverter = new SofterConverter(pathMap, submitterMap, ownerToPathMap, tipDirMap);
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

    private String findOwner(String name) throws Exception
    {
        for (Entry<String, List<String>> entry : ownerMap.entrySet())
        {
            List<String> list = entry.getValue();
            for (String submitter : list)
            {
                if (name.equals(submitter))
                    return entry.getKey();
            }
        }
        throw new Exception(name + " not found in ownerMap");
    }

    private void listHistory()
    {
        DAVRepositoryFactory.setup();

        String url = "http://svn.svnkit.com/repos/svnkit/trunk/doc";
        String name = "anonymous";
        String password = "anonymous";
        long startRevision = 0;
        long endRevision = -1; //HEAD (the latest) revision

        SVNRepository repository = null;
        TabToLevel format = new TabToLevel();
        format.ttl("\nHistory:");
        format.inc();
        try
        {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(IrUrl));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
            repository.setAuthenticationManager(authManager);
            //            ...
            Collection logEntries = null;

            logEntries = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);
            for (Iterator entries = logEntries.iterator(); entries.hasNext();)
            {
                SVNLogEntry logEntry = (SVNLogEntry) entries.next();
                logEntry.toString();
                if (logEntry.getAuthor() == null)
                    continue;
                String owner = findOwner(logEntry.getAuthor());
                if (owner.contains("jason"))
                    log.info("look here: " + owner);
                List<SVNLogEntry> list = submitterMap.get(logEntry.getAuthor());
                if (list == null)
                    list = new ArrayList<>();
                list.add(logEntry);
                submitterMap.put(logEntry.getAuthor(), list);

                if (logEntry.getChangedPaths().size() > 0)
                {
                    Set changedPathsSet = logEntry.getChangedPaths().keySet();
                    for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();)
                    {
                        SVNLogEntryPath entryPath = logEntry.getChangedPaths().get(changedPaths.next());
                        String path = entryPath.getPath();
                        if (entryPath.getPath().endsWith(".emit"))
                        {
                            ModType type = ModType.getType("" + entryPath.getType());
                            switch (type)
                            {
                                case Added:
                                    //                                    log.info("added");
                                    break;
                                case Deleted:
                                    //                                    log.info("deleted");
                                    break;
                                case Modified:
                                    //                                    log.info("modified");
                                    break;
                                case Replaced:
                                    //                                    log.info("replaced");
                                    break;
                                default:
                                    break;
                            }
                            List<SVNLogEntry> plist = pathMap.get(path);
                            if (plist == null)
                            {
                                plist = new ArrayList<>();
                                pathMap.put(path, plist);
                            }
                            plist.add(logEntry);
                            //                            SVNDirEntry dentry = tipDirMap.remove(path);
                            //                            if(dentry != null)
                            //                            {
                            //                                List<String> olist = ownerToPathMap.get(owner);
                            //                                olist.add(path);
                            //                            }
                            //                            else
                            //                                log.info("look here");
                        }
                    }
                } else
                    log.info("look here"); // only the zeroth element which has a null author hits here.
            }

            format.ttl("pathEntries (", pathMap.size(), ")");
            format.inc();
            for (Entry<String, List<SVNLogEntry>> entry : pathMap.entrySet())
            {
                format.ttl(entry.getKey()); // path is key
                format.inc();
                for (SVNLogEntry pentry : entry.getValue())
                {
                    format.ttl("revision: ", pentry.getRevision());
                    format.ttl("author: ", pentry.getAuthor());
                    format.ttl("date: ", pentry.getDate());
                    format.ttl("properties:");
                    format.inc();
                    SVNProperties properties = pentry.getRevisionProperties();
                    boolean found = false;
                    for (Iterator propNames = properties.nameSet().iterator(); propNames.hasNext();)
                    {
                        found = true;
                        String propName = (String) propNames.next();
                        Object propVal = properties.getSVNPropertyValue(propName);
                        format.ttl(propName, " = ", propVal);
                    }
                    if (!found)
                        format.ttl("none");
                    format.dec();

                    if (pentry.getChangedPaths().size() > 0)
                    {
                        format.ttl("changed paths:");
                        format.inc();
                        Set changedPathsSet = pentry.getChangedPaths().keySet();
                        for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();)
                        {
                            SVNLogEntryPath entryPath = pentry.getChangedPaths().get(changedPaths.next());
                            ModType type = ModType.getType("" + entryPath.getType());
                            format.ttl("modtype: ", type);
                            String path = entryPath.getPath();
                            format.ttl("path: ", path);
                            format.inc();
                            if (entryPath.getCopyPath() != null)
                            {
                                format.ttl("type: ", entryPath.getType());
                                format.ttl("from: ", entryPath.getCopyPath());
                                format.ttl("revision: ", entryPath.getCopyRevision());
                            }
                            format.dec();
                        }
                        format.dec();
                    }
                }
                format.dec();
            }
            format.dec();
            format.ttl("submitterEntries (", submitterMap.size(), ")");
            format.inc();
            for (Entry<String, List<SVNLogEntry>> entry : submitterMap.entrySet())
                format.ttl(entry.getKey());

            for (Entry<String, List<SVNLogEntry>> entry : submitterMap.entrySet())
            {
                format.ttl(entry.getKey());
                format.inc();
                for (SVNLogEntry lentry : entry.getValue())
                {
                    format.ttl("revision: ", lentry.getRevision());
                    format.ttl("author: ", lentry.getAuthor());
                    format.ttl("date: ", lentry.getDate());
                    format.ttl("properties:");
                    format.inc();
                    SVNProperties properties = lentry.getRevisionProperties();
                    boolean found = false;
                    for (Iterator propNames = properties.nameSet().iterator(); propNames.hasNext();)
                    {
                        found = true;
                        String propName = (String) propNames.next();
                        Object propVal = properties.getSVNPropertyValue(propName);
                        format.ttl(propName, " = ", propVal);
                    }
                    if (!found)
                        format.ttl("none");
                    format.dec();
                    if (lentry.getChangedPaths().size() > 0)
                    {
                        format.ttl("changed paths:");
                        format.inc();
                        Set changedPathsSet = lentry.getChangedPaths().keySet();
                        for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();)
                        {
                            SVNLogEntryPath entryPath = lentry.getChangedPaths().get(changedPaths.next());
                            ModType type = ModType.getType("" + entryPath.getType());
                            format.ttl("modtype: ", type);
                            String path = entryPath.getPath();
                            format.ttl("path: ", path);
                            format.inc();
                            if (entryPath.getCopyPath() != null)
                            {
                                format.ttl("type: ", entryPath.getType());
                                format.ttl("from: ", entryPath.getCopyPath());
                                format.ttl("revision: ", entryPath.getCopyRevision());
                            }
                            format.dec();
                        }
                        format.dec();
                    }
                }
                format.dec();
            }
            format.dec();
            format.ttl("ownerToPathMap");
            format.inc();
            for (Entry<String, List<String>> entry : ownerToPathMap.entrySet())
            {
                List<String> paths = entry.getValue();
                format.ttl(entry.getKey(), " (", paths.size(), ")");
                format.inc();
                for (String path : paths)
                    format.ttl(path);
                format.dec();
            }
            format.dec();
            format.dec();

            format.ttl("tipDirMap (", tipDirMap.size(), ")");
            format.inc();
            for (Entry<String, SVNDirEntry> entry : tipDirMap.entrySet())
                format.ttl(entry.getKey());
            format.dec();
            log.info(format.toString());
        } catch (Throwable t)
        {
            format.ttl("failed");
            log.error(format.toString(), t);
        }
        //            ...
    }

    private void listTree()
    {
        DAVRepositoryFactory.setup();

        String url = "http://svn.svnkit.com/repos/svnkit/trunk/doc";
        String name = "anonymous";
        String password = "anonymous";

        SVNRepository repository = null;
        try
        {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(IrUrl));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
            repository.setAuthenticationManager(authManager);
            log.info("Repository Root: " + repository.getRepositoryRoot(true));
            log.info("Repository UUID: " + repository.getRepositoryUUID(true));
            SVNNodeKind nodeKind = repository.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE)
            {
                log.error("There is no entry at '" + url + "'.");
                System.exit(1);
            } else if (nodeKind == SVNNodeKind.FILE)
            {
                log.error("The entry at '" + url + "' is a file while a directory was expected.");
                System.exit(1);
            }
            TabToLevel format = new TabToLevel();
            format.ttl("\nEntries:");
            format.inc();
            listEntries(repository, "", format);
            format.ttl("\n\ntotal .emit files: ", tipDirMap.size());
            log.info(format.toString());
            long latestRevision = repository.getLatestRevision();
            log.info("Repository latest revision: " + latestRevision);
        } catch (Throwable t)
        {
            log.error("failed", t);
        }
    }

    public void listEntries(SVNRepository repository, String path, TabToLevel format) throws Exception
    {
        Collection entries = repository.getDir(path, -1, null, (Collection) null);
        Iterator iterator = entries.iterator();
        while (iterator.hasNext())
        {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            if (entry.getName().endsWith(".emit"))
            {
                String p = "/" + (path.equals("") ? "" : path + "/") + entry.getName();
                format.ttl(p, " ( author: '", entry.getAuthor(), "'; revision: ", entry.getRevision(), "; date: ", entry.getDate(), ")");
                tipDirMap.put(p, entry);
                String owner = findOwner(entry.getAuthor());
                List<String> olist = ownerToPathMap.get(owner);
                olist.add(p);
            }
            if (entry.getKind() == SVNNodeKind.DIR)
            {
                listEntries(repository, (path.equals("")) ? entry.getName() : path + "/" + entry.getName(), format);
            }
        }
    }

    private static void error(String message, Exception e)
    {
        System.err.println(message + (e != null ? ": " + e.getMessage() : ""));
        System.exit(1);
    }

    private static void showInfo(File wcPath, SVNRevision revision, boolean isRecursive) throws SVNException
    {
        ourClientManager.getWCClient().doInfo(wcPath, revision, isRecursive, new InfoHandler());
    }

    //@formatter:off
    private static void showStatus(
                    File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll,
                    boolean isIncludeIgnored, boolean isCollectParentExternals) throws SVNException 
    //@formatter:on
    {

        //doStatus(File, SVNRevision, SVNDepth, boolean, boolean, boolean, boolean, ISVNStatusHandler, Collection)}

        //@formatter:off
        ourClientManager.getStatusClient().doStatus(
                        wcPath, isRecursive, isRemote, isReportAll,
                        isIncludeIgnored, isCollectParentExternals, 
                        new StatusHandler(isRemote));
        //@formatter:on
    }

    private enum ModType
    {
        Added("A"), Deleted("D"), Modified("M"), Replaced("R");

        private ModType(String type)
        {
            this.type = type;
        }

        public static ModType getType(String type)
        {
            if (Added.type.equals(type))
                return Added;
            if (Deleted.type.equals(type))
                return Deleted;
            if (Modified.type.equals(type))
                return Modified;
            if (Replaced.type.equals(type))
                return Replaced;
            throw new RuntimeException("invalid type: " + type);
        }

        private String type;
    }

    private static final Map<String, List<String>> ownerMap;
    private static List<String> deployedIids;
    static
    {
        String logPath = "/var/opt/opendof/irMailer/log/IrMailer.log";
        System.setProperty(LogFilePathKey, logPath);
        String base = logPath;
        int idx = logPath.lastIndexOf('.');
        if (idx != -1)
            base = logPath.substring(0, idx);
        System.setProperty(LogFileBaseKey, logPath);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        log = LoggerFactory.getLogger(IrMailer.class);
        log.info(LogFilePathKey + " = " + logPath);
        
        

        ownerMap = new HashMap<>();
        List<String> names = new ArrayList<>();
        names.add("aaron.mauchley@us.panasonic.com");
        ownerMap.put("aaron.mauchley@us.panasonic.com", names);

        names = new ArrayList<>();
        names.add("jason.reber@us.panasonic.com");
        names.add("mark.nelson@us.panasonic.com");
        names.add("mnelson");
        ownerMap.put("jason.reber@us.panasonic.com", names);

        names = new ArrayList<>();
        names.add("zachary.rasmussen@us.panasonic.com");
        ownerMap.put("john.winder@us.panasonic.com", names);

        names = new ArrayList<>();
        names.add("joseph.clark@us.panasonic.com");
        names.add("jclark");
        names.add("bill.adams@us.panasonic.com");
        names.add("bryant.eastham@us.panasonic.com");
        names.add("beastham");
        names.add("chad.adams@us.panasonic.com");
        names.add("cadams");
        names.add("clint.jones@us.panasonic.com");
        names.add("david.ethington@us.panasonic.com");
        names.add("devon.sumner@us.panasonic.com");
        names.add("dsumner");
        names.add("dsunley");
        names.add("james.simister@us.panasonic.com");
        names.add("jsimister");
        names.add("jstricker");
        names.add("Jonathan.Stringer@us.panasonic.com");
        names.add("jonathan.stringer@us.panasonic.com");
        names.add("jstringer");
        names.add("sbaker");
        names.add("tmilligan");
        ownerMap.put("joseph.clark@us.panasonic.com", names);

        names = new ArrayList<>();
        names.add("aizu.kazuhiro@jp.panasonic.com");
        names.add("fujiwara.eiki@jp.panasonic.com");
        names.add("jishizaka");
        names.add("hara.hiroki0@jp.panasonic.com"); //0??
        names.add("hashiguchi.akira@jp.panasonic.com");
        names.add("hkamihara");
        names.add("hotsubo");
        names.add("ishihara.tomoko@jp.panasonic.com");
        names.add("kai.toshifumi@jp.panasonic.com");
        names.add("knakakita");
        names.add("mkawasaki");
        names.add("murakami.shuji@jp.panasonic.com");
        names.add("smurakami");
        names.add("oda.tomo@jp.panasonic.com");
        names.add("rashid.mahbub@jp.panasonic.com");
        names.add("sakamoto.hiroyuki@jp.panasonic.com");
        names.add("sekine.osamu@jp.panasonic.com osekine");
        names.add("sunagawa.mika@jp.panasonic.com");
        names.add("takeuchi.yoshiyasu@jp.panasonic.com");
        names.add("tanaka.toshifusa@jp.panasonic.com");
        names.add("thatanaka");
        names.add("takano.satoshi@jp.panasonic.com");
        names.add("tsujimura.soushi@jp.panasonic.com");
        names.add("umaso@panasonic-denko.co.jp");
        names.add("watanabe.yasuhiko@jp.panasonic.com");
        names.add("yoshimura.yuya001@jp.panasonic.com");
        names.add("osekine");
        names.add("sekine.osamu@jp.panasonic.com");
        ownerMap.put("murakami.shuji@jp.panasonic.com", names);

        deployedIids = new ArrayList<>();
        for (int j = 0; j < 2; j++)
        {
            File[] children = new File(DeployedPublish).listFiles();
            if (j == 1)
                children = new File(DeployedWorking).listFiles();

            for (int i = 0; i < children.length; i++)
            {
                File child = children[i];
                if (children[i].isFile())
                {
                    String ciid;
                    try
                    {
                        deployedIids.add(getIidFromDotEmit(child));
                    } catch (Exception e)
                    {
                        throw new RuntimeException("did not find iid in file: " + child.getAbsolutePath());
                    }

                }
            }
        }
    }

    private static SVNClientManager ourClientManager;
    private static ISVNEventHandler myCommitEventHandler;
    private static ISVNEventHandler myUpdateEventHandler;
    private static ISVNEventHandler myWCEventHandler;

    @Override
    public void run()
    {
        try
        {
            FSRepositoryFactory.setup();

            SVNURL repositoryURL = null;
            try
            {
                //                repositoryURL = SVNURL.parseURIEncoded("file://localhost/testRep");
                repositoryURL = SVNURL.parseURIEncoded(IrUrl);
            } catch (SVNException e)
            {
                log.error("failed to obtain repositoryURL: " + IrUrl, e);
            }

            myCommitEventHandler = new CommitEventHandler();
            myUpdateEventHandler = new UpdateEventHandler();
            myWCEventHandler = new WCEventHandler();

            ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
            ourClientManager = SVNClientManager.newInstance(options, authManager);
            ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
            ourClientManager.getUpdateClient().setEventHandler(myUpdateEventHandler);
            ourClientManager.getWCClient().setEventHandler(myWCEventHandler);


// for first phase merge of .emit and .meta's uncomment the following two lines
//            Converter converter = new Converter(null);
//            converter.doMerge();

// to produce the mailers structure uncomment the following two lines -- utilizes the previously converted files
//           listTree();
//           postAlloc.createMailerDirectories();
            
// work on the final conversions.            
//           listTree();
//           softerConverter.convert();
           softerConverter.fnSize();
// the following was used for discovery only - determined all the submitters and who they should fall under            
//            listHistory();
            
// the following line is now obsolete            
//            preAlloc.createPreAllocList();
            log.info("completed ok");
        } catch (Throwable t)
        {
            log.error("failed to initialize", t);
        }
    }

    public static void main(String args[])
    {
        new IrMailer(args).start();
    }
}
