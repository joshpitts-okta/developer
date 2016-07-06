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
package com.ccc.tools.app.serviceUtility.status;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFServer;


/**
 * A <code>DOFServer.StateListener</code> implementation that reflects the 
 * server state in a <code>StatusTracker</code>.
 * </p>
 * The server name will be used as the basis for the subsystem name to be used with the <code>StatusTracker</code>.
 * The user can optionally specify a prefix to be prepended to the server name. Because the status 
 * subsystem name is based on the server name, a single instance of this listener can be added
 * to multiple <code>DOFServer</code>s.
 * </p>
 * If not started, the status will be set to <code>StatusTracker.Status.Warn</code>.
 * If started, the servers's named status will be set to <code>StatusTracker.Status.Ok</code>.
 * When the server is destroyed the named status is removed from the <code>StatusTracker</code>
 * </p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  create the <code>DOFServer</code>.  The System Level can also instantiate this listener and 
 *  add it to the server.  
 * @see StatusTracker
 * @see Service
 */

public class StatusServerStateListener implements DOFServer.StateListener
{
    private volatile StatusTracker statusTracker;
    private volatile String prefixName;
    
    /**
     * Default Constructor.
     */
    public StatusServerStateListener()
    {
    }
    
    /**
     * Initialization method that provides the <code>StatusTracker</code> and optionally a prefix name.
     * If the prefix name is null or zero length, only the connection name will be used.
     * @param statusTracker the <code>StatusTracker</code> to be used.  Must not 
     * be null;
     * @param prefixName the status subsystem prefix to prepend to the connection name.  If null or zero length 
     * the connection name is used.
     * @throws IllegalArgumentException if statusTracker is null.
     */
    public void init(StatusTracker statusTracker, String prefixName)
    {
        if(statusTracker == null)
            throw new IllegalArgumentException("statusTracker == null"); 
        if(prefixName != null && prefixName.length() > 0)
            this.prefixName = prefixName;
        else
            this.prefixName = "";
        this.statusTracker = statusTracker;
   }
    
    
    /* ************************************************************************
     * DOFServer.StateListener implementation
     **************************************************************************/
    
    @Override
    public void removed(DOFServer server, DOFException exception) 
    {
        String name = prefixName + server.getState().getName();
        statusTracker.removeStatus(name);
    }

    @Override
    public void stateChanged(DOFServer server, DOFServer.State state)
    {
        String name = (prefixName != null ? prefixName : "") + server.getState().getName();
        if(state.isStarted())
            statusTracker.setStatus(name, StatusTracker.Status.Ok);
        else
            statusTracker.setStatus(name, StatusTracker.Status.Warn);
    }

}
