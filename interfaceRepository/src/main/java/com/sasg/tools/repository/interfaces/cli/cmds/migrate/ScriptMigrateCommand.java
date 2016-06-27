package com.sasg.tools.repository.interfaces.cli.cmds.migrate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.opendof.tools.repository.interfaces.cli.util.CliBase;
import org.opendof.tools.repository.interfaces.cli.util.CliCommand;
import org.opendof.tools.repository.interfaces.cli.util.StrH;


@SuppressWarnings("javadoc")
public class ScriptMigrateCommand extends MigrateCommand
{
    // c, h, l, v used

    public static final String SvnShortCl = "s";
    public static final String SvnLongCl = "svn-path";
    public static final String AlloctShortCl = "a";
    public static final String AllocLongCl = "alloc-file";

    private volatile ScriptCmdData scriptCmdData;

    public ScriptMigrateCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    public ScriptCmdData getScriptCmdData()
    {
        return scriptCmdData;
    }

    @Override
    protected void cliSetup()
    {
        super.cliSetup();
        //@formatter:off
        options.addOption(
            Option.builder(SvnShortCl)
                .desc("File Path to SVN parent export folder. This folder is expected to have the working and publish SVN exports. Required")
                .longOpt(SvnLongCl)
                .hasArg()
                .required()
                .build());
        options.addOption(
            Option.builder(AlloctShortCl)
                .desc("The output file path for the generated script.  Required")
                .longOpt(AllocLongCl)
                .hasArg()
                .required()
                .build());
        //@formatter:on
    }

    @Override
    protected void customInit()
    {
        String svnPath = null;
        String scriptPath = null;
        
        StringBuilder initsb = cliBase.getInitsb();
        if (commandline.hasOption(SvnShortCl))
        {
            svnPath = commandline.getOptionValue(SvnShortCl);
            StrH.ttl(initsb, 1, "--", SvnLongCl, " = " + svnPath);
        }
        if (commandline.hasOption(AlloctShortCl))
        {
            scriptPath = commandline.getOptionValue(AlloctShortCl);
            StrH.ttl(initsb, 1, "--", AllocLongCl, " = " + scriptPath);
        }

        scriptCmdData = new ScriptCmdData(svnPath, scriptPath);
    }

    public class ScriptCmdData
    {
        public final String svnPath;
        public final String scriptPath;
        
        public ScriptCmdData(String svnPath, String scripPath)
        {
            String path = svnPath;
            if(path.endsWith("/"))
                path = path.substring(0, path.length() - 2);
            this.svnPath = path;
            this.scriptPath = scripPath;
        }

        @SuppressWarnings("unused")
        public String validate(CommandLine commandline)
        {
            return null;
        }
    }
}