package samples.userguide;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;




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

    public static String parseResponse(OMElement result)
            throws XMLStreamException {
        QName gQR =
                new QName("http://www.webserviceX.NET/", "GetQuoteResponse");

        OMElement qResp = (OMElement) result.getChildrenWithName(gQR).next();

        String text = qResp.getText();


        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(
                text.getBytes()));

        OMElement parse = builder.getDocumentElement();

        OMElement last = (OMElement) parse.getFirstElement().getFirstElement()
                .getNextOMSibling();
        OMText lastText = (OMText) last.getFirstOMChild();
        return lastText.getText();

    }
	


}
