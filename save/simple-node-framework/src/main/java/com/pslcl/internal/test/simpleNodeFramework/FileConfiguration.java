package com.pslcl.internal.test.simpleNodeFramework;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.apache.commons.daemon.DaemonInitException;
import org.opendof.core.oal.DOFObjectID;
import org.pslcl.service.PropertiesFile;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.StrH;
import com.pslcl.dsp.PlatformConfiguration;
import com.pslcl.dsp.SystemConfiguration;

@SuppressWarnings("javadoc")
public class FileConfiguration implements PlatformConfiguration, SystemConfiguration
{
    
    private static final String nodeIdDefault = "[128:{" + UUID.randomUUID().toString() + "}]";
    private static final String systemIdDefault = "pdsp-0";
    private static final String connectionsThresholdDefault = "0";
    private static final String threadpoolThresholdDefault = "0";
    private static final String crServerPortDefault = "3567";
    private static final String crServerCredentialsDefault = "crServer.cred";
    private static final String asServerPortDefault = "3569";

    // Defaults
    private String baseConfigPath;
    private String baseSecurePath;
    private static String nodePropertiesFileName;
    private DOFObjectID nodeId;
    private String systemId;
    private int connectionsThreshold;
    private int threadpoolThreshold;
    private int crServerPort; 
    private String crServerCredentialsFile;
    private int asServerPort;
    private ExecutorService executor;
    
    public FileConfiguration()
    {
    }
    
    // Platform Configuration

    @Override
    public void init() throws Exception
    {
        StringBuilder sb = new StringBuilder("\n" + getClass().getName() + " init PlatformConfiguration:\n");
        String value = System.getProperty(SimpleNodeFramework.PropertyPathKey);
        if(value == null)
        {
            baseConfigPath = SimpleNodeFramework.baseConfigurationPathDefault;
            baseSecurePath = SimpleNodeFramework.baseSecureConfigurationPathDefault;
            nodePropertiesFileName = SimpleNodeFramework.propertyFilenameDefault;
        }else
        {
            value = value.replace('\\', '/');
            baseConfigPath = StrH.getPenultimateNameFromPath(value);
            String svalue = System.getProperty(SimpleNodeFramework.BaseSecurityPathKey);
            if(svalue == null)
                baseSecurePath = StrH.getPenultimateNameFromPath(value) + "/secure";
            else
                baseSecurePath = svalue;
            nodePropertiesFileName = StrH.getAtomicNameFromPath(value);
        }
        
        sb.append("\t" + SimpleNodeFramework.BaseConfigPathKey + "=" + baseConfigPath + "\n");
        sb.append("\t" + SimpleNodeFramework.BaseSecurityPathKey + "=" + baseSecurePath + "\n");
        sb.append("\tpropertiesFileName=" + nodePropertiesFileName + "\n");
        LoggerFactory.getLogger(getClass()).info(sb.toString());
    }

    @Override
    public String getBaseConfigurationPath()
    {
        return baseConfigPath;
    }

    @Override
    public String getBaseSecureConfigurationPath()
    {
        return baseSecurePath;
    }

    // System Configuration

    @Override
    public void init(PlatformConfiguration platformConfig) throws Exception
    {
        StringBuilder sb = new StringBuilder("\n" + getClass().getName() + " init SystemConfiguration:\n");
        
        // Parse properties
        Properties properties = new Properties();
        String msg = "Unable to read properties file: '" + baseConfigPath + "/" + nodePropertiesFileName + "'";
        try
        {
            PropertiesFile.load(properties, platformConfig.getBaseConfigurationPath() + "/" + nodePropertiesFileName);
        } catch (FileNotFoundException fnfe)
        {
            sb.append("\t" + baseConfigPath + "/" + nodePropertiesFileName + " not found, using all defaults");
        } catch (IOException ioe)
        {
            sb.append("\t" + msg + "\n");
            LoggerFactory.getLogger(getClass()).error(sb.toString());
            throw new DaemonInitException(msg, ioe);
        }

        // Get Node ID
        String nodeIDString = properties.getProperty(SimpleNodeFramework.NodeIdKey, nodeIdDefault);
        sb.append("\t" + SimpleNodeFramework.NodeIdKey + "=" + nodeIDString + "\n");
        msg = "Invalid nodeId: '" + nodeIDString + "'";
        try
        {
            nodeId = DOFObjectID.create(properties.getProperty(SimpleNodeFramework.NodeIdKey, nodeIDString));
            systemId = properties.getProperty(SimpleNodeFramework.SystemIdKey, systemIdDefault);
            sb.append("\t" + SimpleNodeFramework.SystemIdKey + "=" + systemId + "\n");
            String value = properties.getProperty(SimpleNodeFramework.ConnectionsThresholdKey, connectionsThresholdDefault);
            sb.append("\t" + SimpleNodeFramework.ConnectionsThresholdKey + "=" + value + "\n");
            msg = "Invalid threshold number, must be an integer: '" + value + "'";
            connectionsThreshold = Integer.parseInt(value);
            msg = "Invalid threshold number, must be an integer: '" + value + "'";
            value = properties.getProperty(SimpleNodeFramework.ThreadpoolThresholdKey, threadpoolThresholdDefault);
            sb.append("\t" + SimpleNodeFramework.ThreadpoolThresholdKey + "=" + value + "\n");
            threadpoolThreshold = Integer.parseInt(value);
            
            msg = "Invalid port number, must be an integer: '" + value + "'";
            value = properties.getProperty(SimpleNodeFramework.CrServerPortKey, crServerPortDefault);
            sb.append("\t" + SimpleNodeFramework.CrServerPortKey + "=" + value + "\n");
            crServerPort = Integer.parseInt(value);
            value = properties.getProperty(SimpleNodeFramework.AsServerPortKey, asServerPortDefault);
            sb.append("\t" + SimpleNodeFramework.AsServerPortKey + "=" + value + "\n");
            asServerPort = Integer.parseInt(value);
            
            crServerCredentialsFile = properties.getProperty(SimpleNodeFramework.CrServerCredentialsKey, crServerCredentialsDefault);
            sb.append("\t" + SimpleNodeFramework.CrServerCredentialsKey + "=" + crServerCredentialsFile + "\n");
            
            msg = "failed to initialize EmitBlockingExecutor";
        } catch (Exception e)
        {
            sb.append("\t" + msg + "\n");
            LoggerFactory.getLogger(getClass()).error(sb.toString());
            throw new DaemonInitException(msg, e);
        }
        LoggerFactory.getLogger(getClass()).info(sb.toString());
    }

    @Override
    public DOFObjectID getNodeID()
    {
        return nodeId;
    }

    @Override
    public String getSystemID()
    {
        return systemId;
    }

    @Override
    public int getConnectionCountThreshold()
    {
        return connectionsThreshold;
    }

    @Override
    public int getThreadpoolSizeThreshold()
    {
        return threadpoolThreshold;
    }
    
    public int getCrServerPort()
    {
        return crServerPort;
    }
    
    public String getCrServerCredentialsFile()
    {
        return crServerCredentialsFile;
    }
    
    public int getAsServerPort()
    {
        return asServerPort;
    }
    
    public ExecutorService getExecutor()
    {
        return executor;
    }
    
}
