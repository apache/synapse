/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse;


/**
 *
 * 
 * <p>Common stuff needed to embed Synapse into a given runtime (e.g. Axis2) 
 * <p>This interface is used by the processors, and also by EnvironmentAware mediators
 * 
 *
 */
/**
 * @author Paul
 *
 */
public interface SynapseEnvironment {
	
	/* 
	 * This method injects a new message into the Synapse engine
	 * It is used in a couple of ways. Firstly, this is how, for example, 
	 * Axis2 kicks messages into Synapse to start with.
	 * <p>
	 * Also mediators can use this to send messages that they want to be mediated by Synapse
	 * <p>For example if you want to send a copy of a message somewhere, you can clone it and then 
	 * injectMessage() 
	 */
	public void injectMessage(SynapseMessage smc);
	
	/*
	 * Processors or Mediators that wish to load classes should use the ClassLoader given here
	 */
	public ClassLoader getClassLoader();
	
	
	/**
	 * This method allows you send messages on. As opposed to injectMessage send message does not 
	 * process these through Synapse.
	 * <p>
	 * This will send request messages on, and send response messages back to the client
	 */
	public void send(SynapseMessage smc);
	
	
	/**
	 * This is used by the references to find a processor with a given name
	 * 
	 */
	public Processor lookupProcessor(String name);
	
	
	/**
	 * This is how you add a processor to the list of processors. The name which it can be
	 * retrieved by is the processor.getName()
	 */
	public void addProcessor(Processor p);
	
	
	/**
	 * This returns the "Master Processor" which is the root processor for this instance of 
	 * Synapse. Usually this would be the processor derived from &ltsynapse>.  
	 */
	public Processor getMasterProcessor();
	
	
	/**
	 * This sets the root processor for the engine. 
	 */
	public void setMasterProcessor(Processor p);
}
