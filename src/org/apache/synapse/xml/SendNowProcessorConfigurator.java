package org.apache.synapse.xml;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.SendNowProcessor;
import org.apache.axis2.om.OMElement;

import javax.xml.namespace.QName;

/**
 */
public class SendNowProcessorConfigurator extends AbstractProcessorConfigurator{
    private static final QName SEND_NOW_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"sendnow");
    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        return new SendNowProcessor();
    }

    public QName getTagQName() {
        return SEND_NOW_Q;
    }
}
