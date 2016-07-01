/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.provide;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;

import com.pslcl.service.util.provide.ProvideFactory;

public class FactoryWrapper
{
    public final ProvideFactory factory;
    public final Set<DOFInterfaceID> iidFilters;
    public final HashMap<Integer, List<DOFObjectID>> rdidBaseOidsMap;
    public final HashMap<DOFObjectID, List<DOFObjectID>> baseOidOidsMap;
    public final HashMap<DOFObjectID, List<ProvideInfoEntry>> oidProvidersMap;
    
    public FactoryWrapper(ProvideFactory factory, Set<DOFInterfaceID> iidFilters)
    {
        this.factory = factory;
        this.iidFilters = iidFilters;
        rdidBaseOidsMap = new HashMap<Integer, List<DOFObjectID>>();
        baseOidOidsMap = new HashMap<DOFObjectID, List<DOFObjectID>>();
        oidProvidersMap = new HashMap<DOFObjectID, List<ProvideInfoEntry>>();
    }
    
    public void clear()
    {
        oidProvidersMap.clear();
        baseOidOidsMap.clear();
        rdidBaseOidsMap.clear();
        iidFilters.clear();
    }
    
    @Override
    public String toString()
    {
        return "factory: " + factory.toString() + " iidFilters: " + iidFilters.toString() + " baseOids: " + rdidBaseOidsMap.toString();
    }
}
