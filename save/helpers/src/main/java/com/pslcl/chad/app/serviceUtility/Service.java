/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.chad.app.serviceUtility;


/**
 * Application level Service.
 * </p>
 *  It is desirable that a service is implemented independent of configuration methodologies 
 *  as well as the platform the service will be deployed to and executed on. For example a 
 *  service should be deployable to:
 *  <ul><li>Linux systems (i.e. Apache Daemon)</li>
 *  <li>OSX systems (i.e. Apache Daemon)</li>
 *  <li>Windows systems (i.e. Apache Procrun)</li> 
 *  <li>OSGi systems (i.e. Apache felix)</li></ul>
 *  It is also desirable to separate the EMIT security implementation from the application 
 *  specific implementation.  
 *  </p>
 *  This can be achieved by architecturally dividing an EMIT application into a 
 *  "System Level" and an "Application Level", where the platform, configuration and security 
 *  issues are implemented at the System Level and only the application specific implementation 
 *  is found at the Application Level.
 *  </p>
 *  This interface declares the entry point into the Application Level and provides the means for
 *  the System Level to inject any required Application Level configuration.  Java Generics are used
 *  here to allow for totally custom configuration objects specific to the application to be injected.
 *  </p>
 *  The lifecycle of the Service implementation object is controlled by the System Level, where that 
 *  service object will be instantiated, the optional configuration object is instantiated of type <code>&ltT&gt 
 *  (&ltVoid&gt is valid)</code> and the <code>Service.init</code> method is called to start the
 *  lifecycle and <code>Service.destroy</code> is called to end it.
 *  </p> 
 *  A given EMIT application (System Level and Application Level) may optionally utilize other Service
 *  Utils subsytems which have been implemented to support the separation of configuration, platform, 
 *  security and application implementation.  For example, an application level thread pool is encouraged
 *  see the <code>org.emitdo.service.util.executor</code> package.  For application basic health status
 *  utilities see the <code>org.emitdo.service.util.status</code> package.  For Dynamic Provide support
 *  see the <code>org.emitdo.service.util.status</code> package.  For separation/typing of core exceptions
 *  to middleware/application level exceptions see the <code>org.emitdo.service.util.exception</code> package.
 *  
 * @param <T> The custom configuration object created by the System Level and injected into the Application Level.
 * @see org.pslcl.service.internal.util.executor 
 * @see org.pslcl.service.internal.util.status 
*/

public interface Service<T>
{
    /**
     * Initialize the Service with the given custom configuration object.
     * </p>
     * The service should initialize and start performing its functionality when 
     * this method is called. 
     * @param config the custom configuration object.  Can be declared <code>&ltVoid&gt</code>.
     * @throws Exception if the application could not initialize and start.
     */
    public void init(T config) throws Exception;
    
    /**
     * Shutdown the application level and release all resources.
     * </p>
     * This Service implementation object should be considered unusable after making this call. 
     */
    public void destroy();
}