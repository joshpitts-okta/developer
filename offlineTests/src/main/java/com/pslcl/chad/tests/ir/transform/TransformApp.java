package com.pslcl.chad.tests.ir.transform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.opendof.tools.repository.interfaces.allseen.transform.AllseenTransform;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.cli.CliBase;
import com.pslcl.chad.app.cli.CliCommand;

@SuppressWarnings("javadoc")
public class TransformApp extends CliBase
{
    private volatile CliCommand command;
    
    public TransformApp(String[] args)
    {
        super(args, null, true, false, DefaultMaxHelpWidth);
        CliCommand transCmd = new TransformCommand(this, Command.Trans.command);
        addCommand(transCmd);
        command = new AllToDofCommand(this, Command.AllToDof.command); 
        transCmd.addChild(command);
        command = validateCommands();
    }
    
    public static String fileToString(File file) throws Exception
    {
        BufferedReader bufferedReader = null;
        StringBuilder sb = new StringBuilder();
        try
        {
            FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null)
                sb.append(line).append("\n");
        }
        finally
        {
            if(bufferedReader != null)
                try{bufferedReader.close();} catch (IOException e)
                {
                    LoggerFactory.getLogger(TransformApp.class).warn("failed to close bufferedReader cleanly", e);
                }
        }
        return sb.toString();
    }
    
    @Override
    public void run()
    {
        try
        {
            AllseenTransform transform = new AllseenTransform();
            transform.doit(fileToString(((AllToDofCommand)command).transformFile));
            log.info("Transform exiting with no exception");
            close(0);
        } catch (Throwable t)
        {
            System.err.println("\n"+getClass().getSimpleName() + " failed: " + t.toString());
            t.printStackTrace();
            close(CliBase.ApplicationError);
        }
    }

    @Override
    public void close(int ccode)
    {
        super.close(ccode);
    }

    public enum Command
    {
        Trans("trans"), AllToDof("atd");
        
        Command(String command)
        {
            this.command = command;
        }
        
        public static Command getCommand(String arg)
        {
            if(arg.equals(Trans.command))
                return Trans;
            if(arg.equals(AllToDof.command))
                return AllToDof;
            return null;
        }
        public final String command;
    }

    public static void main(String args[])
    {
        new TransformApp(args).start();
    }
}
