/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.internal.test.simpleNodeFramework.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Source;

import com.pslcl.internal.test.simpleNodeFramework.config.NodeConfiguration.NodeConfig;

@SuppressWarnings("javadoc")
public class DofConfiguration
{
    private static final AtomicInteger nameIndex = new AtomicInteger(-1);
    
    public static final String DofSourceIdKey = "emitdo.dof.sourceId";
    public static final String DofRouterKey = "emitdo.dof.router";
    public static final String DofNameKey = "emitdo.dof.name";
    public static final String DofMaxConnectionsKey = "emitdo.dof.max-connections";
    public static final String DofThreadsKey = "emitdo.dof.threads";
    public static final String DofRankKey = "emitdo.dof.rank";
    public static final String DofValidateKey = "emitdo.dof.validate";

    //    public static final String DofSourceIdDefault = "null";
    public static final String DofRouterDefault = "false";
    //    public static final String DofNameDefault = "null";
    public static final String DofMaxConnectionsDefault = Integer.toString(DOF.UNLIMITED);
    public static final String DofThreadsDefault = Short.toString(DOF.Config.DEFAULT_THREADPOOL_SIZE);
    public static final String DofRankDefault = Short.toString(DOF.Config.DEFAULT_RANK);
    public static final String DofValidateDefault = "true";

    public static DofConfig propertiesToConfig(NodeConfig nodeConfig) throws Exception
    {
        String msg = "ok";
        try
        {
            DOFObjectID.Source sourceId = null;
            String sourceIdStr = nodeConfig.properties.getProperty(DofSourceIdKey);
            if (sourceIdStr != null)
                sourceId = Source.create(sourceIdStr);
            nodeConfig.toSb(DofSourceIdKey, "=", (sourceId == null ? "null" : sourceId.toStandardString()));

            msg = "invalid boolean value for router";
            boolean router = Boolean.parseBoolean(nodeConfig.properties.getProperty(DofRouterKey, DofRouterDefault));
            nodeConfig.toSb(DofRouterKey, "=", ""+router);

            String dofName = nodeConfig.properties.getProperty(DofNameKey);
            if(dofName.endsWith("*"))
            {
                dofName = dofName.substring(0, dofName.length() - 1);
                dofName += ""+nameIndex.incrementAndGet();
            }
            nodeConfig.toSb(DofNameKey, "=", dofName);

            msg = "invalid short value for maxConnections";
            short maxConnections = Short.parseShort(nodeConfig.properties.getProperty(DofMaxConnectionsKey, DofMaxConnectionsDefault));
            nodeConfig.toSb(DofMaxConnectionsKey, "=", ""+maxConnections);

            msg = "invalid short value for threadPoolSize";
            short threadPoolSize = Short.parseShort(nodeConfig.properties.getProperty(DofThreadsKey, DofThreadsDefault));
            nodeConfig.toSb(DofThreadsKey, "=", ""+threadPoolSize);

            msg = "invalid short value for rank";
            short rank = Short.parseShort(nodeConfig.properties.getProperty(DofRankKey, DofRankDefault));
            nodeConfig.toSb(DofRankKey, "=", ""+rank);

            msg = "invalid boolean value for validate";
            boolean validate = Boolean.parseBoolean(nodeConfig.properties.getProperty(DofValidateKey, DofValidateDefault));
            nodeConfig.toSb(DofValidateKey, "=", ""+validate);

            msg = "DOF.Config.Builder.build failed";
            DOF.Config config = new DOF.Config.Builder()
                .setSourceID(sourceId)
                .setRouter(router)
                .setName(dofName)
                .setConnectionLimit(maxConnections)
                .setThreadPoolSize(threadPoolSize)
                .setRank(rank)
            //  .setProtocolFactory(DOFProtocolFactory protocolFactory)
                 .setParameterValidation(validate).build();
            
            nodeConfig.properties.remove(DofSourceIdKey);
            nodeConfig.properties.remove(DofRouterKey);
            nodeConfig.properties.remove(DofNameKey);
            nodeConfig.properties.remove(DofMaxConnectionsKey);
            nodeConfig.properties.remove(DofThreadsKey);
            nodeConfig.properties.remove(DofRankKey);
            nodeConfig.properties.remove(DofValidateKey);

            return new DofConfig(config);
                      
        } catch (Exception e)
        {
            nodeConfig.toSb( msg);
//            log.error(sb.toString(), e);
            throw e;
        }
    }
    
    public static class DofConfig
    {
        public final DOF.Config config;
        private DOF dof;
        
        public DofConfig(DOF.Config config)
        {
            this.config = config;
        }
        
        public synchronized DOF getDof()
        {
            if(dof == null)
                dof = new DOF(config);
            return dof;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()+"\n");
            sb.append("\tname:             " + config.getName() +"\n");
            sb.append("\trouter:           " + config.isRouter() +"\n");
            sb.append("\tvalidate:         " + config.isParameterValidation() +"\n");
            sb.append("\tsourceId:         " + config.getSourceID() +"\n");
            sb.append("\trank:             " + config.getRank() +"\n");
            sb.append("\tthread pool:      " + config.getThreadPoolSize() +"\n");
//FIXME: put back when back to 6.1            
//            sb.append("\tconnection limit: " + config.getConnectionLimit() +"\n");
            sb.append("\tprotocol factory: " + config.getProtocolFactory() +"\n");
            return sb.toString(); 
        }
    }
}
