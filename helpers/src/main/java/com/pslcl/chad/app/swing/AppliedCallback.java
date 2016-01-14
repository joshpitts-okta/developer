/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app.swing;

@SuppressWarnings("javadoc")
public interface AppliedCallback
{
    public void complete(boolean ok, String message, Exception e, Object context);
}
