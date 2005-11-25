package org.apache.synapse.axis2;

import org.apache.axis2.om.OMElement;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.xml.Configurator;

public class Axis2SynapseEnvironment implements SynapseEnvironment {
	private Processor processor = null;

	private ClassLoader cl = null;

	public Axis2SynapseEnvironment(OMElement synapseConfiguration,
			ClassLoader cl) {
		super();
		this.cl = cl;
		processor = Configurator.getProcessor(this, synapseConfiguration);
	}

	public void injectMessage(SynapseMessage smc) {
		processor.process(this, smc);
	}

	public ClassLoader getClassLoader() {
		return cl;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void sendOn(SynapseMessage smc) {
		Axis2Sender.sendOn(smc);

	}

	public void sendBack(SynapseMessage smc) {
		Axis2Sender.sendBack(smc);

	}

}
