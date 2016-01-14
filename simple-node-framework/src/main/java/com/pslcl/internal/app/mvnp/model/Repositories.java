package com.pslcl.internal.app.mvnp.model;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;

import com.pslcl.chad.app.EqHelper;
import com.pslcl.chad.app.StrH;

@SuppressWarnings("javadoc")
public class Repositories
{
    private final ArrayList<Repository> repositories;
    private String lastSelected;
    
    public Repositories(boolean doit)
    {
        repositories = new ArrayList<Repository>();
        Repository rep = new Repository("https://source.ancillary.pewla.com/ap/");
        repositories.add(rep);
        repositories.add(new Repository("https://source.emit-networking.org/emit/"));
        repositories.add(new Repository("https://source.emit-networking.org/emit_app/"));
        repositories.add(new Repository("https://source.emit-networking.org/emit_mw/"));
        setLastSelectedRepository(rep);
    }
    
    public DefaultComboBoxModel<Repository> getRepositoryComboBoxModel()
    {
        DefaultComboBoxModel<Repository> listModel = new DefaultComboBoxModel<Repository>();
        for (int i = 0; i < repositories.size(); i++)
            listModel.addElement(repositories.get(i));
        return listModel;
    }
    
    
    public synchronized void addRepository(Repository repository)
    {
        repositories.add(repository);
        if(lastSelected == null)
            lastSelected = repository.name;
    }
    
    public synchronized void deleteRepository(Repository repository)
    {
        repositories.remove(repository);
    }

    public synchronized void setLastSelectedRepository(Repository repository)
    {
        if(repository == null)
            lastSelected = null;
        else
            lastSelected = repository.name;
    }
    
    public synchronized Repository getLastSelectedRepository()
    {
        for(Repository repository : repositories)
        {
            if(EqHelper.checkEquals(repository.name, lastSelected))
                return repository;
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
            StrH.ttl(sb, tabLevel, "Repositories:");
            ++tabLevel;
        }
        StrH.ttl(sb, tabLevel, "lastSelected: " , ""+lastSelected);
        for(int i=0; i < repositories.size(); i++)
            repositories.get(i).toString(sb, tabLevel, false);
        return sb;
    }
}