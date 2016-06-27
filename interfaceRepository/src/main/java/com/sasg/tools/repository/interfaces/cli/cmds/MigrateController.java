package com.sasg.tools.repository.interfaces.cli.cmds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.tools.repository.interfaces.cli.ManageController;
import org.opendof.tools.repository.interfaces.core.CoreController;
import org.opendof.tools.repository.interfaces.da.DataAccessor;
import org.opendof.tools.repository.interfaces.da.InterfaceData;
import org.opendof.tools.repository.interfaces.da.SubRepositoryNode.TabToLevel;
import org.slf4j.LoggerFactory;

import com.sasg.tools.repository.interfaces.cli.cmds.migrate.AllocMigrateCommand;
import com.sasg.tools.repository.interfaces.cli.cmds.migrate.ScriptMigrateCommand;


@SuppressWarnings("javadoc")
public class MigrateController extends ManageController
{
    public MigrateController(CoreController controller)
    {
        super(controller);
    }
    
    public void migrateAlloc(AllocMigrateCommand.AllocCmdData data) throws Exception
    {
        BufferedReader bufferedReader = null;
        try
        {
            InputStream istream = new FileInputStream(data.iidFile);
            Reader reader = new InputStreamReader(istream, "UTF-8");
            bufferedReader = new BufferedReader(reader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null)
            {
                DOFInterfaceID iid = DOFInterfaceID.create(line);
                InterfaceData iface = new InterfaceData(null, iid.toStandardString(), null, "1", DataAccessor.OpendofAdmin, DataAccessor.AnonymousGroup, CoreController.OpenDofRepo, null, null, false);
                long id = iid.getIdentifier();
                int byteSize = 4;
                if(id < 256)
                    byteSize = 1;
                else if (id < 65536)
                    byteSize = 2;
                String rdn = iid.getRegistry() + "/" + byteSize;
                iface = controller.addInterface(CoreController.CliUser, iface.getRepoType(), iface, rdn);
            }
        }
        finally
        {
            if(bufferedReader != null)
                try{bufferedReader.close();} catch (IOException e)
                {
                    LoggerFactory.getLogger(ManageController.class).warn("failed to close bufferedReader cleanly", e);
                }
        }
    }

    public void migrateScript(ScriptMigrateCommand.ScriptCmdData data) throws Exception
    {
        List<InterfaceData> list = controller.getAllInterfaces(CoreController.CliUser, null, null, null, null);
        HashMap<DOFInterfaceID, InIrEntry> inIrMap = new HashMap<DOFInterfaceID, InIrEntry>();
        HashMap<DOFInterfaceID, InIrEntry> inIrWorkingMap = new HashMap<DOFInterfaceID, InIrEntry>();
        HashMap<DOFInterfaceID, InIrEntry> inIrPublishedMap = new HashMap<DOFInterfaceID, InIrEntry>();
        TabToLevel format = new TabToLevel();
        format.ttl("\nMigrateScript stats:");
        format.level.incrementAndGet();
        format.ttl("In IR: ");
        format.level.incrementAndGet();
        for(InterfaceData iface : list)
        {
            DOFInterfaceID iid = DOFInterfaceID.create(iface.iid);
            if(inIrWorkingMap.get(iid) != null)
                throw new Exception("duplicate interface ID found: " + iid);
            inIrMap.put(iid, new InIrEntry(iid, iface.getName()));
            String msg = iid.toStandardString() + " " + iface.getName();
            if(iface.publish)
            {
                msg += " published";
                inIrPublishedMap.put(iid, new InIrEntry(iid, iface.getName()));
            }
            else
            {
                msg += " working";
                inIrWorkingMap.put(iid, new InIrEntry(iid, iface.getName()));
            }
            format.ttl(msg);
        }
        format.ttl("existing working:       ", inIrWorkingMap.size());
        format.ttl("existing published:     ", inIrPublishedMap.size());
        format.level.decrementAndGet();
        
        HashMap<DOFInterfaceID, DOFInterfaceID> allocList = new HashMap<DOFInterfaceID, DOFInterfaceID>();
        File next = new File(data.svnPath+"/working");
        HashMap<DOFInterfaceID, DOFInterfaceID> inSvnMap = new HashMap<DOFInterfaceID, DOFInterfaceID>();
        
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        AtomicInteger count3 = new AtomicInteger(0);
        AtomicInteger count63 = new AtomicInteger(0);
        AtomicInteger dupcount = new AtomicInteger(0);
        
        format.ttl("\n\nworking svn export file structure traversal:");
        format.level.incrementAndGet();
        traverse(inIrMap, allocList, inSvnMap, count1, count2, count3, count63, dupcount, format, next);
        format.level.decrementAndGet();
        int needWorking = allocList.size();
        format.ttl("total working: ", inSvnMap.size());
        format.ttl("needs allocated: ", needWorking);
        format.ttl("registry 1: ", count1.get());
        format.ttl("registry 2: ", count2.get());
        format.ttl("registry 3: ", count3.get());
        format.ttl("registry 63: ", count63.get());
        format.ttl("duplicates: ", dupcount.get());
        format.ttl("in IR, not in svn:");
        format.level.incrementAndGet();
        boolean hit = false;
        for(Entry<DOFInterfaceID, InIrEntry> entry : inIrWorkingMap.entrySet())
        {
            if(inSvnMap.get(entry.getKey()) == null)
            {
                format.ttl(entry.getKey(), " ", entry.getValue().name);
                hit = true;
            }
        }
        if(!hit)
            format.ttl("none");
        format.level.decrementAndGet();
        next = new File(data.svnPath+"/publish");
        inSvnMap.clear();
        count1.set(0);
        count2.set(0);
        count3.set(0);
        count63.set(0);
        dupcount.set(0);
        format.ttl("\n\npublished svn export file structure traversal:");
        format.level.incrementAndGet();
        traverse(inIrMap, allocList, inSvnMap, count1, count2, count3, count63, dupcount, format, next);
        format.level.decrementAndGet();
        int needPublished = allocList.size() - needWorking;
        format.ttl("total published: ", inSvnMap.size());
        format.ttl("needs allocated ", needPublished);
        format.ttl("registry 1: ", count1.get());
        format.ttl("registry 2: ", count2.get());
        format.ttl("registry 3: ", count3.get());
        format.ttl("registry 63: ", count63.get());
        format.ttl("duplicates: ", dupcount.get());
        format.ttl("in IR, not in svn:");
        format.level.incrementAndGet();
        hit = false;
        for(Entry<DOFInterfaceID, InIrEntry> entry : inIrPublishedMap.entrySet())
        {
            if(inSvnMap.get(entry.getKey()) == null)
            {
                format.ttl(entry.getValue().iid, " ", entry.getValue().name);
                hit = true;
            }
        }
        if(!hit)
            format.ttl("none");
        format.level.decrementAndGet();
        log.info(format.toString());
        
        OutputStream os = new FileOutputStream(data.scriptPath, false);
        for(Entry<DOFInterfaceID, DOFInterfaceID> iid : allocList.entrySet())
        {
            String iidstr = iid.getKey().toStandardString();
            if(iid.getKey().getRegistry() != 1)
            {
                
                int idx = iidstr.indexOf('{');
                String str = "[1:";
                str += iidstr.substring(idx);
                iidstr = str;
            }
            iidstr += "\n";
            os.write(iidstr.getBytes());
        }
        os.close();
    }
    
    //@formatter:off
    private void traverse(
                    HashMap<DOFInterfaceID, InIrEntry> existsMap, 
                    HashMap<DOFInterfaceID, DOFInterfaceID> allocList,
                    HashMap<DOFInterfaceID, DOFInterfaceID> inSvnMap,
                    AtomicInteger count1, 
                    AtomicInteger count2, 
                    AtomicInteger count3, 
                    AtomicInteger count63, 
                    AtomicInteger dupcount,
                    TabToLevel format, 
                    File next) throws Exception
    //@formatter:on
    {
        File[] files = next.listFiles();
        for(File file : files)
        {
            if(file.isDirectory())
                traverse(existsMap, allocList, inSvnMap, count1, count2, count3, count63, dupcount, format, file);
            String name = file.getName();
            int idx = name.lastIndexOf('.');
            if(idx == -1)
                continue;
            String ext = name.substring(++idx);
            if(!ext.equals("emit"))
                continue;
            String xml = fileToString(file);
            String iidpattern = "iid=\"[";
            int sidx = xml.indexOf(iidpattern);
            if (sidx == -1)
                throw new Exception("Did not find " + iidpattern + " in file: " + file.getAbsolutePath());
            sidx += iidpattern.length() - 1;
            int eidx = xml.indexOf('\"', sidx);
            String iidStr = xml.substring(sidx, eidx);
            DOFInterfaceID iid = DOFInterfaceID.create(iidStr);
            inSvnMap.put(iid, iid);
            boolean skip = false;
            boolean is63 = false;
            switch(iid.getRegistry())
            {
                case 1:
                    count1.incrementAndGet();
                    break;
                case 2:
                    count2.incrementAndGet();
                    skip = true;
                    break;
                case 3:
                    count3.incrementAndGet();
                    break;
                case 63:
                    count63.incrementAndGet();
                    is63 = true;
                    break;
                default:
                    throw new Exception("registry type: " + iid.toStandardString());
            }
            if(skip)
                continue;
            String msg = iid.toStandardString();
            if(existsMap.get(iid) == null)
            {
                if(is63)
                {
                    // see if there is already an existing reg 1
                    for(Entry<DOFInterfaceID,InIrEntry> entry : existsMap.entrySet())
                    {
                        if(iid.getIdentifier() == entry.getKey().getIdentifier())
                        {
                            msg += " WARN: 63 where InIr has same identifier";
                            dupcount.incrementAndGet();
                            skip = true;
                            break;
                        }
                    }
                    if(!skip)
                    {
                        idx = iid.toStandardString().indexOf('{');
                        String str = "[1:";
                        str += iid.toStandardString().substring(idx);
                        iid = DOFInterfaceID.create(str);
                    }
                }
                msg += " needs allocation";
                if(allocList.get(iid) != null)
                {
                    msg += " WARN already seen";
                    dupcount.incrementAndGet();
                }else
                {
                    if(!skip)
                        allocList.put(iid, iid);
                }
            }
            msg += " " + file.getAbsolutePath();
            format.ttl(msg);
        }
    }

    private class InIrEntry
    {
        public final DOFInterfaceID iid;
        public final String name;
        private InIrEntry(DOFInterfaceID iid, String name)
        {
            this.iid = iid;
            this.name = name;
        }
    }
}

