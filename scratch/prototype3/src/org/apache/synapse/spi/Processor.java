package org.apache.synapse.spi;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

/**
 * @author Paul Fremantle This interface implements the core of Synapse. The
 *         processor is bootstrapped from a piece of config XML (the el
 *         OMElement).
 * 
 * The processor then deals with a message. It returns false if no further
 * processing is desired It can have a name (or null)
 * 
 * Processors can either devolve processing to other processors (e.g. a rule,
 * stage, etc) or deal with the message itself (e.g. mediator)
 * 
 */
public interface Processor {
	public void compile(SynapseEnvironment se, OMElement el);

	public boolean process(SynapseEnvironment se, SOAPMessageContext smc);

	public String getName();

	public QName getTagQName();
}
