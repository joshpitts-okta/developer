package com.pslcl.internal.test.hubManager;

import org.apache.commons.cli.CommandLine;

import com.pslcl.internal.test.simpleNodeFramework.NodeRunner;


@SuppressWarnings("javadoc")
public class HubManagerNodeRunner extends NodeRunner
{
//    public static final String myLogBase = "HubManagerNodeRunner";
//    public static final char ServiceUtilsModuleShortCl = 's';
//    public static final String ServiceUtilsModuleLongCl = "serviceutils-module";
//    
//    public static final char ActivateRequestingModuleShortCl = 'r';
//    public static final String ActivateRequestingModuleLongCl = "requesting-module";
    
    
    public HubManagerNodeRunner(String[] args)
    {
        super(args);
    }

    @Override
    protected void clToModuleList(CommandLine commandline)
    {
        super.clToModuleList(commandline);
        //        addToClModuleList(ServiceUtilsModuleShortCl, ServiceUtilsModuleLongCl);
        //        addToClModuleList(ActivateRequestingModuleShortCl, ActivateRequestingModuleLongCl);
    }
    
    @Override
    public void run()
    {
        super.run();
    }

    @Override
    public void close(int ccode)
    {
        super.close(ccode);
    }

//    @SuppressWarnings("static-access")
    @Override
    protected CommandLine cliSetup(String[] args)
    {
//        options.addOption(OptionBuilder
//                        .withLongOpt(ServiceUtilsModuleLongCl)
//                        .withDescription("execute the ServiceUtilsModule")
//                        .create(ServiceUtilsModuleShortCl));
//        
//        options.addOption(OptionBuilder
//                        .withLongOpt(ActivateRequestingModuleLongCl)
//                        .withDescription("execute the HubRequestingModule")
//                        .create(ActivateRequestingModuleShortCl));
        
        return super.cliSetup(args);
    }

    public static void main(String args[])
    {
        new HubManagerNodeRunner(args).start();
    }
}