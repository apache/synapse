

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.FakeMediatorFinder;
import org.apache.synapse.RuleList;
import org.apache.synapse.SimpleDispatcher;
import org.jaxen.JaxenException;

public class TestFramework {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FakeMediatorFinder fmf = new FakeMediatorFinder();
		RuleList  rl = null;
		try {
			FileInputStream fis = new FileInputStream(args[0]);
			rl = new RuleList(fis);
			System.out.println(rl.toString());
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		String  xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:mrns0=\"urn:xmethods-delayed-quotes\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">   <soap:Body soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">      <mrns0:getQuote>         <symbol xsi:type=\"xs:string\">IBM</symbol>      </mrns0:getQuote>   </soap:Body></soap:Envelope>";
		
		byte arr[] = xml.getBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(arr);

		XMLStreamReader reader = null;
		try {
			XMLInputFactory xif= XMLInputFactory.newInstance();
			reader= xif.createXMLStreamReader(bais);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		StAXOMBuilder builder= new StAXOMBuilder(reader);
		OMElement testEl = builder.getDocumentElement();
		
		SimpleDispatcher sd = new SimpleDispatcher(rl, fmf);
		try {
			sd.execute(testEl);
		} catch (JaxenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

}
