package org.apache.synapse.mediators;


public class BuiltinMediatorConfiguration extends ClassMediatorConfiguration {

	public BuiltinMediatorConfiguration() {
		super();
	}
		
	public int getMediatorType() {
		
		return MediatorTypes.BUILTIN;
	}

}
