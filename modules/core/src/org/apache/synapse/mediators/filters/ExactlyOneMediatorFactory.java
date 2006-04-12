package org.apache.synapse.mediators.filters;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.ExactlyOneMediator;
import org.apache.synapse.xml.AbstractListMediatorFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.axiom.om.OMElement;
import javax.xml.namespace.QName;

/**
 * This will create <exactlyone>
 *                       <regex/>[0..n]
 *                       <xpath/>[0..n]
 *                       <default/> ?
 *                  </exactlyone>
 *
 *
 */
public class ExactlyOneMediatorFactory extends AbstractListMediatorFactory {

    private static final String EXACTLYONE = "exactlyone";

	private static final QName SWITCH_Q = new QName(org.apache.synapse.xml.Constants.SYNAPSE_NAMESPACE,
			EXACTLYONE);


    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        ExactlyOneMediator exactlyOne = new ExactlyOneMediator();
        super.addChildrenAndSetName(se,el,exactlyOne);
        // now validate all children are ConditionProcessors
        
        
        return exactlyOne;
    }

    public QName getTagQName() {
        return SWITCH_Q;
    }
}
