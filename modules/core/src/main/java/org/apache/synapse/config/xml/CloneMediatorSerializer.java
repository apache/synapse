package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.Target;

import java.util.Iterator;

/**
 * 
 */
public class CloneMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        OMElement cloneElem = fac.createOMElement("clone", synNS);
        finalizeSerialization(cloneElem, m);

        CloneMediator clone = (CloneMediator) m;
        if (clone.isContinueParent()) {
            cloneElem.addAttribute("continueParent", Boolean.toString(true), nullNS);
        }

        for (Iterator itr = clone.getTargets().iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (o instanceof Target) {
                cloneElem.addChild(TargetSerializer.serializeTarget((Target) o));
            }
        }

        if (parent != null) {
            parent.addChild(cloneElem);
        }

        return cloneElem;
    }

    public String getMediatorClassName() {
        return CloneMediator.class.getName();
    }
}
