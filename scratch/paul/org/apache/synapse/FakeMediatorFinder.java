package org.apache.synapse;

import org.apache.axis2.om.OMElement;

public class FakeMediatorFinder implements MediatorFinder {
	
	
	private  class FakeMediator implements Mediator {
		private String name;
		public FakeMediator(String name) {
			this.name = name;
		}
		public boolean mediate(OMElement el) {
			System.out.println(name+ "is Mediating");
			return true;
		}
	}

	public Mediator getMediator(String name) {
		
		return new FakeMediator(name);
	} 
}
