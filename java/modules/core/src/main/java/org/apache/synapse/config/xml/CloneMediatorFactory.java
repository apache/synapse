package org.apache.synapse.config.xml;

import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractFactory;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * The &lt;clone&gt; element is used to copy messages in Synapse to simillar messages but with
 * different message contexts and mediated using the specified targets
 *
 * <pre>
 *  &lt;clone continueParent=(true | false)&gt;
 *   &lt;target to="TO address" [soapAction="urn:Action"] sequence="sequence ref"
 *                                                         endpoint="endpoint ref"&gt;
 *    &lt;sequence&gt; (mediator +) &lt;/sequence&gt;
 *    &lt;endpoint&gt; endpoint &lt;/endpoint&gt;
 *   &lt;/target&gt;
 *  &lt;/iterate&gt;
 * </pre>
 */
public class CloneMediatorFactory extends AbstractMediatorFactory {

    private static final QName CLONE_Q = new QName(Constants.SYNAPSE_NAMESPACE, "clone");
    
    public Mediator createMediator(OMElement elem) {

        CloneMediator mediator = new CloneMediator();
        initMediator(mediator, elem);
        OMAttribute continueParent = elem.getAttribute(new QName(
                Constants.NULL_NAMESPACE, "continueParent"));
        if (continueParent != null) {
            mediator.setContinueParent(Boolean.valueOf(continueParent.getAttributeValue()).booleanValue());
        }

        Iterator targetElements = elem.getChildrenWithName(
                new QName(Constants.SYNAPSE_NAMESPACE, "target"));
        while (targetElements.hasNext()) {
            mediator.addTarget(TargetFactory.createTarget((OMElement) targetElements.next()));
        }

        return mediator;
    }

    public QName getTagQName() {
        return CLONE_Q;
    }
}
