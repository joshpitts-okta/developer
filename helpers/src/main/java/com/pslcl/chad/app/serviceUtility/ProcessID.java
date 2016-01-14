/**
 * 
 */
package com.pslcl.chad.app.serviceUtility;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Interact with the OS ProcessID, or pid
 */
public class ProcessID {
	
	private ProcessID() {
        /* Prevent construction. */
    }
	
    /**
     * From OS, extract ProcessID (PID) for this app and write it to file 
     * @param filename Full path filename of file to write pid to (not written for null)
     * @return The pid
     * @throws Exception Throw if pid number cannot be written to a file.
     */
    public static int fileWrite(String filename) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String pid = (String) mbs.getAttribute(new ObjectName("java.lang:type=Runtime"), "Name");
        if( pid.indexOf('@') != -1 )
            pid = pid.substring(0, pid.indexOf('@'));
        if (filename != null) {	
        	DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
        	try {
        		out.writeBytes(pid);
        	} finally {
        		out.close();
        	}
        }
    	return Integer.parseInt(pid);
    }	
}