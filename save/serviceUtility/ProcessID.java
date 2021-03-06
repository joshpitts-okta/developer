/*
**  Copyright (c) 2016, Cascade Computer Consulting.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/
package com.ccc.tools.app.serviceUtility;

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