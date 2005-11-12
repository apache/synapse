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

import org.apache.axis2.context.SOAPMessageContext;




public class SynapseEngine {

	private RuleEngine[] inphase = null, outphase = null;

	private boolean inoutseparate = false;
	
	private SynapseEnvironment se = null;

	public void processMessage(SOAPMessageContext smc) {
		SynapseEnvironment se = getSynapseEnvironment();
		if (inoutseparate && ResponseIdentifier.isResponse(smc)) {
				for (int i=0; i<outphase.length;i++) {
					boolean ret = outphase[i].process(se, smc);
					if (ret) break;
				}
			
		}else {
			for (int i=0; i<inphase.length;i++) {
				boolean ret = inphase[i].process(se, smc);
				if (ret) break;
			}
		}
	}
	
	
	
	public SynapseEnvironment getSynapseEnvironment() {
		return se;
	}
	public void setSynapseEnvironment(SynapseEnvironment se) {
		this.se =se;
	}

	public void setInphase(RuleEngine[] inphase) {
		this.inphase = inphase;
	}


	public void setOutphase(RuleEngine[] outphase) {
		this.outphase = outphase;
	}


	void setInoutseparate(boolean inoutseparate) {
		this.inoutseparate = inoutseparate;
	}

}
