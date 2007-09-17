package org.apache.synapse.config.xml;

import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * 
 */
public class TargetFactory {

    private static final Log log = LogFactory.getLog(TargetFactory.class);
    private static final QName TARGET_Q = new QName(Constants.SYNAPSE_NAMESPACE, "target");

    public static Target createTarget(OMElement elem) {

        if (!TARGET_Q.equals(elem.getQName())) {
            handleException("Element does not match with the target QName");
        }

        Target target = new Target();
        OMAttribute toAttr = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "to"));
        if (toAttr != null && toAttr.getAttributeValue() != null) {
            target.setTo(toAttr.getAttributeValue());
        }

        OMAttribute soapAction = elem.getAttribute(
                new QName(Constants.NULL_NAMESPACE, "soapAction"));
        if (soapAction != null && soapAction.getAttributeValue() != null) {
            target.setSoapAction(soapAction.getAttributeValue());
        }

        OMAttribute sequenceAttr = elem.getAttribute(
                new QName(Constants.NULL_NAMESPACE, "sequence"));
        if (sequenceAttr != null && sequenceAttr.getAttributeValue() != null) {
            target.setSequenceRef(sequenceAttr.getAttributeValue());
        }

        OMAttribute endpointAttr = elem.getAttribute(
                new QName(Constants.NULL_NAMESPACE, "endpoint"));
        if (endpointAttr != null && endpointAttr.getAttributeValue() != null) {
            target.setEndpointRef(endpointAttr.getAttributeValue());
        }

        OMElement sequence = elem.getFirstChildWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "sequence"));
        if (sequence != null) {
            SequenceMediatorFactory fac = new SequenceMediatorFactory();
            target.setSequence(fac.createAnonymousSequence(sequence));
        }

        OMElement endpoint = elem.getFirstChildWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "endpoint"));
        if (endpoint != null) {
            target.setEndpoint(EndpointAbstractFactory.
                    getEndpointFactroy(endpoint).createEndpoint(endpoint, true));
        }

        return target;
    }

    private static void handleException (String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new SynapseException(message);
    }
}
