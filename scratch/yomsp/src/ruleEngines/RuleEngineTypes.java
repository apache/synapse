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
package org.apache.synapse.ruleEngines;

import org.apache.synapse.RuleEngine;

public class RuleEngineTypes {
	// this class could be replaced with a dynamic registry bootstrapped using JAR services 
	
	public static final String XPATH="xpath";
	public static final String REGEX="regex";
	public static final String ALL="all";
	
	
	public static RuleEngine getRuleEngine(String type) {
		if (type.toLowerCase().equals(XPATH)) return new XPathRuleEngine();
		else if (type.toLowerCase().equals(REGEX)) return new RegexRuleEngine();
		else if (type.toLowerCase().equals(ALL)) return new AllRuleEngine();
		return null;
	}

}
