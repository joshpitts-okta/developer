/*
 * $Source: /cvs/pcc/src/com/neovest/api/jndi/ctx/src/com/ccc/utility/SpawnListener.java,v $
 * $Revision: 1.1 $
 * $Date: 2006/09/29 03:54:54 $
 * $Author: WORKGROUP-cadams $
 *
 * Copyright Cascade Computer Consulting, Inc. 2006
 */

package com.pslcl.chad.app.spawn;

@SuppressWarnings("javadoc")
public interface SpawnListener
{
	public void spawnComplete(Spawn spawn);
	public void inputHandler(String value);
	public void errorHandler(String value);
}
