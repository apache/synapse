package samples.userguide;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import samples.common.StockQuoteHandler;

import java.net.URL;


public class ProxyStockQuoteClient {

    /**
     * <p/>
     * This is a fairly static test client for Synapse using the HTTP Proxy
     * model. It makes a StockQuote request to XMethods stockquote service.
     * There is no WS-Addressing To URL but we set the HTTP proxy URL to point
     * to Synapse. This results in the destination XMethods URL being embedded
     * in the POST header. Synapse will pick this out and use it to direct the
     * message
     */
    public static void main(String[] args) {

        String symbol = "IBM";
        String xurl   = "http://localhost:9000/axis2/services/SimpleStockQuoteService";
        String purl   = "http://localhost:8081";
        String sAction= "urn:getQuote";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) xurl   = args[1];
        if (args.length > 2) purl   = args[2];

        try {
            OMElement getQuote = StockQuoteHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            options.setAction(sAction);

            ServiceClient serviceClient = new ServiceClient();

            // engage HTTP Proxy

            HttpTransportProperties.ProxyProperties proxyProperties =new
                HttpTransportProperties.ProxyProperties();
            URL url = new URL(purl);
            proxyProperties.setProxyName(url.getHost());
            proxyProperties.setProxyPort(url.getPort());
            proxyProperties.setUserName("");
            proxyProperties.setPassWord("");
            proxyProperties.setDomain("");
            options.setProperty(HTTPConstants.PROXY, proxyProperties);

            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Standard :: Stock price = $" +
                StockQuoteHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
