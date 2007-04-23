/*
 * Copyright 2006 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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

package org.apache.sandesha2.security;

class UnitTestSecurityToken implements SecurityToken {

	private int id = 0;
	
	UnitTestSecurityToken(int id) {
		this.id = id;
	}
	
	/**
	 * The SecurityTokenReference that gets encoded into the CreateSequence message
	 * includes an URI string. This method returns the value to use.
	 */
	String getURI() {
		return "#BogusURI/" + id;
	}

	/**
	 * The SecurityTokenReference that gets encoded into the CreateSequence message
	 * includes a ValueType string. This method returns the value to use.
	 */
	String getValueType() {
		return "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";
	}
	
}
