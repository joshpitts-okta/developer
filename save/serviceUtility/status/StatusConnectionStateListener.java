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

import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFConnection.State;
import org.opendof.core.oal.DOFException;

/**
 * A <code>DOFConnection.StateListener</code> implementation that reflects the 
 * connection state in a <code>StatusTracker</code>.
 * </p>
 * The connection name will be used as the basis for the subsystem name to be used with the <code>StatusTracker</code>.
 * The user can optionally specify a prefix to be prepended to the connection name.  Because the status 
 * subsystem name is based on the connection name, a single instance of this listener can be added
 * to multiple <code>DOFConnection</code>s.
 * </p>
 * If not connected, the status will be set to <code>StatusTracker.Status.Warn</code>.
 * If connected, the connection's named status will be set to <code>StatusTracker.Status.Ok</code>.
 * When the connection is destroyed the named status is removed from the <code>StatusTracker</code>
 * </p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  create the <code>DOFConnection</code>.  The System Level can also instantiate this listener and 
 *  add it to the connection.  
 * @see StatusTracker
 * @see Service
 */
public class StatusConnectionStateListener implements DOFConnection.StateListener
{
    private volatile StatusTracker statusTracker;
    private volatile String prefixName;
    
    /**
     * Default Constructor.
     */
    public StatusConnectionStateListener()
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
     * DOFConnection.StateListener implementation
     **************************************************************************/
    
    @Override
    public void removed(DOFConnection connection, DOFException exception) 
    {
        String name = (prefixName != null ? prefixName : "") + connection.getState().getName();
        if(statusTracker != null)
            statusTracker.removeStatus(name);
    }

    @Override
    public void stateChanged(DOFConnection connection, State state)
    {
        String name = prefixName + connection.getState().getName();
        if(state.isConnected())
            statusTracker.setStatus(name, StatusTracker.Status.Ok);
        else
            statusTracker.setStatus(name, StatusTracker.Status.Warn);
    }
}
