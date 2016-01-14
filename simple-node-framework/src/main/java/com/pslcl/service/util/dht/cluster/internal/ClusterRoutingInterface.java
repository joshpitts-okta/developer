package com.pslcl.service.util.dht.cluster.internal;

import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.value.DOFArray;
import org.opendof.core.oal.value.DOFBlob;

/**
 * This interface is used for requesting other nodes' Routing Entries, and for notifying other nodes when requestors have canceled their activate
 * requests.
 */
public class ClusterRoutingInterface
{
	private static final int maxVirtualNodes = 1024;
//	private static final int maxRequestors = 1000;

	public static final DOFArray.Type hashArrayType = new DOFArray.Type(new DOFBlob.Type(19, 21), 0, maxVirtualNodes);
//	public static final DOFArray.Type reqIDArrayType = new DOFArray.Type(DOFObjectID.TYPE, 0, maxRequestors);

	public static final int getRoutingEntries = 0;
	public static final int removeClusterNode = 1;
//	public static final int removeWorkHash = 2;

	public static final DOFInterfaceID IID = DOFInterfaceID.create("[63:{01010101}]");
	public static final DOFInterface DEF = new DOFInterface.Builder(IID)
			.addProperty(getRoutingEntries, false, true, hashArrayType)
			.addMethod(removeClusterNode, new DOFType[] { DOFObjectID.TYPE }, new DOFType[] {})
//			.addMethod(removeWorkHash, new DOFType[] { new DOFBlob.Type(19, 21) }, new DOFType[] {})
			.build();
}
