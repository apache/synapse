package samples.userguide;

<<<<<<< .mine



=======
import org.apache.axis2.Constants;
>>>>>>> .r369437
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContextConstants;
<<<<<<< .mine

import org.apache.axis2.om.OMElement;
=======
import org.apache.axis2.om.*;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.axis2.transport.http.HttpTransportProperties.ProxyProperties;
>>>>>>> .r369437

import javax.xml.namespace.QName;

public class StockQuoteClient {

<<<<<<< .mine
	/**
	 * @param args
	 *            <p>
	 *            This is a fairly static test client for Synapse. It makes a
	 *            StockQuote request to WebServiceX stockquote service. The EPR
	 *            it is sent to is for WebServiceX, but the actual transport URL
	 *            is designed to go to the Synapse listener.
	 * 
	 */
	public static void main(String[] args) {
=======
    /**
     * @param args <p/>
     *             This is a fairly static test client for Synapse. It makes a
     *             StockQuote request to WebServiceX stockquote service. The EPR it
     *             is sent to is for WebServiceX, but the actual transport URL is
     *             designed to go to the Synapse listener.
     */
    public static void main(String[] args) {
>>>>>>> .r369437

		if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
			System.out
					.println("Usage: StockQuoteClient Symbol StockQuoteURL TransportURL");
			System.out
					.println("\nDefault values: IBM http://www.webservicex.net/stockquote.asmx http://localhost:8080");
			System.out
					.println("\nThe XMethods URL will be used in the <wsa:To> header");
			System.out
					.println("The Transport URL will be used as the actual address to send to");
			System.out
					.println("\nTo bypass Synapse, set the transport URL to the WebServiceX URL: \n"
							+ "e.g. StockQuoteClient IBM http://www.webservicex.net/stockquote.asmx  http://www.webservicex.net/stockquote.asmx \n"
							+ "\nTo demonstrate Synapse virtual URLs, set the URL to http://stockquote\n"
							+ "\nTo demonstrate content-based behaviour, set the Symbol to MSFT\n"
							+ "\nAll examples depend on using the sample synapse.xml");
			System.exit(0);
		}

		String symb = "IBM";
		String xurl = "http://www.webservicex.net/stockquote.asmx";
		String turl = "http://localhost:8080";

		if (args.length > 0)
			symb = args[0];
		if (args.length > 1)
			xurl = args[1];
		if (args.length > 2)
			turl = args[2];

<<<<<<< .mine
		try {
=======
        try {
>>>>>>> .r369437

<<<<<<< .mine
			// step 1 - create a request payload
			OMElement getQuote = StockQuoteXMLHandler
					.createRequestPayload(symb);
=======
            // step 1 - create a request payload
            OMElement getQuote = StockQuoteXMLHandler
                    .createRequestPayload(symb);
>>>>>>> .r369437

<<<<<<< .mine
			// step 2 - set up the call object
=======
            // step 2 - set up the call object
>>>>>>> .r369437

<<<<<<< .mine
			// the wsa:To
			EndpointReference targetEPR = new EndpointReference(xurl);
=======
            // the wsa:To
            EndpointReference targetEPR = new EndpointReference(xurl);
>>>>>>> .r369437

<<<<<<< .mine
			Options options = new Options();
			options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
			options.setTo(targetEPR);
=======
            Options options = new Options();
            //options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setTo(targetEPR);
>>>>>>> .r369437

<<<<<<< .mine
			options.setAction("http://www.webserviceX.NET/GetQuote");
			options.setSoapAction("http://www.webserviceX.NET/GetQuote");
=======
>>>>>>> .r369437

<<<<<<< .mine
			// options.setProperty(MessageContextConstants.CHUNKED,
			// Constants.VALUE_FALSE);
			ServiceClient serviceClient = new ServiceClient();
=======
            options.setAction("http://www.webserviceX.NET/GetQuote");
            options.setSoapAction("http://www.webserviceX.NET/GetQuote");
>>>>>>> .r369437

<<<<<<< .mine
			serviceClient.setOptions(options);
=======
            // options.setProperty(MessageContextConstants.CHUNKED, Constants.VALUE_FALSE);
            ServiceClient serviceClient = new ServiceClient();
>>>>>>> .r369437

<<<<<<< .mine
			// step 3 - Blocking invocation
			OMElement result = serviceClient.sendReceive(getQuote);
			// System.out.println(result);
=======
            serviceClient.setOptions(options);
>>>>>>> .r369437

<<<<<<< .mine
			// step 4 - parse result
=======
            // step 3 - Blocking invocation
            OMElement result = serviceClient.sendReceive(new QName("getQuote"),
                    getQuote);
            // System.out.println(result);
>>>>>>> .r369437

<<<<<<< .mine
			System.out.println("Stock price = $"
					+ StockQuoteXMLHandler.parseResponse(result));
=======
            // step 4 - parse result
>>>>>>> .r369437

<<<<<<< .mine
		} catch (Exception e) {
			e.printStackTrace();
		}
=======
            System.out.println("Stock price = $"
                    + StockQuoteXMLHandler.parseResponse(result));
>>>>>>> .r369437

<<<<<<< .mine
	}

}
=======
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}>>>>>>> .r369437
