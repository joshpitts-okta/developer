package com.pslcl.service.util.dht.cluster;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFDuplicateException;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Attribute;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Query;
import org.opendof.core.oal.DOFQuery;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFArray;
import org.pslcl.service.status.StatusTracker.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.Service;
import com.pslcl.service.util.dht.DistributedActivateProvideService;
import com.pslcl.service.util.dht.cluster.internal.ClusterRoutingInterface;
import com.pslcl.service.util.dht.cluster.internal.ClusterRoutingTable;
import com.pslcl.service.util.provide.BindingRequest;

/**
 * This class is an implementation of <code>Service</code> and <code>DistributedService</code>.
 * The implementation of <code>DistributedService</code> provides a way for the <code>ProvideManager</code> to know how to respond to activate
 * requests. It also allows the <code>DistributedService</code> to give the <code>ProvideManager</code> activate requests that need to be started or
 * canceled.
 * This implementation of <code>DistributedService</code> uses virtual nodes to help with more even distribution. Each
 * <code>ClusterRoutingService</code> node joins a logical ring identified by an augmented service id.
 */
public class ClusterRoutingService implements Service<ClusterRoutingConfig>, DistributedActivateProvideService, ClusterRoutingServiceMBean
{
	// following are guarded by finals being set in constructor
	private final Logger logger;
	private final String className;
	private final Map<DOFObjectID, Future<Void>> futures;
	private final Set<DOFObjectID> nodesInRing;

	// following are guarded via volatile, where one time, single init thread initializes values.
	// if any of these later get set (in addition to init method) in another method,
	// they will need to change to regular monitor synch
	private volatile ClusterRoutingConfig config; // internals also guarded by being immutable.
	private volatile ClusterRoutingTable routingTable;
	private volatile DOFObjectID clusterServiceID;
	private volatile DOFObjectID globalID;
	private volatile int removeNodeGracePeriod;
	private volatile int opTimeout;
	private volatile DOFObject clusterProvObject;
	private volatile ClusterRoutingProvider clusterProvider;
	private volatile DOFOperation clusterProvideOp;
	private volatile ClusterInterfaceListener clusterInterfaceListener;

	public ClusterRoutingService()
	{
		logger = LoggerFactory.getLogger(getClass());
		className = getClass().getSimpleName();
		futures = new HashMap<DOFObjectID, Future<Void>>();
		nodesInRing = new HashSet<DOFObjectID>();
		logger.trace("new ClusterRoutingService");
	}

	@Override
	public boolean shouldIProvide(BindingRequest bindingRequest)
	{
		/* *****************
		 * NOTE: This method must not block, if it is possible to block, the ProvideManager
		 * needs to be changed to call this in an executor task.
		 */

		int replication = config.getReplicationFactor();
		replication = Math.min(replication, nodesInRing.size());
		return routingTable.shouldIProvide(bindingRequest, replication);
	}

	@Override
	public void removeWorkRequest(BindingRequest bindingRequest)
	{
		routingTable.removeWorkRequest(bindingRequest);
	}

	@Override
	public void init(ClusterRoutingConfig config) throws Exception
	{
		logger.trace("Cluster Routing Service init");

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(this, new ObjectName("dht.cluster:type=ClusterRoutingService"));

		this.config = config;
		config.getStatusTracker().setStatus(className, Status.Warn);

		opTimeout = config.getOperationTimeout(); // milliseconds
		removeNodeGracePeriod = config.removeNodePeriod(); // milliseconds

		DOFSystem system = config.getSystem();

		// Create cluster service id
		clusterServiceID = DOFObjectID.create(config.getBaseServiceID(), Attribute.create(Attribute.GROUP, config.getClusterID()));

		routingTable = new ClusterRoutingTable(config.getVnodes(), clusterServiceID, config.getGuid());
		globalID = routingTable.getGlobalID();

		// start joining the ring
		this.clusterProvObject = system.createObject(globalID);
		this.clusterProvider = new ClusterRoutingProvider();
		this.clusterProvideOp = clusterProvObject.beginProvide(ClusterRoutingInterface.DEF, DOF.TIMEOUT_NEVER, clusterProvider, null);

		// start looking for other nodes in the ring
		this.clusterInterfaceListener = new ClusterInterfaceListener();
		clusterInterfaceListener.interestOp = system.beginInterest(clusterServiceID, ClusterRoutingInterface.IID, DOFInterestLevel.WATCH);
		DOFQuery query = new DOFQuery.Builder().addFilter(clusterServiceID).addFilter(ClusterRoutingInterface.IID)
				.addRestriction(ClusterRoutingInterface.IID).build();
		clusterInterfaceListener.queryOp = system.beginQuery(query, DOF.TIMEOUT_NEVER, clusterInterfaceListener, null);

		// wait to see own provider
		synchronized (nodesInRing)
		{
			long to = System.currentTimeMillis() + config.getAuthTimeout();
			while (!nodesInRing.contains(globalID))
			{
				try
				{
					nodesInRing.wait(10);
					if (System.currentTimeMillis() >= to)
						throw new Exception("Failed to join the dht cluster");
				} catch (InterruptedException e)
				{
					// running on system layer startup thread, assume its an executor thread
					// we will honor an executor shutdown by exiting immediately
					config.getStatusTracker().setStatus(className, Status.Error);
					return;
				}
			}
		}
		config.getStatusTracker().setStatus(className, Status.Ok);
	}

	@Override
	public void destroy()
	{
		logger.info("Leaving the ring");
		List<LeavingRingNotification> notifications = new ArrayList<LeavingRingNotification>();

		for (DOFObjectID id : nodesInRing)
		{
			if (id.equals(globalID))
				continue;
			notifications.add(new LeavingRingNotification(config.getExecutor(), id));
		}

		try
		{
			config.getExecutor().invokeAll(notifications);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			// TODO, return?
		}

		if (clusterProvObject != null)
			clusterProvObject.destroy();
		if (clusterProvideOp != null)
			clusterProvideOp.cancel();
		if (clusterInterfaceListener != null)
			clusterInterfaceListener.close();
	}

	/* *************************************************************************
	 * ClusterRoutingServiceMBean implementation
	 * ************************************************************************
	 */

	@Override
	public String getGlobalID()
	{
		return globalID.toStandardString();
	}

	@Override
	public int getRingSize()
	{
		return nodesInRing.size();
	}

	@Override
	public int getAllWorkRequestsSize()
	{
		return routingTable.getWorkRequestsSize();
	}

	@Override
	public String getRoutingTableString()
	{
		return routingTable.getRoutingTableString();
	}

	@Override
	public String getWorkRequestsString()
	{
		return routingTable.getWorkRequestsString();
	}

	@Override
	public int getRoutingTableSize()
	{
		return routingTable.getRoutingTableSize();
	}

	@Override
	public void printRoutingTable()
	{
		System.out.println("routing table=" + routingTable.getRoutingTableString());
	}

	@Override
	public void printWorkRequests()
	{
		System.out.println("work requests=" + routingTable.getWorkRequestsString());
	}

	@Override
	public void printWorkRequests(String nodeID)
	{
		System.out.println("work requests for node id=" + nodeID + "=" + routingTable.getWorkRequestsString(DOFObjectID.create(nodeID)));
	}

	@Override
	public void printRing()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		synchronized (nodesInRing)
		{
			for (DOFObjectID id : nodesInRing)
			{
				sb.append("\t");
				sb.append(id);
				sb.append(",\n");
			}
		}

		if (sb.length() > 3)
			sb.delete(sb.length() - 2, sb.length());

		System.out.println("cluster nodes ring=[" + sb.toString() + "]");
	}

	private class ClusterInterfaceListener implements DOFSystem.QueryOperationListener
	{
		private DOFOperation interestOp;
		private DOFOperation queryOp;

		private void close()
		{
			if (queryOp != null)
				queryOp.cancel();
			if (interestOp != null)
				interestOp.cancel();
		}

		@Override
		public void interfaceAdded(Query operation, DOFObjectID objectID, DOFInterfaceID interfaceID)
		{
			// new cluster node joining the ring
			if (!interfaceID.equals(ClusterRoutingInterface.IID))
				return;

			boolean isNewNode = false;
			synchronized (nodesInRing)
			{
				if (!nodesInRing.contains(objectID))
					isNewNode = true;

				if (objectID.equals(globalID))
				{
					nodesInRing.add(objectID);
					nodesInRing.notify();
				}
			}

			if (objectID.equals(globalID))
				return;

			if (!isNewNode)
			{
				synchronized (futures)
				{
					Future<Void> existingFuture = futures.get(objectID);
					if (existingFuture != null && !existingFuture.isCancelled())
					{
						existingFuture.cancel(true);
						futures.remove(objectID);
					}
				}
				return;
			}

			logger.debug(globalID + " need to get routing entries from node id=" + objectID);
			Future<Void> future = config.getExecutor().submit(new AddNode(config.getExecutor(), objectID));
			synchronized (futures)
			{
				futures.put(objectID, future);
			}
		}

		@Override
		public void interfaceRemoved(Query operation, DOFObjectID objectID, DOFInterfaceID interfaceID)
		{
			// a cluster node is leaving the ring

			if (!interfaceID.equals(ClusterRoutingInterface.IID))
				return;

			synchronized (nodesInRing)
			{
				if (!nodesInRing.contains(objectID))
					return;
			}

			synchronized (futures)
			{
				Future<Void> existingFuture = futures.get(objectID);
				if (existingFuture != null && !existingFuture.isCancelled())
				{
					existingFuture.cancel(true);
				}
				Future<Void> future = config.getScheduledExecutor().schedule(new RemoveNode(config.getExecutor(), objectID), removeNodeGracePeriod,
						TimeUnit.MILLISECONDS);
				futures.put(objectID, future);
			}
		}

		@Override
		public void complete(DOFOperation operation, DOFException exception)
		{}

		@Override
		public void providerRemoved(Query operation, DOFObjectID exception)
		{}
	}

	private class AddNode implements Callable<Void>
	{
		private final DOFObjectID nodeID;
		private final ExecutorService executor;
		private final AtomicBoolean onTimer;

		public AddNode(ExecutorService executor, DOFObjectID globalID)
		{
			this.nodeID = globalID;
			this.executor = executor;
			onTimer = new AtomicBoolean(true);
		}

		@Override
		public Void call() throws Exception
		{
			if (onTimer.get())
			{
				onTimer.set(false);
				executor.submit(this);
				return null;
			}
			
			logger.debug("Adding node to ring, node id=" + nodeID);
			
			try
			{
				bootstrap(nodeID);
				futures.remove(nodeID);
			} catch (Exception e)
			{
				logger.error("failed to add node to the ring", e);
			}
			return null;
		}

		private void bootstrap(DOFObjectID id) throws Exception
		{
			DOFObject nodeObj = config.getSystem().createObject(id);
			DOFValue result;
			try
			{
				result = nodeObj
						.get(ClusterRoutingInterface.DEF.getProperty(ClusterRoutingInterface.getRoutingEntries), config.getOperationTimeout()).get();
				routingTable.addRoutingEntries(id, (DOFArray) result);
				nodeObj.destroy();

				synchronized (nodesInRing)
				{
					nodesInRing.add(id);
					nodesInRing.notify();
				}

				stopRemappedWork();

			} catch (DOFException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void stopRemappedWork()
		{
			int replication = config.getReplicationFactor();
			replication = Math.min(replication, nodesInRing.size());
			Set<BindingRequest> removeBindingRequests = routingTable.getRemappedBindings(replication);
			if (removeBindingRequests == null || removeBindingRequests.size() < 1)
				return;
			try
			{
				config.getManager().cancelProvide(removeBindingRequests);
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class RemoveNode implements Callable<Void>
	{
		private final DOFObjectID nodeID;
		private final ExecutorService executor;
		private final AtomicBoolean onTimer;

		public RemoveNode(ExecutorService executor, DOFObjectID nodeID)
		{
			this.nodeID = nodeID;
			this.executor = executor;
			onTimer = new AtomicBoolean(true);
		}

		@Override
		public Void call() throws Exception
		{
			if (onTimer.get())
			{
				onTimer.set(false);
				executor.submit(this);
				return null;
			}

			logger.debug("Removing node from ring, node id=" + nodeID);
			
			routingTable.removeRoutingEntry(nodeID);

			synchronized (nodesInRing)
			{
				nodesInRing.remove(nodeID);
			}

			Set<BindingRequest> bindings = routingTable.getWorkRequests(nodeID);
			for (BindingRequest binding : bindings)
				remapWork(binding);

			futures.remove(nodeID);
			return null;
		}

		private void remapWork(BindingRequest binding)
		{
			int replication = config.getReplicationFactor();
			replication = Math.min(replication, nodesInRing.size());

			if (!routingTable.shouldIProvide(binding, replication))
				return;

			logger.trace(globalID + " start or continue providing for requestor id=" + binding.getObjectID());
			try
			{
				config.getManager().activateRequest(binding);
			} catch (DOFDuplicateException e1)
			{
				// TODO Auto-generated catch block
				// e1.printStackTrace();
				logger.trace("already providing for this binding request");
			}
		}
	}

	/*
	 * Notify other nodes in the ring that this node is leaving.
	 */
	private class LeavingRingNotification implements Callable<Void>
	{
		private DOFObjectID nodeID;
		private final ExecutorService executor;
		private final AtomicBoolean onTimer;
		private DOFObject nodeObj;

		public LeavingRingNotification(ExecutorService executor, DOFObjectID nodeID)
		{
			this.nodeID = nodeID;
			this.executor = executor;
			onTimer = new AtomicBoolean(true);
		}

		@Override
		public Void call() throws Exception
		{
			if (onTimer.get())
			{
				onTimer.set(false);
				executor.submit(this);
				return null;
			}

			nodeObj = config.getSystem().createObject(nodeID);
			nodeObj.invoke(ClusterRoutingInterface.DEF.getMethod(ClusterRoutingInterface.removeClusterNode), opTimeout, globalID);
			nodeObj.destroy();

			return null;
		}
	}

	private class ClusterRoutingProvider extends DOFObject.DefaultProvider
	{
		public void get(DOFOperation.Provide operation, DOFRequest.Get request, DOFInterface.Property property)
		{
			int itemID = property.getItemID();
			DOFInterfaceID iid = property.getInterfaceID();

			if (iid.equals(ClusterRoutingInterface.IID) && itemID == ClusterRoutingInterface.getRoutingEntries)
			{
				logger.trace("sending entries in my routing table");
				request.respond(routingTable.getMyEntries());
			}
		}

		public void invoke(DOFOperation.Provide provOp, DOFRequest.Invoke request, DOFInterface.Method method, List<DOFValue> parameters)
		{
			int itemID = method.getItemID();
			DOFInterfaceID iid = method.getInterfaceID();

			if (iid.equals(ClusterRoutingInterface.IID) && itemID == ClusterRoutingInterface.removeClusterNode)
			{
				DOFObjectID objectID = (DOFObjectID) parameters.get(0);

				synchronized (nodesInRing)
				{
					if (!nodesInRing.contains(objectID))
					{
						request.respond();
						return;
					}
				}

				logger.debug("cluster node leaving, node id=" + objectID);

				synchronized (futures)
				{
					Future<Void> existingFuture = futures.get(objectID);
					if (existingFuture != null && !existingFuture.isCancelled())
					{
						existingFuture.cancel(true);
					}

					Future<Void> future = config.getExecutor().submit(new RemoveNode(config.getExecutor(), objectID));
					futures.put(objectID, future);
				}

				request.respond();
			}
			// This is a workaround for bug #6982
			// int itemID = method.getItemID();
			// DOFInterfaceID iid = method.getInterfaceID();
			//
			// if (iid.equals(ClusterRoutingInterface.IID) && itemID == ClusterRoutingInterface.removeWorkHash)
			// {
			// logger.trace("removeWorkHash, hash=" + parameters.get(0));
			// DOFBlob blob = (DOFBlob)parameters.get(0);
			// BigInteger hash = ClusterRouting.hash(blob.get());
			// BindingRequest bindingRequest = cluster.getWorkRequest(hash);
			// if (bindingRequest != null)
			// {
			// cluster.removeWorkRequest(bindingRequest);
			// }
			// request.respond();
			// }
		}
	}
}
