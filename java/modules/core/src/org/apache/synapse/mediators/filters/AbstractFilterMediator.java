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

package org.apache.synapse.mediators.filters;




import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseMessageConstants;
import org.apache.synapse.mediators.base.AbstractListMediator;


/**
 *	This is a base type for "conditional" mediators. Any mediator that is used in "only once"
 *  processing should be an extension of this. This makes sure that the matched property gets set properly
 *  
 *        
 */
public abstract class AbstractFilterMediator extends AbstractListMediator {

	//private Log log = LogFactory.getLog(getClass());

	public boolean mediate(SynapseMessage smc) {
		if (this.test(smc)) {
		    smc.setProperty(SynapseMessageConstants.MATCHED,Boolean.TRUE);
		    return super.mediate(smc);
		}
		else
		{
			 smc.setProperty(SynapseMessageConstants.MATCHED,Boolean.FALSE);
			 return true;
		}
	}
	
	public abstract boolean test(SynapseMessage sm);
	

	
}
