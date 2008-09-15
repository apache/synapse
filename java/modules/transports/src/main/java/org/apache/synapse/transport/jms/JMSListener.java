/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.transport.jms;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.base.AbstractTransportListener;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.ManagementSupport;

import java.util.HashMap;
import java.util.Map;

/**
 * The JMS Transport listener implementation. A JMS Listner will hold one or
 * more JMS connection factories, which would be created at initialization
 * time. This implementation does not support the creation of connection
 * factories at runtime. This JMS Listener registers with Axis to be notified
 * of service deployment/undeployment/start and stop, and enables or disables
 * listening for messages on the destinations as appropriate.
 * <p/>
 * A Service could state the JMS connection factory name and the destination
 * name for use as Parameters in its services.xml as shown in the example
 * below. If the connection name was not specified, it will use the connection
 * factory named "default" (JMSConstants.DEFAULT_CONFAC_NAME) - if such a
 * factory is defined in the Axis2.xml. If the destination name is not specified
 * it will default to a JMS queue by the name of the service. If the destination
 * should be a Topic, it should be created on the JMS implementation, and
 * specified in the services.xml of the service.
 * <p/>
 * <parameter name="transport.jms.ConnectionFactory" locked="true">
 * myTopicConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="true">
 * dynamicTopics/something.TestTopic</parameter>
 */
public class JMSListener extends AbstractTransportListener implements ManagementSupport {

    public static final String TRANSPORT_NAME = Constants.TRANSPORT_JMS;

    private JMSConnectionFactoryManager connFacManager;
    /** A Map of service name to the JMS EPR addresses */
    private Map<String,String> serviceNameToEPRMap = new HashMap<String,String>();

    /**
     * This is the TransportListener initialization method invoked by Axis2
     *
     * @param cfgCtx   the Axis configuration context
     * @param trpInDesc the TransportIn description
     */
    public void init(ConfigurationContext cfgCtx,
                     TransportInDescription trpInDesc) throws AxisFault {
        super.init(cfgCtx, trpInDesc);

        connFacManager = new JMSConnectionFactoryManager(cfgCtx, this, workerPool);
        // read the connection factory definitions and create them
        connFacManager.loadConnectionFactoryDefinitions(trpInDesc);

        // if no connection factories are defined, we cannot listen for any messages
        if (connFacManager.getNames().length == 0) {
            log.warn("No JMS connection factories are defined. Cannot listen for JMS");
            return;
        }

        log.info("JMS Transport Receiver/Listener initialized...");
    }

    /**
     * Start this JMS Listener (Transport Listener)
     *
     * @throws AxisFault
     */
    public void start() throws AxisFault {
        connFacManager.start();
        super.start();
    }

    /**
     * Stop the JMS Listener, and shutdown all of the connection factories
     */
    public void stop() throws AxisFault {
        super.stop();
        connFacManager.stop();
    }

    /**
     * Returns EPRs for the given service and IP over the JMS transport
     *
     * @param serviceName service name
     * @param ip          ignored
     * @return the EPR for the service
     * @throws AxisFault not used
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        //Strip out the operation name
        if (serviceName.indexOf('/') != -1) {
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        // strip out the endpoint name if present
        if (serviceName.indexOf('.') != -1) {
            serviceName = serviceName.substring(0, serviceName.indexOf('.'));
        }
        return new EndpointReference[]{
            new EndpointReference(serviceNameToEPRMap.get(serviceName))};
    }

    /**
     * Prepare to listen for JMS messages on behalf of the given service
     *
     * @param service the service for which to listen for messages
     */
    protected void startListeningForService(AxisService service) {
        JMSConnectionFactory cf = getConnectionFactory(service);
        if (cf == null) {
            String msg = "Service " + service.getName() + " does not specify" +
                         "a JMS connection factory or refers to an invalid factory. " +
                         "This service is being marked as faulty and will not be " +
                         "available over the JMS transport";
            log.warn(msg);
            BaseUtils.markServiceAsFaulty(service.getName(), msg, service.getAxisConfiguration());
            disableTransportForService(service);
            return;
        }

        // compute service EPR and keep for later use
        String destinationName = JMSUtils.getJNDIDestinationNameForService(service);
        String destinationType = JMSUtils.getDestinationTypeForService(service);
        serviceNameToEPRMap.put(service.getName(),
                JMSUtils.getEPR(cf, destinationType, destinationName));
        
        log.info("Starting to listen on destination : " + destinationName + " of type "
                + destinationType + " for service " + service.getName());
        cf.addDestination(destinationName, destinationType, service.getName());
        cf.startListeningOnDestination(destinationName, destinationType, service.getName());
    }

    /**
     * Stops listening for messages for the service thats undeployed or stopped
     *
     * @param service the service that was undeployed or stopped
     */
    protected void stopListeningForService(AxisService service) {

        JMSConnectionFactory cf = getConnectionFactory(service);
        if (cf != null) {
            // remove from the serviceNameToEprMap
            serviceNameToEPRMap.remove(service.getName());

            String destination = JMSUtils.getJNDIDestinationNameForService(service);
            cf.removeDestination(destination);
        }
    }
    /**
     * Return the connection factory name for this service. If this service
     * refers to an invalid factory or defaults to a non-existent default
     * factory, this returns null
     *
     * @param service the AxisService
     * @return the JMSConnectionFactory to be used, or null if reference is invalid
     */
    private JMSConnectionFactory getConnectionFactory(AxisService service) {
        Parameter conFacParam = service.getParameter(JMSConstants.CONFAC_PARAM);

        // validate connection factory name (specified or default)
        if (conFacParam != null) {
            return connFacManager.getJMSConnectionFactory((String)conFacParam.getValue());
        } else {
            return connFacManager.getJMSConnectionFactory(JMSConstants.DEFAULT_CONFAC_NAME);
        }
    }

    // -- jmx/management methods--
    /**
     * Pause the listener - Stop accepting/processing new messages, but continues processing existing
     * messages until they complete. This helps bring an instance into a maintenence mode
     * @throws AxisFault on error
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            connFacManager.pause();
            state = BaseConstants.PAUSED;
            log.info("Listener paused");
        } catch (AxisJMSException e) {
            log.error("At least one connection factory could not be paused", e);
        }
    }

    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     * @throws AxisFault on error
     */
    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        try {
            connFacManager.resume();
            state = BaseConstants.STARTED;
            log.info("Listener resumed");
        } catch (AxisJMSException e) {
            log.error("At least one connection factory could not be resumed", e);
        }
    }

    /**
     * Stop processing new messages, and wait the specified maximum time for in-flight
     * requests to complete before a controlled shutdown for maintenence
     *
     * @param millis a number of milliseconds to wait until pending requests are allowed to complete
     * @throws AxisFault on error
     */
    public void maintenenceShutdown(long millis) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            stop();
            state = BaseConstants.STOPPED;
            log.info("Listener shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (Exception e) {
            handleException("Error shutting down the listener for maintenence", e);
        }
    }
}
