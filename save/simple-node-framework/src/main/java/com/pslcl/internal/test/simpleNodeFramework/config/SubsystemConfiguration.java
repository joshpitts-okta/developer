/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.opendof.core.oal.DOF;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.Module;
import com.pslcl.internal.test.simpleNodeFramework.ModuleWrapper;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework;
import com.pslcl.internal.test.simpleNodeFramework.SimpleNodeFramework.ModuleStatus;
import com.pslcl.internal.test.simpleNodeFramework.StartedEvent;
import com.pslcl.internal.test.simpleNodeFramework.config.CredConnSysConfiguration.CredConnSysConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.DofConfiguration.DofConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.SystemBase;

@SuppressWarnings("javadoc")
public class SubsystemConfiguration
{
    public static final String DofKey = "emitdo.snf.subsystem.dof-config";
    public static final String NameKey = "emitdo.snf.subsystem.name";
    public static final String CredConnSysKey = "emitdo.snf.subsystem.ccs-config";
    public static final String ModuleBaseKey = "emitdo.snf.subsystem.module";
    public static final String ServiceBaseKey = "emitdo.snf.subsystem.service";
    public static final String SubsytemDofKey = "emitdo.snf.subsystem.subsytem-dof"; // if set get dof from named subsytem instead of DofKey

    public static SubsystemConfig propertiesToConfig(NodeConfig nodeConfig, int priority) throws Exception
    {
        String msg = "ok";
        try
        {
            String name = nodeConfig.properties.getProperty(NameKey);
            if (name == null)
                throw new Exception(NameKey + " name must be given");
            nodeConfig.toSb(NameKey, "=", name);

            DofConfig dofConfig = null;
            String subsystemDof = nodeConfig.properties.getProperty(SubsytemDofKey);
            nodeConfig.toSb(SubsytemDofKey, "=", subsystemDof);
            if (subsystemDof != null)
            {
                SubsystemConfig ssc = nodeConfig.getSubsystemConfig(subsystemDof);
                dofConfig = ssc.dofConfig;
            } else
            {
                msg = "invalid dof configuration file";
                String configFile = nodeConfig.properties.getProperty(DofKey);
                nodeConfig.toSb(DofKey, "=", configFile);
                NodeConfiguration.loadProperties(nodeConfig, configFile);
                nodeConfig.tabLevel.incrementAndGet();
                dofConfig = DofConfiguration.propertiesToConfig(nodeConfig);
                nodeConfig.tabLevel.decrementAndGet();
            }

            SubsystemConfig subsystemConfig = new SubsystemConfig(subsystemDof, nodeConfig, name, dofConfig, priority);

            msg = "invalid CredConnSys configuration file";
            List<Entry<String, String>> list = NodeConfiguration.getPropertiesForBaseKey(CredConnSysKey, nodeConfig.properties);
            int ccsPriority = 1;
            for (Entry<String, String> entry : list)
            {
                nodeConfig.toSb("******** CredConnSys's ********");
                nodeConfig.toSb(entry.getKey(), "=", entry.getValue());
                NodeConfiguration.loadProperties(nodeConfig, entry.getValue());
                nodeConfig.tabLevel.incrementAndGet();
                CredConnSysConfig ccsConfig = CredConnSysConfiguration.propertiesToConfig(nodeConfig, ccsPriority);
                nodeConfig.tabLevel.decrementAndGet();
                ccsConfig.setSubsystem(subsystemConfig);
                subsystemConfig.ccsConfigs.add(ccsConfig);
                ++ccsPriority;
            }

            // Initialize Node services and modules.
            List<Entry<String, String>> moduleList = NodeConfiguration.getPropertiesForBaseKey(ModuleBaseKey, nodeConfig.properties);
            nodeConfig.toSb("******** modules ********");
            nodeConfig.toSb("configured modules:");
            nodeConfig.tabLevel.incrementAndGet();
            instantiateApplications(nodeConfig, moduleList, subsystemConfig, null);
            nodeConfig.tabLevel.decrementAndGet();
            
            List<Entry<String, String>> serviceList = NodeConfiguration.getPropertiesForBaseKey(ServiceBaseKey, nodeConfig.properties);
            nodeConfig.toSb("******** services ********");
            nodeConfig.toSb("configured services:");
            nodeConfig.tabLevel.incrementAndGet();
            instantiateApplications(nodeConfig, serviceList, subsystemConfig, dofConfig.getDof());
            nodeConfig.tabLevel.decrementAndGet();

            nodeConfig.properties.remove(NameKey);
            nodeConfig.properties.remove(DofKey);
            nodeConfig.properties.remove(CredConnSysKey);
            nodeConfig.properties.remove(SubsytemDofKey);
            NodeConfiguration.removePropertiesForBaseKey(CredConnSysKey, nodeConfig.properties);
            NodeConfiguration.removePropertiesForBaseKey(ModuleBaseKey, nodeConfig.properties);
            NodeConfiguration.removePropertiesForBaseKey(ServiceBaseKey, nodeConfig.properties);

            return subsystemConfig;
        } catch (Exception e)
        {
            nodeConfig.toSb(msg);
            //            nodeConfig.log.error(nodeConfig.sb.toString(), e);
            throw e;
        }
    }

    
    private static void instantiateApplications(NodeConfig nodeConfig, List<Entry<String, String>> appList, SubsystemConfig subsystemConfig, DOF dof) throws Exception
    {
        boolean modules = dof == null;
        int count = 0;
        nodeConfig.tabLevel.incrementAndGet();
        for (Entry<String, String> entry : appList)
        {
            ++count;
            String[] values = entry.getValue().split(" ");
//            String msg = "invalid boolean value for start flag";
            boolean start = true;
            if (values.length > 1)
                start = Boolean.parseBoolean(values[1]);
            String moduleName = StrH.getAtomicName(values[0], '.');
            if (values.length > 2)
                moduleName = values[2];

            nodeConfig.toSb(entry.getValue());
//            msg = values[0] + " could not be instantiated";

            char[] chars = values[0].toCharArray();
            int digitIndex = -1;
            if (Character.isDigit(chars[chars.length - 1]))
            {
                for (int i = chars.length - 1; i >= 0; i--)
                {
                    if (!Character.isDigit(chars[i]))
                    {
                        digitIndex = i + 1;
                        break;
                    }
                }
            }
            SnfModule snfModule = null;
            if (digitIndex != -1)
            {
                int moduleIndex = Integer.parseInt(values[0].substring(digitIndex));
                values[0] = values[0].substring(0, digitIndex);
                //                    if(values[0].startsWith(ModuleWrapper.class.getName()))
                {
                    Class<?> clazz = Class.forName(values[0]);
                    Constructor<?> constructor = clazz.getConstructor(String.class, int.class);
                    if(modules)
                        snfModule = new SnfModule((Module) constructor.newInstance(moduleName, moduleIndex), moduleName);
                    else
                    {
                        snfModule = new SnfModule((SystemBase) constructor.newInstance(moduleName, moduleIndex), moduleName, values[3], dof);
                    }
                }
            }
            if (snfModule == null && modules)
            {
                Module module = (Module) Class.forName(values[0]).newInstance();
                snfModule = new SnfModule(module, moduleName);
            }else
            if (snfModule == null && !modules)
            {
                SystemBase systemBase = (SystemBase) Class.forName(values[0]).newInstance();
                snfModule = new SnfModule(systemBase, moduleName, values[3], dof);
            }
            if(snfModule == null)
                throw new Exception("undetermined Module or SystemBase");
            
            snfModule.setStart(start);
            SimpleNodeFramework.getSnf().addSnfModule(snfModule);
            if(modules)
            {
                synchronized (subsystemConfig.modules)
                {
                    subsystemConfig.modules.add(snfModule);
                }
            }else
            {
                synchronized (subsystemConfig.services)
                {
                    subsystemConfig.services.add(snfModule);
                }
            }
                
//            msg = values[0] + " init failed";
            if(modules)
                snfModule.module.init(nodeConfig);
            else
                snfModule.systemBase.init(nodeConfig);
        }
        if (count == 0)
            nodeConfig.toSb("none found");
        nodeConfig.tabLevel.decrementAndGet();
    }
    
    
    
    public static class SubsystemConfig
    {
        public final String subsystemDof;
        public final String name;
        public final List<SnfModule> modules;
        public final List<SnfModule> services;
        public final DofConfig dofConfig;
        public final List<CredConnSysConfig> ccsConfigs;
        public final NodeConfig nodeConfig;
        private final int priority;

        public SubsystemConfig(String subsystemDof, NodeConfig nodeConfig, String name, DofConfig dofConfig, int priority)
        {
            this.name = name;
            this.subsystemDof = subsystemDof;
            this.nodeConfig = nodeConfig;
            this.dofConfig = dofConfig;
            this.ccsConfigs = new ArrayList<CredConnSysConfig>();
            this.priority = priority;
            modules = new ArrayList<SnfModule>();
            services = new ArrayList<SnfModule>();
        }
        
        public void waitForCommsUp() throws Exception
        {
            for (CredConnSysConfig ccsc : ccsConfigs)
            {
                nodeConfig.toSb("waiting for ccsc ", ccsc.name);
                nodeConfig.tabLevel.incrementAndGet();
                ccsc.waitForCommsUp();
                nodeConfig.tabLevel.decrementAndGet();
            }
        }
        
        public DOF getDof()
        {
            return dofConfig.getDof();
        }

        public CredConnSysConfig getCredConnSysConfig(String name)
        {
            for (CredConnSysConfig ccsc : ccsConfigs)
            {
                if (ccsc.name.toLowerCase().equals(name.toLowerCase()))
                    return ccsc;
            }
            return null;
        }

        public void startModule(String moduleName, boolean service) throws Exception
        {
            SnfModule snfModule = findSnfModule(moduleName, service);
            if(service)
                snfModule.systemBase.start(SimpleNodeFramework.getSnf());
            else
                snfModule.module.start(SimpleNodeFramework.getSnf());
        }

        public void stopModule(String moduleName, boolean service) throws Exception
        {
            findSnfModule(moduleName, service).module.stop(SimpleNodeFramework.getSnf());
        }

        public Module getModule(String moduleName) throws Exception
        {
            return findSnfModule(moduleName, false).module;
        }
        
        public SystemBase getSystemBase(String moduleName) throws Exception
        {
            return findSnfModule(moduleName, true).systemBase;
        }
        
        public SnfModule findSnfModule(String moduleName, boolean service) throws Exception
        {
            if(service)
            {
                synchronized (services)
                {
                    for (SnfModule module : services)
                    {
                        if (moduleName.equals(module.moduleName))
                            return module;
                    }
                }
                throw new Exception("moduleName: " + moduleName + " not found in subsystem: " + name);
            }
            
            synchronized (modules)
            {
                for (SnfModule module : modules)
                {
                    if (moduleName.equals(module.moduleName))
                        return module;
                }
            }
            throw new Exception("moduleName: " + moduleName + " not found in subsystem: " + name);
        }

        public boolean setModuleStatus(Module module, ModuleStatus status)
        {
            if (status == ModuleStatus.Failed)
                ((SimpleNodeFramework) nodeConfig.node).failed();
            synchronized (modules)
            {
                for (SnfModule mod : modules)
                {
                    if (mod.module == module)
                    {
                        mod.status = status;
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean waitForStatus(Module module, ModuleStatus status, int timeout) throws Exception
        {
            SnfModule snfModule = null;
            synchronized (modules)
            {
                for (SnfModule mod : modules)
                {
                    if (mod.module == module)
                    {
                        snfModule = mod;
                        break;
                    }
                }
                if (snfModule == null)
                    return false;
            }
            long to = System.currentTimeMillis() + timeout;
            do
            {
                if (snfModule.status == status)
                    return true;
                try
                {
                    Thread.sleep(250);
                } catch (InterruptedException e)
                {
                    nodeConfig.log.warn("unexpected wakeup", e);
                }
                if (System.currentTimeMillis() >= to)
                    throw new Exception(module.getClass().getSimpleName() + " did not reach expected state in given time");
            } while (true);
        }

        public void start() throws Exception
        {
            try
            {
                synchronized (ccsConfigs)
                {
                    for (CredConnSysConfig ccsc : ccsConfigs)
                    {
                        nodeConfig.toSb("starting ccsc: ", ccsc.name);
                        nodeConfig.tabLevel.incrementAndGet();
                        ccsc.start();
                        nodeConfig.tabLevel.decrementAndGet();
                    }
                }

                synchronized (modules)
                {
                    for (SnfModule module : modules)
                    {
                        if (!module.start)
                        {
                            if (module.module instanceof ModuleWrapper)
                                nodeConfig.toSb("starting module: ", ((ModuleWrapper) module.module).getWrappedModule().getClass().getSimpleName(), " delayed");
                            else
                                nodeConfig.toSb("starting module: ", module.module.getClass().getSimpleName(), " delayed");
                            continue;
                        }
                        nodeConfig.toSb("starting: ", module.module.getClass().getName());
                        module.status = ModuleStatus.Starting;
                        nodeConfig.tabLevel.incrementAndGet();
                        module.module.start(nodeConfig.node);
                        nodeConfig.tabLevel.decrementAndGet();
                        module.status = ModuleStatus.Started;
                        SimpleNodeFramework.getSnf().fireEvent(new StartedEvent(module.module.getClass()));
                    }
                }
                //                nodeConfig.log.info(nodeConfig.sb.toString());
            } catch (Exception e)
            {
                nodeConfig.toSb("start failed: ", e.getClass().getSimpleName(), " ", e.getMessage());
                nodeConfig.log.error(nodeConfig.sb.toString());
                throw e;
            }
        }

        public void stop() throws Exception
        {
            synchronized (modules)
            {
                for (SnfModule module : modules)
                {
                    module.status = ModuleStatus.Stopping;
                    try
                    {
                        module.module.stop(nodeConfig.node);
                    } catch (Exception e)
                    {
                        nodeConfig.toSb("stop failed: ", e.getClass().getSimpleName(), " ", e.getMessage());
                        nodeConfig.log.error("stop failed");
                        throw e;
                    }
                    module.status = ModuleStatus.Stopped;
                }
            }
        }

        public void destroy()
        {
            synchronized (modules)
            {
                for (SnfModule module : modules)
                {
                    module.status = ModuleStatus.Destroying;
                    try
                    {
                        module.module.destroy();
                    } catch (Exception e)
                    {
                        nodeConfig.toSb("destroy failed: ", e.getClass().getSimpleName(), " ", e.getMessage());
                        nodeConfig.log.error("destroy failed");
                    }
                    module.status = ModuleStatus.Destroyed;
                }
                modules.clear();
                services.clear();
            }
        }

        @Override
        public String toString()
        {
            return "name: " + name + " priority: " + priority;
        }
    }

    public static class SnfModule
    {
        public final SystemBase systemBase;
        public final Module module;
        public final String moduleName;
        public ModuleStatus status;
        public volatile boolean start;

        private SnfModule(SystemBase systemBase, String moduleName, String serviceName, DOF dof) throws Exception
        {
            this.systemBase = systemBase;
            systemBase.setDof(dof);
            module =null;
            this.moduleName = moduleName;
            status = ModuleStatus.Initing;
        }
        
        private SnfModule(Module module, String moduleName)
        {
            systemBase = null;
            this.module = module;
            this.moduleName = moduleName;
            status = ModuleStatus.Initing;
        }

        public void setStart(boolean value)
        {
            start = value;
        }

        @Override
        public String toString()
        {
            return module.getClass().getSimpleName() + " " + status + " start: " + start;
        }
    }
}
