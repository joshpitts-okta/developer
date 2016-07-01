package com.pslcl.internal.app.mvnp.model;

@SuppressWarnings("javadoc")
public class MvnP
{
    private String svnCheckoutBasePath;
    private String shadowPomBasePath;
    private Repositories repositories;
    private Projects projects;
    
    public MvnP()
    {
    }
    
    public MvnP(boolean doit)
    {
        repositories = new Repositories(doit);
        projects = new Projects();
        svnCheckoutBasePath = "C:/wswip/svn-tree/";
        shadowPomBasePath = "C:/wswip/personalBuild/";
    }
    
    public String getSvnCheckoutBasePath()
    {
        return svnCheckoutBasePath;
    }
    
    public void setSvnCheckoutBasePath(String basePath)
    {
        svnCheckoutBasePath = basePath;
    }
    
    public String getShadowPomBasePath()
    {
        return shadowPomBasePath;
    }
    
    public void setShadowPomBasePath(String basePath)
    {
        shadowPomBasePath = basePath;
    }
    
    
    public Repositories getRepositories()
    {
        return repositories;
    }
    
    public Projects getProjects()
    {
        return projects;
    }
    
    
}