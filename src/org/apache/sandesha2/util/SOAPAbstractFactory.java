/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;

/**
 * A wrapper for SOA11 and SOAP12 factories of OM.
 */

public class SOAPAbstractFactory {

	public static SOAPFactory getSOAPFactory(int SOAPVersion) {

		if (SOAPVersion == Sandesha2Constants.SOAPVersion.v1_1)
			return OMAbstractFactory.getSOAP11Factory();
		else
			return OMAbstractFactory.getSOAP12Factory();

	}
}