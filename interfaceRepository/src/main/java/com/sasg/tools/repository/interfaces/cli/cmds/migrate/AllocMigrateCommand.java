package com.sasg.tools.repository.interfaces.cli.cmds.migrate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.opendof.tools.repository.interfaces.cli.util.CliBase;
import org.opendof.tools.repository.interfaces.cli.util.CliCommand;
import org.opendof.tools.repository.interfaces.cli.util.StrH;


@SuppressWarnings("javadoc")
public class AllocMigrateCommand extends MigrateCommand
{
    // c, h, l, v used

    public static final String AllocShortCl = "a";
    public static final String AllocLongCl = "alloc-file";

    private volatile AllocCmdData allocCmdData;

    public AllocMigrateCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    public AllocCmdData getAllocCmdData()
    {
        return allocCmdData;
    }

    @Override
    protected void cliSetup()
    {
        super.cliSetup();
        //@formatter:off
        options.addOption(
            Option.builder(AllocShortCl)
                .desc("Path to the text file containing the list of iids to be allocated. Required")
                .longOpt(AllocLongCl)
                .hasArg()
                .required()
                .build());
        //@formatter:on
    }

    @Override
    protected void customInit()
    {
        String iidFile = null;
        
        StringBuilder initsb = cliBase.getInitsb();
        if (commandline.hasOption(AllocShortCl))
        {
            iidFile = commandline.getOptionValue(AllocShortCl);
            StrH.ttl(initsb, 1, "--", AllocLongCl, " = " + iidFile);
        }
        allocCmdData = new AllocCmdData(iidFile);
    }

    public class AllocCmdData
    {
        public final String iidFile;
        
        public AllocCmdData(String iidFile)
        {
            this.iidFile = iidFile;
        }

        @SuppressWarnings("unused")
        public String validate(CommandLine commandline)
        {
            return null;
        }
    }
}