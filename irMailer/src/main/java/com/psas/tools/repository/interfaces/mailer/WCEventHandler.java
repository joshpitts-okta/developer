package com.psas.tools.repository.interfaces.mailer;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;

public class WCEventHandler implements ISVNEventHandler
{
    @Override
    public void handleEvent(SVNEvent event, double progress)
    {
        SVNEventAction action = event.getAction();

        if (action == SVNEventAction.ADD)
        {
            /*
             * The item is scheduled for addition.
             */
            System.out.println("A     " + event.getURL());
            return;
        } else if (action == SVNEventAction.COPY)
        {
            /*
             * The  item  is  scheduled for addition  with history (copied,  in 
             * other words).
             */
            System.out.println("A  +  " + event.getURL());
            return;
        } else if (action == SVNEventAction.DELETE)
        {
            /*
             * The item is scheduled for deletion. 
             */
            System.out.println("D     " + event.getURL());
            return;
        } else if (action == SVNEventAction.LOCKED)
        {
            /*
             * The item is locked.
             */
            System.out.println("L     " + event.getURL());
            return;
        } else if (action == SVNEventAction.LOCK_FAILED)
        {
            /*
             * Locking operation failed.
             */
            System.out.println("failed to lock    " + event.getURL());
            return;
        }
    }

    @Override
    public void checkCancelled() throws SVNCancelException
    {
    }
}