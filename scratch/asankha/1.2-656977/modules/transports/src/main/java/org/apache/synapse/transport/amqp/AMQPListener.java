package org.apache.synapse.transport.amqp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterIncludeImpl;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.nclient.Client;
import org.apache.qpidity.nclient.Connection;
import org.apache.synapse.transport.base.AbstractTransportListener;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.jms.JMSConnectionFactory;
import org.apache.synapse.transport.jms.JMSConstants;
import org.apache.synapse.transport.jms.JMSUtils;

public class AMQPListener extends AbstractTransportListener
{
    public static final String TRANSPORT_NAME = "AMQP";
    private static final Log log = LogFactory.getLog(AMQPListener.class);


    /** A Map containing the AMQP connections managed by this, keyed by name */
    private Map<String, AMQPConnection> connections = new HashMap<String, AMQPConnection>();
    /** A Map of service name to the AMQP EPR addresses */
    private Map serviceNameToEPRMap = new HashMap();

    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn) throws AxisFault
    {
        setTransportName(TRANSPORT_NAME);
        super.init(cfgCtx, transportIn);
        loadConnectionDefinitions(transportIn);

        if (connections.isEmpty()) {
            log.warn("No AMQP connections are defined. Cannot listen on AMQP");
            return;
        }

        log.info("AMQP Transport Receiver/Listener initialized...");
    }



    @Override
    public void start() throws AxisFault
    {
       for(String conName: connections.keySet()){
          AMQPConnection conDef = connections.get(conName);
          conDef.start();
       }
        super.start();
    }


    // Need to clean up the sessions as well
    @Override
    public void stop() throws AxisFault
    {
        for(String conName: connections.keySet()){
            AMQPConnection connection = connections.get(conName);
            try{
                connection.stop();
            }catch(Exception e){
                throw new AMQPSynapseException("Error creating a connection to the broker",e);
            }
        }
        super.stop();
    }


    @Override
    protected void startListeningForService(AxisService service)
    {
        if (service.getName().startsWith("__")) {
            return;
        }

        AMQPConnection con = getConnectionFactory(service);
        if (con == null) {
            String msg = "Service " + service.getName() + " does not specify" +
                         "an AMQP connection or refers to an invalid connection. " +
                         "This service is being marked as faulty and will not be " +
                         "available over the AMQP transport";
            log.warn(msg);
            BaseUtils.markServiceAsFaulty(service.getName(), msg, service.getAxisConfiguration());
            return;
        }

        // compute service EPR and keep for later use
        List<AMQPBinding> bindings = AMQPUtils.getBindingsForService(service);

        serviceNameToEPRMap.put(service.getName(), URIParser.getEPR(bindings,con.getUrl()));

        log.info("Starting to listen for service " + service.getName());

        // create bindings for the service
    }

    @Override
    protected void stopListeningForService(AxisService service)
    {
        // TODO Auto-generated method stub

    }

    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault
    {
        //Strip out the operation name
        if (serviceName.indexOf('/') != -1) {
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        return new EndpointReference[]{
            new EndpointReference((String) serviceNameToEPRMap.get(serviceName))};
    }

    /**
     * Create an AMQP Connection instances for the definitions in the transport listener,
     * and add these map keyed by name
     *
     * @param transprtIn the transport-in description for AMQP
     */
    private void loadConnectionDefinitions(TransportInDescription transprtIn) {

        // iterate through all defined connection definitions
        Iterator conIter = transprtIn.getParameters().iterator();

        while (conIter.hasNext()) {
            Parameter conParams = (Parameter) conIter.next();

            ParameterIncludeImpl pi = new ParameterIncludeImpl();
            AMQPConnection conDef = new AMQPConnection();
            try {
                pi.deserializeParameters((OMElement) conParams.getValue());
            } catch (AxisFault axisFault) {
                log.error("Error reading parameters for AMQP Connection definitions" +
                        conParams.getName(), axisFault);
            }
            conDef.setName((String)conParams.getValue());

            Iterator params = pi.getParameters().iterator();
            while (params.hasNext()) {

                Parameter p = (Parameter) params.next();

                if (AMQPConstants.CONNECTION_URL_PARAM.equals(p.getName())) {
                    conDef.setUrl((String) p.getValue());
                }
                else if (AMQPConstants.EXCHANGE_NAME_PARAM.equals(p.getName())) {
                    conDef.setExchangeName((String) p.getValue());
                }
                else if (AMQPConstants.EXCHANGE_TYPE_PARAM.equals(p.getName())) {
                    conDef.setExchangeType((String) p.getValue());
                }
            }

            connections.put(conDef.getName(), conDef);
        }
    }

    /**
     * Return the connection for this service. If this service
     * refers to an invalid connection or defaults to a non-existent default
     * connection, this returns null
     *
     * @param service the AxisService
     * @return the AMQPConnection to be used, or null if reference is invalid
     */
    private AMQPConnection getConnectionFactory(AxisService service) {
        Parameter conNameParam = service.getParameter(AMQPConstants.CONNECTION_NAME_PARAM);
        Parameter conURLParam = service.getParameter(AMQPConstants.CONNECTION_URL_PARAM);

        // validate connection factory name (specified or default)
        if (conNameParam != null) {
            String conFac = (String) conNameParam.getValue();
            if (connections.containsKey(conFac)) {
                return (AMQPConnection) connections.get(conFac);
            } else {
                return null;
            }

        // Next see if service defines it's own connection
        }else if (conURLParam != null){
            AMQPConnection con = new AMQPConnection();
            con.setUrl((String)conURLParam.getValue());
            con.start();
            connections.put(service.getName(), con);
            return con;

        // Next see if there is a default defined in axis2.xml
        }else if (connections.containsKey(AMQPConstants.DEFAULT_CONNECTION)) {
            return (AMQPConnection) connections.get(AMQPConstants.DEFAULT_CONNECTION);

        } else {
            return null;
        }
    }
}
