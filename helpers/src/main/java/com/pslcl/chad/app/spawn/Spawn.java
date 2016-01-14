/*
 * $Source: /cvs/pcc/src/com/neovest/api/jndi/ctx/src/com/ccc/utility/Spawn.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/05/04 18:17:55 $
 * $Author: WORKGROUP-cadams $
 *
 * Copyright Cascade Computer Consulting, Inc. 2006
 */

package com.pslcl.chad.app.spawn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("javadoc")
public class Spawn
{
	public static final int ProcessWaitForError = 10000;
	public static final int RuntimeExecError = 10001;
	public static final int GetCompletionCodeCalledBeforeFinishError = 10002;
	public static final int WriteOutputStreamError = 10003;
	public static final int UnexpectedWakeupStreamReadDelayError = 10004;
	public static final int StreamReadIoExceptionError = 10005;
	
	protected static final long StreamReaderDelay = 10;
	private int ccode;
	private BufferedWriter outputStream;
	private BufferedReader inputStream;
	private BufferedReader errorStream;
	private Vector<String> outputData;
	private Vector<String> inputData;
	private Vector<String> errorData;
	private Process process;
	private final Runtime runtime;
	private final String commandLine;
	private final String[] envp;
	private boolean blocking;
	private final SpawnListener listener;
	private SpawnException spawnException;
	private MonitorProcess monitor;
	private HandleStreamsProcess streamHandler;
	private final AtomicBoolean executing;

	public Spawn( String commandLine)// throws IOException, SpawnException
	{
		this(commandLine, null);
	}
	
	public Spawn( String commandLine, SpawnListener listener)
	{
		this(commandLine, null, listener);
	}
	
	public Spawn( String commandLine, String[] envp, SpawnListener listener)
	{
		// commandLine likely wants to start with "cmd /C "
		this.commandLine = commandLine;
		this.envp = envp;
		this.listener = listener;
		runtime = Runtime.getRuntime();
		if (listener == null)
			blocking = true;
		executing = new AtomicBoolean(false);
	}

	public String getCommandLine()
	{
		return commandLine;
	}
	
	public void destroy()
	{
		executing.set(false);
		process.destroy();
	}

	protected void block() throws SpawnException
	{
		try
		{
			ccode = process.waitFor();
			if (ccode != 0)
			{
				spawnException = new SpawnException(
						commandLine + 
						" terminated with non zero error code " + ccode,
						ccode, 
						getInputData(), getOutputData(), getErrorData());
				throw spawnException;
			}
		}catch(InterruptedException e)
		{
			ccode = ProcessWaitForError;
			spawnException =  new SpawnException("Unexpected wakeup", ccode, getInputData(), getOutputData(), getErrorData(), e);
			throw spawnException;
		}
		finally
		{
			executing.set(false);
			// give streams a chance to finish up
			streamHandler.finishUp();
		}
	}

	public void run() throws SpawnException
	{
		try
		{
			executing.set(true);
			if (envp != null)
				process = runtime.exec(commandLine, envp);
			else
				process = runtime.exec(commandLine);
		}
		catch (IOException e)
		{
			ccode = RuntimeExecError;
			throw new SpawnException("File IO problem on initial spawn", -1, null, null, null, e);
		}
		
		streamHandler = new HandleStreamsProcess();
		streamHandler.setName("MonitorSpawnStreams");
		streamHandler.start();
		if (blocking)
		{
			block();	// block callers thread now
		}
		else
		{
			// fire up a blocking monitor on another thread and return this callers thread now
			monitor = new MonitorProcess();
			monitor.setName("MonitorSpawnCompletion");
			monitor.start();
		}
	}

	// you should call this in your listener to force any exceptions that happened during async spawn
	public int getCompletionCode() throws SpawnException
	{
		if (executing.get())
		{
			ccode = GetCompletionCodeCalledBeforeFinishError;
			throw new SpawnException("Execution not complete when completion code was called for", ccode, getInputData(), getOutputData(), getErrorData());
		}
		
		if (spawnException != null)
			throw spawnException;
		return ccode;
	}

	private String getStringFromVector(Vector<String> data)
	{
		StringBuilder sb = new StringBuilder();
		if (data == null)
			return "<empty>";
		for (String str : data)
		{
			sb.append(str);
		}
		return sb.toString();
	}
	
	public String getInputData()
	{
		return getStringFromVector(inputData);
	}

	public String getErrorData()
	{
		return getStringFromVector(errorData);
	}
	
	public String getOutputData()
	{
		return getStringFromVector(outputData);
	}
	
	public void putOutput(String value) throws SpawnException
	{
		value += "/n";
		try
		{
			outputStream.write(value);
		}
		catch (IOException e)
		{
			ccode = WriteOutputStreamError;
			spawnException = new SpawnException("failed to write to process output stream", ccode, getInputData(), getOutputData(), getErrorData(), e);
			throw spawnException;
		}
		outputData.add(value);
	}
	
  class MonitorProcess extends Thread
  {
  	public MonitorProcess()
  	{
  	}
  	
  	@Override
		public void run()
  	{
  		try
			{
				block();
			}
			catch (SpawnException e1)
			{
				// spawnException is already captured at this point, just report done
				// listener should call back on getCompletionCode to pick it up;
			}
  		listener.spawnComplete(Spawn.this);
  	}
  }
  	
  class HandleStreamsProcess extends Thread
  {
  	private final AtomicBoolean finishUp;
  	private final AtomicBoolean finished;
  	private final AtomicInteger loopCounter;
  	
  	public HandleStreamsProcess()
  	{
  		finishUp = new AtomicBoolean(false);
  		finished = new AtomicBoolean(false);
  		loopCounter = new AtomicInteger(0);
  	}
  	
  	@Override
		public void run()
  	{
			try
			{
	  		inputStream = new BufferedReader(new InputStreamReader(process.getInputStream())); 
	  		outputStream = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
	  		errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
	  		inputData = new Vector<String>();
	  		outputData = new Vector<String>();
	  		errorData = new Vector<String>();
	  		
				while (loopCounter.get() < 3)	// on shut down, three passes to try and make sure it's done
				{
					String line;
					if (inputStream.ready())
					{
						line = inputStream.readLine() + "\n";
						inputData.add(line);
						if (listener != null)
							listener.inputHandler(line);
						loopCounter.set(0);		// 3 loops with no data needed for final exit
					}
					
					if (errorStream.ready())
					{
						line = errorStream.readLine() + "\n";
						errorData.add(line);
						if (listener != null)
							listener.errorHandler(line);
						loopCounter.set(0);		// 3 loops with no data needed for final exit
					}
				
					if (finishUp.get())
					{
						if (loopCounter.incrementAndGet() == 3)
						{
							synchronized (finishUp)
							{
								finishUp.notifyAll();
								finished.set(true);
							}
						}
//						break;
					}
					
					// stay fairly aggressive, but don't eat up all the cpu
					synchronized(this)
					{
						try
						{
							wait(StreamReaderDelay);
						}
						catch (InterruptedException e)
						{
							ccode = UnexpectedWakeupStreamReadDelayError;
							spawnException = new SpawnException("Unexpected wakeup", ccode, getInputData(), getOutputData(), getErrorData(), e);
							return;		// this is bad, quite trying
						}
					}
				}
			}catch (IOException e)
			{
				ccode = StreamReadIoExceptionError;
				spawnException = new SpawnException("Error ready an input stream", ccode, getInputData(), getOutputData(), getErrorData(), e);
				return; // this is bad, quite trying
			}
  	}
  	
  	protected void finishUp()
  	{
  		// don't return until you're pretty sure all streams are done
  		finishUp.set(true);
  		synchronized (finishUp)
  		{
  			try
				{
    			if(!finished.get())
    				finishUp.wait();
				}
				catch (InterruptedException e)
				{
					// not much you can do here, might add logger later
					e.printStackTrace();
				}
  		}
  	}
  }
}
