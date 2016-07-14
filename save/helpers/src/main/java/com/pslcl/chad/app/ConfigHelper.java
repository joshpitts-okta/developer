/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.opendof.core.oal.DOFCredentials;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.serviceUtility.PropertiesFile;

@SuppressWarnings("javadoc")
public class ConfigHelper
{
    public static final String propertyFilenameKey = "emitdo.config-name";
    public static final String baseConfigurationUrlKey = "emitdo.base-config-url";
    public static final String propertyFileUrlKey = "emitdo.config-url";
    public static final String baseSecureConfigurationUrlKey = "emitdo.base-security-url";
//    private static final String sourceIdKey = "emitdo.dof.source-id";
//    private static final String rankKey = keyPrefix + ".rank";
//    private static final String maxThreadsKey = keyPrefix + ".max-threads";
//    private static final String parameterValidationKey = keyPrefix + ".validate";
//    private static final String routerKey = keyPrefix + ".router";
//
    private static final String propertyFilenameDefault = "/" + "node.properties";
    private static final String baseConfigurationUrlDefault = "file:///etc/opt/enc/emitdo/config";
//    private static final String baseSecureConfigurationUrlDefault = baseConfigurationUrlDefault + "/security";
//    private static final String sourceIdDefault = "[128:{" + UUID.randomUUID().toString() + "}]";
//    private static final String rankDefault = "128"; // 0 least wanted, 256 most wanted
//    private static final String maxThreadsDefault = "0"; // no limit
//    private static final String parameterValidationDefault = "false";
//    private static final String routerDefault = "true";

//    protected DOF.Config dofConfig;
//    protected StringBuilder sb;
//    protected Properties properties;
    
    public static Properties loadProperties(URL path, StringBuilder sb) throws Exception
    {
        @SuppressWarnings("unused")
        URL configBaseUrl;
        @SuppressWarnings("unused")
        URL securityBaseUrl;
        
        if(sb == null)
            sb = new StringBuilder();
        Properties properties = new Properties();
        // see if the -D (system properties) properties file URL override is being used
        String configBase = null;
        String propertyFilepath = System.getProperty(propertyFileUrlKey, null);
        if (propertyFilepath != null)
        {
            configBase = propertyFilepath.substring(0, propertyFilepath.lastIndexOf('/'));
            sb.append("\t" + propertyFileUrlKey + "=" + propertyFilepath);
        } else
        {
            // allow for either of these to also come from system properties, but full url has precedence.
            configBase = System.getProperty(baseConfigurationUrlKey, baseConfigurationUrlDefault);
            sb.append("\t" + baseConfigurationUrlKey + "=" + configBase);
            String value = System.getProperty(propertyFilenameKey, propertyFilenameDefault);
            sb.append("\t" + propertyFilenameKey + "=" + value);
            propertyFilepath = configBase + "/" + value;
        }
        try
        {
            configBaseUrl = new URL(configBase);
            URI uri = new URI(propertyFilepath);
            File propertiesFile = new File(uri);
            PropertiesFile.loadFile(properties, propertiesFile);
        } catch (Exception e)
        {
            String msg = "failed to load properties from: " + propertyFilepath;
            sb.append("\t" + msg + "\n");
            LoggerFactory.getLogger(ConfigHelper.class).error(sb.toString(), e);
            throw e;
        }
        return properties;
    }
    
    /**
     * Converts the file's contents into a DOFCredential.
     * @param fileLocation
     *            The location of the file to be converted.
     * @return The DOFCredential created from the file.
     * @throws Exception if unable to create the credentials from the file data.
     */
    public static DOFCredentials retrieveDOFCredentialsFromFile(String fileLocation) throws Exception
    {
        DOFCredentials credentials = null;
        final byte[] credentialsBytes;

        final RandomAccessFile credentialsFile = new RandomAccessFile(fileLocation, "r");

        credentialsBytes = new byte[(int) credentialsFile.length()];
        credentialsFile.read(credentialsBytes);

        credentials = DOFCredentials.create(credentialsBytes);
        credentialsFile.close();

        return credentials;
    }

    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Properties properties)
    {
        ArrayList<Entry<String,String>> entries = new ArrayList<Entry<String,String>>();
        for(Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String)entry.getKey();
            if(key.contains(baseKey))
                entries.add(new StringPair(entry));
        }
        return entries;
    }
    
    public static class StringPair implements Entry<String, String>
    {
        private final String key;
        private String value;
        
        public StringPair(Entry<Object, Object> entry)
        {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
        }
        
        @Override
        public String setValue(String value)
        {
            String old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String getValue()
        {
            return value;
        }
        
        @Override
        public String toString()
        {
            return key+"="+value; 
        }
    }
    
    public static void setCurrentDirectory(String directoryName) throws Exception
    {
        boolean result = false;
        File directory = new File(directoryName).getAbsoluteFile();
        if (directory.exists() || directory.mkdirs())
            result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);

        if(!result)
            throw new Exception("failed to set current working directory to: " + directoryName);
    }
}
