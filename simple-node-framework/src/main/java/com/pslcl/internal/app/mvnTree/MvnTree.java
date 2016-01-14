package com.pslcl.internal.app.mvnTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.spawn.Spawn;

@SuppressWarnings("javadoc")
public class MvnTree
{
    private final static boolean pruneBuild = true;
    private final static boolean checkout = true;

    public static final String apRepoUrl = "https://source.ancillary.pewla.com/ap/";
    public static final String emitRepoUrl = "https://source.emit-networking.org/emit/";
    public static final String emit_appRepoUrl = "https://source.emit-networking.org/emit_app/";
    public static final String emit_mwRepoUrl = "https://source.emit-networking.org/emit_mw/";

    protected static final String svnWorkingRoot = "C:/ws/svn-tree/";
    protected static final String personalBuildRoot = "C:/ws/personalBuild/";
    private static final String artifactPomTemplate = personalBuildRoot+"artifactPom.xml";

    private static final String logDir = "/var/opt/build/MvnTree";
    @SuppressWarnings("unused")
    private static final String svn_settings = "/Program Files/apache-maven-3.0.5/conf/.scm/svn-settings.xml";

    public static final String[] srcRepoBuildFolders = new String[] { "ap", "emit", "emit_app", "emit_mw" };
    public static final String[] srcRepositoryUrls = new String[] { apRepoUrl, emitRepoUrl, emit_appRepoUrl, emit_mwRepoUrl };

    // @formatter:off
    public static final String[] buildFiles = new String[]
    {
        "configure.ac", "INSTALL.html.in", "Makefile.in", "RELEASE.html.in", "COPYING.txt", "INSTALL.TXT", "README.txt", "RELEASE.txt.in", "configure.in", 
    };
    
    // srcRepoBuildFolders index, path after that
    public static final String[] badTests = new String[]
    {
        "1", "/trunk/src/emit-oal-java/emit-inet", 
        "1", "/trunk/src/emit-oal-java/emit-oal", 
        "1", "/trunk/src/emit-sms4-cipher-plugin-java/emit-sms4-cipher-plugin-java", 
        "1", "/trunk/src/emit-twofish-cipher-plugin-java/emit-twofish-cipher-plugin-java", 
//        "3", "tools/trunk/src/sdk-registration-maintainer-java/sdk-registration-maintainer-java",
        "3", "tools/trunk/src/emit-connection-reconnecting-listener-java/emit-connection-reconnecting-listener-java",
    };
    
    public static final String[] eclipseFiles = new String[]
    {
        ".project", ".settings", ".metadata", ".svn",   
    };
    public static final String[] apTrunks = new String[]
    {
        "trunk/src/app-ancillary-power/pesdca-site-controller", "0",
        "trunk/src/app-ancillary-power/pesdca-site-controller", "0",        
        "trunk/src/eng-ancpwr", "0",        
        "trunk/src/eng-local-controller/esc-local-controller", "0",        
        "trunk/src/eng-local-controller/esdc-local-controller", "0",        
        "trunk/src/eng-platform-battery/pesdca-platform-battery-common", "0",        
        "trunk/src/eng-platform-battery/pesdca-platform-battery-queuehandler", "0",        
        "trunk/src/eng-platform-battery/pesdca-platform-battery-site", "0",        
        "trunk/src/eng-platform-energy/pesdca-platform-energy", "0",        
    };
    
    public static final String[] emitTrunks = new String[] 
    {
        "trunk/src/emit-domain-management-java/emit-domain-management-java", "39952",  // 2.1 release
        "trunk/src/emit-oal-java/emit-inet", "45206", // 6.1.3 release
        "trunk/src/emit-oal-java/emit-oal", "45206",  // 6.1.3 release
        "trunk/src/emit-sms4-cipher-plugin-java/emit-sms4-cipher-plugin-java", "44380", // 1.0 release
        "trunk/src/emit-twofish-cipher-plugin-java/emit-twofish-cipher-plugin-java", "44380", // 1.0 release
        
        "trunk/src/eng-sharpen/sharpen.core", "0",
        "trunk/src/frameworks-java/emit-auth", "0",         // "31934", // 5.0.1 release this will not build against the old oal jars
        "trunk/src/frameworks-java/emit-framework", "0",    //"31934", // 5.0.1 release
        "trunk/src/frameworks-java/emit-frameworks", "0",   // "31934", // 5.0.1 release
        "trunk/src/frameworks-java/emit-service", "0",      //"31934", // 5.0.1 release
    };
    
    public static final String[] emit_appTrunks = new String[] 
    { 
        "as/trunk/src/eng-domain-io/domain-io", "9272", // 2.0.0 internal release
        "as/trunk/src/eng-domain-management-cli-support/domain-management-cli-support", "0", // ???
        "as/trunk/src/eng-domain-management-command-line/cred-gen", "9272", // 2.0.0 internal release
        "as/trunk/src/eng-domain-management-command-line/domain-management-command-line", "9272", // 2.0.0 internal release
        
        "as/branches/patch-eng-domain-storage-cassandra-2.1/src/eng-domain-storage-cassandra/domain-storage-cassandra", "14954", // 2.1.2 internal release
        "as/branches/patch-eng-domain-storage-cassandra-2.1/src/eng-domain-storage-cassandra/domain-storage-cassandra-installer", "14954", // 2.1.2 internal release
//        "as/trunk/src/eng-domain-storage-cassandra/domain-storage-cassandra", "14954", // 2.1.2 internal release
//        "as/trunk/src/eng-domain-storage-cassandra/domain-storage-cassandra-installer", "14954", // 2.1.2 internal release
        "as/trunk/src/eng-domain-storage-javadb/domain-storage-javadb", "9282", // 2.0.0 internal release 
        "as/trunk/src/eng-domain-storage-javadb/domain-storage-javadb-installer", "9282", // 2.0.0 internal release
        "as/trunk/src/eng-domain-storage-javadb/domain-storage-javadb-routines", "9282", // 2.0.0 internal release
        "as/trunk/src/eng-domain-storage-oracle/domain-storage-oracle", "9273", // 3.0.0 internal release 
        
        "backup/trunk/src/eng-backup-service/enc-service-backup", "0",  //"8289",       1.0.0 internal release
        
        "common/trunk/src/eng-app-common/emit-common", "0",  // "4662",              // 1.0.0 internal release
        "common/trunk/src/eng-app-common-api/enc-app-common-api", "0", // "4662",   // 1.0.0 internal release
        "common/trunk/src/eng-data-common", "0",  //"4662",                         // 1.0.0 internal release
        "common/trunk/src/eng-data-common-javadb", "0",
        "common/trunk/src/eng-data-common-oracle", "0",
        "common/trunk/src/eng-data-common-postgres", "4662",                // 1.0.0 internal release
        "common/trunk/src/eng-utility-lib/pesdca-utility", "7642", // 1.1.0 internal release
        
        "datatransfer/trunk/src/rpm-data-distribution-service-on-aws/enc-service-datadistribution-aws", "13638", // 2.2.0 internal release
        "datatransfer/trunk/src/rpm-data-store-service-on-aws/enc-service-datastore-aws", "13638",
        "datatransfer/trunk/src/rpm-data-store-service-on-aws/enc-service-datastore-aws-migration", "13638",
        "datatransfer/trunk/src/rpm-data-store-service-on-aws/enc-service-datastore-util", "13638",
        
        "dsp/trunk/src/bundle-dsp-gateway/enc-dsp-gateway-config", "0",
        "dsp/trunk/src/bundle-dsp-gateway/enc-dsp-gateway-dspconn", "0",
        "dsp/trunk/src/bundle-dsp-gateway/enc-dsp-gateway-lanserver", "0",
        
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-authentication", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-content-management", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-dof", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-storage-cassandra", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-storage-s3", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-user-management", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-verticle", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-vertx", "0",
        "dsp/trunk/src/eng-cloud-web-platform/cloud-web-platform-vertx-module", "0",
        
        "dsp/trunk/src/eng-dsp/enc-dsp", "15615",                           // 1.1.0 testing-release
        "dsp/trunk/src/eng-dsp/enc-dsp-authserver", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-connectioncontrol", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-connectionmanager", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-connectionstats", "0",   // did not promote to test
        "dsp/trunk/src/eng-dsp/enc-dsp-domainfactory-cassandra", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-domainfactory-javadb", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-domainfactory-oracle", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-domainloader", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-hubmanager", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-topologyviewer", "15615",
        "dsp/trunk/src/eng-dsp/enc-dsp-wanserver", "15615",
        
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-backup-s3-cassandra", "15615",    // 1.2 testing-release
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-check", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-common", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-config", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-nodemanager", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-regionorder", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-report", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-servermanager", "15615",
        "dsp/trunk/src/eng-dsp-on-aws/enc-dsp-aws-staticipmanager", "15615",
        
        "dsp/trunk/src/rpm-dsp/enc-dsp-linuxconfig", "15615",               // 1.1.0 testing-release
        
        "emnet/trunk/src/app-emnet-bridge/enc-service-emnet-bridge", "10757",    // 1.0 release
        
        "jmx/trunk/src/eng-jmx/jmx-proxy", "0", 
        
        "logging/trunk/src/emit-logging-service/enc-service-logging", "0", // this is mine I think
        "logging/trunk/src/eng-logging-service/enc-service-logging", "0",
        
        "nagios/trunk/src/eng-nagios/emit-nagios", "0", 
        
        "naming/trunk/src/eng-naming-service/enc-service-naming", "0", 
        
        "platform-hostel/trunk/src/emit-platform-hostel-ac-java/emit-platform-hostel-ac", "15832",  // 2.0.1 internal
        
        "schedule/trunk/src/emit-schedule-service/enc-service-schedule", "14030", // 1.0.1 release
        
        "securityconfig/trunk/src/eng-securityconfig-service/enc-service-securityconfig", "0",
        
        "solar/trunk/src/app-solar-data-source-service/alsoenergy-api", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-collectorbridge-alsoenergy", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-collectorbridge-auroravision", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-collectorbridge-deck", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-collectorbridge-egauge", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-collectorbridge-locusenergy", "0",
        "solar/trunk/src/app-solar-data-source-service/solar-data-source-service", "0",
        
        "status/trunk/src/eng-status-service/enc-service-status", "0",
        
        "suitcase-demo/trunk/src/app-suitcase/suitcase-android", "0",
        "suitcase-demo/trunk/src/app-suitcase/suitcase-backend", "0",
        "suitcase-demo/trunk/src/app-suitcase/suitcase-common", "0",
        "suitcase-demo/trunk/src/app-suitcase/suitcase-frontend", "0",
        
        "tunnel-transport/trunk/src/app-tunnel-transport/tunnel-transport", "0", 
        
        "update/trunk/src/eng-update-service/enc-service-update", "0", 
        
        "version/trunk/src/eng-version-service/enc-service-version", "0",
    };

    public static final String[] emit_mwTrunks = new String[] 
    { 
        "distributed_service_platform/trunk/src/emit-data-transfer-java/emit-data-transfer-common", "3226",    // 1.0.7 release
        "distributed_service_platform/trunk/src/emit-data-transfer-java/emit-data-transfer-manager", "3226",
        "distributed_service_platform/trunk/src/emit-data-transfer-java/emit-data-transfer-sink", "3226", 
        "distributed_service_platform/trunk/src/emit-data-transfer-java/emit-data-transfer-snapshot", "3226",
        "distributed_service_platform/trunk/src/emit-data-transfer-java/emit-data-transfer-source", "3226",
        
        "interface_repository/trunk/tech-services/src/app-interface-repository/cocoon", "0", 
        "interface_repository/trunk/tech-services/src/app-interface-repository/merger", "0", 
        "interface_repository/trunk/tech-services/src/app-interface-repository/validate", "0",
        
        "tools/trunk/src/emit-connection-reconnecting-listener-java/emit-connection-reconnecting-listener-java", "2831", // 1.1.0 release
        
        "tools/trunk/src/emit-domain-authenticator-restarting-listener-java/emit-domain-authenticator-restarting-listener-java", "1384", // 1.0.0 release 
        
        "tools/trunk/src/emit-domain-manager-restarting-listener-java/emit-domain-manager-restarting-listener-java", "1384", // 1.0.0 release
        
        "tools/trunk/src/emit-server-restarting-listener-java/emit-server-restarting-listener-java", "2831", // 1.1.0 release
        
        "tools/trunk/src/emit-service-utils/emit-service-utils-common", "0", // 1.0.0 snapshot
        
        "tools/trunk/src/emit-slf4j-log-listener-java/emit-slf4j-log-listener", "2705", // 1.0.1 release
        
        "tools/trunk/src/emit-subscribe-java/emit-subscribe", "2929",   // 1.0.1 release 
        
        "tools/trunk/src/sdk-piped-storage-java/sdk-piped-storage-java", "0",
    };
    // @formatter:on

    private final Logger log;

    private final TreeInfo[] trees;

    public MvnTree()
    {
        System.setProperty("log-file-base-name", logDir);
        log = LoggerFactory.getLogger(getClass());
        log.info("file logging to: " + logDir);
        trees = new TreeInfo[3];
//        trees[0] = new TreeInfo(apRepoUrl, srcRepoBuildFolders[0], apTrunks, false);
        trees[0] = new TreeInfo(emitRepoUrl, srcRepoBuildFolders[1], emitTrunks, false);
        trees[1] = new TreeInfo(emit_appRepoUrl, srcRepoBuildFolders[2], emit_appTrunks, true);
        trees[2] = new TreeInfo(emit_mwRepoUrl, srcRepoBuildFolders[3], emit_mwTrunks, true);
    }

    public void run()
    {
        try
        {
            Coordinates coor = new Coordinates();
            traverseDir(new File(personalBuildRoot), coor);
            log.info(coor.toString());
            buildCheckoutTree();
            checkout();
            prune();
            removeBadTests();
            createShadowBuild();
        } catch (Throwable t)
        {
            log.error("failed: ", t);
        }
    }
    
    private void removeBadTests()
    {
        for(int i=0; i < badTests.length; i+=2)
        {
            String path = svnWorkingRoot + srcRepoBuildFolders[Integer.parseInt(badTests[i])] + "/" + badTests[i+1] + "/src/test" ; 
            recursiveDelete(new File(path));
        }
    }

    private void traverseDir(File file, Coordinates coor) throws Exception
    {
        File[] children = file.listFiles();
        for(int i=0; i < children.length; i++)
        {
            File child = children[i];
            if(child.isDirectory())
                traverseDir(child, coor);
            else
            {
                if(child.getName().equals("pom.xml"))
                {
                    logPom(child, coor);
                }
            }
        }
    }
   
    private void logPom(File file, Coordinates coor) throws Exception
    {
        String name = file.getAbsolutePath();
        name = name.replace('\\', '/');
        name = name.substring(name.indexOf("personalBuild/")+14);
        int index = name.lastIndexOf('/');
        if(index != -1)
            name = name.substring(0, index);
        coor.name.add(name);

        FileReader fr = new FileReader(file);
        BufferedReader bfr = new BufferedReader(fr);
        String line = null;
        String parentGroup = null;
        String parentVersion = null;
        String group = null;
        String version = null;
        String artifactId = null;
        boolean inParent = false;
        do
        {
            line = bfr.readLine();
            if(line == null)
                break;
            if(!inParent && line.indexOf("<parent>") != -1)
                inParent = true;
            if(inParent && line.indexOf("</parent>") != -1)
                inParent = false;

            if(inParent)
            {
                if(parentGroup == null)
                    parentGroup = getElementData(line, "groupId");
                if(parentVersion == null)
                    parentVersion = getElementData(line, "version");
                continue;
            }
            if(group == null)
                group = getElementData(line, "groupId");
            if(artifactId == null)
                artifactId = getElementData(line, "artifactId");
            if(version == null)
                version = getElementData(line, "version");
            
            if(line.indexOf("<dependencies>") != -1)
                break;
            if(line.indexOf("<scm>") != -1)
                break;
            if(line.indexOf("<build>") != -1)
                break;
        }while(true);
        if(group == null)
            group = parentGroup;
        if(version == null)
            version = parentVersion;
        if(bfr != null)
            bfr.close();
        coor.group.add(group);
        coor.artifact.add(artifactId);
        coor.version.add(version);
    }

    private String getElementData(String line, String tag)
    {
        String value = null;
        String t = "<" + tag + ">";
        int index = line.indexOf(t);
        if(index != -1)
        {
            index += t.length();
            t = "</" + tag + ">";
            int end = line.indexOf(t, index);
            value = line.substring(index, end);
        }
        return value;
    }
    
    private void createShadowBuild() throws Exception
    {
        for(int i=0; i < trees.length; i++)
        {
            TreeInfo tinfo = trees[i];
            for(int j=0; j < tinfo.nodes.size(); j++)
            {
                @SuppressWarnings("unused")
                Node node = tinfo.nodes.get(j);
            }
        }
//        private static final String personalBuildRoot = "C:/wsdsp/personalBuild/";
        
        for (int i = 0; i < trees.length; i++)
        {
            TreeInfo tinfo = trees[i];
            for (int j = 0; j < tinfo.repositoryTrunks.length; j+=2)
            {
                File file = new File(tinfo.getShadowBuildAbsolutePath(j));
                if (file.isFile())
                    throw new Exception("expected a directory found a file: " + file.getAbsolutePath());
                if (!file.exists())
                {
                    if(JOptionPane.showConfirmDialog(null, file.getAbsolutePath() + " does not exist, create?") == JOptionPane.YES_OPTION)
                    {
                        if (!file.mkdirs())
                            throw new Exception("mkdirs failed: " + file.getAbsolutePath());
                        log.debug("created directories to: " + file.getAbsolutePath());
                        File pom = new File(file.getAbsoluteFile() + "/pom.xml");
                        copyLeafPom(pom);
                    }
                } else
                    log.debug("directories already existed to: " + file.getAbsolutePath());
            }
        }
    }
    
    private void prune()
    {
        if (!pruneBuild)
            return;
        
        for (int i = 0; i < trees.length; i++)
        {
            TreeInfo tinfo = trees[i];
            for (int j = 0; j < tinfo.repositoryTrunks.length; j+=2)
            {
                prune(new File(tinfo.getSvnAbsolutePath(j)), 0);
            }
        }
    }

    private void checkout() throws Exception
    {
        if (!checkout)
            return;
        for (int i = 0; i < trees.length; i++)
        {
            TreeInfo tinfo = trees[i];
            for (int j = 0; j < tinfo.repositoryTrunks.length; )
            {
                String url = tinfo.getUrl(j);
                File file = new File(tinfo.getSvnAbsolutePath(j++));
                String revision = tinfo.repositoryTrunks[j++];
                String cmd = "cmd /C svn co --depth infinity " + url + " " + file.getAbsolutePath();
                if(!revision.equals("0"))
                    cmd = "cmd /C svn -r " + revision + " co --depth infinity " + url + " " + file.getAbsolutePath();
                log.debug("spawn: " + cmd);
                Spawn spawn = new Spawn(cmd);
                spawn.run();
                int ccode = spawn.getCompletionCode();
                if (ccode != 0)
                    throw new Exception("failed: " + ccode);
                log.debug("ok, svn co " + url);
            }
        }
    }

    private void buildCheckoutTree() throws Exception
    {
        for (int i = 0; i < trees.length; i++)
        {
            TreeInfo tinfo = trees[i];
            for (int j = 0; j < tinfo.repositoryTrunks.length; j+=2)
            {
                File file = new File(tinfo.getSvnAbsolutePath(j));
                if (file.isFile())
                    throw new Exception("expected a directory found a file: " + file.getAbsolutePath());
                if (!file.exists())
                {
                    if (!file.mkdirs())
                        throw new Exception("mkdirs failed: " + file.getAbsolutePath());
                    log.debug("created directories to: " + file.getAbsolutePath());

                } else
                    log.debug("directories already existed to: " + file.getAbsolutePath());
            }
        }
    }

    public static void main(String[] args)
    {
        new MvnTree().run();
    }

    private void prune(File file, int level)
    {
        if(level != 0)
        {
            String abs = file.getAbsolutePath();
            abs = abs.replace('\\', '/');
            int index = abs.lastIndexOf('/');
            String nameAndExtension = abs;
            if (index != -1)
                nameAndExtension = abs.substring(++index);
            boolean deleteIt = false;
            if (pruneBuild)
            {
                if (nameAndExtension.contains(".mf.txt"))
                    deleteIt = true;
                else if (file.isDirectory() && file.getName().equals("build"))
                    deleteIt = true;
                if (!deleteIt)
                {
                    for (int i = 0; i < buildFiles.length; i++)
                    {
                        if (nameAndExtension.equals(buildFiles[i]))
                        {
                            deleteIt = true;
                            break;
                        }
                    }
                }
            }
            if (deleteIt)
            {
                recursiveDelete(file);
                return;
            }
        }
        File[] children = file.listFiles();
        if (children == null)
            return;
        for (int i = 0; i < children.length; i++)
            prune(children[i], ++level);
    }

    private boolean recursiveDelete(File file)
    {
        if (file.isFile())
        {
            if (!file.delete())
            {
                log.warn("unable to delete file: " + file.getAbsolutePath());
                return false;
            }
            return true;
        }
        File[] children = file.listFiles();
        if(children == null)
            return true;
        for (int i = 0; i < children.length; i++)
        {
            if (!recursiveDelete(children[i]))
                return false;
        }
        if (!file.delete())
        {
            log.warn("unable to delete folder: " + file.getAbsolutePath());
            return false;
        }
        return true;
    }

    String convertAbsolutePathToRepoUrl(String repoUrl, String repoBuildFolder, String absolutePath)
    {
        // C:/ws4/mainbuild/ 19
        // C:\ws4\mainbuild\ap\trunk\qa\src\qa-eng-platform-energy\qa-platform-energy\src\main\java
        int size = svnWorkingRoot.length();
        size += repoBuildFolder.length();
        ++size; // step past last '/' in host specific path
        absolutePath = absolutePath.substring(size);
        absolutePath = absolutePath.replace('\\', '/');
        repoUrl += absolutePath;
        return repoUrl;
    }

    public static String convertRepoUrlToAbsolutePath(String url) throws Exception
    {
        for (int i = 0; i < srcRepositoryUrls.length; i++)
        {
            if (url.contains(srcRepositoryUrls[i]))
            {
                int length = srcRepositoryUrls[i].length();
                return svnWorkingRoot + srcRepoBuildFolders[i] + url.substring(--length);
            }
        }
        throw new Exception("url does not contain a known repository: " + url);
    }
    
    public static void copyLeafPom(File destination) throws IOException
    {
        File template = new File(artifactPomTemplate);
        long size = template.length();
        FileInputStream fis = new FileInputStream(template);
        byte[] rawLeaf = new byte[(int)size];
        fis.read(rawLeaf);
        fis.close();
        FileOutputStream fos = new FileOutputStream(destination);
        fos.write(rawLeaf);
        fos.close();
    }
    
    private class Coordinates
    {
        private final List<String>name;
        private final List<String>group;
        private final List<String>artifact;
        private final List<String>version;
        
        public Coordinates()
        {
            name = new ArrayList<String>();
            group = new ArrayList<String>();
            artifact = new ArrayList<String>();
            version = new ArrayList<String>();
       }
        

        private int longest(List<String> list)
        {
            int max = 0;
            for (String value : list)
            {
                if(value.length() > max)
                    max = value.length();
            }
            return max;
        }
        
        private String pad(String value, int length)
        {
            StringBuilder sb = new StringBuilder(value);
            int delta = length - value.length() + 1;
            for(int i=0; i < delta; i++)
                sb.append(" ");
            return sb.toString();
        }
        
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder("\n");
            int glength = longest(group);
            int alength = longest(artifact);
            int vlength = longest(version);
            for(int i=0; i < group.size(); i++)
            {
                sb.append(pad(group.get(i),glength));
                sb.append(pad(artifact.get(i),alength));
                sb.append(pad(version.get(i),vlength));
                sb.append(name.get(i) + "\n");
            }
            return sb.toString();
        }
    }
}
