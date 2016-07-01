package com.pslcl.chad.sourceSink;

import java.util.Date;

import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSystem;
import org.opendof.datatransfer.manager.Manager;
import org.opendof.datatransfer.manager.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DataTransferManagerImpl implements DataTransferImpl{
	private DataTransferType type = DataTransferType.MANAGER;
	private Date initTime;
	private Manager manager;
	private Manager.Config config;
	private NotificationListener listener = new ManagerListener();
	private DOFObjectID instanceID = null;
	
	private Logger log;
	
	public DataTransferManagerImpl(DOFSystem system, DOFObjectID providerID) throws Exception {
		log = LoggerFactory.getLogger(this.getClass().getName());
		initTime = new Date();
        initTime.getTime();
        this.instanceID = providerID;
        config = null;
        try{
        	config = new Manager.Config.Builder(system, providerID, listener)
        				.build();
        }catch(Exception e){
        	System.out.println("Failed to create config");
        	e.printStackTrace();
        	throw e;
        }
	}

	@Override
	public void run() {
		connect();
		
	}
	
	public void disconnect(){
		manager.close();
	}
	
	private void connect() {
		manager = Manager.create(config);
	}
	
	public void sendNotification(DOFObjectID subject, String notice){
		try {
			listener.receiveNotification(manager, DOFObjectID.create("[3:SiteID]"), subject, notice);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class ManagerListener implements NotificationListener{

		@Override
		public void receiveNotification(Manager manager, DOFObjectID siteID, DOFObjectID regarding, String notification)throws Exception {
//			AWSCredentials  creds = new BasicAWSCredentials ("", "");
//			AmazonSNSClient  sns = new AmazonSNSClient (creds);
//			
//			ListTopicsResult topics =  sns.listTopics();
//			ArrayList topicList = (ArrayList) topics.getTopics();
//			Topic topic = (Topic) topicList.get(0);
//			
//			PublishRequest request = new PublishRequest();
//			request.setSubject(siteID.getData() + ": " + regarding.getData());
//			request.setMessage(notification);
//			request.setTopicArn(topic.getTopicArn());
//
//			sns.publish(request);
			log.info(siteID.toStandardString() + ": " + regarding.toStandardString() + ": " + notification);
			
		}
		
	}
	
//	private class ManagerStatusListener implements StatusListener{
//
//		@Override
//		public void statusChanged(StatusLevel severity, Date timestamp, String message, Exception ex) {
//			System.out.println(timestamp.toString() + " " + severity.toString() + ": " + message);
//			if(ex != null){
//				ex.printStackTrace();
//			}
//			
//		}
//	}

	@Override
    public DataTransferType getDataTransferType() {
	    return type;
    }

	@Override
    public void close() {
	    disconnect();
	    
    }	
	
	@Override
    public String toString() {
		String id;
		if(instanceID == null) id = "NULL";
		else id = instanceID.toStandardString();
	    return type.name() + ":" + id + "";
    }
}
