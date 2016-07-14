/*
 * $Source: /cvs/pcc/src/com/neovest/api/jndi/ctx/src/com/ccc/utility/SpawnException.java,v $
 * $Revision: 1.1 $
 * $Date: 2006/09/29 03:54:54 $
 * $Author: WORKGROUP-cadams $
 *
 * Copyright Cascade Computer Consulting, Inc. 2006
 */

package com.pslcl.chad.app.spawn;

@SuppressWarnings("javadoc")
public class SpawnException extends Exception
{
    private static final long serialVersionUID = 8757489065842361569L;
    private final int ccode;
	private final String inputStream;
	private final String outputStream;
	private final String errorStream;

	public SpawnException(String message, int ccode, String inputStream, String outputStream, String errorStream)
	{
		this(message, ccode, inputStream, outputStream, errorStream, null);
	}
	
	public SpawnException(String message, int ccode, String inputStream, String outputStream, String errorStream, Throwable rootCause )
	{
		super(message, rootCause);
		this.ccode = ccode;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.errorStream = errorStream;
	}

	public String getInputStream()
	{
		return inputStream;
	}

	public String getOutputStream()
	{
		return outputStream;
	}

	public String getErrorStream()
	{
		return errorStream;
	}

	public int getCompletionCode()
	{
		return (ccode);
	}
}