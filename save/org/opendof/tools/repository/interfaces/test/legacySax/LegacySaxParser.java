package org.opendof.tools.repository.interfaces.test.legacySax;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xerces.jaxp.SAXParserImpl;
import org.apache.xerces.util.XMLCatalogResolver;
import org.opendof.tools.repository.interfaces.opendof.saxParser.OpenDofSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

@SuppressWarnings("javadoc")
public class LegacySaxParser
{
	public final static String XsdCatalogFileKey = "opendof.tools.interface.repository.catalog.file";
    // private final static String xmlFile = "C:/tmp/irepository/output/working/10020018.xml";
    private final static String xmlFile = "/tmp/irepository/output/working/bidirectional-pcs.xml";
    //  private final static String xmlFile = "/tmp/irepository/output/working/GPSLocation.xml";
    private final static boolean singleFile = false;
    private final static boolean publishOnly = false;
    private final static boolean workingOnly = false;
    private final static boolean doExport = true;
    private final static boolean validate = true;
    private final static boolean pretty = true;

    public static final String logBase = "/var/opt/opendof/tools/irepository/log/LegacySaxParser";
    public final static Hashtable<String, String> nameSpaces = new Hashtable<String, String>();

    public static final String XsdFileInBaseDir = "/ws2/dof-interface-repository/schema/";
    public static final String MergedXmlFileOutBaseDir = "/tmp/irepository/output/";
    public static final String ConvertedXmlFileOutBaseDir = "/tmp/irepository/production/converted/";
    public static final String ConvertedXmlFileOutPubishDir = ConvertedXmlFileOutBaseDir + "publish/";
    public static final String ConvertedXmlFileOutWorkingDir = ConvertedXmlFileOutBaseDir + "working/";

    public static final String AtomicPublish = "publish";
    public static final String AtomicWorking = "working";
    public static final String AtomicHtml = "html";
    public static final String Publish = MergedXmlFileOutBaseDir + AtomicPublish;
    public static final String Working = MergedXmlFileOutBaseDir + AtomicWorking;
    public static final String XsdMainFile = "interface-repository.xsd";
    public static final String XsdMetaFile = "interface-repository-meta.xsd";
    public static final String XsdMainPath = XsdFileInBaseDir + XsdMainFile;
    public static final String XsdMetaPath = XsdFileInBaseDir + XsdMetaFile;
    public static String W3NamespaceLocation = "/ws2/dof-interface-repository/schema-project/xml.xsd";

    public static final Logger log;
    public final static Stack<String> stack = new Stack<String>();

    static
    {
        System.setProperty("log-file-base-name", logBase);
        log = LoggerFactory.getLogger(LegacySaxParser.class);
        log.info("Logging to: " + logBase);
    }

    private static String[] suitcase = new String[]
    {
    	Working + "/GPSLocation.xml",
    	Working + "/Impact.xml",
    	Working + "/LocationHistory.xml",
    	Working + "/Lockable.xml",
    	Working + "/Weight.xml",
    	Working + "/Light.xml",
    };
    
    //@formatter:off
    private static String[] badInterfaces = new String[]
    {
        //duplicates
        // co2_sensor _2.xml
        // DoorLockInterface.xml, DoorOpenInterface.xml
        // FCConfigInfo.xml, FuelCellConfiguration.xml
        // FCErrorNotify.xml, FuelCellErrorMonitor.xml
        // ModbusDeviceManager.xml ModbusManager.xml
        // fccj_v1_00 TankUnitWaterAmount.xml, Pt-4+ FuelCellDeviceUnit.xml
        // end duplicates
        // published
    	
/*    	
        "C:/tmp/irepository/output/publish/air-flow-volume.xml",  //iid="[1:{01000041}] cdata with html
        "C:/tmp/irepository/output/publish/BaseInterface.xml",  //iid="[1:{01000000}] no context
        "C:/tmp/irepository/output/publish/device-control.xml", //iid=[1:{09}] no context
        "C:/tmp/irepository/output/publish/hostel-ac-snapshot.xml", //iid=[1:{01000044}] no context garbled
*/      
        "C:/tmp/irepository/output/publish/hostel-ac-platform.xml", //iid=[1:{01000043}] range being set on array
        
    	// waiting for translation decisions 
        "C:/tmp/irepository/output/publish/MultiPowerMeter.xml", //iid=[1:{0233}] duplicate lang on description typeid 3
        
        
        // working
        "C:/tmp/irepository/output/working/v-r.xml",      
        "C:/tmp/irepository/output/working/v-p.xml",      
        "C:/tmp/irepository/output/working/v-o.xml",      
        "C:/tmp/irepository/output/working/v-f.xml",      
        "C:/tmp/irepository/output/working/v-c.xml",      
        "C:/tmp/irepository/output/working/refrigerant.xml",      
        "C:/tmp/irepository/output/working/PT4Plus FuelCellRealTimeValue.xml",      
        "C:/tmp/irepository/output/working/processing-result-notify.xml",      
        "C:/tmp/irepository/output/working/lamp-state.xml",      
        "C:/tmp/irepository/output/working/i-u.xml",      
        "C:/tmp/irepository/output/working/g-w.xml",      
        "C:/tmp/irepository/output/working/g-r.xml",      
        "C:/tmp/irepository/output/working/g-o.xml",      
        "C:/tmp/irepository/output/working/g-h.xml",      
        "C:/tmp/irepository/output/working/g-g.xml",      
        "C:/tmp/irepository/output/working/g-f.xml",      
        "C:/tmp/irepository/output/working/g-e.xml",      
        "C:/tmp/irepository/output/working/g-c.xml",       
        "C:/tmp/irepository/output/working/FuelCellNetworkBoardErrorMonitor.xml",      
        "C:/tmp/irepository/output/working/energy-saving-mode.xml",      
        "C:/tmp/irepository/output/working/electrical-device-rated-value.xml",      
        "C:/tmp/irepository/output/working/air-conditioner-indoor-unit-operation-time.xml",      
        "C:/tmp/irepository/output/working/air-conditioner-indoor-unit-operation-mode-list.xml",       
        "C:/tmp/irepository/output/working/air-conditioner-indoor-unit-operation-mode-configuration.xml",      
        "C:/tmp/irepository/output/working/air-conditioner-indoor-unit-info.xml",      
        "C:/tmp/irepository/output/working/about-device-data.xml", // cdata null      
        "C:/tmp/irepository/output/working/ACGenerator.xml", // sibling url null      
        "C:/tmp/irepository/output/working/automobile-probe.xml", // cdata empty
        "C:/tmp/irepository/output/working/AccessPointMarker.xml", // context empty
        "C:/tmp/irepository/output/working/Battery-Life-Information.xml", // cdata empty 
        "C:/tmp/irepository/output/working/bmu-s-initial.xml", // context empty
        "C:/tmp/irepository/output/working/bmu-s-periodic.xml", // context empty
        "C:/tmp/irepository/output/working/BuiltinClock.xml", // dup desc lang
        "C:/tmp/irepository/output/working/CommonMessageNotifier.xml", // context empty
       "C:/tmp/irepository/output/working/CookingStove.xml", // context empty
        "C:/tmp/irepository/output/working/co2_refrigerator.xml", // registry 2
        "C:/tmp/irepository/output/working/co2_sensor.xml", // registry 2
        "C:/tmp/irepository/output/working/co2_sensor_2.xml", // registry 2
        "C:/tmp/irepository/output/working/device-temperatures.xml", // dup desc lang
        "C:/tmp/irepository/output/working/EchonetLiteHomeAirConditionerProperty_F.xml", // sibling url null
        "C:/tmp/irepository/output/working/EchonetLitePanasonicHomeAirConditionerProperty_V139.xml", // sibling url null
        "C:/tmp/irepository/output/working/EchonetLitePanasonicSmartElectricEnergyMeterProperty_V141.xml", // sibling url null
        "C:/tmp/irepository/output/working/EchonetLiteSmartElectricEnergyMeterProperty_F.xml", // cdata null
        "C:/tmp/irepository/output/working/ElectricitySupplyMeter.xml", // sibling url null
        "C:/tmp/irepository/output/working/energy-site-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/ErrrorStatusControl.xml", // sibling url null
        "C:/tmp/irepository/output/working/FCController.xml", // cdata empty
        "C:/tmp/irepository/output/working/FRMonitoring.xml", // cdata empty
        "C:/tmp/irepository/output/working/fuelcell-realtime-value-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/FuelCellMaintenance.xml", // number format exception
        "C:/tmp/irepository/output/working/GasConsumptionMeter.xml", // sibling url null
//        "C:/tmp/irepository/output/working/GPSLocation.xml", // boolean element unexpected
        "C:/tmp/irepository/output/working/HotWaterSupplier.xml", // sibling url null
        "C:/tmp/irepository/output/working/HotWaterSupplyHeatingSystemController.xml", // sibling url null
        "C:/tmp/irepository/output/working/Interface-for-BMU-Model(Power Station).xml", // cdata empty
        "C:/tmp/irepository/output/working/Inverter.xml", // min > max 
        "C:/tmp/irepository/output/working/iterator.xml", // context empty
        "C:/tmp/irepository/output/working/JSONMessageEventInterface.xml", // dup desc lang
        "C:/tmp/irepository/output/working/load-site-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/LocationHistory.xml", // range problem
        "C:/tmp/irepository/output/working/LoggingASCII.xml", // lang attributes problem
        "C:/tmp/irepository/output/working/LoggingSJIS.xml", // lang attributes problem
        "C:/tmp/irepository/output/working/LoggingUTF8.xml", // lang attributes problem
        "C:/tmp/irepository/output/working/Maintenance.xml", // cdata null
        "C:/tmp/irepository/output/working/MaintenanceMessageNotifier.xml", // context empty
        "C:/tmp/irepository/output/working/MessageNotifier.xml", // context empty
        "C:/tmp/irepository/output/working/MultiplePropertiesControlInterface.xml", // sibling url null
        "C:/tmp/irepository/output/working/pcs-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/pt4plus-fuelcell-realtime-value-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/ResidentialPlatformDeviceActivate.xml", // dup descr lang
        "C:/tmp/irepository/output/working/ScheduleRequestNotification.xml", // cdata null
        "C:/tmp/irepository/output/working/SecurityConfigurationBaseDevice.xml", // context empty
        "C:/tmp/irepository/output/working/SecurityConfigurationBaseGateway.xml", // context empty
        "C:/tmp/irepository/output/working/SecurityConfigurationServiceGateway.xml", // context empty
        "C:/tmp/irepository/output/working/SecurtiyConfigurationServiceDevice.xml", // context empty
        "C:/tmp/irepository/output/working/ShoulderTapInterface.xml", // context empty
        "C:/tmp/irepository/output/working/SingaporeFrequencyRegulationResponse.xml", // cdata null
        "C:/tmp/irepository/output/working/SingaporeFrequencyRegulationSignal.xml", // cdata null
        "C:/tmp/irepository/output/working/site-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/SleepDevice.xml", // context empty
        "C:/tmp/irepository/output/working/solar-site-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/storage-site-snapshot.xml", // context empty
        "C:/tmp/irepository/output/working/Temp.xml", // cdata null
        "C:/tmp/irepository/output/working/ras_co2_controller.xml", // cdata null
    };
    
    //@formatter:off
    
    public LegacySaxParser()
    {
    }

    private static String convertToFileURL(String filename)
    {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/')
        {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        return "file:" + path;
    }

    private void exportInterface(String inputFile, InterfaceData interfaceData, boolean publish, String catalogPath) throws Exception
    {
        if(!doExport)
            return;
        String xml = interfaceData.export(pretty);
        log.info("\n\n"+xml+"\n\n");
        File converted = new File(inputFile);
        File convertedFile = new File(ConvertedXmlFileOutPubishDir + converted.getName());
        if(!publish)
            convertedFile = new File(ConvertedXmlFileOutWorkingDir + converted.getName());
        FileOutputStream fos = new FileOutputStream(convertedFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out = new OutputStreamWriter(bos,"UTF-8");
        out.write(xml);
        out.close();
        if(validate)
            validateExport(convertedFile, publish, catalogPath);
    }

    private void validateExport(File xmlFile, boolean publish, String catalogPath) throws Exception
    {
        // setup oasis catalog
        XMLCatalogResolver resolver = new XMLCatalogResolver();
        resolver.setPreferPublic(true);
        resolver.setCatalogList(new String[] { catalogPath });

        System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        SAXParserFactoryImpl spf = (SAXParserFactoryImpl) SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(false);
        SAXParserImpl saxParser = (SAXParserImpl)spf.newSAXParser();
        //saxParser.setFeature("http://xml.org/sax/features/validation", true);
        //        saxParser.setFeature("http://xml.org/sax/features/validation/schema", true);
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setProperty("http://apache.org/xml/properties/internal/entity-resolver", resolver);
        xmlReader.setErrorHandler(new LegacyErrorHandler());
        OpenDofSaxHandler handler = new OpenDofSaxHandler();
        OpenDofSaxHandler.setPublish(publish);
        xmlReader.setContentHandler(handler);
        
        FileInputStream fis = new FileInputStream(xmlFile);
        InputSource inSource = new InputSource(fis);
        xmlReader.parse(inSource);
        
//        URL schemaFile = new URL(InterfaceData.InterfaceSchemaLocation);
//        Source xmlSource = new StreamSource(xmlFile);
//        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//        Schema schema = factory.newSchema(schemaFile);
//        Validator validator = schema.newValidator();
//        validator.validate(xmlSource);
    }
    
    
    private void parseIt(String file, boolean publish, String catalogPath) throws Exception
    {
        file = file.replace('\\', '/');
        for(int i=0; i < badInterfaces.length; i++)
        {
            if(badInterfaces[i].contains(file))
                return;
        }
        System.out.print(file + " ");
//        if(++fileCount % 4 == 0)
            System.out.println("");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setErrorHandler(new LegacyErrorHandler());
        LegacySaxHandler handler = new LegacySaxHandler();
        xmlReader.setContentHandler(handler);
        xmlReader.parse(convertToFileURL(file));
        exportInterface(file, handler.getInterface(), publish, catalogPath);
    }
     
    public void doit(String catalogPath)
    {
        File currentfile = null;
        File file = null;
        File[] files = null;
        try
        {
            if(singleFile)
            {
                currentfile = new File(xmlFile);
                parseIt(xmlFile, false, catalogPath);
                logFoundNamespaces();
                return;
            }
            if(!workingOnly)
            {
                for(int i=0; i < suitcase.length; i++)
                    parseIt(suitcase[i], false, catalogPath);
                file = new File(Publish);
                files = file.listFiles();
                for(int i=0; i < files.length; i++)
                {
                    currentfile = files[i];
                    parseIt(currentfile.getAbsolutePath(), true, catalogPath);
                }
                if(publishOnly)
                {
                    logFoundNamespaces();
                    return;
                }
            }
            file = new File(Working);
            files = file.listFiles();
            for(int i=0; i < files.length; i++)
            {
                currentfile = files[i];
                parseIt(currentfile.getAbsolutePath(), false, catalogPath);
            }
            logFoundNamespaces();
        } catch (Exception e)
        {
            if(currentfile != null)
                log.error("file: " + currentfile.toString());
            log.error("Failed to traverse the trees", e);
            System.exit(1);
        }
    }

    public void logFoundNamespaces()
    {
        StringBuilder sb = new StringBuilder("\nLegacy Interface Namespaces:");
        for (Entry<String, String> entry : nameSpaces.entrySet())
            sb.append("\n\t" + entry.getValue());
        log.info(sb.toString());
    }
    
    public class LegacyErrorHandler implements ErrorHandler
    {
        @Override
        public void warning(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " warning", ce);
            log.warn(getClass().getSimpleName() + " warning SAXParseException", e);
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " error", ce);
            log.warn(getClass().getSimpleName() + " error SAXParseException", e);
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException
        {
            Exception ce = e.getException();
            if(ce != null)
                log.error(getClass().getSimpleName() + " fatal", ce);
            log.warn(getClass().getSimpleName() + " fatal SAXParseException", e);
            throw e;
        }
    }
    
    public static void main(String args[])
    {
        new LegacySaxParser().doit(null);
    }
}
