package com.pslcl.chad.tests.ir.transform;

import com.pslcl.chad.app.cli.CliBase;
import com.pslcl.chad.app.cli.CliCommand;


@SuppressWarnings("javadoc")
public class TransformCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version"
    
    public TransformCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public TransformCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
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