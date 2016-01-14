package com.pslcl.internal.app.mvnp.model;

import com.pslcl.chad.app.EqHelper;
import com.pslcl.chad.app.StrH;


@SuppressWarnings("javadoc")
public class Project
{
    public final Repository repository;
    public final String repositoryPath;
    public final String group;
    public final String artifact;
    public final String svnRevision;
    public final String buildNumber;
    public final String qbId;
    public final String releaseDate;
    public final String releaseVersion;
    
    //@formatter:off
    public Project(
               Repository repository,
               String repositoryPath,
               String group,
               String artifact,
               String svnRevision,
               String qbNumber,
               String qbId,
               String releaseDate,
               String releaseVersion)
    //@formatter:on
    {
        this.repository = repository;
        this.repositoryPath = repositoryPath;
        this.group = group;
        this.artifact = artifact;
        this.svnRevision = svnRevision;
        this.buildNumber = qbNumber;
        this.qbId = qbId;
        this.releaseDate = releaseDate;
        this.releaseVersion = releaseVersion;
    }
    
    @Override
    public String toString()
    {
        return
            repository.name +
            repositoryPath + "/" + 
            group + "/" +
            artifact + ":" +
            svnRevision;
    }
    
    public StringBuilder toString(StringBuilder sb, int tabLevel, boolean header)
    {
        if(header)
        {
            StrH.ttl(sb, tabLevel, "Project:");
            ++tabLevel;
        }
        StrH.ttl(sb, tabLevel, "repository:     ", repository.name);
        StrH.ttl(sb, tabLevel, "repositoryPath:        ", repositoryPath);
        StrH.ttl(sb, tabLevel, "group:       ", group);
        StrH.ttl(sb, tabLevel, "artifact:    ", artifact);
        StrH.ttl(sb, tabLevel, "svnRevision:    ", svnRevision);
        StrH.ttl(sb, tabLevel, "qbNumber:           ", buildNumber);
        StrH.ttl(sb, tabLevel, "qbId:           ", qbId);
        StrH.ttl(sb, tabLevel, "releaseDate:    ", releaseDate);
        StrH.ttl(sb, tabLevel, "releaseVersion: ", releaseVersion);
        return sb;
    }
    
@Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repository == null) ? 0 : repository.hashCode());
        result = prime * result + ((repositoryPath == null) ? 0 : repositoryPath.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
        result = prime * result + ((svnRevision == null) ? 0 : svnRevision.hashCode());
        result = prime * result + ((buildNumber == null) ? 0 : buildNumber.hashCode());
        result = prime * result + ((qbId == null) ? 0 : qbId.hashCode());
        result = prime * result + ((releaseDate == null) ? 0 : releaseDate.hashCode());
        result = prime * result + ((releaseVersion == null) ? 0 : releaseVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Project))
            return false;
        Project other = (Project) obj;
        if(!EqHelper.checkEquals(repository, other.repository))
            return false;
        if(!EqHelper.checkEquals(repositoryPath, other.repositoryPath))
            return false;
        if(!EqHelper.checkEquals(group, other.group))
            return false;
        if(!EqHelper.checkEquals(artifact, other.artifact))
            return false;
        if(!EqHelper.checkEquals(svnRevision, other.svnRevision))
            return false;
        if(!EqHelper.checkEquals(buildNumber, other.buildNumber))
            return false;
        if(!EqHelper.checkEquals(qbId, other.qbId))
            return false;
        if(!EqHelper.checkEquals(releaseDate, other.releaseDate))
            return false;
        return EqHelper.checkEquals(releaseVersion, other.releaseVersion);
    }
}
