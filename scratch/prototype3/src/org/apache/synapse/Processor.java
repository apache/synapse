package org.apache.synapse;


/**
 * @author Paul Fremantle This interface implements the core of Synapse. 
 * 
 * The processor then deals with a message. It returns false if no further
 * processing is desired. It can have a name (or null)
 * 
 * Processors can either devolve processing to other processors (e.g. a rule,
 * stage, etc) or deal with the message itself (e.g. mediator)
 * 
 */
public interface Processor {
	public boolean process(SynapseEnvironment se, SynapseMessage sm);
	public String getName();
	public void setName(String name);
}
