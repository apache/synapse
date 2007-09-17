package org.apache.synapse.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractSerializer;

/**
 * 
 */
public class TargetSerializer {

    private static final Log log = LogFactory.getLog(TargetSerializer.class);
    private static final OMFactory fac = OMAbstractFactory.getOMFactory();
    private static final OMNamespace synNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE, "syn");
    private static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");

    public static OMElement serializeTarget(Target target) {

        OMElement targetElem = fac.createOMElement("target", synNS);
        if (target.getTo() != null) {
            targetElem.addAttribute("to", target.getTo(), nullNS);
        }

        if (target.getSoapAction() != null) {
            targetElem.addAttribute("soapAction", target.getSoapAction(), nullNS);
        }

        if (target.getSequenceRef() != null) {
            targetElem.addAttribute("sequence", target.getSequenceRef(), nullNS);
        }

        if (target.getEndpointRef() != null) {
            targetElem.addAttribute("endpoint", target.getEndpointRef(), nullNS);
        }

        if (target.getSequence() != null) {
            SequenceMediatorSerializer serializer = new SequenceMediatorSerializer();
            serializer.serializeAnonymousSequence(targetElem, target.getSequence());
        }

        if (target.getEndpoint() != null) {
            targetElem.addChild(EndpointAbstractSerializer.getEndpointSerializer(
                    target.getEndpoint()).serializeEndpoint(target.getEndpoint()));
        }

        return targetElem;
    }
}
