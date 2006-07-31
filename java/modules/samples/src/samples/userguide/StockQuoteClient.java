package samples.userguide;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;

import javax.xml.namespace.QName;

import samples.common.StockQuoteHandler;

/**
 * The EPR to the actual service is set, but the transport is set to
 * the Synapse url.
 */
public class StockQuoteClient {

    public static void main(String[] args) {

        String symbol  = "IBM";
        String xurl    = "http://localhost:9000/axis2/services/SimpleStockQuoteService";
        String turl    = "http://localhost:8080";
        String sAction = "urn:getQuote";
        String repo    = "client_repo";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) xurl   = args[1];
        if (args.length > 2) turl   = args[2];
        if (args.length > 3) repo   = args[3];

        try {
            OMElement getQuote = StockQuoteHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            if (turl != null)
                options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setAction(sAction);

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            serviceClient.engageModule(new QName("addressing"));
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Standard :: Stock price = $" +
                StockQuoteHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
