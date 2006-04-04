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

package org.apache.synapse.mediators.rules;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.HeaderType;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.base.AbstractConditionMediator;


/**
 *
 * <p>
 * This checks the pattern against a property and if true processes the message with its child or children
 * <p>
 * TODO add to the set of properties you can search
 * 
 * 
 */
public class RegexMediator extends AbstractConditionMediator{
	private Pattern pattern = null;

	private Log log = LogFactory.getLog(getClass());

	private HeaderType headerType = new HeaderType();

	private String property = null;

	public void setHeaderType(String header) {
		headerType.setHeaderType(header);
	}

	public String getHeaderType() {
		return headerType.getHeaderType();
	}

	public void setPattern(String p) {
		pattern = Pattern.compile(p);
	}

	public String getPattern() {
		return pattern.toString();
	}

	public void setPropertyName(String p) {
		this.property = p;
	}

	public String getPropertyName() {
		return property;
	}

	public boolean test(SynapseMessage smc) {

		if (pattern == null) {
			log.debug("trying to process with empty pattern");
			return true;
		}
		String toMatch = null;
		if (property != null) {
			toMatch = smc.getProperty(property).toString();
		} else {
			toMatch = headerType.getHeader(smc);
		}

		if (toMatch==null) return false;  

		if (pattern.matcher(toMatch).matches()) {
			log.debug("Regex pattern " + pattern.toString() + " matched "
					+ toMatch);
            return true;
		}
		return false;
	}

}
