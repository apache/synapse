package samples.userguide;



import javax.xml.namespace.QName;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;

import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.OMText;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;

public class DumbStockQuoteClient {

	/**
	 * 
	 * <p>
	 * This is a fairly static test client for Synapse. It makes a StockQuote
	 * request to XMethods stockquote service. There is no EPR and there is no proxy config.
	 * It's sort of a Gateway case. It relies on a Synapse config that will look at the URL or 
	 * message and send it to the right place
	 */
	public static void main(String[] args) {

		if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
			System.out
					.println("This client demonstrates Synapse as a gateway\n"
							+ "Usage: ProxyStockQuoteClient Symbol SynapseURL");
			System.out
					.println("\nDefault values: IBM http://localhost:8080/StockQuote"
							+ "\nAll examples depend on using the sample synapse.xml");
			System.exit(0);
		}

		String symb = "IBM";
		String url = "http://localhost:8080/StockQuote";
		

		if (args.length > 0)
			symb = args[0];
		if (args.length > 1)
			url = args[1];

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
			EndpointReference targetEPR = new EndpointReference(url);

			Options options = new Options();

			options.setTo(targetEPR);

						
			// create a lightweight Axis Config with no addressing to demonstrate "dumb" SOAP
			AxisConfiguration ac = new AxisConfiguration();
			ConfigurationContext cc = new ConfigurationContext(ac);
			AxisServiceGroup asg = new AxisServiceGroup(ac);
			AxisService as = new AxisService("AnonymousService");
			asg.addService(as);
			ServiceGroupContext sgc = new ServiceGroupContext(cc, asg);
			ServiceContext sc = sgc.getServiceContext(as);
			AxisOperation axisOperationTemplate = new OutInAxisOperation(
					new QName("getQuote"));
			as.addOperation(axisOperationTemplate);
			cc.getAxisConfiguration().addService(as);
			TransportOutDescription tod = new TransportOutDescription(
					new QName(Constants.TRANSPORT_HTTP));
			tod.setSender(new CommonsHTTPTransportSender());
			ac.addTransportOut(tod);

			// make the ServiceClient
			ServiceClient serviceClient = new ServiceClient();

			serviceClient.setOptions(options);

			// step 3 - Blocking invocation
			OMElement result = serviceClient.sendReceive(getQuote);

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
