package org.apache.synapse.processors.conditions;

import org.apache.synapse.xml.AbstractListProcessorConfigurator;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.axis2.om.OMElement;

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
public class ExactlyOneProcessorConfigurator extends AbstractListProcessorConfigurator {

    private static final String EXACTLYONE = "exactlyone";

	private static final QName SWITCH_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			EXACTLYONE);


    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        ExactlyOneProcessor exactlyOneProcessor = new ExactlyOneProcessor();
        super.addChildrenAndSetName(se,el,exactlyOneProcessor);

        return exactlyOneProcessor;
    }

    public QName getTagQName() {
        return SWITCH_Q;
    }
}
