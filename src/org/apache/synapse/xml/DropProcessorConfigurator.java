package org.apache.synapse.xml;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.DropProcessor;
import org.apache.axis2.om.OMElement;

import javax.xml.namespace.QName;

/**
 */
public class DropProcessorConfigurator extends AbstractProcessorConfigurator{

    private static final QName DROP_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"drop");
    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        return new DropProcessor();
    }

    public QName getTagQName() {
        return DROP_Q;
    }
}
