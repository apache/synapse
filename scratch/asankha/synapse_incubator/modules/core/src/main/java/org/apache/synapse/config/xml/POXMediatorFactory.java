/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.POXMediator;


import javax.xml.namespace.QName;

/**
 * This creates a rest mediator instance
 *
 * <pre>
 * &lt;rest value="true|false"/&gt;
 * </pre>
 */
public class POXMediatorFactory extends AbstractMediatorFactory  {

    private static final QName POX_Q = new QName(Constants.SYNAPSE_NAMESPACE, "pox");

    public Mediator createMediator(OMElement el) {
		POXMediator restMediator = new POXMediator();
		OMAttribute value = el.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(restMediator,el);

        if (value != null) {
			String valueString = value.getAttributeValue();
			if (valueString.toLowerCase().equals("true")) {
				restMediator.setValue(true);
			} else {
				restMediator.setValue(false);
			}
		} 
        return restMediator;
    }

    public QName getTagQName() {
        return POX_Q;
    }
}
