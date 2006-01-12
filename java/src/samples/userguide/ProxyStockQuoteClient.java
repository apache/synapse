package samples.userguide;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;

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
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;

public class ProxyStockQuoteClient {

	/**
	 * 
	 * <p>
	 * This is a fairly static test client for Synapse using the HTTP Proxy model.
	 *  It makes a StockQuote request to XMethods stockquote service. There is no
	 *  WS-Addressing To URL but we set the HTTP proxy URL to point to Synapse. 
	 *  This results in the destination XMethods URL being embedded in the POST header. 
	 *  Synapse will pick this out and use it to direct the message
	 * 
	 */
	public static void main(String[] args) {

		if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
			System.out
					.println("This client demonstrates Synapse as a proxy\n"
							+ "Usage: ProxyStockQuoteClient Symbol XmethodsURL ProxyURL");
			System.out
					.println("\nDefault values: IBM http://64.124.140.30:9090/soap http://localhost:8080");
			System.out
					.println("\nThe XMethods URL will be used in the <wsa:To> header");
			System.out.println("The Proxy URL will be used as an HTTP proxy");
			System.out
					.println("\nTo demonstrate Synapse virtual URLs, set the xmethods URL to urn:xmethods-delayed-quotes\n"
							+ "\nTo demonstrate content-based behaviour, set the Symbol to MSFT\n"
							+ "\nAll examples depend on using the sample synapse.xml");
			System.exit(0);
		}

		String symb = "IBM";
		String xurl = "http://64.124.140.30:9090/soap";
		String purl = "http://localhost:8080";

		if (args.length > 0)
			symb = args[0];
		if (args.length > 1)
			xurl = args[1];
		if (args.length > 2)
			purl = args[2];

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

			URL url = new URL(purl);

			
			//engage HTTP Proxy
			
			HttpTransportProperties httpProps = new HttpTransportProperties();
			HttpTransportProperties.ProxyProperties proxyProperties = httpProps.new ProxyProperties();
			proxyProperties.setProxyName(url.getHost());
			proxyProperties.setProxyPort(url.getPort());

			
			options.setProperty(HTTPConstants.PROXY, proxyProperties);

			
			// create a lightweight Axis Config with no addressing to demonstrate "dumb" SOAP
			AxisConfiguration ac = new AxisConfiguration();
			ConfigurationContext cc = new ConfigurationContext(ac);
			AxisServiceGroup asg = new AxisServiceGroup(ac);
			AxisService as = new AxisService("AnonymousService");
			asg.addService(as);
			
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
