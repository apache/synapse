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

import org.apache.synapse.mediators.ClassMediatorConfigurator;
import org.apache.synapse.mediators.E4XMediatorConfigurator;
import org.apache.synapse.mediators.ServiceMediatorConfigurator;
import org.apache.synapse.mediators.SpringMediatorConfigurator;

public class MediatorTypes {
	
	public static final int SERVICE=0, SPRING=1, E4X=2, CLASS=3;
	public static final String STRSERVICE = "service", STRCLASS = "class", STRSPRING = "spring", STRE4X = "e4x";

	
	public static MediatorConfigurator getMediatorConfigurator(String attributeValue) {
		String type = attributeValue.toLowerCase().trim();
		if (type.equals(STRSPRING))  
			return new SpringMediatorConfigurator();
		else if (type.equals(STRSERVICE)) 
			return new ServiceMediatorConfigurator();
		else if (type.equals(STRE4X))
			return new E4XMediatorConfigurator();
		else if (type.equals(STRCLASS)) 
			return new ClassMediatorConfigurator();
		else return null;
	}

	public static int getType(String attributeValue) {
		String type = attributeValue.toLowerCase().trim();
		if (type.equals("spring"))  
			return SPRING;
		else if (type.equals("service")) 
			return SERVICE;
		else if (type.equals("e4x"))
			return E4X;
		else if (type.equals("class")) 
			return CLASS;
		else return -1;
	}

}
