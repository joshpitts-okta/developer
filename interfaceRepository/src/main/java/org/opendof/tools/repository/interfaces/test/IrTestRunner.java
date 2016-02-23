package org.opendof.tools.repository.interfaces.test;

import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.emitdo.internal.test.TestRunner;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.tools.repository.interfaces.core.CoreController;
import org.opendof.tools.repository.interfaces.core.InterfaceRequest;
import org.opendof.tools.repository.interfaces.core.PropertiesFile;
import org.opendof.tools.repository.interfaces.core.RequestData;
import org.opendof.tools.repository.interfaces.core.RequestData.CommandType;
import org.opendof.tools.repository.interfaces.core.SubmitterRequest;
import org.opendof.tools.repository.interfaces.da.DataAccessor;
import org.opendof.tools.repository.interfaces.da.InterfaceData;
import org.opendof.tools.repository.interfaces.servlet.HttpCommandParser;

@SuppressWarnings("javadoc")
public class IrTestRunner extends TestRunner
{
    //    'a', "add-class-path"
    //    'b', "security-base"      "/env/opt/opendof/internal/test/secure/"
    //    'h',"help"
    //    'i', "config-path"        "/env/opt/opendof/internal/test/testrunner.properties"
    //    'l', "logger-name"        "/var/opt/opendof/tools/logs/testRunner"

    public static final char ConvertShortCl = 'c';
    public static final String ConvertLongCl = "convert";
    public static final char ListInterfacesShortCl = 'd';
    public static final String ListInterfacesLongCl = "list-iface";
    public static final char AddGroupShortCl = 'g';
    public static final String AddGroupLongCl = "add-group";
    public static final char MergeShortCl = 'm';
    public static final String MergeLongCl = "merge";
    public static final char AddCreatorShortCl = 'o';
    public static final String AddCreatorLongCl = "add-creator";
    public static final char SearchShortCl = 's';
    public static final String SearchLongCl = "search";
    public static final char UploadShortCl = 'u';
    public static final String UploadLongCl = "upload-xml";
    public static final char AddBasicsShortCl = 'z';
    public static final String AddBasicsLongCl = "add-basics";

    public IrTestRunner(String[] args)
    {
        super(args, CoreController.IrServletConfigKey);
    }

    @Override
    protected void clToModuleList(CommandLine commandline)
    {
        super.clToModuleList(commandline);
        addToClModuleList(ConvertShortCl, ConvertLongCl);
        addToClModuleList(ListInterfacesShortCl, ListInterfacesLongCl);
        addToClModuleList(AddGroupShortCl, AddGroupLongCl);
        addToClModuleList(MergeShortCl, MergeLongCl);
        addToClModuleList(UploadShortCl, UploadLongCl);
        addToClModuleList(SearchShortCl, SearchLongCl);
        addToClModuleList(UploadShortCl, UploadLongCl);
    }

    private void addCreator() throws Exception
    {
        // SubmitterRequest submitter = new SubmitterRequest(null, "chad.adams@us.panasonic.com", "Senior Software Engineer");
        SubmitterRequest submitter = new SubmitterRequest(null, "chad.adams@us.panasonic.com", "Senior Software Engineer");
        RequestData request = new RequestData(null, "Chad Adams", null, submitter, null);
        controller.addCreator(request);
    }

    private void addGroup() throws Exception
    {
        SubmitterRequest submitter = new SubmitterRequest(Converter.ConverterGroup, "admin@opendof.org", "Group administrator contact");
        RequestData request = new RequestData(null, "Chad Adams", null, submitter, null);
        controller.addGroup(request);
    }

    private void ListInterfaces() throws Exception
    {
    	InterfaceRequest ir = new InterfaceRequest(null, null, null, null); 
    	RequestData request = new RequestData(CommandType.ListAll, null, null, null, ir);
        List<InterfaceData> list = controller.getAllInterfaces(request);
        for (InterfaceData iface : list)
            log.info(iface.toString());
        log.info("look here");
    }
    
    private void iidVerify() throws Exception
    {
    	String stdStr1 = "[1:{01}]";
    	DOFInterfaceID iid1 = DOFInterfaceID.create(stdStr1);
    	String stdStr2 = "[01:{1}]";
    	DOFInterfaceID iid2 = DOFInterfaceID.create(stdStr2);
    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
    	if(!iid1.equals(iid2))
    		throw new Exception("did not like it");
    	String str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
    	str = HttpCommandParser.StandardFormToDofUri(iid2.toStandardString());

    	stdStr1 = "%5B"+"1" + "%3A" + "%7B" + "01"+ "%7D" + "%5D";
    	stdStr2 = "%5B"+"01" + "%3A" + "%7B" + "1"+ "%7D" + "%5Dy";

    	str = HttpCommandParser.fixupIidStandardForm(stdStr1);    	
    	str = HttpCommandParser.fixupIidStandardForm(stdStr2);    	
    	

    	stdStr1 = "[1:{01aa}]";
    	
    	iid1 = DOFInterfaceID.create(stdStr1);
    	stdStr2 = "[01:{1aa}]";
    	iid2 = DOFInterfaceID.create(stdStr2);
    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
    	if(!iid1.equals(iid2))
    		throw new Exception("did not like it");
    	str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
    	
    	stdStr1 = "[1:{01aa22bb}]";
    	iid1 = DOFInterfaceID.create(stdStr1);
    	stdStr2 = "[1:{1aa22bb}]";
    	iid2 = DOFInterfaceID.create(stdStr2);
    	log.info(iid1.toStandardString() + " " + iid2.toStandardString());
    	if(!iid1.equals(iid2))
    		throw new Exception("did not like it");
    	str = HttpCommandParser.StandardFormToDofUri(iid1.toStandardString());
    	
    	str = HttpCommandParser.StandardFormToDofUri("source.opendof.org");
    }

    private static final String ContextRealBaseDefault = "c:/ws2/interface-repository-web/WebContent/";

    private void init() throws Exception 
    {
        properties = new Properties();
        PropertiesFile.load(properties, System.getProperty(CoreController.IrServletConfigKey));
        properties.setProperty(DataAccessor.NonServletInitKey, "true");
        properties.setProperty(CoreController.ContextRealBaseKey, ContextRealBaseDefault); //TODO: add another command line switch for this

        controller = new CoreController();
    }
    
    private volatile CoreController controller;
    private volatile Properties properties;

    @Override
    public void run()
    {
        try
        {
            iidVerify();

            init();
            if (commandline.hasOption(ConvertShortCl))
                new Converter(controller).run(Converter.TestType.Convert, properties);
            else if (commandline.hasOption(MergeShortCl))
                new Converter(controller).run(Converter.TestType.Merge, properties);
            else
            {
                controller.init(properties);

                if (commandline.hasOption(ListInterfacesShortCl))
                    ListInterfaces();
                else if (commandline.hasOption(AddGroupShortCl))
                    addGroup();
                else if (commandline.hasOption(AddCreatorShortCl))
                    addCreator();
                else if (commandline.hasOption(SearchShortCl))
                    new GoogleSearch().run();
                else if (commandline.hasOption(UploadShortCl))
                    new Converter(controller).run(Converter.TestType.Upload, properties);
                else if (commandline.hasOption(AddBasicsShortCl))
                {
                    addCreator();
                    addGroup();
                    new Converter(controller).run(Converter.TestType.Upload, properties);
                }
                else
                {
                    if (controller != null)
                        controller.destroy();
                    help(1);
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

    @SuppressWarnings("static-access")
    @Override
    protected CommandLine cliSetup(String[] args)
    {
        //*@formatter:off
        options.addOption(
            OptionBuilder.withLongOpt(ConvertLongCl)
            .withDescription("Convert merged.xml to new xml")
            .create(ConvertShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(ListInterfacesLongCl)
            .withDescription("List interfaces")
            .create(ListInterfacesShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(AddGroupLongCl)
            .withDescription("Add a Group to the database")
            .create(AddGroupShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(AddCreatorLongCl)
            .withDescription("Add a Submitter to the database")
            .create(AddCreatorShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(MergeLongCl)
            .withDescription("Merge legacy multiple xml's to merged xml")
            .create(MergeShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(SearchLongCl)
            .withDescription("Google Search")
            .create(SearchShortCl));
        options.addOption(
            OptionBuilder.withLongOpt(UploadLongCl)
            .withDescription("Upload all converted files to database")
            .create(UploadShortCl));
        options.addOption(
             OptionBuilder.withLongOpt(AddBasicsLongCl)
             .withDescription("Add User, group and upload all to database")
             .create(AddBasicsShortCl));
        //*@formatter:on
        return super.cliSetup(args);
    }

    public static void main(String args[])
    {
        new IrTestRunner(args).start();
    }
} 