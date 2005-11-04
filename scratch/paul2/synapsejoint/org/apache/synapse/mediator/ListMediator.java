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
package org.apache.synapse.mediator;

import java.util.List;
import java.util.Iterator;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.SynapseException;

public class ListMediator implements Mediator {
	
	private List mediatorList = null;
	
	public void setMediatorList(List ml) {
		this.mediatorList = ml;
	}
	
	
	
	public boolean mediate(MessageContext messageContext) {
		if (mediatorList==null) throw new SynapseException("empty list in ListMediation"); 
		Iterator it = mediatorList.iterator();
		while (it.hasNext()) {
			Mediator m = (Mediator)it.next();
			boolean r = m.mediate(messageContext);
			if (!r) return false;
		}
		
		return true;
	}
}
