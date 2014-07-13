package org.apache.synapse.transport.amqp;

import java.util.HashMap;
import java.util.Map;

/** sample uri formats - this is temporary until the AMQP WG defines a proper addressing scheme
*
* uri="amqp:/direct?transport.amqp.RoutingKey=SimpleStockQuoteService&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672"
* amqp:/topic?transport.amqp.RoutingKey=weather.us.ny&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672
*/
public class URIParser
{

    public static Map parse(String uri){
       Map props = new HashMap();
       String dest = uri.substring(6,uri.indexOf("?"));
       if (dest == null || dest.trim().equals("")){
          throw new IllegalArgumentException("destination cannot be null");
       }
       props.put(AMQPConstants.EXCHANGE_NAME_PARAM, dest);
       String paramStr =  uri.substring(uri.indexOf("?")+1,uri.length());
       String[] params = paramStr.split("&amp;");
       for (String param:params){
           String key = param.substring(0,param.indexOf("="));
           String value = param.substring(param.indexOf("=")+1,param.length());
           props.put(key, value);
       }
       return props;
    }

    public static void main(String[] args){
        Map p = URIParser.parse("amqp:/direct?routing_key=SimpleStockQuoteService&amp;transport.amqp.ConnectionURL=qpid:virtualhost=test;client_id=foo@tcp:myhost.com:5672");
    }
}
