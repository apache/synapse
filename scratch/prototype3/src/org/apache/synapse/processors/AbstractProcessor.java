package org.apache.synapse.processors;

import org.apache.synapse.Processor;

public abstract class AbstractProcessor implements Processor {
	private String name = null;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
