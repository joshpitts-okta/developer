package com.pslcl.service.util.dht.cluster.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFUtil;
import org.opendof.core.oal.value.DOFArray;
import org.opendof.core.oal.value.DOFBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.provide.BindingRequest;

/**
 * This class will create and help maintain a routing table.
 * The routing table is made up of a set of routing entries and a mapping of Binding Requests to global IDs. A routing
 * entry is a node's global ID and hash.
 */
public class ClusterRoutingTable
{
	private DOFObjectID globalID;
	private TreeMap<BigInteger, DOFObjectID> myIDs;
	private DOFArray myRoutingEntries;

	private TreeSet<ClusterRoutingEntry> routingEntrySet;
	private HashMap<BindingRequest, List<DOFObjectID>> workMap; // BindingRequest : globalIDs (primary,redundant)

	private static Logger logger = LoggerFactory.getLogger(ClusterRoutingTable.class);

	public ClusterRoutingTable(int vnodes, DOFObjectID baseOID, String guid)
	{
		if (vnodes < 1 || baseOID == null)
			throw new IllegalArgumentException("vnodes < 1 || baseOID == null");

		routingEntrySet = new TreeSet<ClusterRoutingEntry>();
		workMap = new HashMap<BindingRequest, List<DOFObjectID>>();

		if (guid == null || guid.length() == 0)
			createRandomIDs(vnodes, baseOID);
		else
			createNonRandomIDs(vnodes, baseOID, guid);
	}

	/**
	 * Returns the cluster node's global ID which is the base ID with a unique provider attribute
	 * 
	 * @return the global ID
	 */
	public DOFObjectID getGlobalID()
	{
		return globalID;
	}

	/**
	 * Returns a DOF array of blobs, which is an array of this node's hash table entries
	 * 
	 * @return this node's hash entries
	 */
	public DOFArray getMyEntries()
	{
		return myRoutingEntries;
	}

	/**
	 * Add entries to the routing table given the node's global ID and array of entries
	 * 
	 * @param nodeID the node's global ID to add to the table
	 * @param hashArray the node's routing table entries, given as an array of blobs
	 */
	public void addRoutingEntries(DOFObjectID nodeID, DOFArray hashArray)
	{
		for (int i = 0; i < hashArray.size(); i++)
		{
			DOFBlob blob = (DOFBlob) hashArray.get(i);
			BigInteger hash = new BigInteger(1, blob.get());
			addEntry(nodeID, hash);
		}
	}

	/**
	 * Remove references for the given node from the routing table
	 * 
	 * @param globalID the node's global ID to remove from the routing table
	 */
	public void removeRoutingEntry(DOFObjectID globalID)
	{
		synchronized (routingEntrySet)
		{
			TreeSet<BigInteger> hashes = new TreeSet<BigInteger>();
			for (ClusterRoutingEntry e : routingEntrySet)
				if (e.getGlobalID().equals(globalID))
					hashes.add(e.getHash());
			for (BigInteger b : hashes)
				removeEntry(b);
		}
	}

	/**
	 * Determines whether this node should provide for the given Binding Request
	 * 
	 * @param binding the Binding Request that needs a provider
	 * @param replication the number of nodes that will provide for the Binding Request
	 * @return true if this node needs to provide, false otherwise
	 */
	public boolean shouldIProvide(BindingRequest binding, int replication)
	{
		if (binding == null)
			return false;
		List<DOFObjectID> providers = getMappedProviders(binding, replication);
		if (providers == null)
			return false;

		synchronized (workMap)
		{
			workMap.put(binding, providers);
		}

		return providers.contains(globalID);
	}

	/**
	 * Return the set of Binding Requests that belong to the given node
	 * 
	 * @param nodeID the node's global ID
	 * @return the set of Binding Requests that belong to the given node ID
	 */
	public Set<BindingRequest> getWorkRequests(DOFObjectID nodeID)
	{
		logger.trace("getting work requests that belong to " + nodeID);

		Set<BindingRequest> workIDs = new HashSet<BindingRequest>();
		synchronized (workMap)
		{
			for (Map.Entry<BindingRequest, List<DOFObjectID>> e : workMap.entrySet())
			{
				List<DOFObjectID> providers = e.getValue();
				if (providers.contains(nodeID))
					workIDs.add(e.getKey());
			}
		}
		return workIDs;
	}

	/**
	 * Removes a Binding Request from the work map. This is usually called after an activate request is canceled.
	 * 
	 * @param binding the Binding Request to remove from the work map
	 */
	public void removeWorkRequest(BindingRequest binding)
	{
		if (binding == null)
			return;
		logger.trace("Removing binding=" + binding.getObjectID() + ", " + binding.getRemoteDomainID());
		synchronized (workMap)
		{
			workMap.remove(binding);
		}
	}

	/**
	 * Return the set of Binding Requests that have been remapped from this node to providers on other nodes. These Binding Requests have
	 * corresponding provide operations that need to be canceled.
	 * 
	 * @param replication the replication factor
	 * @return the set of Binding Requests that have been remapped
	 */
	public Set<BindingRequest> getRemappedBindings(int replication)
	{
		Map<BindingRequest, List<DOFObjectID>> remappedWork = new HashMap<BindingRequest, List<DOFObjectID>>();
		synchronized (workMap)
		{
			for (Map.Entry<BindingRequest, List<DOFObjectID>> e : workMap.entrySet())
			{
				BindingRequest br = e.getKey();
				List<DOFObjectID> foundProviders = e.getValue();
				List<DOFObjectID> mappedProviders = getMappedProviders(br, replication);
				if (foundProviders.contains(globalID) && !mappedProviders.contains(globalID)) // check if this node no longer needs to provide
					remappedWork.put(br, mappedProviders);

				workMap.put(br, mappedProviders); // keep work map updated
			}
		}
		return remappedWork.keySet();
	}

	private void addEntry(DOFObjectID nodeID, BigInteger hash)
	{
		if (nodeID == null || hash == null)
			throw new IllegalArgumentException("globalID == null || hash == null");
		logger.trace("adding to routing table, id=" + nodeID + " hash=" + hash);

		synchronized (routingEntrySet)
		{
			routingEntrySet.add(new ClusterRoutingEntry(nodeID, hash));
		}
	}

	private void removeEntry(BigInteger hash)
	{
		ClusterRoutingEntry entry = new ClusterRoutingEntry(hash);
		synchronized (routingEntrySet)
		{
			routingEntrySet.remove(entry);
		}
		return;
	}

	private List<DOFObjectID> getMappedProviders(BindingRequest bindingRequest, int replication)
	{
		if (routingEntrySet.size() == 0 || bindingRequest == null)
			return null;

		Set<DOFObjectID> providers = new LinkedHashSet<DOFObjectID>();
		BigInteger hash = hash(bindingRequest);
		ClusterRoutingEntry result = getMappedEntry(hash);
		DOFObjectID providerID = result.globalID;
		providers.add(providerID);

		for (int i = 1; i < replication && i < routingEntrySet.size(); i++)
		{
			hash = result.hash.add(BigInteger.ONE);
			result = getMappedEntry(hash);
			providerID = result.globalID;
			while (providers.contains(providerID))
			{
				hash = result.hash.add(BigInteger.ONE);
				result = getMappedEntry(hash);
				providerID = result.globalID;
			}
			providers.add(providerID);
		}

		List<DOFObjectID> provList = new ArrayList<DOFObjectID>(providers);
		return provList;
	}

	private ClusterRoutingEntry getMappedEntry(BigInteger hash)
	{
		logger.trace("getting mapped entry for hash=" + hash);
		ClusterRoutingEntry entry = new ClusterRoutingEntry(hash);
		ClusterRoutingEntry result = routingEntrySet.ceiling(entry);
		if (result == null && routingEntrySet.size() > 0)
			return routingEntrySet.first();
		return result;
	}

	// BindingRequest getWorkRequest(BigInteger hash)
	// {
	// return this.routingTable.getWorkRequest(hash);
	// }

	private void createNonRandomIDs(int n, DOFObjectID baseOID, String GUID)
	{
		List<DOFBlob> myHashes = new ArrayList<DOFBlob>();
		myIDs = new TreeMap<BigInteger, DOFObjectID>();
		DOFObjectID.Attribute attribute = DOFObjectID.Attribute.create(DOFObjectID.Attribute.PROVIDER, DOFObjectID.create("[128:{" + GUID + "}]"));
		DOFObjectID id = DOFObjectID.create(baseOID, attribute);

		globalID = id;
		BigInteger hash = hash(id);
		myIDs.put(hash, id);
		addEntry(globalID, hash);
		myHashes.add(new DOFBlob(hash.toByteArray()));

		List<BigInteger> hashes = getHashes(id, n - 1);
		for (BigInteger h : hashes)
		{
			myIDs.put(h, id);
			myHashes.add(new DOFBlob(h.toByteArray()));
			addEntry(globalID, h);
		}

		myRoutingEntries = new DOFArray(new DOFArray.Type(new DOFBlob.Type(19, 21), myHashes.size()), myHashes);
	}

	private List<BigInteger> getHashes(DOFObjectID id, int n)
	{
		List<BigInteger> hashes = new ArrayList<BigInteger>();

		byte[] hashedID = DigestUtils.sha1(id.toStandardString()); // ignore the first one
		for (int i = 0; i < n; i++)
		{
			hashedID = DigestUtils.sha1(hashedID);
			hashes.add(new BigInteger(1, hashedID));
		}

		return hashes;
	}

	private void createRandomIDs(int n, DOFObjectID baseOID)
	{
		List<DOFBlob> myHashes = new ArrayList<DOFBlob>();
		DOFObjectID id = getRandomOID(baseOID);
		BigInteger hash = hash(id);
		globalID = id;
		myIDs = new TreeMap<BigInteger, DOFObjectID>();
		myIDs.put(hash, id);
		addEntry(globalID, hash);
		myHashes.add(new DOFBlob(hash.toByteArray()));

		for (int i = 1; i < n; i++)
		{
			id = getRandomOID(baseOID);
			hash = hash(id);
			myIDs.put(hash, id);
			addEntry(globalID, hash);

			myHashes.add(new DOFBlob(hash.toByteArray()));
		}

		myRoutingEntries = new DOFArray(new DOFArray.Type(new DOFBlob.Type(19, 21), myHashes.size()), myHashes);
	}

	private DOFObjectID getRandomOID(DOFObjectID baseOID)
	{
		DOFObjectID.Attribute attribute = DOFObjectID.Attribute.create(DOFObjectID.Attribute.PROVIDER,
				DOFObjectID.create("[128:{" + DOFUtil.bytesToHexString(DOFUtil.createGuid()) + "}]"));
		DOFObjectID id = DOFObjectID.create(baseOID, attribute);

		return id;
	}

	public static BigInteger hash(DOFObjectID id)
	{
		String s = id.toStandardString();
		return hash(s);
	}

	public static BigInteger hash(BindingRequest br)
	{
		String s = br.getObjectID().toStandardString() + br.getRemoteDomainID() + br.getInterfaceID();
		return hash(s);
	}

	public static BigInteger hash(String s)
	{
		byte[] hashedID = DigestUtils.sha1(s);
		return hash(hashedID);
	}

	public static BigInteger hash(byte[] hash)
	{
		return new BigInteger(1, hash);
	}

	/* MBean helpers */
	public int getRoutingTableSize()
	{
		return routingEntrySet.size();
	}

	public int getWorkRequestsSize()
	{
		return workMap.size();
	}

	public String getRoutingTableString()
	{
		return routingEntrySet.toString();
	}

	public String getWorkRequestsString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Map.Entry<BindingRequest, List<DOFObjectID>> e : workMap.entrySet())
			sb.append("\n\t" + e.getKey().getObjectID() + ", " + e.getKey().getRemoteDomainID() + ", " + e.getValue().toString() + ",");
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}

	public String getWorkRequestsString(DOFObjectID nodeID)
	{
		Set<BindingRequest> requests = getWorkRequests(nodeID);
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (BindingRequest request : requests)
		{
			sb.append("\n\t");
			sb.append(request.getObjectID());
			sb.append(",");
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}

	private class ClusterRoutingEntry implements Comparable<Object>
	{
		private DOFObjectID globalID;
		private BigInteger hash;

		public ClusterRoutingEntry(DOFObjectID globalID, BigInteger range)
		{
			this.globalID = globalID;
			this.hash = range;
		}

		// used for a helper method in RoutingTable
		private ClusterRoutingEntry(BigInteger hash)
		{
			this.hash = hash;
		}

		public DOFObjectID getGlobalID()
		{
			return this.globalID;
		}

		public BigInteger getHash()
		{
			return this.hash;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((hash == null) ? 0 : hash.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClusterRoutingEntry other = (ClusterRoutingEntry) obj;
			if (hash == null)
			{
				if (other.hash != null)
					return false;
			} else if (!hash.equals(other.hash))
				return false;

			return true;
		}

		@Override
		public String toString()
		{
			return "\n\t[globalID=" + globalID + ", hash=" + hash + "]";
		}

		@Override
		public int compareTo(Object obj)
		{
			ClusterRoutingEntry other = (ClusterRoutingEntry) obj;
			return this.hash.compareTo(other.hash);
		}
	}
}