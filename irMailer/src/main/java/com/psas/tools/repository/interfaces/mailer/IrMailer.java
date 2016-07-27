package com.psas.tools.repository.interfaces.mailer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
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

@SuppressWarnings("javadoc")
public class IrMailer extends Thread
{
    private static SVNClientManager ourClientManager;
    private static ISVNEventHandler myCommitEventHandler;
    private static ISVNEventHandler myUpdateEventHandler;
    private static ISVNEventHandler myWCEventHandler;

    private static final String IrUrl = "https://interfaces.emit-networking.org/files";

    private final Logger log;

    public IrMailer(String[] args)
    {
        log = LoggerFactory.getLogger(getClass());
    }

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

            //            String myWorkingCopyPath = "/MyWorkingCopy";
            //
            //            String importDir = "/importDir";
            //            String importFile = importDir + "/importFile.txt";
            //            String importFileText = "This unversioned file is imported into a repository";
            //
            //            String newDir = "/newDir";
            //            String newFile = newDir + "/newFile.txt";
            //            String fileText = "This is a new file added to the working copy";

            /*
             * That's where a new directory will be created
             */
            //            SVNURL url = repositoryURL.appendPath("MyRepos", false);
            /*
             * That's where '/MyRepos' will be copied to (branched)
             */
            //            SVNURL copyURL = repositoryURL.appendPath("MyReposCopy", false);

            /*
             * That's where a local directory will be imported into.
             * Note that it's not necessary that the '/importDir' directory must already
             * exist - it will created if necessary. 
             */
            //            SVNURL importToURL = url.appendPath(importDir, false);

            myCommitEventHandler = new CommitEventHandler();
            myUpdateEventHandler = new UpdateEventHandler();
            myWCEventHandler = new WCEventHandler();

            ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
            ourClientManager = SVNClientManager.newInstance(options, authManager);
            ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
            ourClientManager.getUpdateClient().setEventHandler(myUpdateEventHandler);
            ourClientManager.getWCClient().setEventHandler(myWCEventHandler);

            listTree();
            listHistory();
        } catch (Throwable t)
        {
            log.error("failed to initialize", t);
        }
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
                System.out.println("---------------------------------------------");
                System.out.println("revision: " + logEntry.getRevision());
                System.out.println("author: " + logEntry.getAuthor());
                System.out.println("date: " + logEntry.getDate());
                System.out.println("log message: " + logEntry.getMessage());

                if (logEntry.getChangedPaths().size() > 0)
                {
                    System.out.println();
                    System.out.println("changed paths:");
                    Set changedPathsSet = logEntry.getChangedPaths().keySet();

                    for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();)
                    {
                        SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
                        System.out.println(" " + entryPath.getType() + " " + entryPath.getPath() + ((entryPath.getCopyPath() != null) ? " (from " + entryPath.getCopyPath() + " revision " + entryPath.getCopyRevision() + ")" : ""));
                    }
                }
            }
        } catch (Throwable t)
        {
            log.error("failed", t);
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
            System.out.println("Repository Root: " + repository.getRepositoryRoot(true));
            System.out.println("Repository UUID: " + repository.getRepositoryUUID(true));
            SVNNodeKind nodeKind = repository.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE)
            {
                System.err.println("There is no entry at '" + url + "'.");
                System.exit(1);
            } else if (nodeKind == SVNNodeKind.FILE)
            {
                System.err.println("The entry at '" + url + "' is a file while a directory was expected.");
                System.exit(1);
            }
            listEntries(repository, "");
            long latestRevision = repository.getLatestRevision();
            System.out.println("Repository latest revision: " + latestRevision);
        } catch (Throwable t)
        {
            log.error("failed", t);
        }
    }

    public void listEntries(SVNRepository repository, String path) throws SVNException
    {
        Collection entries = repository.getDir(path, -1, null, (Collection) null);
        Iterator iterator = entries.iterator();
        while (iterator.hasNext())
        {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            System.out.println("/" + (path.equals("") ? "" : path + "/") + entry.getName() + " ( author: '" + entry.getAuthor() + "'; revision: " + entry.getRevision() + "; date: " + entry.getDate() + ")");
            if (entry.getKind() == SVNNodeKind.DIR)
            {
                listEntries(repository, (path.equals("")) ? entry.getName() : path + "/" + entry.getName());
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

    private static final void createLocalDir(File aNewDir, File[] localFiles, String[] fileContents)
    {
        if (!aNewDir.mkdirs())
        {
            error("failed to create a new directory '" + aNewDir.getAbsolutePath() + "'.", null);
        }

        for (int i = 0; i < localFiles.length; i++)
        {
            File aNewFile = localFiles[i];
            try
            {
                if (!aNewFile.createNewFile())
                {
                    error("failed to create a new file '" + aNewFile.getAbsolutePath() + "'.", null);
                }
            } catch (IOException ioe)
            {
                aNewFile.delete();
                error("error while creating a new file '" + aNewFile.getAbsolutePath() + "'", ioe);
            }

            String contents = null;
            if (i > fileContents.length - 1)
            {
                continue;
            }
            contents = fileContents[i];

            /*
             * writing a text into the file
             */
            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream(aNewFile);
                fos.write(contents.getBytes());
            } catch (FileNotFoundException fnfe)
            {
                error("the file '" + aNewFile.getAbsolutePath() + "' is not found", fnfe);
            } catch (IOException ioe)
            {
                error("error while writing into the file '" + aNewFile.getAbsolutePath() + "'", ioe);
            } finally
            {
                if (fos != null)
                {
                    try
                    {
                        fos.close();
                    } catch (IOException ioe)
                    {
                        //
                    }
                }
            }
        }
    }

    public static void main(String args[])
    {
        new IrMailer(args).start();
    }
}
