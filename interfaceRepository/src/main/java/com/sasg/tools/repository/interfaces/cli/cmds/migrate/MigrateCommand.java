package com.sasg.tools.repository.interfaces.cli.cmds.migrate;

import org.opendof.tools.repository.interfaces.cli.util.CliBase;
import org.opendof.tools.repository.interfaces.cli.util.CliCommand;

@SuppressWarnings("javadoc")
public class MigrateCommand extends CliCommand
{
    public MigrateCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public MigrateCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    @Override
    protected void cliSetup()
    {
        cliBase.cliReservedSetup(options, cliBase.isLogToFile(), cliBase.isConfigSwitch());
    }
    
    @Override
    protected void customInit()
    {
    }
    
    public enum Command
    {
        Script("script"), Alloc("alloc");
        
        Command(String command)
        {
            this.command = command;
        }
        
        public static Command getCommand(String arg)
        {
            if(arg.equals(Script.command))
                return Script;
            if(arg.equals(Alloc.command))
                return Alloc;
            return null;
        }
        public String command;
    }
}