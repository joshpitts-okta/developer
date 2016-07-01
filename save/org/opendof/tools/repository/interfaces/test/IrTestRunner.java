package org.opendof.tools.repository.interfaces.test;

import org.apache.commons.cli.CommandLine;
import org.opendof.tools.repository.interfaces.cli.util.CliBase;
import org.opendof.tools.repository.interfaces.cli.util.CliCommand;
import org.opendof.tools.repository.interfaces.core.CoreController;
import org.opendof.tools.repository.interfaces.da.DataAccessor;

@SuppressWarnings("javadoc")
public class IrTestRunner extends CliBase
{
    private final CliCommand command;
    private final CommandLine commandline;
    
    public IrTestRunner(String[] args)
    {
        super(args, CoreController.IrServletConfigKey);
        CliCommand irtrCmd = new IrTestCliCommand(this, null);
        addCommand(irtrCmd);
        command = validateCommands();
        commandline = command.getCommandLine();
    }

//    @Override
//    protected void clToModuleList(CommandLine commandline)
//    {
//        super.clToModuleList(commandline);
//        addToClModuleList(ConvertShortCl, ConvertLongCl);
//        addToClModuleList(ListInterfacesShortCl, ListInterfacesLongCl);
//        addToClModuleList(AddGroupShortCl, AddGroupLongCl);
//        addToClModuleList(MergeShortCl, MergeLongCl);
//        addToClModuleList(UploadShortCl, UploadLongCl);
//        addToClModuleList(SearchShortCl, SearchLongCl);
//        addToClModuleList(UploadShortCl, UploadLongCl);
//    }

//    private void addCreator() throws Exception
//    {
//        // SubmitterRequest submitter = new SubmitterRequest(null, "chad.adams@us.panasonic.com", "Senior Software Engineer");
//        SubmitterRequest submitter = new SubmitterRequest("Chad Adams", Converter.ConverterGroup, "chad.adams@us.panasonic.com", "Senior Software Engineer");
//        RequestData request = new RequestData(null, null, submitter, null);
//        controller.addCreator(request);
//    }
//
//    private void addGroup() throws Exception
//    {
//        SubmitterRequest submitter = new SubmitterRequest("admin", Converter.ConverterGroup, "admin@opendof.org", "Group administrator contact");
//        RequestData request = new RequestData(null, null, submitter, null);
//        controller.addGroup(request);
//    }

    private void ListInterfaces() throws Exception
    {
//    	InterfaceRequest ir = new InterfaceRequest(null, null, null); 
//    	RequestData request = new RequestData(CommandType.ListAll, ir);
//        List<InterfaceData> list = controller.getAllInterfaces(request);
//        for (InterfaceData iface : list)
//            log.info(iface.toString());
//        log.info("look here");
    }
    
//    private void iidVerify() throws Exception
//    {
//    	String stdStr1 = "[1:{01}]";
//    	DOFInterfaceID iid1 = DOFInterfaceID.create(stdStr1);
//    	String stdStr2 = "[01:{1}]";
//    	DOFInterfaceID iid2 = DOFInterfaceID.create(stdStr2);
//    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
//    	if(!iid1.equals(iid2))
//    		throw new Exception("did not like it");
//    	String str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
//    	str = HttpCommandParser.StandardFormToDofUri(iid2.toStandardString());
//
//    	stdStr1 = "%5B"+"1" + "%3A" + "%7B" + "01"+ "%7D" + "%5D";
//    	stdStr2 = "%5B"+"01" + "%3A" + "%7B" + "1"+ "%7D" + "%5Dy";
//
//    	str = HttpCommandParser.fixupIidStandardForm(stdStr1);    	
//    	str = HttpCommandParser.fixupIidStandardForm(stdStr2);    	
//    	
//
//    	stdStr1 = "[1:{01aa}]";
//    	
//    	iid1 = DOFInterfaceID.create(stdStr1);
//    	stdStr2 = "[01:{1aa}]";
//    	iid2 = DOFInterfaceID.create(stdStr2);
//    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
//    	if(!iid1.equals(iid2))
//    		throw new Exception("did not like it");
//    	str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
//    	
//    	stdStr1 = "[1:{01aa22bb}]";
//    	iid1 = DOFInterfaceID.create(stdStr1);
//    	stdStr2 = "[1:{1aa22bb}]";
//    	iid2 = DOFInterfaceID.create(stdStr2);
//    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
//    	if(!iid1.equals(iid2))
//    		throw new Exception("did not like it");
//    	str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
//    	
//    	str = HttpCommandParser.StandardFormToDofUri("source.opendof.org");
//    }

    private static final String ContextRealBaseDefault = "c:/ws2/interface-repository-web/WebContent/";

    private void init() throws Exception 
    {
        properties.setProperty(DataAccessor.NonServletInitKey, "true");
        properties.setProperty(CoreController.ContextRealBaseKey, ContextRealBaseDefault); //TODO: add another command line switch for this
        controller = new CoreController();
    }
    
    private volatile CoreController controller;

    @Override
    public void run()
    {
        try
        {
//            iidVerify();

            init();
            if (commandline.hasOption(IrTestCliCommand.ConvertShortCl))
                new Converter(controller).run(Converter.TestType.Convert, properties);
            else if (commandline.hasOption(IrTestCliCommand.MergeShortCl))
                new Converter(controller).run(Converter.TestType.Merge, properties);
            else
            {
                controller.init(properties);

                if (commandline.hasOption(IrTestCliCommand.ListInterfacesShortCl))
                    ListInterfaces();
//                else if (commandline.hasOption(IrTestCliCommand.AddGroupShortCl))
//                    addGroup();
//                else if (commandline.hasOption(IrTestCliCommand.AddCreatorShortCl))
//                    addCreator();
                else if (commandline.hasOption(IrTestCliCommand.SearchShortCl))
                    new GoogleSearch().run();
                else if (commandline.hasOption(IrTestCliCommand.UploadShortCl))
                    new Converter(controller).run(Converter.TestType.Upload, properties);
//                else if (commandline.hasOption(IrTestCliCommand.AddBasicsShortCl))
//                {
//                    addCreator();
//                    addGroup();
//                    new Converter(controller).run(Converter.TestType.Upload, properties);
//                }
                else
                {
                    if (controller != null)
                        controller.destroy();
//                    command.help(1, "no command line switch given");
                }
                log.info("IrTestRunner exiting with no exception");
            }
        } catch (Throwable t)
        {
            log.error("failed: ", t);
            close(1);
        }
        //        super.run();  // if you want a showoptionpane main thread blocker
    }

    @Override
    public void close(int ccode)
    {
        if (controller != null)
            controller.destroy();
        super.close(ccode);
    }

//    @SuppressWarnings("static-access")
//    @Override
//    protected CommandLine cliSetup(String[] args)
//    {
//        //*@formatter:off
//        options.addOption(
//            OptionBuilder.withLongOpt(ConvertLongCl)
//            .withDescription("Convert merged.xml to new xml")
//            .create(ConvertShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(ListInterfacesLongCl)
//            .withDescription("List interfaces")
//            .create(ListInterfacesShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(AddGroupLongCl)
//            .withDescription("Add a Group to the database")
//            .create(AddGroupShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(AddCreatorLongCl)
//            .withDescription("Add a Submitter to the database")
//            .create(AddCreatorShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(MergeLongCl)
//            .withDescription("Merge legacy multiple xml's to merged xml")
//            .create(MergeShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(SearchLongCl)
//            .withDescription("Google Search")
//            .create(SearchShortCl));
//        options.addOption(
//            OptionBuilder.withLongOpt(UploadLongCl)
//            .withDescription("Upload all converted files to database")
//            .create(UploadShortCl));
//        options.addOption(
//             OptionBuilder.withLongOpt(AddBasicsLongCl)
//             .withDescription("Add User, group and upload all to database")
//             .create(AddBasicsShortCl));
//        //*@formatter:on
//        return super.cliSetup(args);
//    }

    public static void main(String args[])
    {
        new IrTestRunner(args).start();
    }
} 