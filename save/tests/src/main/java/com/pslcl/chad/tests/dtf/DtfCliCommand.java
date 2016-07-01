package com.pslcl.chad.tests.dtf;

import com.pslcl.dtf.core.util.cli.CliBase;
import com.pslcl.dtf.core.util.cli.CliCommand;

@SuppressWarnings("javadoc")
public class DtfCliCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version"
    // 'p', "person"
    
    public DtfCliCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public DtfCliCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
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
}