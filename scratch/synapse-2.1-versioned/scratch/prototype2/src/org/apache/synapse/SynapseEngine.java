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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.api.SOAPMessageContext;
import org.apache.synapse.api.SynapseEnvironment;
import org.apache.synapse.spi.RuleEngine;




public class SynapseEngine {
	Log log = LogFactory.getLog(getClass());

	private RuleEngine[] inphase = null, outphase = null;

	private boolean inoutseparate = false;
	
	private SynapseEnvironment se = null;

	public void process(SOAPMessageContext smc) {
		log.debug("processing message");
		SynapseEnvironment se = getSynapseEnvironment();
		if (inoutseparate && smc.isResponse()) {
				for (int i=0; i<outphase.length;i++) {
					boolean ret = outphase[i].process(se, smc);
					if (ret) break;
				}
			
		}else {
			for (int i=0; i<inphase.length;i++) {
				boolean ret = inphase[i].process(se, smc);
				if (!ret) break;
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
