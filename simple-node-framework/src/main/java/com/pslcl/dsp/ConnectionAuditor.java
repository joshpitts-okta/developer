package com.pslcl.dsp;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFAddress;
import org.opendof.core.oal.DOFAuditListener;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.pslcl.service.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionAuditor implements DOFAuditListener {
	private final Logger logger = LoggerFactory.getLogger(ConnectionAuditor.class);	
	
	private final String name;
	private final int auditPeriod;

    private final SecureRandom random = new SecureRandom();
    private final Set<Long> tickets = new HashSet<Long>();
    private long nextTicket = 0;
	
	/**
	 * Create an auditor using the specified audit period, in milliseconds.
	 * @param auditPeriod
	 */
	public ConnectionAuditor(String name, int auditPeriod) {
		this.name = name;
		this.auditPeriod = auditPeriod;
	}

	@Override
	public int getAuditPeriod() {
		return auditPeriod;
	}
	
	private void report(Long ticket, String updateType, DOFConnection.State connState) {
		final StringBuilder sb = new StringBuilder();
		boolean isDebug = false;
        sb.append(name);
        sb.append(" ");
        if (connState != null)
        	sb.append(connState.getDirection().toString()).append(" ").append(connState.getConnectionType().name());
        sb.append(" DOFConnection ").append(updateType);
        if (ticket != null) {
        	sb.append(" ").append(String.format("%08x",ticket));
        }
        if (connState != null && connState.getDirection() == DOFConnection.Direction.OUTBOUND)
        	sb.append(" '").append(connState.getName()).append("'");
        sb.append(" ");
        if (connState != null) {
	        DOFAddress address = connState.getAddress();
			sb.append((address != null)? address.toString() : "null");
			DOFCredentials peerCredentials = connState.getPeerCredentials();
			sb.append(" [").append((peerCredentials != null)? peerCredentials.toString() : "null");
			final long connectTS = connState.getConnectTime();
			sb.append("]: Start=").append(DateFormatUtils.formatUTC(connectTS));
	        sb.append(", Duration=").append((System.currentTimeMillis() - connectTS)/1000);
	        sb.append("s, ");
			final  DOF.TrafficStats trafficStats = connState.getTrafficStats();
			if (trafficStats != null) {
		        sb.append(trafficStats.getReceiveByteCount());
				sb.append("B (");
				sb.append(trafficStats.getReceivePacketCount());
				sb.append("pkts) in, ");
				sb.append(trafficStats.getSendByteCount());
				sb.append("B (");
				sb.append(trafficStats.getSendPacketCount());
				sb.append("pkts) out");
			} else {
				sb.append("<Traffic Stats Unavailable>");
			}
			Exception exception = connState.getException();
	        if (exception == null && ticket == null) {
	        	exception = new Exception("Connection Failed Without Reporting an Exception");
	        }
			if (exception != null) {
				sb.append(", Exception: ").append(exception.getMessage());
			}
		} else {
			sb.append("<Connection State Unavailable>");
		}
        if (isDebug)
        	logger.debug(sb.toString());
        else
        	logger.info(sb.toString());
	}

	@Override
	public Object connectionOpened(DOFConnection.State connState) throws Exception {
		long ticket;
		synchronized(tickets) {
			ticket = nextTicket++;
			while (!tickets.add(ticket)) {
				ticket = random.nextLong();
			}
		}
		
        report(ticket, "OPENED", connState);
        return ticket;
	}

	@Override
	public void connectionUpdate(Object ticket, DOFConnection.State connState) throws Exception {
        report((Long)ticket, "UPDATE", connState);
	}

	@Override
	public void connectionClosed(Object ticket, DOFConnection.State connState) throws Exception {
        report((Long)ticket, "CLOSED", connState);
        synchronized(tickets) {
        	tickets.remove(ticket);
        }
	}

	@Override
	public void connectionFailed(DOFConnection.State connState) throws Exception {
        report(null, "FAILED", connState);
	}
}
