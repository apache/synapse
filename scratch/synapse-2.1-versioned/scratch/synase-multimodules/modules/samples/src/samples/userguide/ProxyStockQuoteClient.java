package samples.userguide;

import java.net.URL;

import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;

import org.apache.axis2.om.OMElement;

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

        if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
            System.out
                    .println("This client demonstrates Synapse as a proxy\n"
                            +
                            "Usage: ProxyStockQuoteClient Symbol StockQuoteURL ProxyURL");
            System.out
                    .println(
                            "\nDefault values: IBM http://www.webservicex.net/stockquote.asmx http://localhost:8080");
            System.out
                    .println(
                            "\nThe XMethods URL will be used in the <wsa:To> header");
            System.out.println("The Proxy URL will be used as an HTTP proxy");
            System.out
                    .println(
                            "\nTo demonstrate Synapse virtual URLs, set the URL to http://stockquote\n"
                                    +
                                    "\nTo demonstrate content-based behaviour, set the Symbol to MSFT\n"
                                    +
                                    "\nAll examples depend on using the sample synapse.xml");
            System.exit(0);
        }

        String symb = "IBM";
        String xurl = "http://www.webservicex.net/stockquote.asmx";
        String purl = "http://localhost:8080";

        if (args.length > 0)
            symb = args[0];
        if (args.length > 1)
            xurl = args[1];
        if (args.length > 2)
            purl = args[2];

        boolean repository = false;
        if (args.length > 3) repository = true;

        try {
            ServiceClient serviceClient;
            if (repository) {
                ConfigurationContextFactory fac =
                        new ConfigurationContextFactory();
                ConfigurationContext configContext =
                        fac.createConfigurationContextFromFileSystem(args[3]);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }

            // step 1 - create a request payload
            OMElement getQuote = StockQuoteXMLHandler
                    .createRequestPayload(symb);

            // step 2 - set up the call object

            // the wsa:To
            EndpointReference targetEPR = new EndpointReference(xurl);

            Options options = new Options();
            options.setTo(targetEPR);

            URL url = new URL(purl);

            // engage HTTP Proxy

            HttpTransportProperties httpProps = new HttpTransportProperties();

            HttpTransportProperties.ProxyProperties proxyProperties =
                    httpProps.new ProxyProperties();
            proxyProperties.setProxyName(url.getHost());
            proxyProperties.setProxyPort(url.getPort());
            proxyProperties.setUserName("");
            proxyProperties.setPassWord("");
            proxyProperties.setDomain("");

            options.setProperty(HTTPConstants.PROXY, proxyProperties);

            options.setAction("http://www.webserviceX.NET/GetQuote");
            // create a lightweight Axis Config with no addressing to
            // demonstrate "dumb" SOAP
            options.setProperty(
                    Constants.Configuration.DISABLE_ADDRESSING_FOR_OUT_MESSAGES,
                    new Boolean(true));


            serviceClient.setOptions(options);

            // step 3 - Blocking invocation
            OMElement result = serviceClient.sendReceive(getQuote);
            // System.out.println(result);

            // step 4 - parse result

            System.out.println("Stock price = $"
                    + StockQuoteXMLHandler.parseResponse(result));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
