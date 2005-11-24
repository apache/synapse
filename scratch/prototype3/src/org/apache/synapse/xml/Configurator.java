package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;

/**
 * @author Paul Fremantle
 * 
 * This class will get a Synapse processor for any given element in the
 * synapse.xml
 */
public class Configurator {

	/**
	 * This method returns a Processor given an OMElement. This will be used
	 * recursively by the elements which contain processor elements themselves
	 * (e.g. rules)
	 * 
	 * @param synapseEnv
	 * @param element
	 * @return Processor
	 */
	public static Processor getProcessor(SynapseEnvironment synapseEnv, OMElement element) {
		OMNamespace n = element.getNamespace();
		Class cls = ProcessorFinder.find(new QName(n.getName(), element
				.getLocalName()));
		try {
			Processor p = (Processor) cls.newInstance();
			p.compile(synapseEnv, element);
			return p;
		} catch (InstantiationException e) {
			throw new SynapseException(e);
		} catch (IllegalAccessException e) {
			throw new SynapseException(e);
		}
	}
}
