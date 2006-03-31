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
 * @deprecated
 * 
 * Processors are the main extension mechanism for Synapse. 
 * <p>
 * A Processor together with the XML ProcessorConfigurator extends both the synapse.xml 
 * and the actual processing power of Synapse.
 * <p>
 * Some of the processors are "builtin" and those are defined in the ProcessorConfiguratorFinder
 * <p>
 * Other processors can be registered using the JAR Service Provider model:
 * <br><a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider">http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider</a><p>
 * 
 * The processor then deals with a message. It returns false if no further
 * processing is desired. It can have a name (or null)
 * <p>
 * Processors can either devolve processing to other processors (e.g. a rule,
 * stage, etc) or deal with the message itself (e.g. mediator)
 * 
 */
public interface Processor {
	
	
	/**
	 * This method is used to process a message. A response of true indicates continue processing.
	 * A response of false indicates to stop processing.
	 */
	public boolean process(SynapseEnvironment se, SynapseMessage sm);
	
	
	/**
	 * the name of the instance of this processor
	 */
	public String getName();
	
	
	/**
	 * set the name of the instance of the processor
	 */
	public void setName(String name);
}
