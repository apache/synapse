package org.apache.synapse.transport.amqp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** sample uri formats - this is temporary until the AMQP WG defines a proper addressing scheme
*
* uri="amqp:/direct/amq.direct?transport.amqp.RoutingKey=SimpleStockQuoteService&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672"
* amqp:/topic/amq.topic?transport.amqp.RoutingKey=weather.us.ny&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672
*/
public class URIParser
{

    public static Map parse(String uri){
       Map props = new HashMap();
       String temp = uri.substring(6,uri.indexOf("?"));
       String exchangeType =  temp.substring(0,temp.indexOf("/"));
       String exchangeName =  temp.substring(temp.indexOf("/"),temp.length());
       if (exchangeType == null || exchangeType.trim().equals("")){
          throw new IllegalArgumentException("exchange type cannot be null");
       }
       if (exchangeType == null || exchangeType.trim().equals("")){
           throw new IllegalArgumentException("exchange name cannot be null");
        }
       props.put(AMQPConstants.EXCHANGE_NAME_PARAM, exchangeName);
       props.put(AMQPConstants.EXCHANGE_TYPE_PARAM, exchangeType);
       String paramStr =  uri.substring(uri.indexOf("?")+1,uri.length());
       String[] params = paramStr.split("&amp;");
       for (String param:params){
           String key = param.substring(0,param.indexOf("="));
           String value = param.substring(param.indexOf("=")+1,param.length());
           if("connectionURL".equals(key)){
               key = AMQPConstants.CONNECTION_URL_PARAM;
           }
           props.put(key, value);
       }
       return props;
    }

    /**
     * Get the EPR for the given AMQP details
     * the form of the URL is
     * uri="amqp:/direct?routingKey=SimpleStockQuoteService&amp;connectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672"
     *
     * Creates the EPR with the primary binding
     *
     */
    public static String getEPR(List<AMQPBinding> list, String url) {

        String epr = null;
        for (AMQPBinding binding:list){

            if (binding.isPrimary()){
                StringBuffer sb = new StringBuffer();
                sb.append(AMQPConstants.AMQP_PREFIX).append("/").append(binding.getExchangeType());
                sb.append("/").append(binding.getExchangeName());
                sb.append("?").append(AMQPConstants.BINDING_ROUTING_KEY_ATTR).append("=").append(binding.getRoutingKey());
                sb.append("&amp;").append("connectionURL=").append(url);
                epr = sb.toString();
            }
        }

        // If no primary is defined just get the first
        if(epr == null){
            AMQPBinding binding = list.get(0);
            StringBuffer sb = new StringBuffer();
            sb.append(AMQPConstants.AMQP_PREFIX).append("/").append(binding.getExchangeType());
            sb.append("/").append(binding.getExchangeName());
            sb.append("?").append(AMQPConstants.BINDING_ROUTING_KEY_ATTR).append("=").append(binding.getRoutingKey());
            sb.append("&amp;").append("connectionURL=").append(url);
            epr = sb.toString();
        }

        return epr;
    }

    public static void main(String[] args){
        Map p = URIParser.parse("amqp:/direct?routing_key=SimpleStockQuoteService&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672");
    }
}
