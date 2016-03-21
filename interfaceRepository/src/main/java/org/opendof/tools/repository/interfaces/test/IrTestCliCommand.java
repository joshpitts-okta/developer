package org.opendof.tools.repository.interfaces.test;

import java.util.Properties;

import org.apache.commons.cli.Option;

import com.pslcl.chad.app.cli.CliBase;
import com.pslcl.chad.app.cli.CliCommand;

@SuppressWarnings("javadoc")
public class IrTestCliCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version" 

    public static final String ConvertShortCl = "a";
    public static final String ConvertLongCl = "convert";
    public static final String ListInterfacesShortCl = "d";
    public static final String ListInterfacesLongCl = "list-iface";
    public static final String AddGroupShortCl = "g";
    public static final String AddGroupLongCl = "add-group";
    public static final String MergeShortCl = "m";
    public static final String MergeLongCl = "merge";
    public static final String AddCreatorShortCl = "o";
    public static final String AddCreatorLongCl = "add-creator";
    public static final String SearchShortCl = "s";
    public static final String SearchLongCl = "search";
    public static final String UploadShortCl = "u";
    public static final String UploadLongCl = "upload-xml";
    public static final String AddBasicsShortCl = "z";
    public static final String AddBasicsLongCl = "add-basics";
    
    public final Properties properties;

    public IrTestCliCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
        properties = new Properties();
    }

    public IrTestCliCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
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
            Option.builder(ConvertShortCl)
                .desc("Convert.")
                .longOpt(ConvertLongCl)
                .build());
        
        options.addOption(
            Option.builder(ListInterfacesShortCl)
                .desc("List interfaces.")
                .longOpt(ListInterfacesLongCl)
                .build());
        
        options.addOption(
            Option.builder(AddGroupShortCl)
                .desc("Add group.")
                .longOpt(AddGroupLongCl)
                .build());
        options.addOption(
            Option.builder(MergeShortCl)
                .desc("Merge.")
                .longOpt(MergeLongCl)
                .build());
        
        options.addOption(
            Option.builder(AddCreatorShortCl)
                .desc("Add creator")
                .longOpt(AddCreatorLongCl)
                .build());
        
        options.addOption(
            Option.builder(SearchShortCl)
                .desc("Search")
                .longOpt(SearchLongCl)
                .build());
        
        options.addOption(
            Option.builder(UploadShortCl)
                .desc("Upload")
                .longOpt(UploadLongCl)
                .build());
        
        options.addOption(
            Option.builder(AddBasicsShortCl)
                .desc("Add basics")
                .longOpt(AddBasicsLongCl)
                .build());
        //@formatter:on
    }

    @Override
    protected void customInit()
    {
    }
}