package samples.userguide;



import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.addressing.EndpointReference;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axiom.om.OMElement;

public class DumbStockQuoteClient {

    /**
     * <p/>
     * This is a fairly static test client for Synapse. It makes a StockQuote
     * request to XMethods stockquote service. There is no EPR and there is no
     * proxy config. It's sort of a Gateway case. It relies on a Synapse config
     * that will look at the URL or message and send it to the right place
     */
    public static void main(String[] args) {

        if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
            System.out
                    .println("This client demonstrates Synapse as a gateway\n"
                            + "Usage: DumbStockQuoteClient Symbol SynapseURL");
            System.out
                    .println(
                            "\nDefault values: IBM http://localhost:8080/StockQuote"
                                    +
                                    "\nAll examples depend on using the sample synapse.xml");
            System.exit(0);
        }

        String symb = "IBM";
        String url = "http://localhost:8080/StockQuote";

        if (args.length > 0)
            symb = args[0];
        if (args.length > 1)
            url = args[1];
        boolean repository = false;
        if (args.length > 2) repository = true;
        try {
            ServiceClient serviceClient;
            if (repository) {

                ConfigurationContext configContext =
                        ConfigurationContextFactory.createConfigurationContextFromFileSystem(args[2],null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }

            // step 1 - create a request payload
            OMElement getQuote = StockQuoteXMLHandler
                    .createRequestPayload(symb);

            // step 2 - set up the call object

            // the wsa:To
            EndpointReference targetEPR = new EndpointReference(url);

            Options options = new Options();
            options.setTo(targetEPR);

            options.setAction("http://www.webserviceX.NET/GetQuote");
                        
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
