package com.pslcl.chad.sourceSink;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.SLF4JLogListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAddress;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.State;
import org.opendof.core.oal.DOFConnection.StateListener;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.transport.inet.InetTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceConnectionManager implements Runnable{
	
	private int TIMEOUT = 1000 * 60 * 60;
	
	private DOFAddress dofAddress;
	private DOF dof;
	private ReconnectingStateListener reconnectingListener;
	private DOFConnection connection;
	private DOFCredentials credentials;
	private boolean initialized;
	private StateListener statusListener;
	private InitListener initListener;
	private DOFSystem system = null;
	boolean active = false;

	private Logger logger;
	
	
	public ServiceConnectionManager(DOFCredentials credentials, String host, InitListener initListener) throws Exception {
		if(!host.contains(":")) host = host + ":3567";
		this.dofAddress = InetTransport.createAddress(host);
		this.credentials = credentials;
		this.statusListener = new ConnectionStatusListener();
		this.initListener = initListener;
		this.logger = LoggerFactory.getLogger(this.getClass().getName());
		initialized = false;
		
	}
	
	@Override
	public void run() {
		try {
			connect();
		} catch (Exception e) {
			logger.error("Failed to connect service. " + e.getMessage(), e);
		}
		
	}
	
	public void connect() throws Exception {
    	try{
    		active = true;
        	DOFConnection.Config connectionConfig = new DOFConnection.Config.Builder(DOFConnection.Type.STREAM, dofAddress)
        				 .setCredentials(credentials)
        				 .setSecurityDesire(SecurityDesire.SECURE)
        				 .setMaxReceiveSilence(60*1000)
//         				 .setMaxReceiveSilence(45*1000)
        				 .build();
        	DOF.Config dofConfig = new DOF.Config.Builder().build();
        	dof = new DOF(dofConfig);
        	DOF.Log.addListener(DOF.Log.Level.INFO, new SLF4JLogListener());                       // OK: DEBUG           // OK: INFO
        	DOF.Log.message(DOF.Log.Level.INFO, "DOF SLF4JLogListener up at info level");          // OK: DEBUG, debug    // OK: INFO, info
        	connection = dof.createConnection(connectionConfig);
        	connection.addStateListener(statusListener);
//        	reconnectingListener = new ReconnectingStateListener();
//        	connection.addStateListener(reconnectingListener);
        	connection.connect(60*1000);
        	reconnectingListener = new ReconnectingStateListener();
        	connection.addStateListener(reconnectingListener);

        	createSystem();
        	//if(!new File("/etc/opt/enc/datadistribution/secure/dataservice.cred").exists())
        	//	CredentialsFile.write(credentials, "/etc/opt/enc/datadistribution/secure/dataservice.cred");
        }catch(Exception e){
        	shutdown();
        	throw e;
        }
		
	}

	public DOFSystem getPlatform() {
		if(!connection.isConnected()){
			try {
				connect();
			} catch (Exception e) {
				
			}
		}
		if(system == null){
			try{
				Thread.sleep(400);
			}catch(Exception e){}
		}
		DOFSystem.Config sysConfig = new DOFSystem.Config.Builder().setCredentials(credentials)
				.build();
		if(system == null)
			try {
				return dof.createSystem(sysConfig, TIMEOUT);
			} catch (DOFException e) {

			}
		return system;
	}
	
	private void createSystem(){
		while(!initialized){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		DOFSystem.Config sysConfig = new DOFSystem.Config.Builder().setCredentials(credentials)
				.build();
		try {
			system = dof.createSystem(sysConfig, TIMEOUT);
		} catch (DOFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initListener.isInitialized();
	}
	
	public void shutdown() {
		logger.info("ServiceConnectionManager.shutdown() called");
		if(dof != null){
            dof.destroy();
        }
    	if(reconnectingListener != null)
    	{
    	    reconnectingListener.cancel();
    	    connection.disconnect();
    	    connection.destroy();
    	    
    	}
    	active = false;		
	}
	
	
	private class ConnectionStatusListener implements DOFConnection.StateListener{	

		@Override
		public void removed(DOFConnection connection, DOFException exception) {
			// nothing to do
			
		}

		@Override
		public void stateChanged(DOFConnection connection, State state) {
			logger.debug("Connection state change: " + state);
			if(!initialized){
				if(state.isConnected()){
					initialized = true;
				}
			}else{
				if(!state.isConnected()){
					initListener.lostConnection();
				}else{
					initListener.reconnected();
				}
			}
		}		
	}

	public boolean isActive(){
		return active;
	}
	
	public boolean isConnected() {
		return connection.isConnected();
	}
}
