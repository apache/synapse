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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseMessageConstants;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

/**
 *
 * 
 * <p>
 * This class executes an XPath test against the message envelope and then processes all subsequent rules/mediations
 * if the test is true
 * <p>
 * TODO add the context into the XPath matching space
 * 
 */
public class XPathMediator extends AbstractFilterMediator {

	private Log log = LogFactory.getLog(getClass());

	private AXIOMXPath xp = null;


	public boolean test(SynapseMessage smc) {
		if (xp == null) {
			log.debug("trying to process xpath without being set");
			return true;
		}
		try {
			if (xp.booleanValueOf(smc.getEnvelope())) {
				log.debug("matched xpath: " + xp.toString());
				// now do "all"
                smc.setProperty(SynapseMessageConstants.MATCHED,Boolean.TRUE);
                return true;
			}

		} catch (JaxenException je) {
			throw new SynapseException("Problem evaluating " + xp.toString(),
					je);
		}
		return false;
	}

	public void setXPathExpr(String expr) {
		try {
			xp = new AXIOMXPath(expr);
		} catch (JaxenException je) {
			throw new SynapseException(je);
		}
	}

	public String getXPathExpr() {
		return xp.toString();
	}

	public void addXPathNamespace(String prefix, String uri) {
		try {
			xp.addNamespace(prefix, uri);
		} catch (JaxenException je) {
			throw new SynapseException(je);
		}

	}

}
