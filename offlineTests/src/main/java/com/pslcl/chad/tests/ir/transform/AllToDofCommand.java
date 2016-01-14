package com.pslcl.chad.tests.ir.transform;

import java.io.File;

import org.apache.commons.cli.Option;

import com.pslcl.chad.app.cli.CliBase;
import com.pslcl.chad.app.cli.CliCommand;

@SuppressWarnings("javadoc")
public class AllToDofCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version" 
    public static final String TransformFileShortCl = "t";
    public static final String TransformFileLongCl = "trans-file";
    
    public volatile File transformFile;

    public AllToDofCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public AllToDofCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    @Override
    protected void cliSetup()
    {
        cliBase.cliReservedSetup(options, cliBase.isLogToFile(), cliBase.isConfigSwitch());
        //@formatter:off
        options.addOption(
            Option.builder(TransformFileShortCl)
                .desc("Transform file path.")
                .longOpt(TransformFileLongCl)
                .hasArgs()
                .valueSeparator(' ')
                .build());
        //@formatter:on
    }

    @Override
    protected void customInit()
    {
        String path = null;
        boolean ok = false;
        if(commandline.hasOption(TransformFileShortCl))
        {
            path = commandline.getOptionValue(TransformFileShortCl);
            transformFile = new File(path);
            ok = true;
            if(!transformFile.exists())
                ok = false;
        }
        if(!ok)
            help(CliBase.ApplicationError, TransformFileLongCl + "=" +path + " not found");
    }
}