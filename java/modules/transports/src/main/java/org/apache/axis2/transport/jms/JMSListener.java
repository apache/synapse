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
package org.apache.axis2.transport.jms;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.transport.base.AbstractTransportListener;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.commons.logging.LogFactory;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.*;

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
public class JMSListener extends AbstractTransportListener {

    public static final String TRANSPORT_NAME = Constants.TRANSPORT_JMS;

    /** A Map containing the JMS connection factories managed by this, keyed by name */
    private Map connectionFactories = new HashMap();
    /** A Map of service name to the JMS EPR addresses */
    private Map serviceNameToEPRMap = new HashMap();

    // setup the logging for the transport
    static {
        log = LogFactory.getLog(JMSListener.class);
    }

    /**
     * This is the TransportListener initialization method invoked by Axis2
     *
     * @param cfgCtx   the Axis configuration context
     * @param trpInDesc the TransportIn description
     */
    public void init(ConfigurationContext cfgCtx,
                     TransportInDescription trpInDesc) throws AxisFault {
        setTransportName(TRANSPORT_NAME);
        super.init(cfgCtx, trpInDesc);        

        // read the connection factory definitions and create them
        loadConnectionFactoryDefinitions(trpInDesc);

        // if no connection factories are defined, we cannot listen for any messages
        if (connectionFactories.isEmpty()) {
            log.warn("No JMS connection factories are defined. Cannot listen for JMS");
            return;
        }

        // iterate through deployed services and validate connection factory
        // names, and mark services as faulty where appropriate
        Iterator services = cfgCtx.getAxisConfiguration().getServices().values().iterator();

        // start connection factories
        start();

        while (services.hasNext()) {
            AxisService service = (AxisService) services.next();
            if (BaseUtils.isUsingTransport(service, transportName)) {
                startListeningForService(service);
            }
        }

        log.info("JMS Transport Receiver/Listener initialized...");
    }

    /**
     * Start this JMS Listener (Transport Listener)
     *
     * @throws AxisFault
     */
    public void start() throws AxisFault {

        Iterator iter = connectionFactories.values().iterator();
        while (iter.hasNext()) {
            JMSConnectionFactory conFac = (JMSConnectionFactory) iter.next();
            conFac.setJmsMessageReceiver(
                new JMSMessageReceiver(this, conFac, workerPool, cfgCtx));

            try {
                conFac.connectAndListen();
            } catch (JMSException e) {
                handleException("Error starting connection factory : " + conFac.getName(), e);
            } catch (NamingException e) {
                handleException("Error starting connection factory : " + conFac.getName(), e);
            }
        }
    }

    /**
     * Stop the JMS Listener, and shutdown all of the connection factories
     */
    public void stop() throws AxisFault {
        super.stop();
        Iterator iter = connectionFactories.values().iterator();
        while (iter.hasNext()) {
            ((JMSConnectionFactory) iter.next()).stop();
        }
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
        return new EndpointReference[]{
            new EndpointReference((String) serviceNameToEPRMap.get(serviceName))};
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
            return;
        }

        // compute service EPR and keep for later use
        String destinationName = JMSUtils.getJNDIDestinationNameForService(service);
        serviceNameToEPRMap.put(service.getName(), JMSUtils.getEPR(cf, destinationName));

        log.info("Starting to listen on destination : " + destinationName +
            " for service " + service.getName());
        cf.addDestination(destinationName, service.getName());
        cf.startListeningOnDestination(destinationName);
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
            String conFac = (String) conFacParam.getValue();
            if (connectionFactories.containsKey(conFac)) {
                return (JMSConnectionFactory) connectionFactories.get(conFac);
            } else {
                return null;
            }

        } else if (connectionFactories.containsKey(JMSConstants.DEFAULT_CONFAC_NAME)) {
            return (JMSConnectionFactory) connectionFactories.get(JMSConstants.DEFAULT_CONFAC_NAME);

        } else {
            return null;
        }
    }

    /**
     * Create JMSConnectionFactory instances for the definitions in the transport listener,
     * and add these into our collection of connectionFactories map keyed by name
     *
     * @param transprtIn the transport-in description for JMS
     */
    private void loadConnectionFactoryDefinitions(TransportInDescription transprtIn) {

        // iterate through all defined connection factories
        Iterator conFacIter = transprtIn.getParameters().iterator();

        while (conFacIter.hasNext()) {
            Parameter conFacParams = (Parameter) conFacIter.next();

            JMSConnectionFactory jmsConFactory =
                new JMSConnectionFactory(conFacParams.getName(), cfgCtx);
            JMSUtils.setConnectionFactoryParameters(conFacParams, jmsConFactory);

            connectionFactories.put(jmsConFactory.getName(), jmsConFactory);
        }
    }
    

}
