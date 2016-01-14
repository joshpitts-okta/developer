
/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFSystem.ActivateInterestListener;
import org.opendof.core.oal.security.DOFPermission;
import org.opendof.core.oal.security.DOFPermissionSet;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.PropertiesFile;
import org.pslcl.service.executor.BlockingExecutor;
import org.pslcl.service.executor.BlockingExecutorConfig;
import org.pslcl.service.executor.ExecutorQueueConfig;
import org.pslcl.service.executor.ScheduledExecutor;
import org.pslcl.service.status.StatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dsp.Configuration;
import com.pslcl.dsp.Module;
import com.pslcl.dsp.Node;

//@ThreadSafe
public class HubManagerModule implements Module, ActivateInterestListener
{
    public static final String PropertyFileNameKey = "org.emitdo.dsp.hubmanager.properties-file";
    public static final String CredentialsFileNameKey = "emitdo.dsp.hubmanager.cred";
    public static final String ModuleNameKey = "emitdo.dsp.hubmanager.module-name";
    public static final String CommTimeoutKey = "emitdo.dsp.hubmanager.comm-timeout";

    public static final String MaxProvidesKey = "emitdo.dsp.hubmanager.max-provides";
    public static final String ServerPollRateKey = "emitdo.dsp.hubmanager.server-poll-rate";
    public static final String ServerLbPortKey = "emitdo.dsp.hubmanager.server-lb-port";

    public static final String CorePoolSizeKey = "emitdo.service-utils.executor.core-size";
    public static final String MaximumQueueSizeKey = "emitdo.service-utils.executor.max-queue-size";
    public static final String MaxBlockingTimeKey = "emitdo.service-utils.executor.max-blocking-time";
    public static final String ThreadNamePrefixKey = "emitdo.service-utils.executor.thread-name";
    public static final String KeepAliveDelayKey = "emitdo.service-utils.executor.keep-alive-delay";
    public static final String AllowCoreThreadTimeoutKey = "emitdo.service-utils.executor.core-timeout";
    public static final String StatusNameKey = "emitdo.service-utils.executor.status-name";

    public static final String CorePoolSizeDefault = "2";
    public static final int TimerCorePoolSizeDefault = 2;
    public static final String MaximumQueueSizeDefault = "10";
    public static final String MaxBlockingTimeDefault = "120000";
    public static final String ThreadNamePrefixDefault = "BlockingExecutor";
    public static final String TimerThreadNamePrefixDefault = "ScheduledExecutor";
    public static final String KeepAliveDelayDefault = "120000";
    public static final String AllowCoreThreadTimeoutDefault = "true";
    public static final String StatusNameDefault = "BlockingExecutor";
    public static final String TimerStatusNameDefault = "ScheduledExecutor";

    public static final String PropertyFileNameDefault = "hubmanager.properties";
    public static final String CredentialsFileNameDefault = "dsp.cred";
    public static final String CommTimeoutDefault = "15000";
    public static final String MaximumProvidesDefault = "1000";
    public static final String ProviderBaseIdDefault = "[3:HubManager@pdsp.pewla.com]";
    public static final String ProviderIdDefault = "[3:HubStatus1@pdsp.pewla.com]";
    public static final String ModuleNameDefault = "HubManager";
    public static final String MaxProvidesDefault = "10";
    public static final String ServerPollRateDefault = "1000";
    public static final String ServerLbPortDefault = Integer.toString(InetTransport.DEFAULT_UNICAST_PORT);

    public static final boolean HubsTunnelDomains = true;
    public static final int HubsMaxReceiveSilence = DOFConnection.Config.DEFAULT_HUB_POINT_MAX_RECEIVE_SILENCE;
    public static final int HubsMaxSendSilence = DOFConnection.Config.DEFAULT_HUB_POINT_MAX_SEND_SILENCE;

    private final Logger log;

    // volatile, the following are effectively immutable (only set once in init/start method). Guard for visibility on getters being called in a different thread than init.   
    private volatile ExecutorService executor;
    private volatile DOFSystem coreSystem;
    private volatile StatusTracker statusTracker;
    private volatile String moduleName;
    private volatile DOFCredentials credentials;
    private volatile int maxProviders;
    private volatile int commTimeout;
    private volatile ServerMonitor serverMonitor;
    private volatile HubProvideFactory hubProvideFactory;
    private volatile ScheduledExecutorService timerExecutor;
    
    private volatile int corePoolSize;
    private volatile int maxQueueSize;
    private volatile int maxBlockingTime;
    private volatile String threadName;
    private volatile int keepAliveDelay;
    private volatile boolean allowCoreThreadTimeout;

    public HubManagerModule()
    {
        log = LoggerFactory.getLogger(getClass());
    }

    /**************************************************************************
     * Module implementation
     **************************************************************************/
    @Override
    public void init(Configuration config) throws Exception
    {
        StringBuilder sb = new StringBuilder("\n" + getClass().getName() + " init:\n");

        Properties properties = new Properties();
        String propertyFilename = System.getProperty(PropertyFileNameKey, PropertyFileNameDefault);
        //        sb.append("\t" + PropertyFileNameKey + "=" + propertyFilename + "\n");
        String configPath = config.platformConfig.getBaseConfigurationPath() + "/" + propertyFilename;
        sb.append("\tconfigPath=" + configPath + "\n");
        try
        {
            PropertiesFile.load(properties, configPath);
        } catch (FileNotFoundException fnfe)
        {
            sb.append("\t\tconfigPath not found, using default configuration values\n");
        } catch (IOException ioe)
        {
            String msg = "Unable to read properties file: '" + configPath + "'";
            sb.append("\t\t" + msg + "\n");
            log.error(sb.toString());
            throw new Exception(msg, ioe);
        }

        String credFile = properties.getProperty(CredentialsFileNameKey, CredentialsFileNameDefault);
        sb.append("\t" + CredentialsFileNameKey + "=" + credFile + "\n");
        String credPath = config.platformConfig.getBaseSecureConfigurationPath() + "/" + credFile;
        sb.append("\tcredPath=" + credPath + "\n");

        try
        {
            credentials = DOFCredentials.create(credPath);
        } catch (Exception e)
        {
            sb.append("failed to read credentials file " + credPath);
            log.error(sb.toString());
            throw e;
        }

        String msg = null;
        try
        {
            moduleName = properties.getProperty(ModuleNameKey, ModuleNameDefault);
            sb.append("\t" + ModuleNameKey + "=" + moduleName + "\n");
            msg = "invalid maxProviders value";
            maxProviders = Integer.parseInt(properties.getProperty(MaxProvidesKey, MaxProvidesDefault));
            sb.append("\t" + MaxProvidesKey + "=" + maxProviders + "\n");
            msg = "invalid commTimeout value";
            commTimeout = Integer.parseInt(properties.getProperty(CommTimeoutKey, CommTimeoutDefault));
            sb.append("\t" + CommTimeoutKey + "=" + commTimeout + "\n");
            msg = "invalid serverPollRate value";
            int serverPollRate = Integer.parseInt(properties.getProperty(ServerPollRateKey, ServerPollRateDefault));
            sb.append("\t" + ServerPollRateKey + "=" + serverPollRate + "\n");
            msg = "invalid server lb port value";
            int serverLbPort = Integer.parseInt(properties.getProperty(ServerLbPortKey, ServerLbPortDefault));
            sb.append("\t" + ServerLbPortKey + "=" + serverLbPort + "\n");

            String value = properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            sb.append("\t" + CorePoolSizeKey + "=" + value + "\n");
            msg = "invalid corePoolSize value";
            corePoolSize = Integer.parseInt(value);
            value = properties.getProperty(MaximumQueueSizeKey, MaximumQueueSizeDefault);
            sb.append("\t" + MaximumQueueSizeKey + "=" + value + "\n");
            msg = "invalid maxQueueSize value";
            maxQueueSize = Integer.parseInt(value);
            value = properties.getProperty(MaxBlockingTimeKey, MaxBlockingTimeDefault);
            sb.append("\t" + MaxBlockingTimeKey + "=" + value + "\n");
            msg = "invalid maxBlockingTime value";
            maxBlockingTime = Integer.parseInt(value);
            threadName = properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            sb.append("\t" + ThreadNamePrefixKey + "=" + value + "\n");
            value = properties.getProperty(KeepAliveDelayKey, KeepAliveDelayDefault);
            sb.append("\t" + KeepAliveDelayKey + "=" + value + "\n");
            msg = "invalid keepAliveDelay value";
            keepAliveDelay = Integer.parseInt(value);
            value = properties.getProperty(AllowCoreThreadTimeoutKey, AllowCoreThreadTimeoutDefault);
            sb.append("\t" + AllowCoreThreadTimeoutKey + "=" + value + "\n");
            msg = "invalid allowCoreThreadTimeout value";
            allowCoreThreadTimeout = Boolean.parseBoolean(value);

            msg = "init serverMonitor failed";
            serverMonitor = new ServerMonitor(serverLbPort, serverPollRate, commTimeout);
        } catch (Exception e)
        {
            sb.append(msg);
            log.error(sb.toString(), e);
            throw e;
        }

        log.info(sb.toString());
    }

    @Override
    public void start(Node node) throws Exception
    {
        statusTracker = node.getStatusTracker();
        statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        timerExecutor = new ScheduledExecutor();
        ((ScheduledExecutor)timerExecutor).init(new ExecutorQueueConfig(){
            @Override public int getCorePoolSize(){return TimerCorePoolSizeDefault;}
            @Override public String getThreadNamePrefix(){return TimerThreadNamePrefixDefault;}
            @Override public StatusTracker getStatusTracker(){return statusTracker;}
            @Override public String getStatusSubsystemName(){return TimerStatusNameDefault;}
        });
        
        executor = new BlockingExecutor();
        ((BlockingExecutor) executor).init(new BlockingExecutorConfig()
        {
            @Override public int getCorePoolSize(){return corePoolSize;}
            @Override public String getThreadNamePrefix(){return threadName;}
            @Override public StatusTracker getStatusTracker(){return statusTracker;}
            @Override public String getStatusSubsystemName(){return StatusNameDefault;}
            @Override public int getMaximumPoolSize(){return maxQueueSize;}
            @Override public boolean isAllowCoreThreadTimeout(){return allowCoreThreadTimeout;}
            @Override public int getKeepAliveTime(){return keepAliveDelay;}
            @Override public int getMaximumBlockingTime(){return maxBlockingTime;}
        });
        executor.submit(new StartTask(node, this));
    }

    @Override
    public void stop(Node node) throws Exception
    {
        log.debug("\n" + getClass().getName() + " stop");

        synchronized (this)
        {
            if (serverMonitor != null)
                serverMonitor.stop();

            if (coreSystem != null)
                coreSystem.destroy();
            coreSystem = null;
            if(statusTracker != null)
                statusTracker.setStatus(moduleName, StatusTracker.Status.Warn);
        }
    }

    @Override
    public void destroy()
    {
        log.debug("\n" + getClass().getName() + " destroy");
        credentials = null;

        if (timerExecutor != null)
            timerExecutor.shutdown(); // get it started, finish up below

        if (executor != null)
        {
            executor.shutdown();
            try
            {
                executor.awaitTermination(commTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e)
            { /* Shutting down; don't care */
            }
            if (!executor.isTerminated())
                executor.shutdownNow();
            executor = null;
        }
        if (timerExecutor != null)
        {
            try
            {
                timerExecutor.awaitTermination(commTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e)
            { /* Shutting down; don't care */
            }
            if (!timerExecutor.isTerminated())
                timerExecutor.shutdownNow();
        }
        if (hubProvideFactory != null)
            hubProvideFactory.removed();
        if (serverMonitor != null)
            serverMonitor.destroy();
        if (statusTracker != null)
            statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
        serverMonitor = null;
        statusTracker = null;
    }

    //    @ThreadSafe
    private class StartTask implements Callable<Void>
    {
        private final Node node;
        private final HubManagerModule module;

        private StartTask(Node node, HubManagerModule module)
        {
            this.node = node;
            this.module = module;
        }

        @Override
        public Void call() throws Exception
        {
            log.debug(HubManagerModule.class.getName() + " start");

            try
            {
	            DOFPermissionSet coreSystemPermissions = new DOFPermissionSet.Builder().addPermission(new DOFPermission.ActAsAny(), new DOFPermission.Provider(), new DOFPermission.Requestor(), new DOFPermission.Binding.Builder(DOFPermission.Binding.ACTION_ALL).setAllAttributesAllowed(true).build(), new DOFPermission.TunnelDomain(DOFPermission.TunnelDomain.ALL)).build();
	            //@formatter:off
	            DOFSystem.Config coreSystemConfig = new 
	                            DOFSystem.Config.Builder()
	                            .setCredentials(credentials)
	                            .setTunnelDomains(true)
	                            .setPermissions(coreSystemPermissions)
	                            .setPermissionsExtendAllowed(true)
	                            .build();
                //@formatter:on
	            coreSystem = DynamicSystemFactory.getSystemFuture(executor, node.getDOF(), coreSystemConfig, commTimeout).get();
	
	            serverMonitor.start(node.getDOF(), statusTracker, timerExecutor);
	            hubProvideFactory = new HubProvideFactory(timerExecutor, executor, statusTracker, serverMonitor, node.getDOF(), coreSystem, credentials, commTimeout);
	            hubProvideFactory.init();
	
	            log.debug("HubManager obtained system for credentials: " + credentials.toString() + " from dof: " + node.getDOF().getState().getName());
	            serverMonitor.setHubProvideFactory(hubProvideFactory);
	            statusTracker.setStatus(moduleName, StatusTracker.Status.Ok);
	            coreSystem.addActivateInterestListener(module);
	            return null;
            }catch(Exception e)
            {
                log.error("HubManagerModule failed to start", e);
                statusTracker.setStatus(moduleName, StatusTracker.Status.Error);
                throw e; 
            }
        }
    }

    @Override
    public void activate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
    {
        if (!interfaceID.equals(HubRequestInterface.IID))
            return;
        if (hubProvideFactory != null)
            hubProvideFactory.activate(objectID, interfaceID);
    }

    @Override
    public void cancelActivate(DOFSystem system, DOFRequest request, DOFObjectID objectID, DOFInterfaceID interfaceID)
    {
        if (!interfaceID.equals(HubRequestInterface.IID))
            return;

        if (hubProvideFactory != null)
            hubProvideFactory.cancelActivate(objectID, interfaceID);
    }

    @Override
    public void removed(DOFSystem system, DOFException exception)
    {
    }
}
