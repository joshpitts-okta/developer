package com.pslcl.internal.app.mvnp.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import com.pslcl.chad.app.StrH;

@SuppressWarnings("javadoc")
public class Projects
{
    private final List<Project> projects;
    private int lastSelected;
    
    public Projects()
    {
        projects = new ArrayList<Project>();
    }
    
    public synchronized DefaultListModel<Project> getProjectListModel()
    {
        DefaultListModel<Project> listModel = new DefaultListModel<Project>();
        for(Project project : projects)
            listModel.addElement(project);
        return listModel;
    }
    
    public synchronized void addProject(Project project)
    {
        projects.add(project);
        if(lastSelected == 0)
            lastSelected = project.hashCode();
    }
    
    public synchronized void deleteProject(Project project)
    {
        projects.remove(project);
    }

    public synchronized void setLastSelectedProject(Project project)
    {
        if(project == null)
            lastSelected = 0;
        else
            lastSelected = project.hashCode();
    }
    
    public synchronized Project getLastSelectedProject()
    {
        for(Project project : projects)
        {
            if(project.hashCode() == lastSelected)
                return project;
        }
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        return toString(sb, 0, true).toString();
    }
    
    public StringBuilder toString(StringBuilder sb, int tabLevel, boolean header)
    {
        if(header)
        {
            StrH.ttl(sb, tabLevel, "Projects:");
            ++tabLevel;
        }
        StrH.ttl(sb, tabLevel, "lastSelected: " , ""+lastSelected);
        for(int i=0; i < projects.size(); i++)
            projects.get(i).toString(sb, tabLevel, false);
        return sb;
    }
}