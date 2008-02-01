package org.apache.synapse.transport.amqp;

import java.util.Map;

import org.apache.axis2.transport.OutTransportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AMQPOutTransportInfo implements OutTransportInfo
{

    private static final Log log = LogFactory.getLog(OutTransportInfo.class);
    private String address = null;
    private String contentType = null;
    private String conURL = null;
    private String exchangeName = null;
    private String routingKey = null;

    public AMQPOutTransportInfo(String address)
    {
        this.address = address;
        if (!address.startsWith(AMQPConstants.AMQP_PREFIX)) {
            handleException("Invalid prefix for a AMQP EPR : " + address);
        } else {
           Map props = URIParser.parse(address);
           conURL = (String)props.get(AMQPConstants.CONNECTION_URL_PARAM);
           routingKey = (String)props.get(AMQPConstants.BINDING_ROUTING_KEY_ATTR);
           exchangeName = (String)props.get(AMQPConstants.EXCHANGE_NAME_PARAM);
        }
    }

    public String getAddress(){
        return address;
    }

    public String getConnectionURL(){
        return conURL;
    }

    public String getExchangeName(){
        return exchangeName;
    }

    public String getRoutingKey(){
        return routingKey;
    }

    public void setContentType(String contentType){
        this.contentType = contentType;
    }

    private void handleException(String s, Exception e) {
        log.error(s, e);
        throw new AMQPSynapseException(s,e);
    }

    private void handleException(String s) {
        log.error(s);
        throw new AMQPSynapseException(s);
    }
}
