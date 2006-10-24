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

package org.apache.sandesha2.wsrm;

import org.apache.sandesha2.SandeshaException;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;

/**
 * This is the base interface for RM infoset objects that are added directly so 
 * SOAPBody or SOAPHeader (we call them MessageParts).
 */

public interface IOMRMPart extends IOMRMElement {
	public void toSOAPEnvelope (SOAPEnvelope envelope) throws AxisFault;
}
