package com.pslcl.chad.tests.dtf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.futures.ConfigureFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeployFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.RunFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.RunFuture.TimeoutData;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.provider.machine.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.network.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;
import com.pslcl.dtf.runner.process.RunnerMachine;

@SuppressWarnings("javadoc")
public class BindAwsTest implements PreStartExecuteInterface
{
    public final static String InspectGzPath = "/wsp/testing-framework/platform/dtf-runner/dtf-dtf-runner-1.0.src.tar.gz";
    public final static boolean actualTar = true;
    public final static String TimeoutKey = "pslcl.resource.aws.test.timeout";
    public final static String TimeoutDefault = "15";

    private Logger log;
    private volatile RunnerConfig config;
    private volatile AwsTestConfig myConfig;
    private volatile AwsMachineProvider machineProvider;
    private volatile AwsNetworkProvider networkProvider;
    private volatile AwsPersonProvider personProvider;
    private volatile AwsResourcesManager manager;
    private volatile List<ResourceReserveDisposition> machineResult;
    private volatile List<ResourceReserveDisposition> networkResult;
    private volatile List<ResourceReserveDisposition> personResult;
    private volatile String templateId;
    private final MachineInstance[] machineInstances;
    private final NetworkInstance[] networkInstances;
    private final PersonInstance[] personInstances;
    private final CableInstance[] cableInstances;

    public BindAwsTest()
    {
        log = LoggerFactory.getLogger(getClass());
        cableInstances = new CableInstance[4];
        machineInstances = new MachineInstance[4];
        networkInstances = new NetworkInstance[2];
        personInstances = new PersonInstance[4];
    }

    private List<ResourceDescription> getMachineResourceDescriptions(Properties appProperties)
    {
        List<ResourceDescription> list = new ArrayList<ResourceDescription>();
        getMachineResourceDescription(ResourceProvider.MachineName, 11, 80, appProperties, list);
        getMachineResourceDescription(ResourceProvider.MachineName, 22, 80, appProperties, list);
        getMachineResourceDescription(ResourceProvider.MachineName, 33, 80, appProperties, list);
        getMachineResourceDescription(ResourceProvider.MachineName, 44, 80, appProperties, list);
        return list;
    }

    private List<ResourceDescription> getMachineResourceDescription(String name, int resourceId, int runId, Properties appProperties, List<ResourceDescription> list)
    {
        Map<String, String> attrs = new HashMap<String, String>();
        addAttribute(ProviderNames.InstanceTypeKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageArchitectureKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageHypervisorKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageImageIdKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageImageTypeKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageIsPublicKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageNameKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageOwnerKey, appProperties, attrs);
        addAttribute(ProviderNames.ImagePlatformKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageRootDevTypeKey, appProperties, attrs);
        addAttribute(ProviderNames.ImageStateKey, appProperties, attrs);
        addAttribute(ProviderNames.BlockingDeviceVolumeSizeKey, appProperties, attrs);
        addAttribute(ProviderNames.BlockingDeviceVolumeTypeKey, appProperties, attrs);
        addAttribute(ProviderNames.BlockingDeviceDeleteOnTerminationKey, appProperties, attrs);
        addAttribute(ProviderNames.LocationYearKey, appProperties, attrs);
        addAttribute(ProviderNames.LocationMonthKey, appProperties, attrs);
        addAttribute(ProviderNames.LocationDotKey, appProperties, attrs);

        List<Entry<String, String>> flist = PropertiesFile.getPropertiesForBaseKey(ProviderNames.LocationFeatureKey, appProperties);
        for (Entry<String, String> entry : flist)
            attrs.put(entry.getKey(), entry.getValue());

        getNetworkResourceDescription(name, resourceId, runId, appProperties, list, attrs);
        templateId = getTemplateId();
        ResourceCoordinates coord = new ResourceCoordinates(templateId, resourceId, runId);
        ResourceDescImpl resource = new ResourceDescImpl(name, coord, attrs);
        list.add(resource);
        return list;
    }

    private void addAttribute(String key, Properties appProperties, Map<String, String> attrs)
    {
        String value = appProperties.getProperty(key);
        if (value == null)
            return;
        attrs.put(key, value);
    }

    private List<ResourceDescription> getNetworkResourceDescriptions(Properties appProperties)
    {
        List<ResourceDescription> list = new ArrayList<ResourceDescription>();
        getMachineResourceDescription(ResourceProvider.NetworkName, 111, 80, appProperties, list);
        getMachineResourceDescription(ResourceProvider.NetworkName, 222, 80, appProperties, list);
        return list;
    }

    private List<ResourceDescription> getNetworkResourceDescription(String name, int resourceId, int runId, Properties appProperties, List<ResourceDescription> list, Map<String, String> attrsIn)
    {
        Map<String, String> attrs = new HashMap<String, String>();
        if (attrsIn != null)
            attrs = attrsIn;

        addAttribute(InstanceNames.VpcNameKey, appProperties, attrs);
        addAttribute(InstanceNames.VpcCidrKey, appProperties, attrs);
        addAttribute(InstanceNames.VpcTenancyKey, appProperties, attrs);
        addAttribute(InstanceNames.VpcMaxDelayKey, appProperties, attrs);
        addAttribute(InstanceNames.VpcMaxRetriesKey, appProperties, attrs);
        addAttribute(InstanceNames.SubnetKeyBase, appProperties, attrs);
        addAttribute(InstanceNames.SubnetSizeKey, appProperties, attrs);
        addAttribute(InstanceNames.SubnetNameKey, appProperties, attrs);
        addAttribute(InstanceNames.SubnetCidrKey, appProperties, attrs);
        addAttribute(InstanceNames.SubnetVpcIdKey, appProperties, attrs);

        List<Entry<String, String>> flist = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermProtocolKey, appProperties);
        for (Entry<String, String> entry : flist)
            attrs.put(entry.getKey(), entry.getValue());
        flist = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermIpRangeKey, appProperties);
        for (Entry<String, String> entry : flist)
            attrs.put(entry.getKey(), entry.getValue());
        flist = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermPortKey, appProperties);
        for (Entry<String, String> entry : flist)
            attrs.put(entry.getKey(), entry.getValue());

        addAttribute(InstanceNames.AvailabilityZoneKey, appProperties, attrs);

        if (attrsIn == null)
        {
            ResourceCoordinates coord = new ResourceCoordinates(getTemplateId(), resourceId, runId);
            ResourceDescImpl resource = new ResourceDescImpl(name, coord, attrs);
            list.add(resource);
        }
        return list;
    }

    private List<ResourceDescription> getPersonResourceDescriptions(Properties appProperties, boolean useSiteDefault)
    {
        List<ResourceDescription> list = new ArrayList<ResourceDescription>();
        getPersonResourceDescription(ResourceProvider.PersonName, 1111, 50, appProperties, list, useSiteDefault);
        getPersonResourceDescription(ResourceProvider.PersonName, 2222, 50, appProperties, list, useSiteDefault);
        getPersonResourceDescription(ResourceProvider.PersonName, 3333, 50, appProperties, list, useSiteDefault);
        getPersonResourceDescription(ResourceProvider.PersonName, 4444, 50, appProperties, list, useSiteDefault);
        return list;
    }

    private List<ResourceDescription> getPersonResourceDescription(String name, int resourceId, int runId, Properties appProperties, List<ResourceDescription> list, boolean useSiteDefault)
    {
        Map<String, String> attrs = new HashMap<String, String>();
        addAttribute(ProviderNames.SesMaxDelayKey, appProperties, attrs);
        addAttribute(ProviderNames.SesMaxRetriesKey, appProperties, attrs);

        if (!useSiteDefault)
        {
            List<Entry<String, String>> ilist = PropertiesFile.getPropertiesForBaseKey(ProviderNames.SesInspectorKey, appProperties);
            for (Entry<String, String> entry : ilist)
                attrs.put(entry.getKey(), entry.getValue());
        }

        ResourceCoordinates coord = new ResourceCoordinates(getTemplateId(), resourceId, runId);
        ResourceDescImpl resource = new ResourceDescImpl(name, coord, attrs);
        list.add(resource);
        return list;
    }

    public String getTemplateId()
    {
        byte[] raw = new byte[32];
        for (byte i = 0; i < raw.length; i++)
            raw[i] = i;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length; i++)
        {
            String value = Integer.toHexString(raw[i]);
            if (value.length() == 1)
                sb.append("0");
            sb.append(value);
        }
        return sb.toString();
    }

    private void reserveMachine(Properties appProperties) throws InterruptedException, ExecutionException
    {
        List<ResourceDescription> resources = getMachineResourceDescriptions(appProperties);
        //        ReserveWorker worker = new ReserveWorker(true, resources);
        //        config.blockingExecutor.submit(worker).get();   // block but do them all in parallel 
        Future<List<ResourceReserveDisposition>> future = machineProvider.reserve(resources, myConfig.timeout);
        machineResult = future.get();
        log.info(machineResult.toString());
    }

    private void reserveNetwork(Properties appProperties)
    {
        try
        {
            List<ResourceDescription> resources = getNetworkResourceDescriptions(appProperties);
            Future<List<ResourceReserveDisposition>> future = networkProvider.reserve(resources, myConfig.timeout);
            networkResult = future.get();
            log.info(networkResult.toString());
        } catch (Exception e)
        {
            log.error("reserve failed", e);
        }
    }

    private void reservePerson(Properties appProperties, boolean useSiteDefault)
    {
        try
        {
            List<ResourceDescription> resources = getPersonResourceDescriptions(appProperties, useSiteDefault);
            Future<List<ResourceReserveDisposition>> future = personProvider.reserve(resources, myConfig.timeout);
            personResult = future.get();
            log.info(personResult.toString());
        } catch (Exception e)
        {
            log.error("reserve failed", e);
        }
    }

    private void bindMachine(boolean earlyout)
    {
        try
        {
            List<ReservedResource> resources = new ArrayList<ReservedResource>();
            for (ResourceReserveDisposition disposition : machineResult)
                resources.add(disposition.getReservedResource());
            List<Future<MachineInstance>> futures = machineProvider.bind(resources);
            int index = 0;
            if (earlyout)
                return;
            for (Future<MachineInstance> future : futures)
                machineInstances[index++] = future.get();
        } catch (Exception e)
        {
            log.error("bind machine failed", e);
        }
    }

    private void bindNetwork()
    {
        try
        {
            List<ReservedResource> resources = new ArrayList<ReservedResource>();
            for (ResourceReserveDisposition disposition : networkResult)
                resources.add(disposition.getReservedResource());
            List<Future<NetworkInstance>> futures = networkProvider.bind(resources);
            int index = 0;
            for (Future<NetworkInstance> future : futures)
                networkInstances[index++] = future.get();
        } catch (Exception e)
        {
            log.error("bind network failed", e);
        }
    }

    private void bindPerson()
    {
        try
        {
            List<ReservedResource> resources = new ArrayList<ReservedResource>();
            for (ResourceReserveDisposition disposition : personResult)
                resources.add(disposition.getReservedResource());
            List<Future<PersonInstance>> futures = personProvider.bind(resources);
            int index = 0;
            for (Future<PersonInstance> future : futures)
                personInstances[index++] = future.get();
        } catch (Exception e)
        {
            log.error("bind network failed", e);
        }
    }

    private void connect(int machineIndex) throws Exception
    {
        cableInstances[machineIndex] = machineInstances[machineIndex].connect(networkInstances[machineIndex < 2 ? 0 : 1]).get();
    }

    private void connect()
    {
        try
        {
            List<Future<Void>> list = new ArrayList<Future<Void>>();
            for (int i = 0; i < machineInstances.length; i++)
                list.add(config.blockingExecutor.submit(new ConnectWorker(this, i)));
            for (int i = 0; i < list.size(); i++)
                list.get(i).get();
        } catch (Exception e)
        {
            log.error("connect failed", e);
        }
    }

    private void deploy(int machineIndex) throws Exception
    {
        String partialDestPath = "toplevel";
        String url = "http://mirrors.koehn.com/apache//commons/cli/binaries/commons-cli-1.3.1-bin.zip";
        machineInstances[machineIndex].deploy(partialDestPath, url).get();
        partialDestPath = "bin/doit.bat";
        url = "http://mirrors.koehn.com/apache//commons/cli/binaries/commons-cli-1.3.1-bin.zip";
        machineInstances[machineIndex].deploy(partialDestPath, url).get();
        partialDestPath = "lib/someApp.jar";
        url = "http://mirrors.koehn.com/apache//commons/cli/source/commons-cli-1.3.1-src.zip";
        machineInstances[machineIndex].deploy(partialDestPath, url).get();
    }

    private void deploy()
    {
        try
        {
            List<Future<Void>> list = new ArrayList<Future<Void>>();
            for (int i = 0; i < machineInstances.length; i++)
                list.add(config.blockingExecutor.submit(new DeployWorker(this, i)));
            for (int i = 0; i < list.size(); i++)
                list.get(i).get();
        } catch (Exception e)
        {
            log.error("connect failed", e);
        }
    }

    private void inspect(int personIndex) throws Exception
    {
        String includeContent = "the quick brown fox jumped over the lazy dog. THE QUICK BROWN FOX JUMPED OVER THE LAZY DOG. 01234567890";
        ByteArrayInputStream rawStream = new ByteArrayInputStream(includeContent.getBytes());
        String instructions = "see the attached file";

        File tarball = new File(InspectGzPath);
        FileInputStream fis = new FileInputStream(tarball);

        InputStream is = rawStream;
        String includeName = "attachments.tar.gz";
        if (actualTar)
        {
            is = fis;
            includeName = tarball.getName();
        }
        personInstances[personIndex].inspect(instructions, is, includeName).get();
    }

    private void inspect()
    {
        try
        {
            List<Future<Void>> list = new ArrayList<Future<Void>>();
            for (int i = 0; i < personInstances.length; i++)
                list.add(config.blockingExecutor.submit(new InspectWorker(this, i)));
            for (int i = 0; i < list.size(); i++)
                list.get(i).get();
        } catch (Exception e)
        {
            log.error("inspect failed", e);
        }
    }

    private void releaseTemplate(boolean machine)
    {
        if (machine)
            manager.release(templateId, false);
        else
            manager.release(templateId, false);
    }

    private class AwsTestConfig
    {
        public final int timeout;

        private AwsTestConfig(Properties properties)
        {
            String value = properties.getProperty(TimeoutKey, TimeoutDefault);
            timeout = Integer.parseInt(value);
        }
    }

    private class ConnectWorker implements Callable<Void>
    {
        private final BindAwsTest test;
        private final int machineIndex;

        private ConnectWorker(BindAwsTest test, int machineIndex)
        {
            this.test = test;
            this.machineIndex = machineIndex;
        }

        @Override
        public Void call() throws Exception
        {
            test.connect(machineIndex);
            return null;
        }
    }

    private class InspectWorker implements Callable<Void>
    {
        private final BindAwsTest test;
        private final int personIndex;

        private InspectWorker(BindAwsTest test, int personIndex)
        {
            this.test = test;
            this.personIndex = personIndex;
        }

        @Override
        public Void call() throws Exception
        {
            try
            {
                test.inspect(personIndex);
            } catch (Exception e)
            {
                log.error("inspect failed", e);
            }
            return null;
        }
    }

    private class DeployWorker implements Callable<Void>
    {
        private final BindAwsTest test;
        private final int personIndex;

        private DeployWorker(BindAwsTest test, int personIndex)
        {
            this.test = test;
            this.personIndex = personIndex;
        }

        @Override
        public Void call() throws Exception
        {
            try
            {
                test.deploy(personIndex);
            } catch (Exception e)
            {
                log.error("deploy failed", e);
            }
            return null;
        }
    }

    private enum RunType
    {
        Config, Run, Start
    }

    private class RunWorker implements Callable<Void>
    {
        private final BindAwsTest test;
        private final int machineIndex;
        private final RunType type;
        private final boolean windows;

        private RunWorker(BindAwsTest test, int machineIndex, RunType type, boolean windows)
        {
            this.test = test;
            this.machineIndex = machineIndex;
            this.type = type;
            this.windows = windows;
        }

        @Override
        public Void call() throws Exception
        {
            //            test.runit(machineIndex);
            return null;
        }
    }

    private void manualDeploy() throws Exception
    {
        String partialDestPath = "toplevel";
//        String host = "localhost";
        String host = "52.91.5.197";
        String linuxBase = "/opt/dtf/sandbox";
        String winBase = "\\opt\\dtf\\sandbox";
        boolean windows = false;

        String url = "http://mirrors.koehn.com/apache//commons/cli/binaries/commons-cli-1.3.1-bin.zip";
        DeployFuture df = new DeployFuture(host, linuxBase, winBase, partialDestPath, url, windows);
        df.call();
        partialDestPath = "bin/doit.bat";
        url = "http://mirrors.koehn.com/apache//commons/cli/binaries/commons-cli-1.3.1-bin.zip";
        df = new DeployFuture(host, linuxBase, winBase, partialDestPath, url, windows);
        df.call();
        partialDestPath = "lib/someApp.jar";
        url = "http://mirrors.koehn.com/apache//commons/cli/source/commons-cli-1.3.1-src.zip";
        df = new DeployFuture(host, linuxBase, winBase, partialDestPath, url, windows);
        df.call();
    }

    private void runFuturesTests() throws Exception
    {
//        String host = "localhost";
        String host = "52.91.5.197";
        String linuxBase = "/opt/dtf/sandbox";
        String winBase = "\\opt\\dtf\\sandbox";
        TimeoutData tod = RunFuture.TimeoutData.getTimeoutData(5, TimeUnit.MINUTES, 1, TimeUnit.MINUTES);
        String[] runPartialDestPath = new String[]{"l1doit.bat", "bin/l2doit.bat", "c:\\opt\\dtf\\sandbox\\l1doit.bat"};
        String[] startPartialDestPath = new String[]{"l1doitPause.bat", "bin/l2doitPause.bat","c:\\opt\\dtf\\sandbox\\l1doitPause.bat"};
        boolean doConfig = false;
        boolean linuxOnly = true;
        boolean winOnly = false;

        for(int i=0; i < 2; i++)
        {
            if(winOnly && i == 1)
                continue;
            if(linuxOnly && i == 0)
                continue;
            log.info(i==0?"windows" : "linux");
            for(int j=0; j < 3; j++)
            {
                boolean windows = i == 0;
                switch (j)
                {
                    case 0:
                        log.info("toplevel");
                        break;
                    case 1:
                        log.info("with penultimate");
                        break;
                    case 2:
                        log.info("full path");
                        break;
                    default:
                        break;
                }
                for(int k=0; k < 3; k++)
                {
                    ConfigureFuture configFuture = null;
                    RunFuture runFuture = null;

                    Integer configRc = null;
                    StafRunnableProgram runnableProgram;
                    switch (k)
                    {
                        case 0:
                            log.info("configureFuture");
                            configFuture = new ConfigureFuture(host, linuxBase, winBase, runPartialDestPath[j], windows, this);
                            configRc = configFuture.call();
                            if(configRc != 0)
                                throw new Exception("configureFuture application returned non-zero");
                            break;
                        case 1:
                            log.info("runFuture");
                            runFuture = new RunFuture(host, linuxBase, winBase, runPartialDestPath[j], null, windows, this);
                            runnableProgram = (StafRunnableProgram) runFuture.call();
                            if(runnableProgram.getRunResult() != 0)
                                throw new Exception("runFuture application returned non-zero");
                            break;
                        case 2:
                            log.info("startFuture");
                            runFuture = new RunFuture(host, linuxBase, winBase, startPartialDestPath[j], config.blockingExecutor, windows, this);
                            runnableProgram = (StafRunnableProgram) runFuture.call();
                            if(!runnableProgram.isRunning())
                                throw new Exception("startFuture application returned not running");
                            if(runnableProgram.getRunResult() != null)
                                throw new Exception("startFuture application returned non-null, it should not know results yet");
                            Integer ccode = runnableProgram.kill().get();
                            if(ccode != 0)
                                throw new Exception("kill returned non-zero");
                            if(runnableProgram.getRunResult() != 0)
                                throw new Exception("startFuture application returned non-zero after stop");
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void execute(RunnerConfig config, Properties appMachineProperties, CommandLine activeCommand) throws Exception
    {
        this.config = config;
        myConfig = new AwsTestConfig(appMachineProperties);
        manager = (AwsResourcesManager) ((RunnerMachine) config.runnerService.getRunnerMachine()).getTemplateProvider().getResourceProviders().getManagers().get(0);
        machineProvider = manager.getMachineProvider();
        networkProvider = manager.getNetworkProvider();
        personProvider = manager.getPersonProvider();

        boolean machine = activeCommand.hasOption(AwsCliCommand.MachineShortCl);
        boolean person = activeCommand.hasOption(AwsCliCommand.PersonShortCl);
        boolean deploy = activeCommand.hasOption(AwsCliCommand.DeployShortCl);
        boolean cleanup = activeCommand.hasOption(AwsCliCommand.CleanupShortCl);
        boolean run = activeCommand.hasOption(AwsCliCommand.RunShortCl);

        if(run)
        {
            runFuturesTests();
            return;
        }
        if(deploy)
        {
            manualDeploy();
            return;
        }
        if (machine)
        {
            reserveMachine(appMachineProperties);
            bindMachine(cleanup);
            if (cleanup)
            {
                log.info("look at aws console for all pendings here, you have 5 seconds");
                Thread.sleep(5000); // 
            }
            if (!cleanup)
            {
                reserveNetwork(appMachineProperties);
                bindNetwork();
                connect();
                log.info("giving deploy 10 secs");
                Thread.sleep(10000);
                log.info("giving deploy 10 secs is up");
                deploy();
            }
            releaseTemplate(true);
            if (cleanup)
                log.info("look at aws console for all shutting down here, it will be several minutes before termination");
            return;
        }
        if (person)
        {
            boolean useSiteDefaults = activeCommand.hasOption(AwsCliCommand.PersonDefaultShortCl);
            reservePerson(appMachineProperties, useSiteDefaults);
            bindPerson();
            inspect();
            releaseTemplate(false);
        }
        log.info("BindAwsTest end");
    }
}