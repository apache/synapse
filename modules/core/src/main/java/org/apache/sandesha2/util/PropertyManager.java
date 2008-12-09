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

package org.apache.sandesha2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PolicySubject;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;

/**
 * Loads properties from sandesha2.properties file (from Sandesha2Constants if
 * this is not available).
 */

public class PropertyManager {

	public static void loadPropertiesFromDefaultValues(SandeshaPolicyBean propertyBean) {
		propertyBean.setAcknowledgementInterval(Sandesha2Constants.Properties.DefaultValues.AcknowledgementInterval);
		propertyBean.setExponentialBackoff(Sandesha2Constants.Properties.DefaultValues.ExponentialBackoff);
		propertyBean.setInactiveTimeoutInterval(Sandesha2Constants.Properties.DefaultValues.InactivityTimeout,
				Sandesha2Constants.Properties.DefaultValues.InactivityTimeoutMeasure);

		propertyBean.setSequenceRemovalTimeoutInterval(Sandesha2Constants.Properties.DefaultValues.sequenceRemovalTimeout,
				Sandesha2Constants.Properties.DefaultValues.sequenceRemovalTimeoutMeasure);
		
		propertyBean.setInOrder(Sandesha2Constants.Properties.DefaultValues.InvokeInOrder);
		propertyBean.setMsgTypesToDrop(null);
		propertyBean.setRetransmissionInterval(Sandesha2Constants.Properties.DefaultValues.RetransmissionInterval);
		// propertyBean.setStorageManagerClass(Sandesha2Constants.Properties.DefaultValues.StorageManager);
		propertyBean.setInMemoryStorageManagerClass(Sandesha2Constants.Properties.DefaultValues.InMemoryStorageManager);
		propertyBean
				.setPermanentStorageManagerClass(Sandesha2Constants.Properties.DefaultValues.PermanentStorageManager);
		propertyBean
				.setMaximumRetransmissionCount(Sandesha2Constants.Properties.DefaultValues.MaximumRetransmissionCount);

		String msgTypesToDrop = Sandesha2Constants.Properties.DefaultValues.MessageTypesToDrop;
		loadMessageTypesToDrop(msgTypesToDrop, propertyBean);

		propertyBean.setSecurityManagerClass(Sandesha2Constants.Properties.DefaultValues.SecurityManager);
		propertyBean.setEPRDecoratorClass(Sandesha2Constants.Properties.DefaultValues.EPRDecorator);
		propertyBean.setContextManagerClass(Sandesha2Constants.Properties.DefaultValues.ContextManager);
		propertyBean.setEnableMakeConnection(Sandesha2Constants.Properties.DefaultValues.EnableMakeConnection);
		propertyBean.setEnableRMAnonURI(Sandesha2Constants.Properties.DefaultValues.EnableRMAnonURI);
		propertyBean.setUseMessageSerialization(Sandesha2Constants.Properties.DefaultValues.UseMessageSerialization);
		propertyBean.setEnforceRM(Sandesha2Constants.Properties.DefaultValues.enforceRM);
		
	}

	public static SandeshaPolicyBean loadPropertiesFromModuleDesc(AxisModule desc) throws SandeshaException {
		SandeshaPolicyBean propertyBean = new SandeshaPolicyBean();

		Parameter expoBackoffParam = desc.getParameter(Sandesha2Constants.Properties.ExponentialBackoff);
		String expoBackoffStr = (String) expoBackoffParam.getValue();
		loadExponentialBackoff(expoBackoffStr, propertyBean);

		Parameter retransmissionIntParam = desc.getParameter(Sandesha2Constants.Properties.RetransmissionInterval);
		String retransmissionIntStr = (String) retransmissionIntParam.getValue();
		loadRetransmissionInterval(retransmissionIntStr, propertyBean);

		Parameter acknowledgementIntParam = desc.getParameter(Sandesha2Constants.Properties.AcknowledgementInterval);
		String acknowledgementIntStr = (String) acknowledgementIntParam.getValue();
		loadAcknowledgementInterval(acknowledgementIntStr, propertyBean);

		Parameter inactivityTimeoutParam = desc.getParameter(Sandesha2Constants.Properties.InactivityTimeout);
		String inactivityTimeoutStr = (String) inactivityTimeoutParam.getValue();
		Parameter inactivityTimeoutMeasureParam = desc
				.getParameter(Sandesha2Constants.Properties.InactivityTimeoutMeasure);
		String inactivityTimeoutMeasure = (String) inactivityTimeoutMeasureParam.getValue();
		loadInactivityTimeout(inactivityTimeoutStr, inactivityTimeoutMeasure, propertyBean);

		Parameter sequenceRemovalTimeoutParam = desc.getParameter(Sandesha2Constants.Properties.SequenceRemovalTimeout);
		String sequenceRemovalTimeoutStr = (String) sequenceRemovalTimeoutParam.getValue();
		Parameter sequenceRemovalTimeoutMeasureParam = desc
				.getParameter(Sandesha2Constants.Properties.SequenceRemovalTimeoutMeasure);
		String sequenceRemovalTimeoutMeasure = (String) sequenceRemovalTimeoutMeasureParam.getValue();
		loadSequenceRemovalTimeout(sequenceRemovalTimeoutStr, sequenceRemovalTimeoutMeasure, propertyBean);

		// Parameter storageMgrClassParam =
		// desc.getParameter(Sandesha2Constants.Properties.StorageManager);
		// String storageMgrClassStr = (String) storageMgrClassParam.getValue();
		// loadStoragemanagerClass(storageMgrClassStr,propertyBean);

		Parameter inMemoryStorageMgrClassParam = desc
				.getParameter(Sandesha2Constants.Properties.InMemoryStorageManager);
		String inMemoryStorageMgrClassStr = (String) inMemoryStorageMgrClassParam.getValue();
		loadInMemoryStoragemanagerClass(inMemoryStorageMgrClassStr, propertyBean);

		Parameter permanentStorageMgrClassParam = desc
				.getParameter(Sandesha2Constants.Properties.PermanentStorageManager);
		String permanentStorageMgrClassStr = (String) permanentStorageMgrClassParam.getValue();
		loadPermanentStoragemanagerClass(permanentStorageMgrClassStr, propertyBean);

		Parameter inOrderInvocationParam = desc.getParameter(Sandesha2Constants.Properties.InOrderInvocation);
		String inOrderInvocation = (String) inOrderInvocationParam.getValue();
		loadInOrderInvocation(inOrderInvocation, propertyBean);
		
		Parameter enableMakeConnectionParam = desc.getParameter(Sandesha2Constants.Properties.EnableMakeConnection);
		String enableMakeConnection = (String) enableMakeConnectionParam.getValue();
		loadEnableMakeConnection(enableMakeConnection, propertyBean);

		Parameter useSerializationParam = desc.getParameter(Sandesha2Constants.Properties.UseMessageSerialization);
		String useSerialization = (String) useSerializationParam.getValue();
		loadUseSerialization(useSerialization, propertyBean);

		Parameter messageTypesToDropParam = desc.getParameter(Sandesha2Constants.Properties.MessageTypesToDrop);
		String messageTypesToDrop = (String) messageTypesToDropParam.getValue();
		loadMessageTypesToDrop(messageTypesToDrop, propertyBean);

		Parameter securityManagerClassParam = desc.getParameter(Sandesha2Constants.Properties.SecurityManager);
		String securityManagerClassStr = (String) securityManagerClassParam.getValue();
		loadSecurityManagerClass(securityManagerClassStr,propertyBean);
		
		Parameter eprDecoratorClassParam = desc.getParameter(Sandesha2Constants.Properties.EPRDecorator);
		String eprDecoratorClassString = (String) eprDecoratorClassParam.getValue();
		loadEPRDecoratorClass(eprDecoratorClassString,propertyBean);

		return propertyBean;
	}

	public static SandeshaPolicyBean loadPropertiesFromModuleDescPolicy(AxisModule desc,
			SandeshaPolicyBean parentPropertyBean) throws SandeshaException {

        boolean found = false;
        Assertion assertion = null;

        PolicySubject policySubject = desc.getPolicySubject();
        if (policySubject == null) {
            return null;
        }
        Collection<PolicyComponent> policyComponents = policySubject.getAttachedPolicyComponents();
        if (policyComponents == null) {
            return null;
        }
        Iterator<PolicyComponent> policies = policyComponents.iterator();
        while (!found && policies.hasNext()) {
        	Object next = policies.next();
        	if(next instanceof Policy){
        		Policy policy = (Policy) next;
        		Iterator<List<Assertion>> iterator = policy.getAlternatives();
        		while (!found && iterator.hasNext()) {
        			List<Assertion> assertionList = (List<Assertion>) iterator.next();
        			Iterator<Assertion> assertions = assertionList.iterator();
        			while (!found && assertions.hasNext()) {
        				assertion = (Assertion) assertions.next();
        				if (assertion instanceof SandeshaPolicyBean) {
        					found = true;
        					break;
        				}
        			}
        		}
        	}
        }

        // no RMAssertion found
        if (!found) {
            return null;
        }

        SandeshaPolicyBean propertyBean = (SandeshaPolicyBean) assertion;
        propertyBean.setParent(parentPropertyBean);

        return propertyBean;
    }

    public static SandeshaPolicyBean loadPropertiesFromAxisDescription(AxisDescription desc,
			SandeshaPolicyBean parentPropertyBean) throws SandeshaException {
		
        Policy policy = desc.getPolicyInclude().getEffectivePolicy();

        if (policy == null) {
            return null; // no pilicy is available in the module description
        }
        
        Iterator<List<Assertion>> iterator = policy.getAlternatives();
        if (! iterator.hasNext()) {
            throw new SandeshaException("No Policy Alternative found");
        }

        List<Assertion> assertionList = (List<Assertion>) iterator.next();
        Assertion assertion = null;
        
        boolean found = false;
        
        for (Iterator<Assertion> assertions = assertionList.iterator(); assertions.hasNext();) {
            assertion = (Assertion) assertions.next();
            
            if (assertion instanceof SandeshaPolicyBean) {
                found = true;
                break;
            }
        }
        
        if (! found) {
            // no RMAssertion found
            return null;
        }
        
        SandeshaPolicyBean propertyBean = (SandeshaPolicyBean) assertion;
        
        if (propertyBean!=parentPropertyBean) {
        	if(parentPropertyBean != null) propertyBean.setParent(parentPropertyBean);
        	return propertyBean;
        } 
        
        //propertyBean and parent being the same object means that there is no policy in this level, this is simply the reflection of 
        //the parent.
        return null;               
	}

	/**
	 * Loads wsp:exponentianbackoff.
	 * 
	 * @param properties
	 */
	private static void loadExponentialBackoff(String expoBackoffStr, SandeshaPolicyBean propertyBean) {

		if (expoBackoffStr != null) {
			expoBackoffStr = expoBackoffStr.trim();
			if (expoBackoffStr.equals("true")) {
				propertyBean.setExponentialBackoff(true);
			} else if (expoBackoffStr.equals("false")) {
				propertyBean.setExponentialBackoff(false);
			}
		}
	}

	/**
	 * Loads wsp:retransmissionInterval.
	 * 
	 * @param properties
	 */
	private static void loadRetransmissionInterval(String retransmissionIntStr, SandeshaPolicyBean propertyBean)
			throws SandeshaException {

		if (retransmissionIntStr != null) {
			try {
				retransmissionIntStr = retransmissionIntStr.trim();
				int retransmissionInterval = Integer.parseInt(retransmissionIntStr);
				if (retransmissionInterval > 0) {
					propertyBean.setRetransmissionInterval(retransmissionInterval);
				}
			} catch (NumberFormatException e) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDerriveRetransInterval);
				throw new SandeshaException(message, e);
			}
		}
	}

	/**
	 * Loads wsp:acknowldgementInterval.
	 * 
	 * @param properties
	 */
	private static void loadAcknowledgementInterval(String acknowledgementIntStr, SandeshaPolicyBean propertyBean)
			throws SandeshaException {

		if (acknowledgementIntStr != null) {
			try {
				acknowledgementIntStr = acknowledgementIntStr.trim();
				int acknowledgementInt = Integer.parseInt(acknowledgementIntStr);
				if (acknowledgementInt > 0) {
					propertyBean.setAcknowledgementInterval(acknowledgementInt);
				}
			} catch (NumberFormatException e) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDerriveAckInterval);
				throw new SandeshaException(message, e);
			}
		}
	}

	/**
	 * Loads wsp:inactivityInterval.
	 * 
	 * @param properties
	 */
	private static void loadInactivityTimeout(String inactivityTimeoutStr, String inactivityTimeoutMeasure,
			SandeshaPolicyBean propertyBean) throws SandeshaException {

		if (inactivityTimeoutStr != null && inactivityTimeoutMeasure != null) {
			try {
				inactivityTimeoutStr = inactivityTimeoutStr.trim();
				inactivityTimeoutMeasure = inactivityTimeoutMeasure.trim();

				int inactivityTimeoutVal = Integer.parseInt(inactivityTimeoutStr);
				if (inactivityTimeoutVal > 0) {
					propertyBean.setInactiveTimeoutInterval(inactivityTimeoutVal, inactivityTimeoutMeasure);
				}
			} catch (NumberFormatException e) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDerriveInactivityTimeout);
				throw new SandeshaException(message, e);
			}
		}
	}

	/**
	 * Loads wsp:inactivityInterval.
	 * 
	 * @param properties
	 */
	private static void loadSequenceRemovalTimeout(String sequenceRemovalTimeoutStr, String sequenceRemovalTimeoutMeasure,
			SandeshaPolicyBean propertyBean) throws SandeshaException {

		if (sequenceRemovalTimeoutStr != null && sequenceRemovalTimeoutMeasure != null) {
			try {
				sequenceRemovalTimeoutStr = sequenceRemovalTimeoutStr.trim();
				sequenceRemovalTimeoutMeasure = sequenceRemovalTimeoutMeasure.trim();

				int sequenceRemovalTimeoutVal = Integer.parseInt(sequenceRemovalTimeoutStr);
				if (sequenceRemovalTimeoutVal > 0) {
					propertyBean.setSequenceRemovalTimeoutInterval(sequenceRemovalTimeoutVal, sequenceRemovalTimeoutMeasure);
				}
			} catch (NumberFormatException e) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDerriveInactivityTimeout);
				throw new SandeshaException(message, e);
			}
		}
	}

	/**
	 * Loads the InMemoryStorageManager class name.
	 * 
	 * @param properties
	 */
	private static void loadInMemoryStoragemanagerClass(String inMemoryStorageMgrClassStr,
			SandeshaPolicyBean propertyBean){
		if (inMemoryStorageMgrClassStr != null) {
			inMemoryStorageMgrClassStr = inMemoryStorageMgrClassStr.trim();
			propertyBean.setInMemoryStorageManagerClass(inMemoryStorageMgrClassStr);
		}
	}

	/**
	 * Loads the PermanentStorageManager class name.
	 * 
	 * @param properties
	 */
	private static void loadPermanentStoragemanagerClass(String permanentStorageMgrClassStr,
			SandeshaPolicyBean propertyBean) {
		if (permanentStorageMgrClassStr != null) {
			permanentStorageMgrClassStr = permanentStorageMgrClassStr.trim();
			propertyBean.setPermanentStorageManagerClass(permanentStorageMgrClassStr);
		}
	}

	private static void loadInOrderInvocation(String inOrderInvocation, SandeshaPolicyBean propertyBean) {

		if (inOrderInvocation != null) {
			inOrderInvocation = inOrderInvocation.trim();
			if (inOrderInvocation.equalsIgnoreCase("true")) {
				propertyBean.setInOrder(true);
			} else if (inOrderInvocation.equalsIgnoreCase("false")) {
				propertyBean.setInOrder(false);
			}
		}
	}
	
	private static void loadEnableMakeConnection(String enableMakeConnection, SandeshaPolicyBean propertyBean) {

		if (enableMakeConnection != null) {
			enableMakeConnection = enableMakeConnection.trim();
			if (enableMakeConnection.equalsIgnoreCase("true")) {
				propertyBean.setEnableMakeConnection(true);
			} else if (enableMakeConnection.equalsIgnoreCase("false")) {
				propertyBean.setEnableMakeConnection(false);
			}
		}
	}

	private static void loadUseSerialization(String useSerialization, SandeshaPolicyBean propertyBean) {

		if (useSerialization != null) {
			useSerialization = useSerialization.trim();
			if (useSerialization.equalsIgnoreCase("true")) {
				propertyBean.setUseMessageSerialization(true);
			} else if (useSerialization.equalsIgnoreCase("false")) {
				propertyBean.setUseMessageSerialization(false);
			}
		}
	}

	private static void loadMessageTypesToDrop(String messageTypesToDrop, SandeshaPolicyBean propertyBean) {

		if (messageTypesToDrop != null
				&& !Sandesha2Constants.VALUE_NONE.equals(messageTypesToDrop)) {
			messageTypesToDrop = messageTypesToDrop.trim();
			messageTypesToDrop = "[" + messageTypesToDrop + "]";
			ArrayList<String> messageTypesLst = SandeshaUtil.getArrayListFromString(messageTypesToDrop);

			Iterator<String> itr = messageTypesLst.iterator();
			while (itr.hasNext()) {
				String typeStr = (String) itr.next();
				Integer typeNo = new Integer(typeStr);
				propertyBean.addMsgTypeToDrop(typeNo);
			}
		}

	}

	/**
	 * Loads the EPR decorator class name.
	 * 
	 * @param properties
	 */
	private static void loadEPRDecoratorClass(String eprDecoratorClassStr, SandeshaPolicyBean propertyBean) {
		if (eprDecoratorClassStr != null) {
			eprDecoratorClassStr = eprDecoratorClassStr.trim();
			propertyBean.setEPRDecoratorClass(eprDecoratorClassStr);
		}
	}
	
	/**
	 * Loads the SecurityManager class name.
	 * 
	 * @param properties
	 */
	private static void loadSecurityManagerClass(String securityManagerClassStr, SandeshaPolicyBean propertyBean) {
		if (securityManagerClassStr != null) {
			securityManagerClassStr = securityManagerClassStr.trim();
			propertyBean.setSecurityManagerClass(securityManagerClassStr);
		}
	}
}
