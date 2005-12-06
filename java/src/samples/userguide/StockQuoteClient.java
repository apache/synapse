package samples.userguide;

import javax.xml.namespace.QName;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Call;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContextConstants;

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.OMText;

public class StockQuoteClient {

	/**
	 * @param args
	 *            <p>
	 *            This is a fairly static test client for Synapse. It makes a
	 *            StockQuote request to XMethods stockquote service. The EPR it
	 *            is sent to is for XMethods, but the actual transport URL is
	 *            designed to go to the Synapse listener.
	 * 
	 */
	public static void main(String[] args) {

		if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
			System.out
					.println("Usage: StockQuoteClient Symbol XmethodsURL TransportURL");
			System.out
					.println("\nDefault values: IBM http://64.124.140.30:9090/soap http://localhost:8080");
			System.out
					.println("\nThe XMethods URL will be used in the <wsa:To> header");
			System.out
					.println("The Transport URL will be used as the actual address to send to");
			System.out
					.println("\nTo bypass Synapse, set the transport URL to the XMethods URL: \n"
							+ "e.g. StockQuoteClient IBM http://64.124.140.30:9090/soap http://64.124.140.30:9090/soap\n"
							+ "\nTo demonstrate Synapse virtual URLs, set the xmethods URL to urn:xmethods-delayed-quotes\n"
							+ "\nTo demonstrate content-based behaviour, set the Symbol to MSFT\n"
							+ "\nAll examples depend on using the sample synapse.xml");
			System.exit(0);
		}

		String symb = "IBM";
		String xurl = "http://64.124.140.30:9090/soap";
		String turl = "http://localhost:8080";

		if (args.length > 0)
			symb = args[0];
		if (args.length > 1)
			xurl = args[1];
		if (args.length > 2)
			turl = args[2];

		try {

			// step 1 - create a request payload
			OMFactory factory = OMAbstractFactory.getOMFactory(); // access to
			// OM
			OMNamespace xNs = factory.createOMNamespace(
					"urn:xmethods-delayed-quotes", "x");
			OMElement getQuote = factory.createOMElement("getQuote", xNs);
			OMElement symbol = factory.createOMElement("symbol", xNs);
			getQuote.addChild(symbol);
			symbol.setText(symb);

			// step 2 - set up the call object
			
			// the wsa:To
			EndpointReference targetEPR = new EndpointReference(xurl);

			Options options = new Options();
			options.setTo(targetEPR);

			// the transport URL
			options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);

			Call call;
			call = new Call();
			call.setClientOptions(options);
			
			// step 3 - Blocking invocation
			OMElement result = call.invokeBlocking("getQuote", getQuote);

			// step 4 - parse result
			QName gQR = new QName("urn:xmethods-delayed-quotes",
					"getQuoteResponse");
			QName Result = new QName("Result");
			OMElement qResp = (OMElement) result.getChildrenWithName(gQR)
					.next();
			OMText res = (OMText) qResp.getChildrenWithName(Result).next();

			System.out.println("Stock price = $" + res.getText());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
