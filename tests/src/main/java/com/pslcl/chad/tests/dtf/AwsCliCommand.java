package com.pslcl.chad.tests.dtf;

import java.io.File;
import java.util.Properties;

import org.apache.commons.cli.Option;

import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.cli.CliBase;
import com.pslcl.dtf.core.util.cli.CliCommand;

@SuppressWarnings("javadoc")
public class AwsCliCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version" 
    public static final String AppConfigShortCl = "a";
    public static final String AppConfigLongCl = "app-config";
    public static final String PersonShortCl = "p";
    public static final String PersonLongCl = "person";
    public static final String PersonDefaultShortCl = "d";
    public static final String PersonDefaultLongCl = "default";
    public static final String CleanupShortCl = "e";
    public static final String CleanupLongCl = "earlyout";
    public static final String DeployShortCl = "g";
    public static final String DeployLongCl = "deploy";
    public static final String MachineShortCl = "m";
    public static final String MachineLongCl = "machine";

    public final Properties properties;

    public AwsCliCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
        properties = new Properties();
    }

    public AwsCliCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
        properties = new Properties();
    }

    @Override
    protected void cliSetup()
    {
        cliBase.cliReservedSetup(options, cliBase.isLogToFile(), cliBase.isConfigSwitch());
        //@formatter:off
        options.addOption(
            Option.builder(AppConfigShortCl)
                .desc("Application configuration (think generator).")
                .longOpt(AppConfigLongCl)
                .hasArgs()
                .valueSeparator(' ')
                .build());
        
        options.addOption(
            Option.builder(PersonShortCl)
                .desc("Do PersonProvider reserve/bind instead of Machine/Network.")
                .longOpt(PersonLongCl)
                .build());
        
        options.addOption(
            Option.builder(CleanupShortCl)
                .desc("Cleanup race condition test.")
                .longOpt(CleanupLongCl)
                .build());
        options.addOption(
            Option.builder(PersonDefaultShortCl)
                .desc("Do PersonProvider reserve/bind instead of Machine/Network, use site defaults.")
                .longOpt(PersonDefaultLongCl)
                .build());
        
        options.addOption(
            Option.builder(DeployShortCl)
                .desc("Deploy to ec2 instances")
                .longOpt(DeployLongCl)
                .build());
        
        options.addOption(
            Option.builder(MachineShortCl)
                .desc("Reserve and Bind ec2 instances")
                .longOpt(MachineLongCl)
                .build());
        //@formatter:on
    }

    @Override
    protected void customInit()
    {
        String path = commandline.getOptionValue(AppConfigShortCl);
        try
        {
            PropertiesFile.loadFile(properties, new File(path));
        } catch (Exception e)
        {
            help(CliBase.ConfigNotFound, AppConfigLongCl + "=" +path + " not found");
        }
    }
}