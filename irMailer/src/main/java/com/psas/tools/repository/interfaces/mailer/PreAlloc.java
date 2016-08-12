package com.psas.tools.repository.interfaces.mailer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendof.core.oal.DOFInterfaceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNLogEntry;

@SuppressWarnings("javadoc")
public class PreAlloc extends Thread
{
    private final static String AllocateFile = "/share/interfaceRepository/scripts/preAllocatList.txt";
    
    private final HashMap<String, List<SVNLogEntry>> pathMap;
    private final HashMap<String, List<SVNLogEntry>> submitterMap;
    private final HashMap<String, List<String>> ownerToPathMap;
    private final HashMap<String, SVNDirEntry> tipDirMap; // path is ksy
    private final Logger log;
    
    //@formatter:off
    public PreAlloc(
            HashMap<String, List<SVNLogEntry>> pathMap,
            HashMap<String, List<SVNLogEntry>> submitterMap,
            HashMap<String, List<String>> ownerToPathMap,
            HashMap<String, SVNDirEntry> tipDirMap)
    //@formatter:on

    {
        this.pathMap = pathMap;
        this.submitterMap = submitterMap;
        this.ownerToPathMap = ownerToPathMap;
        this.tipDirMap = tipDirMap;
        log = LoggerFactory.getLogger(getClass());
    }

    public void createPreAllocList() throws Exception
    {
        
        OutputStream os = new FileOutputStream(AllocateFile, false);
        for (Entry<String, SVNDirEntry> entry : tipDirMap.entrySet())
        {
            String path = entry.getKey();
            String svn = IrMailer.SvnDir + path;
            File dotEmit = new File(path);
            File parent = dotEmit.getParentFile();
            String siid = IrMailer.getIidFromDotEmit(new File(svn));
            DOFInterfaceID iid = DOFInterfaceID.create(siid);
            int registry = iid.getRegistry();
            if(registry != 1)
                continue;
            if(alreadyAllocated(siid))
                continue;
            String owner = getOwnerFromPath(path);
            siid += " " + owner + " " + emailToNameMap.get(owner) + "\n";
            os.write(siid.getBytes());
        }
        os.close();
    }
    
    public String getOwnerFromPath(String path) throws Exception
    {
        for(Entry<String, List<String>> entry : ownerToPathMap.entrySet())
        {
            String owner = entry.getKey();
            List<String> paths = entry.getValue();
            for(String p : paths)
            {
                if(path.equals(p))
                    return owner;
            }
        }
        throw new Exception("Owner for path: " + path + " not found");
    }
    
    public static boolean alreadyAllocated(String iid)
    {
        boolean found = false;
        for(int i=0; i < allocatedList.length; i++)
        {
            if(allocatedList[i].equals(iid))
            {
                found = true;
                break;
            }
        }
        return found;
    }
    
    private static final String[] allocatedList = new String[]
    {
        "[1:{01000000}]",   
        "[1:{01000001}]",   
        "[1:{01000002}]",   
        "[1:{0100000A}]", 
        "[1:{01000022}]", 
        "[1:{01000023}]", 
        "[1:{01000024}]", 
        "[1:{01000025}]", 
        "[1:{01000026}]", 
        "[1:{01000027}]", 
        "[1:{01000028}]", 
        "[1:{0100002E}]", 
        "[1:{01000048}]", 
        "[1:{01000049}]", 
        "[1:{0100004A}]", 
        "[1:{0100004B}]", 
        "[1:{0100004C}]", 
        "[1:{0100004D}]", 
        "[1:{01000052}]", 
        "[1:{01000053}]", 
        "[1:{01000054}]", 
        "[1:{01000055}]", 
        "[1:{01000056}]", 
        "[1:{01000057}]", 
        "[1:{01000058}]", 
        "[1:{01}]", 
        "[1:{0211}]", 
        "[1:{0212}]", 
        "[1:{021C}]", 
        "[1:{021E}]", 
        "[1:{0221}]", 
        "[1:{0225}]", 
        "[1:{0226}]", 
        "[1:{0227}]", 
        "[1:{0228}]", 
        "[1:{0239}]", 
        "[1:{023A}]", 
        "[1:{023B}]", 
        "[1:{08}]", 
        "[1:{09}]", 
        "[1:{0A}]", 
        "[1:{0B}]", 
    };
    private static final Map<String, String> emailToNameMap;

    static
    {
        emailToNameMap = new HashMap<>();
        emailToNameMap.put("joseph.clark@us.panasonic.com", "Joseph Clark");
        emailToNameMap.put("murakami.shuji@jp.panasonic.com", "Murakami Shuji");
    }
}
