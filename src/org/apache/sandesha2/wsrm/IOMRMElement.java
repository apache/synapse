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

/**
 * This is the base interface for all RM infoset objects.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 * @author Saminda Abeyruwan  <saminda@opensource.lk>
 */

import org.apache.sandesha2.SandeshaException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;

public interface IOMRMElement {
	public String getNamespaceValue();
	public Object fromOMElement(OMElement element) throws OMException,SandeshaException ;
	public OMElement toOMElement(OMElement element) throws OMException, SandeshaException ;
	public boolean isNamespaceSupported (String namespaceName);
}
