package com.pslcl.chad.tests.dtf;

import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.daemon.DaemonContext;

import com.pslcl.dtf.core.runner.Runner;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.util.cli.CliBase;
import com.pslcl.dtf.core.util.cli.CliCommand;
import com.pslcl.dtf.core.util.cli.CliDaemonContext;
import com.pslcl.dtf.runner.RunnerService;

@SuppressWarnings("javadoc")
public class DtfCliApp extends CliBase
{
    public static final String PreStartKey = "pslcl.dtf.runner.cli.pre-start-class";
    public static final String PostStartKey = "pslcl.dtf.runner.cli.post-start-class";
    
    private final DaemonContext context;
    private final Runner dtfService;
    private volatile AwsCliCommand awsSwitchCommand;
    
    public DtfCliApp(String[] args)
    {
        super(args, null, true, false, DefaultMaxHelpWidth);
        context = new CliDaemonContext(this);
        ((CliDaemonContext)context).setArguments(args);
        dtfService = new RunnerService();
    }

    private void preStart(RunnerConfig config, Properties appProperties, CommandLine activeCommand) throws Exception
    {
        String value = appProperties.getProperty(PreStartKey);
        if (value == null)
            return;
        PreStartExecuteInterface pre = (PreStartExecuteInterface) Class.forName(value).newInstance();
        pre.execute(config, awsSwitchCommand.properties, activeCommand);
    }
    
    private void postStart(RunnerConfig config) throws Exception
    {
        String value = config.properties.getProperty(PostStartKey);
        if (value == null)
            return;
        PostStartExecuteInterface post = (PostStartExecuteInterface) Class.forName(value).newInstance();
        post.execute(config);
    }
    
    @Override
    public void run()
    {
        try
        {
            CliCommand bindCmd = new BindCliCommand(this, Command.Bind.command);
            addCommand(bindCmd);
            awsSwitchCommand = new AwsCliCommand(this, Command.Aws.command);
            bindCmd.addChild(awsSwitchCommand);
            if(validateCommands() != awsSwitchCommand)
                throw new Throwable("wrong command returned");
            Thread.currentThread().setName(getClass().getSimpleName());
            dtfService.init(context);
            RunnerConfig config = dtfService.getConfig();
            preStart(config, awsSwitchCommand.properties, activeCommand.getCommandLine());
            dtfService.start();
            postStart(config);
            JOptionPane.showConfirmDialog(null, "Any button Exits " + getClass().getSimpleName());
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
        try
        {
            dtfService.stop();
        } catch (Throwable e)
        {
            System.err.println(getClass().getSimpleName() + ".close dtfService.stop failed: " + e.toString());
            e.printStackTrace();
        }
        dtfService.destroy();
        super.close(ccode);
    }

    public enum Command
    {
        Bind("bind"), Aws("aws");
        
        Command(String command)
        {
            this.command = command;
        }
        
        public static Command getCommand(String arg)
        {
            if(arg.equals(Bind.command))
                return Bind;
            if(arg.equals(Aws.command))
                return Aws;
            return null;
        }
        public String command;
    }

    public static void main(String args[])
    {
        new DtfCliApp(args).start();
    }
}
