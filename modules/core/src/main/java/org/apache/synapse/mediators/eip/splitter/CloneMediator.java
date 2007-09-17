package org.apache.synapse.mediators.eip.splitter;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.Mediator;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This mediator will clone the message in to different messages and mediated as specified in
 * the target elements.
 */
public class CloneMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(CloneMediator.class);

    private boolean continueParent = false;

    private List targets = new ArrayList();

    public boolean mediate(MessageContext synCtx) {
        
        if (targets.size() != 0) {
            
            for (int i=0; i<targets.size(); i++) {
                MessageContext newContext = getClonedMessageContext(synCtx, i, targets.size());
                Object o = targets.get(i);
                
                if (o instanceof Target) {
                    Target target = (Target) o;
                    target.mediate(newContext);
                }
            }
        }
        
        return continueParent;
    }

    private MessageContext getClonedMessageContext(MessageContext synCtx,
                                                   int messageSequence, int messageCount) {
        
        MessageContext newCtx = EIPUtils.createNewMessageContext(synCtx, synCtx.getEnvelope());
        newCtx.setProperty(Constants.MESSAGE_SEQUENCE,
                String.valueOf(messageSequence) + Constants.MESSAGE_SEQUENCE_DELEMITER + messageCount);

        return newCtx;
    }

    public boolean isContinueParent() {
        return continueParent;
    }

    public void setContinueParent(boolean continueParent) {
        this.continueParent = continueParent;
    }

    public List getTargets() {
        return targets;
    }

    public void setTargets(List targets) {
        this.targets = targets;
    }

    public void addTarget(Target target) {
        this.targets.add(target);
    }

}
