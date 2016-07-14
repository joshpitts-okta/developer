/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;


@SuppressWarnings("javadoc")
public class JmxBase implements NotificationListener
{
    private static final String JMX_CREDENTIALS = "jmx.remote.credentials";
    
    private JMXConnector jmxc;
    private MBeanServerConnection mbsc;
    private ConnectTask connectTask;
    private final AtomicBoolean jmxConnectionLost;
    
    /**
     * Instantiate a JmxBase instance.
     */
    public JmxBase()
    {
        jmxConnectionLost = new AtomicBoolean(false);
    }
    
    /**
     * Connect to a JMX server.
     * 
     * @param url the JMX formated URL to connect to.  May not be null.
     * @param user the authentication user for the connection.  May be null, in which case no authentication is done.
     * @param password the authentication password associated with the user for the connection.  May be null.
     * @param timeout the amount of time to wait for the connection.
     * @throws Exception if the connection could not be established
     */
    public void connect(String url, String user, String password, int timeout) throws Exception
    {
        if(url == null)
            throw new IllegalArgumentException("url == null");
        if(user != null && password == null)
            throw new IllegalArgumentException("password == null");
        
        connectTask = new ConnectTask(url, user, password, timeout);
        connectTask.waitForComplete();
        connectTask = null;
    }

    /**
     * Disconnect from the JMX server.
     * @throws IOException if the connection does not close cleanly.
     */
    public void disconnect() throws IOException 
    {
        if(connectTask != null)
        {
            connectTask.cancel();
            connectTask = null;
        }
        if(jmxc!=null)
        {
            jmxc.close();
            jmxc = null;
        }
    }

    /**
     * Best try disconnect
     */
    public void close()
    {
        try
        {
            disconnect();
        }catch(Exception e)
        {
        }
    }
    
    /**
     * Return the attribute object associated with the given object name.
     * @param objectName the object name, in valid JMX <code>ObjectName</code> format to obtain the attribute from.
     * @param attributeName the attribute of the given object name to be returned.
     * @return the attribute object.
     * @throws Exception if the attribute object can not be obtained.
     */
    public Object getAttribute(String objectName, String attributeName) throws Exception
    {
        ObjectName objName = new ObjectName(objectName);
        return mbsc.getAttribute(objName, attributeName);
    }

    /**
     * Return the attribute object associated with the given object name.
     * @param objectName the object name to obtain the attribute from.
     * @param attributeName the attribute of the given object name to be returned.
     * @return the attribute object.
     * @throws Exception if the attribute object can not be obtained.
     */
    public Object getAttribute(ObjectName objectName, String attributeName) throws Exception
    {
        return mbsc.getAttribute(objectName, attributeName);
    }

    /**
     * Return an array of MBean objects for a given object name.
     * @param objectName the object name, in valid JMX <code>ObjectName</code> format to obtain the array for.
     * @return an array of MBean objects.
     * @throws Exception if the array can not be obtained.
     */
    public ObjectInstance[] getMBeans(String objectName) throws Exception
    {
        ObjectName objName = new ObjectName(objectName);
        Set<ObjectInstance> mbeans = mbsc.queryMBeans(objName, null);
        ObjectInstance[] objects = new ObjectInstance[mbeans.size()];
        mbeans.toArray(objects);
        return objects;
    }
    
    /**
     * Return an array of MBean objects for a given domain and set of key filters.
     * @param domain the JMX domain to apply the filters to.
     * @param userKeySet map of filters used to obtain the array.
     * @return an array of MBean objects.
     * @throws Exception if the array can not be obtained.
     */
    public ObjectInstance[] getMBeans(String domain, Hashtable<String, String> userKeySet) throws Exception
    {
        StringBuilder keyList = new StringBuilder(domain).append(":");
        if(userKeySet != null)
        {
            for( String userKey: userKeySet.keySet() )
            {
                String userValue = userKeySet.get(userKey);
                keyList.append(userKey).append("=").append(userValue).append(",");
            }
        }
        keyList.append("*");
        return getMBeans(keyList.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleNotification(Notification notification, Object handback) 
    {
        if(jmxConnectionLost.get())
            return; // already handled
        String type = notification.getType();
        if("jmx.remote.connection.failed".equals(type))
        {
            jmxConnectionLost.set(true);
            try
            {
                disconnect();
            } catch (IOException e)
            {
                // best try
            }
        }
    }
    
    private class ConnectTask extends Thread
    {
        private final TimerHelper timer;
        private final String url;
        private final String user;
        private final String password;
        private final int timeout;
        private final AtomicBoolean monitor;
        private final AtomicBoolean error;
        
        ConnectTask(String url, String user, String password, int timeout)
        {
            this.url = url;
            this.user = user;
            this.password = password;
            this.timeout = timeout;
            this.monitor = new AtomicBoolean(false);
            this.error = new AtomicBoolean(false);
            timer = new TimerHelper(monitor, error);
            TimerHelper.scheduleTask(timer, timeout);
            start();
        }
        
        void cancel()
        {
            timer.cancel();
            synchronized(monitor)
            {
                monitor.notifyAll();
            }
        }
        
        void waitForComplete() throws Exception
        {
            synchronized(monitor)
            {
                if(!monitor.get())
                {
                    try
                    {
                        monitor.wait();
                    } catch (InterruptedException e)
                    {
                        throw new Exception("unexpected wakeup waiting for connection: " + url);
                    }
                    if(error.get())
                    {
                        throw new Exception(timeout + " timeout for connection: " + url);
                    }
                }
            }
        }
        
        @Override
        public void run()
        {
            boolean ok = true;
            try
            {
                 JMXServiceURL jmxUrl = new JMXServiceURL(url);
                 if(user != null)
                 {
                     Hashtable<String, String[]> clientEnv = new Hashtable<String, String[]>();
                     String[] cred = new String[]{user, password};
                     clientEnv.put(JMX_CREDENTIALS, cred);
                     jmxc = JMXConnectorFactory.connect(jmxUrl, clientEnv);
                 }else
                     jmxc = JMXConnectorFactory.connect(jmxUrl);
                 mbsc = jmxc.getMBeanServerConnection();
//                 mbsc.addNotificationListener(null, JmxBase.this, null, null);
            }catch(Exception e)
            {
                ok = false;
            }
            finally
            {
                timer.cancel();
                synchronized(monitor)
                {
                    monitor.set(true);
                    error.set(!ok);
                    monitor.notifyAll();
                }
            }
        }
    }
}
