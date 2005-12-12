package samples.userguide;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Call;
import org.apache.axis2.client.MEPClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.description.TransportInDescription;
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
	 * This is a fairly static test client for Synapse. It makes a StockQuote
	 * request to XMethods stockquote service. The EPR it is sent to is for
	 * XMethods, but the actual transport URL is designed to go to the Synapse
	 * listener.
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
		String purl = "http://localhost:8080/soap";

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
			EndpointReference targetEPR = new EndpointReference(purl);

			Options options = new Options();

			options.setTo(targetEPR);

			URL url = new URL(purl);

			HttpTransportProperties httpProps = new HttpTransportProperties();
			HttpTransportProperties.ProxyProperties proxyProperties = httpProps.new ProxyProperties();
			// proxyProperties.setProxyName(url.getHost());
			// proxyProperties.setProxyPort(url.getPort());

			proxyProperties.setProxyName("localhost");
			proxyProperties.setProxyPort(6060);
			proxyProperties.setUserName("anonymous");
			proxyProperties.setPassWord("anonymous");
			proxyProperties.setDomain("anonymous");
			options.setProperty(HTTPConstants.PROXY, proxyProperties);

			AxisConfiguration ac = new AxisConfiguration();
			ConfigurationContext cc = new ConfigurationContext(ac);
			AxisServiceGroup asg = new AxisServiceGroup(ac);
			AxisService as = new AxisService(new QName("AnonymousService"));

			asg.addService(as);

			ServiceGroupContext sgc = new ServiceGroupContext(cc, asg);

			ServiceContext sc = sgc.getServiceContext("AnonymousService");

			AxisOperation axisOperationTemplate = new OutInAxisOperation(
					new QName("getQuote"));

			as.addOperation(axisOperationTemplate);
			cc.getAxisConfiguration().addService(as);
			TransportOutDescription tod = new TransportOutDescription(
					new QName(Constants.TRANSPORT_HTTP));
			tod.setSender(new CommonsHTTPTransportSender());
			ac.addTransportOut(tod);

			Call call;
			call = new Call(sc);

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
