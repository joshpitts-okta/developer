package com.pslcl.internal.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.opendof.core.oal.DOF;
import org.pslcl.service.ClassInfo;
import org.pslcl.service.PropertiesFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.Configuration;


@SuppressWarnings("javadoc")
public class TestRunner extends Thread
{
    public static final String ConfigPathKey = "opendof.internal.test.config-path";
    public static final String BaseSecurityPathKey = "opendof.internal.test.secure";

    public static final String ConfigPathDefault = "/env/opt/opendof/internal/test/testrunner.properties";
    public static final String BaseSecurityPathDefault = "/env/opt/opendof/internal/test/secure/";
    public static final String LogFileBaseDefault = "testRunner";

    public static final String logBase = "/var/opt/opendof/tools/logs";

    public static final char HelpShortCl = 'h';
    public static final String HelpLongCl = "help";

    public static final char LoggerNameShortCl = 'l';
    public static final String LoggerNameLongCl = "logger-name";

    public static final char ConfigurationPathShortCl = 'i';
    public static final String ConfigurationBasePathLongCl = "config-path";

    public static final char SecurityBasePathShortCl = 'b';
    public static final String SecurityBasePathLongCl = "security-base";

    public static final char AddToClassPathShortCl = 'a';
    public static final String AddToClassPathLongCl = "add-class-path";

    protected final Logger log;
    protected final Options options;
    protected final CommandLine commandline;
    private final String name;

    public TestRunner(String[] args, String configPathKey)
    {
        options = new Options();
        commandline = cliSetup(args);
        name = StrH.getAtomicNameFromPath(commandline.getOptionValue(LoggerNameShortCl, LogFileBaseDefault));
        String lb = logBase + "/" + name;
        System.setProperty("log-file-base-name", lb);
        log = LoggerFactory.getLogger(getClass());
        log.info("file logging to: " + lb);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        if(configPathKey == null)
            configPathKey = ConfigPathKey;
        log.info("configuration key: " + configPathKey);
        log.info("configuration file path: " + commandline.getOptionValue(ConfigurationPathShortCl));
        System.setProperty(configPathKey, commandline.getOptionValue(ConfigurationPathShortCl, ConfigPathKey));
        System.setProperty(BaseSecurityPathKey, commandline.getOptionValue(SecurityBasePathShortCl, BaseSecurityPathDefault));

        clToModuleList(commandline);
        ClassInfo classInfo = ClassInfo.getInfo(DOF.class);
        log.info("OAL JAR: " + classInfo.getLocation().toExternalForm());

        StringBuilder sb = new StringBuilder();
        sb.append("\nclasspath: \n");
        String path = System.getProperty("java.class.path");
        if (commandline.hasOption(AddToClassPathShortCl))
        {
            if (!path.endsWith(";"))
                path += ";";
            path += commandline.getOptionValue(AddToClassPathShortCl);
            System.setProperty("java.class.path", path);
        }
        do
        {
            int index = path.indexOf(';');
            if (index == -1)
                break;
            String element = path.substring(0, index);
            sb.append("\t" + element + "\n");
            path = path.substring(++index);
        } while (true);
        path = path.replace(';', '\n');
        //        String path = getClass().getName().replace('.', '/') + ".class";
        //        path="META-INF/org.emitdo.dsp.PlatformConfiguration";
        //        path="META-INF";
        //        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        log.trace(sb.toString());
    }

    private boolean secondClModuleList;
    private StringBuilder clModuleList;

    protected void clToModuleList(CommandLine commandline)
    {
        secondClModuleList = false;
        clModuleList = new StringBuilder();
        addToClModuleList(LoggerNameShortCl, LoggerNameLongCl);
        addToClModuleList(SecurityBasePathShortCl, SecurityBasePathLongCl);
        addToClModuleList(AddToClassPathShortCl, AddToClassPathLongCl);
    }

    protected void addToClModuleList(char shortOption, String longOption)
    {
        if (commandline.hasOption(shortOption))
        {
            clModuleList.append((secondClModuleList ? "," : "") + longOption);
            secondClModuleList = true;
        }
    }

    @Override
    public void run()
    {
        JOptionPane.showConfirmDialog(null, "Press any key to exit " + name);
        close(0);
    }

    public void close(int ccode)
    {
        System.exit(ccode);
    }

    @SuppressWarnings({ "null", "static-access" })
    protected CommandLine cliSetup(String[] args)
    {
        //        OptionBuilder
        //        .hasArg()
        //        .hasArg(true)
        //        .hasArgs()
        //        .hasArgs(3)
        //        .hasOptionalArg()
        //        .hasOptionalArgs()
        //        .hasOptionalArgs(3)
        //        .isRequired()
        //        .isRequired(true)
        //        .withArgName("name")
        //        .withDescription("desc")
        //        .withLongOpt("longTime")
        //        .withType(new Integer(0))
        //        .withValueSeparator()
        //        .withValueSeparator(':')
        //        .create();
        //        .create('a')
        //        .create("all");

        //@formatter:off
        options.addOption(
            OptionBuilder.withLongOpt(LoggerNameLongCl)
            .withDescription("Set the logger base name")
            .hasArg()
            .create(LoggerNameShortCl));

        options.addOption(
            OptionBuilder.withLongOpt(HelpLongCl)
            .withDescription("display help")
            .create(HelpShortCl));

        options.addOption(
            OptionBuilder.withLongOpt(ConfigurationBasePathLongCl)
            .withDescription("path to configuration base directory")
            .hasArg()
            .create(ConfigurationPathShortCl));

        options.addOption(
            OptionBuilder.withLongOpt(SecurityBasePathLongCl)
            .withDescription("path to security base directory")
            .hasArg()
            .create(SecurityBasePathShortCl));

        options.addOption(
            OptionBuilder.withLongOpt(AddToClassPathLongCl)
            .withDescription("add to class path")
            .hasArg()
            .create(AddToClassPathShortCl));
        //@formatter:on

        // see this page for 1.2 info on using withType ... http://liviutudor.com/2013/04/18/using-apache-commons-cli-to-parse-arguments/#sthash.BQvtu7Jh.dpbs        
        //        options.addOption(OptionBuilder
        //                        .withLongOpt(IntCheckLongCl)
        //                        .withArgName("integer-type")
        //                        .hasOptionalArg()
        //                        .withType(Number.class)
        //                        .withDescription("Integer type check")
        //                        .create(IntCheckShortCl));

        CommandLineParser parser = new PosixParser(); //GnuParser(); BasicParser();
        CommandLine commandLine = null;
        try
        {
            commandLine = parser.parse(options, args);
        } catch (ParseException e)
        {
            help(1);
        }
        if (commandLine.hasOption(HelpShortCl))
            help(0);

        String[] leftovers = commandLine.getArgs();
        if (leftovers != null && leftovers.length > 0)
        {
            StringBuilder sb = new StringBuilder();
            for (String arg : leftovers)
                sb.append(arg + " ");
            System.out.println("unrecognized command line parameters: " + sb.toString());
            help(1);
        }
        return commandLine;
    }

    protected void help(int exitCode)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TestRunner", options, true);
        System.exit(exitCode);
    }

    public static Properties loadPropertiesFile(Configuration config, String pathKey, String pathDefault, StringBuilder sb, Logger log) throws Exception
    {
        Properties properties = new Properties();
        String propertyFilename = System.getProperty(pathKey, pathDefault);
        //        sb.append("\t" + PropertyFileNameKey + "=" + propertyFilename + "\n");
        String configPath = config.platformConfig.getBaseConfigurationPath() + "/" + propertyFilename;
        sb.append("\tconfigPath=" + configPath + "\n");
        try
        {
            PropertiesFile.load(properties, configPath);
        } catch (FileNotFoundException fnfe)
        {
            sb.append("\t\tconfigPath not found, using default configuration values\n");
        } catch (IOException ioe)
        {
            String msg = "Unable to read properties file: '" + configPath + "'";
            sb.append("\t\t" + msg + "\n");
            log.error(sb.toString());
            throw new Exception(msg, ioe);
        }
        return properties;
    }

    public static void main(String args[])
    {
        new TestRunner(args, null).start();
    }
}