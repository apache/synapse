/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.addressing;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;

public abstract class EPRDecorator {

	final ConfigurationContext ctx;
	
	public EPRDecorator(ConfigurationContext configCtx){
		this.ctx = configCtx;
	}
	
	/**
	 * Decorates the endpoint reference with any additional routing information required
	 * @param ref
	 * @return
	 */
	public abstract EndpointReference decorateEndpointReference(EndpointReference ref);
	
	/**
	 * This method is called for outbound msgs in order to verify the TO EPR is valid
	 * @param ref
	 */
	public void checkEndpointReference(EndpointReference ref){
		//NO-OP unless overriden
	}
	
}
