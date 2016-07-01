package com.pslcl.chad.tests.dtf;

import com.pslcl.dtf.core.util.cli.CliBase;
import com.pslcl.dtf.core.util.cli.CliCommand;

@SuppressWarnings("javadoc")
public class BindCliCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version"
    
    public BindCliCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public BindCliCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    @Override
    protected void cliSetup()
    {
    }
    
    @Override
    protected void customInit()
    {
    }
}