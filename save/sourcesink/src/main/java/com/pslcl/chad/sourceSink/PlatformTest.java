package com.pslcl.chad.sourceSink;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformTest implements InitListener{

	private static final String domain = "domain";
	private static final String user = "user";
	private static final String password = "pass";
	private static final String host = "host";
	private static final String port = "port";
	private static final String provider = "provider";
	private static final String path = "path";
	private static final String credSelect = "credselect";
//	private static final String strTrue = "true";
//	private static final String strFalse = "false";
	
	private Preferences prefs = Preferences.userRoot();
	private JFrame frame;
	
	private JTextField txtPassword;
	private JTextField txtHost;
	private JTextField txtBrowse;
	private JTextField txtPort;
	private JTextField txtUser;
	private JTextField txtID;
	private JTextField txtProviderID;
	private JTextArea txtMessage;
	
	private JRadioButton rdbtnManager;
	private JRadioButton rdbtnSource;
	private JRadioButton rdbtnSink;

	private JButton btnStop;
	private JButton btnDisconnect;
	private JButton btnStart;
	private JButton btnConnect;
	
	private boolean connected = false;
	private DOFSystem system = null;
	private ServiceConnectionManager serviceConnection = null;
	private ArrayList<DataTransferManagerImpl> managers;
	private ArrayList<DataTransferSourceImpl> sources;
	private ArrayList<DataTransferSinkImpl> sinks;
	private Logger logger;
	private JTextField txtDomain;
	private JLabel lblDelay;
	private JTextField txtDelay;
	private JComboBox<DataTransferImpl> comboBox;
	
	private Requestor requestor = null;
	private JCheckBox chkbxUseCred;
	private JButton btnBrowse;
	private JLabel lblUser;
	private JLabel lblPassword;
	private JLabel lblHost;
	private JLabel lblDomain;
	private JLabel lblBrowse;
	
	private File credFile = null;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
            public void run() {
				try {
					PlatformTest window = new PlatformTest();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public PlatformTest() {
		initialize();
		populateTxtFields();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		managers = new ArrayList<DataTransferManagerImpl>();
		sources = new ArrayList<DataTransferSourceImpl>();
		sinks = new ArrayList<DataTransferSinkImpl>();
		
		logger = LoggerFactory.getLogger(this.getClass());
		
		frame = new JFrame();
		frame.setBounds(100, 100, 457, 488);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		txtUser = new JTextField();
		txtUser.setBounds(60, 52, 196, 20);
		frame.getContentPane().add(txtUser);
		txtUser.setColumns(10);
		
		txtPassword = new JTextField();
		txtPassword.setBounds(338, 52, 86, 20);
		frame.getContentPane().add(txtPassword);
		txtPassword.setColumns(10);
		
		txtHost = new JTextField();
		txtHost.setBounds(60, 77, 196, 20);
		frame.getContentPane().add(txtHost);
		txtHost.setColumns(10);
		
		txtBrowse = new JTextField();
		txtBrowse.setVisible(false);
		txtBrowse.setBounds(59, 21, 196, 20);
		frame.getContentPane().add(txtBrowse);
		txtBrowse.setColumns(10);
		
		txtPort = new JTextField();
		txtPort.setBounds(338, 77, 86, 20);
		frame.getContentPane().add(txtPort);
		txtPort.setColumns(10);
		
		lblUser = new JLabel("User:");
		lblUser.setBounds(10, 55, 46, 14);
		frame.getContentPane().add(lblUser);
		
		lblPassword = new JLabel("Password:");
		lblPassword.setBounds(266, 55, 62, 14);
		frame.getContentPane().add(lblPassword);
		
		lblHost = new JLabel("Host:");
		lblHost.setBounds(10, 80, 46, 14);
		frame.getContentPane().add(lblHost);
		
		JLabel lblPort = new JLabel("Port:");
		lblPort.setBounds(266, 80, 46, 14);
		frame.getContentPane().add(lblPort);
		
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent arg0) {
				connect();
			}
		});
		btnConnect.setBounds(10, 108, 89, 23);
		frame.getContentPane().add(btnConnect);
		
		btnStop = new JButton("Stop");
		btnStop.setEnabled(false);
		btnStop.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		btnStop.setBounds(342, 196, 89, 23);
		frame.getContentPane().add(btnStop);
		
		txtID = new JTextField();
		txtID.setBounds(70, 270, 354, 20);
		txtID.setText("SOL03-15618-00050");
		frame.getContentPane().add(txtID);
		txtID.setColumns(10);
		
		JLabel lblId = new JLabel("ID:");
		lblId.setBounds(10, 273, 46, 14);
		frame.getContentPane().add(lblId);
		
		txtMessage = new JTextArea();
		txtMessage.setText("This is a test of the aws sns system." + new Date());
		txtMessage.setLineWrap(true);
		txtMessage.setBounds(70, 301, 354, 107);
		frame.getContentPane().add(txtMessage);
		
		JLabel lblSubject = new JLabel("Subject:");
		lblSubject.setBounds(10, 306, 67, 14);
		frame.getContentPane().add(lblSubject);
		
		JButton btnSendMessage = new JButton("Send Message");
		btnSendMessage.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent arg0) {
				sendMessage();
			}
		});
		btnSendMessage.setBounds(306, 419, 118, 23);
		frame.getContentPane().add(btnSendMessage);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		rdbtnManager = new JRadioButton("Manager");
		rdbtnManager.setBounds(10, 138, 109, 23);
		frame.getContentPane().add(rdbtnManager);
		
		rdbtnSource = new JRadioButton("Source");
		rdbtnSource.setBounds(118, 138, 109, 23);
		frame.getContentPane().add(rdbtnSource);
		
		rdbtnSink = new JRadioButton("Sink");
		rdbtnSink.setBounds(229, 138, 62, 23);
		frame.getContentPane().add(rdbtnSink);
		
		buttonGroup.add(rdbtnManager);
		buttonGroup.add(rdbtnSink);
		buttonGroup.add(rdbtnSource);
		
		JLabel lblTestNotification = new JLabel("Test Notification:");
		lblTestNotification.setBounds(10, 245, 156, 14);
		frame.getContentPane().add(lblTestNotification);
		
		JLabel lblConnection = new JLabel("Connection:");
		lblConnection.setBounds(10, 1, 89, 14);
		frame.getContentPane().add(lblConnection);
		
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});
		btnDisconnect.setEnabled(false);
		btnDisconnect.setBounds(109, 108, 109, 23);
		frame.getContentPane().add(btnDisconnect);
		
		btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent arg0) {
				start();
			}
		});
		btnStart.setEnabled(false);
		btnStart.setBounds(243, 196, 89, 23);
		frame.getContentPane().add(btnStart);
		
		JLabel lblProviderId = new JLabel("Provider ID:");
		lblProviderId.setBounds(10, 168, 77, 14);
		frame.getContentPane().add(lblProviderId);
		
		txtProviderID = new JTextField();
		txtProviderID.setText("[3:dave.manager@battery.pewla.com]");
		txtProviderID.setBounds(80, 165, 206, 20);
		frame.getContentPane().add(txtProviderID);
		txtProviderID.setColumns(10);
		
		lblDomain = new JLabel("Domain:");
		lblDomain.setBounds(10, 26, 46, 14);
		frame.getContentPane().add(lblDomain);
		
		lblBrowse = new JLabel("File:");
		lblBrowse.setVisible(false);
		lblBrowse.setBounds(10, 26, 46, 14);
		frame.getContentPane().add(lblBrowse);
		
		txtDomain = new JTextField();
		txtDomain.setText("[3:david.ethington@us.panasonic.com]");
		txtDomain.setBounds(59, 21, 196, 20);
		frame.getContentPane().add(txtDomain);
		txtDomain.setColumns(10);
		
		lblDelay = new JLabel("Delay:");
		lblDelay.setBounds(306, 142, 46, 14);
		frame.getContentPane().add(lblDelay);
		
		txtDelay = new JTextField();
		txtDelay.setBounds(302, 165, 86, 20);
		frame.getContentPane().add(txtDelay);
		txtDelay.setColumns(10);
		
		comboBox = new JComboBox<DataTransferImpl>();
		comboBox.setEditable(true);
		comboBox.setBounds(35, 197, 198, 20);
		frame.getContentPane().add(comboBox);
		
		JButton btnRequestData = new JButton("Request Data");
		btnRequestData.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent arg0) {
				requestData();
			}
		});
		btnRequestData.setBounds(243, 230, 118, 23);
		frame.getContentPane().add(btnRequestData);
		
		chkbxUseCred = new JCheckBox("Use cred file");
		chkbxUseCred.addItemListener(new ItemListener(){
			@Override
            public void itemStateChanged(ItemEvent arg0) {
	            switchDisplay(chkbxUseCred.isSelected());	            
            }			
		});
		chkbxUseCred.setBounds(338, 17, 97, 23);
		frame.getContentPane().add(chkbxUseCred);
		
		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser c = new JFileChooser();
				int returnVal = c.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            credFile = c.getSelectedFile();
		            txtBrowse.setText(credFile.getAbsolutePath());
		        } else {
		        }
			}
		});
		btnBrowse.setBounds(167, 45, 89, 23);
		btnBrowse.setVisible(false);
		frame.getContentPane().add(btnBrowse);
		
		requestor = new Requestor();
		Thread t = new Thread(requestor);
		t.start();
	}
	
	protected void requestData() {
		requestor.request();
	}

	protected void start() {
		saveSettings();
		DataTransferImpl impl = null;
		if(rdbtnManager.isSelected()){
			impl = startManager();			
		}
		if(rdbtnSource.isSelected()){
			impl = startSource();
		}
		if(rdbtnSink.isSelected()){
			impl = startSink();
		}
		comboBox.addItem(impl);
	}
	
	protected void stop(){
//		if(rdbtnManager.isSelected()){
//			for(DataTransferManagerImpl manager : managers){
//				if(manager != null){
//					manager.disconnect();
//				}
//			}
//			managers.clear();
//		}
//		if(rdbtnSource.isSelected()){
//			for(DataTransferSourceImpl source : sources){
//				if(source != null){
//					source.close();
//				}
//			}
//			sources.clear();
//		}
//		if(rdbtnSink.isSelected()){
//			for(DataTransferSinkImpl sink : sinks){
//				if(sink != null){
//					sink.close();
//				}
//			}
//			sinks.clear();
//		}
		if(comboBox.getItemCount() == 0) return;
		DataTransferImpl impl = comboBox.getItemAt(comboBox.getSelectedIndex());
		if(impl == null) return;
		impl.close();
		switch(impl.getDataTransferType()){
			case SINK:
				sinks.remove(impl);
				break;
			case SOURCE:
				sources.remove(impl);
				break;
			case MANAGER:
				managers.remove(impl);
				break;
            default:
                break;
		}
		comboBox.removeItem(impl);
	}

	protected void sendMessage() {
	    DOFObjectID id = DOFObjectID.create(txtID.getText());
	    if(connected){
		    for(DataTransferManagerImpl manager : managers){
				if(manager!=null){
					manager.sendNotification(id, txtMessage.getText());
				}
		    }
	    }
	}

	protected void disconnect() {
		
		for(DataTransferManagerImpl manager : managers){
			if(manager != null){
				manager.disconnect();
			}
		}
		managers.clear();
		for(DataTransferSourceImpl source : sources){
			if(source != null){
				source.close();
			}
		}
		sources.clear();
		for(DataTransferSinkImpl sink : sinks){
			if(sink != null){
				sink.close();
			}
		}
		sinks.clear();
		for(int x = 0; x < comboBox.getItemCount(); x++){
			comboBox.removeItemAt(x);
		}
		serviceConnection.shutdown();
		connected = false;
		switchButtons();
	}

	protected void connect() {
		saveSettings();
		DOFObjectID userID;
		String user = txtUser.getText();
		if(serviceConnection != null && serviceConnection.isConnected()){
			connected = true;
			system = serviceConnection.getPlatform();
			switchButtons();
			return;
		}
		
		if(!user.contains(":")){
			System.out.println("Please put user in DOFObjectID format.");
			return;
		}
		
		userID = DOFObjectID.create(user);
		if(!connected){
			Authentication authID = DOFObjectID.Authentication.create(userID);
    		DOFObjectID.Domain domainID = DOFObjectID.Domain.create(txtDomain.getText());
    		DOFCredentials credentials;
    		if(chkbxUseCred.isSelected()){
    			try {
	                credentials = DOFCredentials.create(txtBrowse.getText());
                } catch (Exception e) {
	                txtMessage.setText("Exception reading credfile: " + e.getMessage());
	                return;
                }
    		}else{
	    		if(txtPassword.getText().length() != 64){
	    			credentials = DOFCredentials.Password.create(domainID, authID, txtPassword.getText());
	    		}else{
	    			credentials = DOFCredentials.Key.create(domainID, authID, DOFUtil.hexStringToBytes(txtPassword.getText()));
	    		}
    		}
    		String host = txtHost.getText() + ":" + txtPort.getText();
    		try {
				serviceConnection = new ServiceConnectionManager(credentials, host, this);
				Thread t = new Thread(serviceConnection);
				t.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void switchDisplay(boolean useCred){
		txtPassword.setVisible(!useCred);
		txtUser.setVisible(!useCred);
		txtDomain.setVisible(!useCred);
		lblUser.setVisible(!useCred);
		lblPassword.setVisible(!useCred);
		lblDomain.setVisible(!useCred);
		
		btnBrowse.setVisible(useCred);
		txtBrowse.setVisible(useCred);
		lblBrowse.setVisible(useCred);
	}
	
	private DataTransferImpl startSink() {
		DOFObjectID providerID;
		String id = txtProviderID.getText();
		int delay = 0;
		if(txtDelay.getText().length() > 0){
			try{
				delay = Integer.parseInt(txtDelay.getText());
			}catch(Exception e){}
		}
		if(!id.contains(":")){
			System.out.println("ProviderID needs to be in DOFObjectID format");
			return null;
		}
		providerID = DOFObjectID.create(id);
		try {
			DataTransferSinkImpl sink = new DataTransferSinkImpl(system, providerID);
			sink.setDelay(delay);
			Thread t = new Thread(sink);
			logger.debug("Before: " + new Date().toString());
			t.start();
			logger.debug("After: " + new Date().toString());
			sinks.add(sink);
			return sink;
		} catch (Exception e) {
			System.out.println("sink not initialized.");
			e.printStackTrace();
		}
		return null;
	}

	private DataTransferImpl startSource() {
		DOFObjectID providerID;
		String id = txtProviderID.getText();
		if(!id.contains(":")){
			System.out.println("ProviderID needs to be in DOFObjectID format");
			return null;
		}
		providerID = DOFObjectID.create(id);
		try {
			DataTransferSourceImpl source = new DataTransferSourceImpl(system, providerID);
			Thread t = new Thread(source);
			t.start();
			sources.add(source);
			return source;
		} catch (Exception e) {
			System.out.println("source not initialized.");
			e.printStackTrace();
		}
		return null;
	}

	private DataTransferImpl startManager() {
		DOFObjectID providerID;
		String id = txtProviderID.getText();
		if(!id.contains(":")){
			System.out.println("ProviderID needs to be in DOFObjectID format");
			return null;
		}
		providerID = DOFObjectID.create(id);
		try {
			DataTransferManagerImpl manager = new DataTransferManagerImpl(system, providerID);
			Thread t = new Thread(manager);
			t.start();
			managers.add(manager);
			return manager;
		} catch (Exception e) {
			System.out.println("manager not initialized.");
			e.printStackTrace();
		}
		return null;
	}

	private void saveSettings(){
		prefs.put(domain, txtDomain.getText());
		prefs.put(user, txtUser.getText());
		prefs.put(password, txtPassword.getText());
		prefs.put(host, txtHost.getText());
		prefs.put(port, txtPort.getText());
		prefs.put(provider, txtProviderID.getText());
		prefs.put(path, txtBrowse.getText());
		prefs.putBoolean(credSelect, chkbxUseCred.isSelected());
	}
	
	protected void populateTxtFields() {
		String strDomain = prefs.get(domain, "[6:test.pewla.com]");
		String strUser = prefs.get(user, "[3:manager@test.pewla.com]");
		String strPass = prefs.get(password, "manager");
		String strHost = prefs.get(host, "pdsp-solutions.pewla.com");
		String strPort = prefs.get(port, "3567");
		String strProvider = prefs.get(provider, "[3:manager@test.pewla.com]");
		String strPath = prefs.get(path, "c:\\creds\\test.cred");
		boolean usingCred = prefs.getBoolean(credSelect, false);
		
		txtDomain.setText(strDomain);
		txtBrowse.setText(strPath);
		txtUser.setText(strUser);
		txtPassword.setText(strPass);
		txtHost.setText(strHost);
		txtPort.setText(strPort);
		txtProviderID.setText(strProvider);
		chkbxUseCred.setSelected(usingCred);
		switchDisplay(usingCred);
	}
	
	private void switchButtons() {
		if(connected){
			btnConnect.setEnabled(false);
			btnDisconnect.setEnabled(true);
			btnStart.setEnabled(true);
			btnStop.setEnabled(true);
		}else{
			btnConnect.setEnabled(true);
			btnDisconnect.setEnabled(false);
			btnStart.setEnabled(false);
			btnStop.setEnabled(false);
		}
		
	}

	@Override
	public boolean isInitialized() {
		connected = true;
		switchButtons();
		system = serviceConnection.getPlatform();
		return true;
	}

	@Override
	public void lostConnection() {
		connected = false;
		switchButtons();
	}

	@Override
	public void reconnected() {
		connected = true;
		switchButtons();	
	}
	
	enum APIType { SINK, SOURCE, MANAGER }
	
//	private class DatatransferAPIObj {
//		APIType type;
//		
//	}
	
	private class Requestor implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
		public void request(){
			DataTransferSinkImpl sink = sinks.get(0);
			sink.requestData();
		}
	}
}
