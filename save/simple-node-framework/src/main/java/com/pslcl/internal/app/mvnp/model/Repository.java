package com.pslcl.internal.app.mvnp.model;

import com.pslcl.chad.app.EqHelper;
import com.pslcl.chad.app.StrH;

@SuppressWarnings("javadoc")
public class Repository
{
    public final String name;
    
    public Repository(String name)
    {
        this.name = StrH.stripTrailingSeparator(name);
    }
    
    public boolean isRepository(String repository)
    {
        return EqHelper.checkEquals(name, repository);
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public StringBuilder toString(StringBuilder sb, int tabLevel, boolean header)
    {
        StrH.ttl(sb, tabLevel, "Repository: ", name);
        return sb;
    }
    
@Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Repository))
            return false;
        Repository other = (Repository) obj;
        return EqHelper.checkEquals(name, other.name);
    }
}