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
