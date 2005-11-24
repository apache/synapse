package org.apache.synapse.axis2;

import org.apache.axis2.om.OMElement;

import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;

import org.apache.synapse.processors.SynapseProcessor;
import org.apache.synapse.spi.Processor;

public class Axis2SynapseEnvironment implements SynapseEnvironment {
	private Processor processor = new SynapseProcessor();

	private ClassLoader cl = null;

	public Axis2SynapseEnvironment(OMElement synapseConfiguration,
			ClassLoader cl) {
		super();
		this.cl = cl;
		processor.compile(this, synapseConfiguration);
	}

	public void injectMessage(SOAPMessageContext smc) {
		processor.process(this, smc);
	}

	public ClassLoader getClassLoader() {
		return cl;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void sendOn(SOAPMessageContext smc) {
		Axis2Sender.sendOn(smc);

	}

	public void sendBack(SOAPMessageContext smc) {
		Axis2Sender.sendBack(smc);

	}

}
