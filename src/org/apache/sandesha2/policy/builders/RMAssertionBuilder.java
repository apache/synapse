/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
package org.apache.sandesha2.policy.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.xml.XmlPrimtiveAssertion;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.policy.SandeshaPolicyBean;

public class RMAssertionBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {

        SandeshaPolicyBean propertyBean = new SandeshaPolicyBean();
        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());

        processElements(policy.getPolicyComponents(), propertyBean);
        return propertyBean;
    }

    public QName[] getKnownElements() {
        return new QName[] { new QName(
                Sandesha2Constants.Assertions.URI_RM_POLICY_NS, "RMAssertion") };
    }

    private void processElements(List policyComponents,
            SandeshaPolicyBean propertyBean) {

        XmlPrimtiveAssertion xmlPrimtiveAssertion;

        for (Iterator iterator = policyComponents.iterator(); iterator
                .hasNext();) {
            xmlPrimtiveAssertion = (XmlPrimtiveAssertion) iterator.next();
            OMElement element = xmlPrimtiveAssertion.getValue();
            String name = element.getLocalName();

            if (Sandesha2Constants.Assertions.ELEM_ACK_INTERVAL.equals(name)) {
                propertyBean.setAcknowledgementInterval(Long.parseLong(element
                        .getText().trim()));

            } else if (Sandesha2Constants.Assertions.ELEM_EXP_BACKOFF
                    .equals(name)) {
                propertyBean.setExponentialBackoff(Boolean.valueOf(
                        element.getText().trim()).booleanValue());

            } else if (Sandesha2Constants.Assertions.ELEM_INACTIVITY_TIMEOUT
                    .equals(name)) {

            		propertyBean.setInactiveTimeoutValue (Long.parseLong(element
            				.getText().trim()));

            } else if (Sandesha2Constants.Assertions.ELEM_INACTIVITY_TIMEOUT_MEASURES
                    .equals(name)) {
            	//using the previously set Inavtivity Timeout
                propertyBean.setInactivityTimeoutMeasure (element.getText().trim());

            }  else if (Sandesha2Constants.Assertions.ELEM_INVOKE_INORDER
                    .equals(name)) {
            	String value = element.getText().trim();
            	boolean inOrder = false;
            	
            	if (value!=null && Constants.VALUE_TRUE.equals(value))
            		propertyBean.setInOrder(inOrder);
            }  else if (Sandesha2Constants.Assertions.ELEM_MAX_RETRANS_COUNT
                    .equals(name)) {
                propertyBean.setMaximumRetransmissionCount (Integer.parseInt(element.getText().trim()));
            }   else if (Sandesha2Constants.Assertions.ELEM_MSG_TYPES_TO_DROP
                    .equals(name)) {
            	ArrayList types = new ArrayList ();
            	String str = element.getText().trim();
            	String[] items  = str.split(Sandesha2Constants.LIST_SEPERATOR);
            	if (items!=null) {
            		int size = items.length;
            		for (int i=0;i<size;i++) {
            			String itemStr = items[i];
            			if (!itemStr.equals("") && !itemStr.equals(Sandesha2Constants.VALUE_NONE) )
            				types.add(new Integer (itemStr));
            		}
            	}
                propertyBean.setMsgTypesToDrop (types);
            }  else if (Sandesha2Constants.Assertions.ELEM_RETRANS_INTERVAL
                    .equals(name)) {
                propertyBean.setRetransmissionInterval (Long.parseLong (element.getText().trim()));
            }  else if (Sandesha2Constants.Assertions.ELEM_SEC_MGR
                    .equals(name)) {
                propertyBean.setSecurityManagerClass (element.getText().trim());
            }  else if (Sandesha2Constants.Assertions.ELEM_STORAGE_MGR
                    .equals(name)) {
            	
                if (element!=null) {
                    //finding out storage managers.
                	
                	OMElement inmemoryStorageManagerElem = element.getFirstChildWithName(Sandesha2Constants.Assertions.Q_ELEM_INMEMORY_STORAGE_MGR);
                	if (inmemoryStorageManagerElem!=null) {
                		String inMemoryStorageMgr = inmemoryStorageManagerElem.getText().trim();
                		propertyBean.setInMemoryStorageManagerClass(inMemoryStorageMgr);
                	}
                	
                	OMElement permanentStorageManagerElem = element.getFirstChildWithName(Sandesha2Constants.Assertions.Q_ELEM_PERMANENT_STORAGE_MGR);
                	if (permanentStorageManagerElem!=null) {
                		String permanentStorageMgr = permanentStorageManagerElem.getText().trim();
                		propertyBean.setPermanentStorageManagerClass(permanentStorageMgr);
                	}
                	
                }
            }
        }
    }

}
