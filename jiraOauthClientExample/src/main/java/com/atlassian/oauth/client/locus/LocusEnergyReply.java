/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package org.opendof.tools.repository.interfaces.servlet.auth.jira.locus;

import com.google.gson.annotations.Expose;

@SuppressWarnings("javadoc")
public class LocusEnergyReply
{
    @Expose
    private String message;
    @Expose
    private int statusCode;
    
    public String getMessage()
    {
        return message;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    @Override
    public String toString()
    {
        return "message: " + message + "; statusCode: " + statusCode;
    }
}
