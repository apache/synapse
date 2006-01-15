package samples.userguide;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
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
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;

public class StockQuoteXMLHandler {

	public static OMElement createRequestPayload(String symb) {
		OMFactory factory = OMAbstractFactory.getOMFactory(); // access to
		// OM
		OMNamespace xNs = factory.createOMNamespace(
				"http://www.webserviceX.NET/", "");
		OMElement getQuote = factory.createOMElement("GetQuote", xNs);
		OMElement symbol = factory.createOMElement("symbol", xNs);
		getQuote.addChild(symbol);
		symbol.setText(symb);
		return getQuote;
	}

	public static String parseResponse(OMElement result) throws XMLStreamException {
		QName gQR = new QName("http://www.webserviceX.NET/", "GetQuoteResponse");

		OMElement qResp = (OMElement) result.getChildrenWithName(gQR).next();

		String text = ((OMText) qResp.getFirstOMChild()).getText();
		// this odd webservice embeds XML as a string inside other XML
		// hmmmm can't say its useful!

		

		
			StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(
					text.getBytes()));

			OMElement parse = builder.getDocumentElement();
		
		OMElement last = (OMElement) parse.getFirstElement().getFirstElement()
				.getNextOMSibling();
		OMText lastText = (OMText) last.getFirstOMChild();
		return lastText.getText();
		
	}
	
	public static ServiceClient createServiceClient() throws AxisFault {
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

	
		ServiceClient serviceClient = new ServiceClient(cc, as);
		return serviceClient;
	}

}
