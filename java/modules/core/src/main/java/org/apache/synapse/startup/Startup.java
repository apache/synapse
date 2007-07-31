package org.apache.synapse.startup;

import javax.xml.namespace.QName;

import org.apache.synapse.ManagedLifecycle;

public interface Startup extends ManagedLifecycle{
	public QName getTagQName();
}
