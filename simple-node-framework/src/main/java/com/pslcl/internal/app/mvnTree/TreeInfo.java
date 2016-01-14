package com.pslcl.internal.app.mvnTree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("javadoc")
public class TreeInfo
{
    final String[] repositoryTrunks;
    final boolean hasSuperGroups;
    final String repoRootUrl;
    final String repoFolder;
    final List<Node> nodes;

    public TreeInfo(String repoRootUrl, String repoFolder, String[] repositoryTrunks, boolean hasSuperGroups)
    {
        this.hasSuperGroups = hasSuperGroups;
        this.repoRootUrl = repoRootUrl;
        this.repositoryTrunks = repositoryTrunks;
        nodes = new ArrayList<Node>();
        this.repoFolder = repoFolder;
        // for(int i=0; i < repositoryTrunks.length; i++)
        // nodes.add(new Node(repositoryTrunks[i]));
    }

    public String getSvnAbsolutePath(int trunkIndex)
    {
        return MvnTree.svnWorkingRoot + repoFolder + "/" + repositoryTrunks[trunkIndex];
    }

    public String getShadowBuildAbsolutePath(int trunkIndex)
    {
        return MvnTree.personalBuildRoot + repoFolder + "/" + repositoryTrunks[trunkIndex];
    }

    public String getUrl(int trunkIndex)
    {
        return new String(repoRootUrl + repositoryTrunks[trunkIndex]);
    }

    void addProjects(String[] projects) throws Exception
    {
        // "", "0","eng-ancpwr",
        // "common", "eng-app-common-api", "1", "enc-app-common-api",
        // "common", "", "0", "eng-data-common",\
        for (int i = 0; i < projects.length;)
        {
            boolean itsAGroup = true;
            String superGroup = null;
            if (hasSuperGroups)
                superGroup = projects[i++];
            String group = projects[i++];
            if (group.length() == 0)
                itsAGroup = false;
            int jarCount = Integer.parseInt(projects[i++]);
            boolean createPom = Boolean.parseBoolean(projects[i++]);
            if (!itsAGroup && jarCount == 0)
                jarCount = 1;
            String[] jars = new String[jarCount];
            for (int j = 0; j < jars.length; j++)
                jars[j] = projects[i++];
            File found = null;
            for (int j = 0; j < repositoryTrunks.length; j+=2)
            {
                String path = getSvnAbsolutePath(j);
                String atomic = path.substring(path.lastIndexOf('/')+1);
                path += "/";
                if (!itsAGroup)
                    path += jars[0];
                else
                {
                    if(!atomic.equals(group))
                        path += group;
                }
                File file = new File(path);
                if (file.exists())
                {
                    found = file;
                    break;
                }
            }
            if (found == null)
                throw new Exception("error in projects lists, directory not found superGroup: " + superGroup + " group: " + group);
            String abspath = found.getAbsolutePath();
            nodes.add(new Node(abspath, itsAGroup, createPom));
            if (itsAGroup)
            {
                for (int j = 0; j < jars.length; j++)
                {
                    File file = new File(abspath);
                    if (!file.exists())
                        throw new Exception("expected artifact folder to exist: " + abspath);
                    nodes.add(new Node(abspath, false, createPom));
                }
            }
        }
    }
}
