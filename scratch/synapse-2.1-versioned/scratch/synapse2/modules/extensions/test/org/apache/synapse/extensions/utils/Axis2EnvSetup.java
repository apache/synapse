package org.apache.synapse.extensions.utils;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.axis2.SynapseMessageReceiver;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;

/**
 * Utils for extensions
 */
public class Axis2EnvSetup {
    public static MessageContext axis2Deployment(String testingRepository)
            throws AxisFault {
        final ConfigurationContext configCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(testingRepository,null);
        MessageContext msgCtx = new MessageContext();
        msgCtx.setConfigurationContext(configCtx);
        msgCtx.setEnvelope(testEnvSetup());
        msgCtx.setServerSide(true);

        AxisConfiguration axisConfiguration = msgCtx.getConfigurationContext()
                .getAxisConfiguration();
        AxisService service = new AxisService("se");
        msgCtx.setAxisService(service);
        service.setClassLoader(axisConfiguration.getServiceClassLoader());
        AxisOperation axisOp = new InOutAxisOperation(
                new QName("op"));
        msgCtx.setAxisOperation(axisOp);
        axisOp.setMessageReceiver(new SynapseMessageReceiver());
        service.addOperation(axisOp);
        axisConfiguration.addService(service);
        msgCtx.setTo(
                new EndpointReference("/axis2/services/" + "se" + "/" + "op"));
        msgCtx.setSoapAction("op");
        return msgCtx;
    }

    private static SOAPEnvelope testEnvSetup() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        SOAPEnvelope env = OMAbstractFactory.getSOAP11Factory()
                .getDefaultEnvelope();
        OMDocument doc = fac.createOMDocument();
        doc.addChild(env);
        OMElement ele = fac.createOMElement("text", "urn:text-body", "ns");
        env.getBody().addChild(ele);
        return env;
    }

    public static OMElement getSynapseConfigElement(String synapseXml)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance()
                .createXMLStreamReader(
                        new ByteArrayInputStream(synapseXml.getBytes()));
        OMFactory fac = OMAbstractFactory.getOMFactory();
        StAXOMBuilder staxBuilder = new StAXOMBuilder(fac, parser);
        return staxBuilder.getDocumentElement();

    }
}
