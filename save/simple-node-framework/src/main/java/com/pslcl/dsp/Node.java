package com.pslcl.dsp;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.opendof.core.SLF4JLogListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFServer;
import org.opendof.core.transport.inet.InetTransport;
import org.pslcl.service.ClassInfo;
import org.pslcl.service.status.StatusTracker;
import org.pslcl.service.status.StatusTracker.Status;
import org.pslcl.service.status.StatusTrackerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Node implements Daemon, NodeMBean, UncaughtExceptionHandler {

	private static final int DEFAULT_CONNECTION_AUDIT_PERIOD = 1000*60 * 10; // 10 minutes, from the DSP technical specification eng-dsp_ts.xml

	/**
	 * The frequency at which reports are logged of current state and health.
	 */
	private static final long REPORT_PERIOD = 5 * 60 * 1000;
	
	/**
	 * The frequency at which the log level of the logger is checked for changes.
	 */
	private static final long LOG_LEVEL_PERIOD = 30 * 1000;
	
    /**
     * The logger used to log any messages.
     */
    protected static final Logger logger = LoggerFactory.getLogger(Node.class); //Setup the SLF4J logger
    
    /**
     * The hook to tell us that we need DOFConfig.setRouter(true).
     */
    private boolean isRouterNode = false; // default is no
    
    /**
     * Timer.
     */
    private Timer timer;
    
    /**
     * The DOF.
     */
    protected DOF dof = null;
    
    /**
     * The status tracker.
     */
    protected StatusTracker statusTracker = null;
    
    /**
     * The Node configuration.
     */
    private Configuration config;
    
    /**
     * The Connection Auditor used for the node.
     */
    private ConnectionAuditor connectionAuditor;
    
    /**
     * The list of loaded Node modules.
     */
    protected List<Module> modules = new ArrayList<Module>();
    
    private SLF4JLogListener logListener;
    private DOF.Log.Level logLevel;

    public Node() {
    	// Setup what we can, prior to knowing configuration
    	DOF.Log.Level level = DOF.Log.Level.INFO;
    	if (logger.isTraceEnabled())
    		level = DOF.Log.Level.TRACE;
    	else if (logger.isDebugEnabled())
    		level = DOF.Log.Level.DEBUG;
    	logListener = new SLF4JLogListener();
        DOF.Log.addListener(level, logListener);
//        SMS4Cipher.register();
//        TwofishCipher.register();
    	Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		// jsvc calls this to open config files and to create everything
		// Initialize the service (with elevated privileges).
    	logger.info("Initializing DSP Node.");
    	logger.info(ClassInfo.getInfo(System.class).toString());
    	logger.info(ClassInfo.getInfo(this.getClass()).toString());
    	logger.info(ClassInfo.getInfo(DOF.class).toString());
    	logger.info(ClassInfo.getInfo(InetTransport.class).toString());
    	synchronized (this) {
	        PlatformConfiguration platformConfig;
	        logger.debug("Loading platform configuration.");
	        ServiceLoader<PlatformConfiguration> platformConfigurationLoader = ServiceLoader.load(PlatformConfiguration.class);
	        Iterator<PlatformConfiguration> platformConfigurationIterator = platformConfigurationLoader.iterator();
	        logger.debug("Found platform config class: " + platformConfigurationIterator.hasNext());
	        if (platformConfigurationIterator.hasNext()) {
				try {
					platformConfig = platformConfigurationIterator.next();
				} catch (ServiceConfigurationError sce) {
					throw new DaemonInitException("Unable to instantiate PlatformConfiguration: " + sce, sce); // jsvc won't call destroy() when init() fails
				}
		        if (platformConfigurationIterator.hasNext()) {
		        	throw new DaemonInitException("Too many PlatformConfiguration implementations found (only one should exist in the classpath).");
		        }
		        logger.info("Loading PlatformConfiguration: " + ClassInfo.getInfo(platformConfig.getClass()));
	            platformConfig.init();
	        } else {
	        	throw new DaemonInitException("No PlatformConfiguration implementations found (one should exist in the classpath).");
	        }

	        SystemConfiguration systemConfig;
	        logger.debug("Loading system configuration.");
	        ServiceLoader<SystemConfiguration> systemConfigurationLoader = ServiceLoader.load(SystemConfiguration.class);
	        Iterator<SystemConfiguration> systemConfigurationIterator = systemConfigurationLoader.iterator();
	        logger.debug("Found system config class: " + systemConfigurationIterator.hasNext());
	        if (systemConfigurationIterator.hasNext()) {
	        	try {
	        		systemConfig = systemConfigurationIterator.next();
				} catch (ServiceConfigurationError sce) {
					throw new DaemonInitException("Unable to instantiate SystemConfiguration: " + sce, sce); // jsvc won't call destroy() when init() fails
				}
		        if (systemConfigurationIterator.hasNext()) {
		        	throw new DaemonInitException("Too many SystemConfiguration implementations found (only one should exist in the classpath).");
		        }
		        logger.info("Loading SystemConfiguration: " + ClassInfo.getInfo(systemConfig.getClass()));
	            systemConfig.init(platformConfig);
	        } else {
	        	throw new DaemonInitException("No SystemConfiguration implementations found (one should exist in the classpath).");
	        }

	        config = new Configuration(platformConfig, systemConfig);

	        logger.debug("Loading routing configuration.");
	        ServiceLoader<RoutingConfiguration> routingConfigurationLoader = ServiceLoader.load(RoutingConfiguration.class);
	        Iterator<RoutingConfiguration> routingConfigurationIterator = routingConfigurationLoader.iterator();
	        logger.debug("Found routing config class: " + routingConfigurationIterator.hasNext());
	        while (routingConfigurationIterator.hasNext()) {
	        	try {
	        		RoutingConfiguration routingConfig = routingConfigurationIterator.next();
			        logger.info("Loading RoutingConfiguration: " + ClassInfo.getInfo(routingConfig.getClass()));
	        		isRouterNode = true; // an implementation was found
	        		// We choose not to break here so that all found implementations are reported in the log.
				} catch (ServiceConfigurationError sce) {
					// swallow this failure to find an implementation of RoutingConfiguration- leave isRouterNode at false 
				} catch (NoSuchElementException nse) {
					// swallow this failure to find an implementation of RoutingConfiguration- leave isRouterNode at false 
				} catch (Exception e) {
	        		throw new DaemonInitException("Unable to initialize Node due to a problem found while examining configuration.", e);
				}
	        }
        	
	        // Setup JMX monitoring capability.
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(this, new ObjectName("org.emitdo.dsp:type=Node"));
	
			// Initialize Node modules.
	        ServiceLoader<Module> moduleLoader = ServiceLoader.load(Module.class);
	        Iterator<Module> moduleIterator = moduleLoader.iterator();
	        while (moduleIterator.hasNext()) {
	        	Module module = null;
	        	try {
	        		module = moduleIterator.next();
	        		logger.info("Loading Module: " + ClassInfo.getInfo(module.getClass()));
	        		module.init(config);
		        	modules.add(module);
				} catch (ServiceConfigurationError sce) {
	        		destroy(); // iterate through "modules" and clean them up
					throw new DaemonInitException("Unable to instantiate Module: " + sce, sce);
	        	} catch (Exception e) {
					destroy(); // because jsvc won't call destroy() for us, when init() fails
	        		throw new DaemonInitException("Unable to initialize Module: " + module, e);
	        	}
	        }
    	}
	}

    /**
     * Start the service.
     * Create the connection to the cloud and attach the ReconnectingStateListener to ensure the connection will reconnect when needed.
     */
	@Override
	public void start() throws Exception {
		// jsvc calls this to start our threads and to accept incoming connections
    	logger.info("Starting DSP Node.");
    	synchronized (this) {
	        // Create the DOF
    		short maxThreads = 0;
    		if (config.systemConfig.getThreadpoolSizeThreshold() <= Short.MAX_VALUE)
    			maxThreads = (short) config.systemConfig.getThreadpoolSizeThreshold();
    		DOF.Config dofConfig = new DOF.Config.Builder()
    			.setName(config.systemConfig.getSystemID())
    			.setRouter(isRouterNode)
    			.setParameterValidation(false)
    			.setConnectionLimit(config.systemConfig.getConnectionCountThreshold())
    			.setThreadPoolSize(maxThreads)
    			.build();
	        dof = new DOF(dofConfig);
	        timer = new Timer();
	        timer.scheduleAtFixedRate(new Report(), 0L, REPORT_PERIOD);
	        timer.scheduleAtFixedRate(new LogLevelUpdate(), LOG_LEVEL_PERIOD, LOG_LEVEL_PERIOD);

    		// Create the Status Tracker
	        statusTracker = new StatusTrackerProvider();

	        // Create the Connection Auditor.
    		connectionAuditor = new ConnectionAuditor(config.systemConfig.getSystemID(), DEFAULT_CONNECTION_AUDIT_PERIOD);

			// Start Node Modules.
	        for (Module module : modules) {
	        	module.start(this);
	        }
    	}
    	logger.info("DSP Node Running" + (isRouterNode ? ", DOF routes" : ""));
	}

	@Override
	public void stop() throws Exception {
    	logger.info("Stopping DSP Node.");
		synchronized (this) {
			dof.setNodeDown();

			// Stop Node Modules.
	        for (Module module : modules) {
	        	module.stop(this);
	        }

	        // Destroy the Connection Auditor
	        connectionAuditor = null;
	        
	        // Destroy the Status Tracker
			statusTracker.endStatusProvider();
			statusTracker = null;
			
			// Destroy the DOF
			timer.cancel();
			timer = null;
			dof.destroy();
			dof = null;
			logger.debug("Node.stop() exits");
		}
	}

	@Override
	public void destroy() {
		// jsvc calls this to destroy any object created in init()
    	logger.info("Destroying DSP Node.");
		synchronized (this) {
			// Destroy Node Modules.
	        for (Module module : modules) {
	        	module.destroy();
	        }
	        modules.clear();
		}
    	logger.info("DSP Node Terminated.");
	}

	@Override
	public short getStatus() {
		Status status = StatusTracker.Status.Warn;
		synchronized (this) {
			if (statusTracker != null)
				status = statusTracker.getStatus(); 
		}
		return (short)status.ordinal();
	}

	@Override
	public float getLoad() {
		float load = 0.0f;
		synchronized (this) {
			if (dof != null) {
				// TODO Is there a better measurement of load than this?
				double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
				MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
				double memLoad = (double) memUse.getUsed() / memUse.getMax();
				if (memLoad > cpuLoad)
					return (float) memLoad;
				return (float) cpuLoad;
			}
		}
		return load;
	}

	public DOF getDOF() {
		return dof;
	}
	
	public Configuration getConfig() {
		return config;
	}
	
	public StatusTracker getStatusTracker() {
		return statusTracker;
	}
	
	public ConnectionAuditor getConnectionAuditor() {
		return connectionAuditor;
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		String msg = "FATAL ERROR: Uncaught exception in thread " + thread;
		logger.error(msg, ex);
		// Since the state is unknown at this point, we may not be able to perform a graceful exit.
		System.exit(1); // forces termination of all threads in the JVM
	}
	
	private class LogLevelUpdate extends TimerTask {

		@Override
		public void run() {
			DOF.Log.Level level = DOF.Log.Level.INFO;
	    	if (logger.isTraceEnabled())
	    		level = DOF.Log.Level.TRACE;
	    	else if (logger.isDebugEnabled())
	    		level = DOF.Log.Level.DEBUG;
	    	
	    	if(!level.equals(logLevel)){
		    	SLF4JLogListener newlogListener = new SLF4JLogListener();
		        DOF.Log.addListener(level, newlogListener);
		        DOF.Log.removeListener(logListener);
		        logListener = newlogListener;
	    	}
		}
		
	}

	private class Report extends TimerTask {

		@Override
		public void run() {
			DOF.Runtime runtime = dof.getRuntime();
			DOF.TrafficStats trafficStats = runtime.getTrafficStats();
			DOF.State state = dof.getState();
			StringBuilder sb = new StringBuilder();
			sb.append(state.getName());
			sb.append(" DOF State: ");
			sb.append(runtime.getConnectionCount());
			sb.append(" of ");
			sb.append(state.getConnectionLimit());
			sb.append(" connections, ");
			sb.append(runtime.getServerCount());
			sb.append(" servers, ");
			sb.append(runtime.getProvideCount());
			sb.append(" provides, ");
			sb.append(state.getThreadPoolSize());
			sb.append(" threads, ");
			sb.append(trafficStats.getReceiveByteCount());
			sb.append("B (");
			sb.append(trafficStats.getReceivePacketCount());
			sb.append("pkts) in, ");
			sb.append(trafficStats.getSendByteCount());
			sb.append("B (");
			sb.append(trafficStats.getSendPacketCount());
			sb.append("pkts) out, ");
			Runtime jvmr = Runtime.getRuntime();
			sb.append((jvmr.freeMemory()+524288)/1048576);
			sb.append("M free of ");
			sb.append((jvmr.totalMemory()+524288)/1048576);
			sb.append("M (max ");
			sb.append((jvmr.maxMemory()+524288)/1048576);
			sb.append("M), Load: ");
			sb.append(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
			sb.append(" CPU, ");
			MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
			sb.append((double) memUse.getUsed() / memUse.getMax());
			sb.append(" Mem, ");
			sb.append(getLoad());
			sb.append(" Agg");
			logger.info(sb.toString());
			// Report servers.
			TreeSet<String> servers = new TreeSet<String>();
			for (DOFServer server : runtime.getServers()) {
				DOFServer.State srvstate = server.getState();
				if (srvstate.isStarted()) {
					String name = srvstate.getName();
					if (name.contains("Server - "))
						name = name.substring(name.indexOf("Server - ", 0)+9);
					servers.add(name + "/" + srvstate.getAddress().toString());
				}
			}
			sb = new StringBuilder();
			sb.append(state.getName());
			sb.append(" DOF Servers: ");
			sb.append(servers.toString());
			logger.info(sb.toString());
			// Connection Auditor should be reporting connections, so this is at debug level.
			logger.debug(runtime.getConnections().toString());
		}
	}
	
}
