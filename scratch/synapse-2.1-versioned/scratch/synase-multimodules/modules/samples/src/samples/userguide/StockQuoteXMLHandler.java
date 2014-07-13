package samples.userguide;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;


import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.om.OMText;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;


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
	


}
