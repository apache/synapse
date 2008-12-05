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

package org.apache.sandesha2;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.axis2.util.TargetResolver;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.wsdl.codegen.extension.ModulePolicyExtension;
import org.apache.axis2.wsdl.codegen.extension.PolicyExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.RMPolicyExtension;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.SandeshaThread;

/**
 * The Module class of Sandesha2.
 */

public class SandeshaModule implements Module, ModulePolicyExtension {
    
    private Log log = LogFactory.getLog(SandeshaModule.class);
    
	// initialize the module
	public void init(ConfigurationContext configContext,
			AxisModule module) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Entry: SandeshaModule::init, " + configContext);

		EndpointReference.addAnonymousEquivalentURI(Sandesha2Constants.SPEC_2007_02.ANONYMOUS_URI_PREFIX);
		AxisConfiguration config = configContext.getAxisConfiguration();

		//load the Sandesha2Constants.STORAGE_MANAGER_PARAMETER if it is availabel in module.xml
        Parameter storageManagerParameter = module.getParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER);
        if ((storageManagerParameter != null)
				&& (config.getParameter(Sandesha2Constants.STORAGE_MANAGER_PARAMETER) == null)) {
            config.addParameter(storageManagerParameter.getName(), storageManagerParameter.getValue());
        }

		//storing the Sandesha module as a parameter.
		Parameter parameter = new Parameter(Sandesha2Constants.MODULE_CLASS_LOADER,module.getModuleClassLoader());
		config.addParameter(parameter);

		//init the i18n messages
		SandeshaMessageHelper.innit();
		
		//storing the module as a static variable
		SandeshaUtil.setAxisModule(module);
		
		// continueUncompletedSequences (storageManager,configCtx);

        SandeshaPolicyBean constantPropertyBean = new SandeshaPolicyBean ();
		SandeshaPolicyBean propertyBean = PropertyManager.loadPropertiesFromModuleDescPolicy(module,constantPropertyBean);
		
		if (propertyBean==null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotLoadModulePolicies);
			log.error (message);
			propertyBean = new SandeshaPolicyBean ();
		} else {
			if (log.isDebugEnabled()) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.modulePoliciesLoaded);
				log.info (message);
			}
		}
		
		parameter = new Parameter (Sandesha2Constants.SANDESHA_PROPERTY_BEAN, propertyBean);
		config.addParameter(parameter);
		
		// Reset both storage managers
		parameter = config.getParameter(Sandesha2Constants.INMEMORY_STORAGE_MANAGER);
		if(parameter != null) config.removeParameter(parameter);
		parameter = config.getParameter(Sandesha2Constants.PERMANENT_STORAGE_MANAGER);
		if(parameter != null) config.removeParameter(parameter);

		try {
			StorageManager inMemorytorageManager = SandeshaUtil.getInMemoryStorageManager(configContext);
			inMemorytorageManager.initStorage(module);
		} catch (SandeshaStorageException e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitInMemoryStorageManager,
					e.toString());
			if (log.isDebugEnabled()) log.debug(message,e);
		}
		
		try {
			StorageManager permanentStorageManager = SandeshaUtil.getPermanentStorageManager(configContext);
			permanentStorageManager.initStorage(module);
		} catch (SandeshaStorageException e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitPersistentStorageManager,
					e.toString());
			if (log.isDebugEnabled()) log.debug(message,e);
		}
		
		// Reset the security manager, and then load it
		parameter = config.getParameter(Sandesha2Constants.SECURITY_MANAGER);
		if(parameter != null) config.removeParameter(parameter);
		SecurityManager util = SandeshaUtil.getSecurityManager(configContext);
		util.initSecurity(module);

		// Mark the config context so that we can run sync 2-way interations over
		// RM, but at the same time switch it off for unreliable messages.
		// We do a similar trick with the code that does an early HTTP 202 for
		// messages that don't need their backchannel.
		configContext.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.TRUE);
		configContext.getAxisConfiguration().addTargetResolver(
				new TargetResolver() {
					public void resolveTarget(MessageContext messageContext) {
						
						//if Sandesha2 is not engaged we can set the property straight away 
						
						boolean engaged = false;
						
						//checking weather the module is engaged at the System level
						AxisConfiguration axisConfiguration = messageContext.getConfigurationContext().getAxisConfiguration();
						if (axisConfiguration!=null) {
							Collection modules = axisConfiguration.getEngagedModules();
							for (Iterator iter = modules.iterator();iter.hasNext();) {
								AxisModule module = (AxisModule) iter.next();
								String moduleName = module.getName();
								if (moduleName!=null && moduleName.startsWith (Sandesha2Constants.MODULE_NAME)) {
									engaged = true;
								}
							}
						}
						
						//checking weather the module is engaged at the Service level
						AxisService service = messageContext.getAxisService();
						if (service!=null) {
							Collection modules = service.getEngagedModules();
							for (Iterator iter = modules.iterator();iter.hasNext();) {
								AxisModule module = (AxisModule) iter.next();
								String name = module.getName();
								if (name!=null && name.startsWith (Sandesha2Constants.MODULE_NAME)) {
									engaged = true;
								}
							}
						}

						//checking weather the module is engaged at the Operation level
						AxisOperation operation = messageContext.getAxisOperation();
						if (operation!=null) {
							Collection modules = operation.getEngagedModules();
							for (Iterator iter = modules.iterator();iter.hasNext();) {
								AxisModule module = (AxisModule) iter.next();
								String name = module.getName();
								if (name!=null && name.startsWith (Sandesha2Constants.MODULE_NAME)) {
									engaged = true;
								}
							}
						}
						
						//if the module is not engaed we mark the message as unreliable.
						if (!engaged) {
							if(log.isDebugEnabled()) log.debug("Unsetting USE_ASYNC_OPERATIONS and DISABLE_RESPONSE_ACK for unreliable message");
							messageContext.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.FALSE);
							messageContext.setProperty(Constants.Configuration.DISABLE_RESPONSE_ACK, Boolean.FALSE);
						}
						
						//Even when Sandesha2 is engaged this may be marked as unreliable.
						if(log.isDebugEnabled()) log.debug("Entry: SandeshaModule::resolveTarget");
						if(SandeshaUtil.isMessageUnreliable(messageContext)) {
							if(log.isDebugEnabled()) log.debug("Unsetting USE_ASYNC_OPERATIONS for unreliable message");
							messageContext.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.FALSE);
						}
						if(log.isDebugEnabled()) log.debug("Exit: SandeshaModule::resolveTarget");
					}
				}
		);
		
		Parameter propertiesFromRefMsg = module.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceMessage);
		if (propertiesFromRefMsg!=null) {
			String value = (String) propertiesFromRefMsg.getValue();
			if (value!=null) {
				value = value.trim();
				String[] propertyNames = value.split(",");
				Parameter param = new Parameter();
				param.setName(Sandesha2Constants.propertiesToCopyFromReferenceMessageAsStringArray);
				param.setValue(propertyNames);
				module.addParameter(param);
			}
		}
		
		Parameter propertiesFromRefReqMsg = module.getParameter(Sandesha2Constants.propertiesToCopyFromReferenceRequestMessage);
		if (propertiesFromRefReqMsg!=null) {
			String value = (String) propertiesFromRefReqMsg.getValue();
			if (value!=null) {
				value = value.trim();
				String[] propertyNames = value.split(",");
				Parameter param = new Parameter();
				param.setName(Sandesha2Constants.propertiesToCopyFromReferenceRequestMessageAsStringArray);
				param.setValue(propertyNames);
				module.addParameter(param);
			}
		}

		if(log.isDebugEnabled()) log.debug("Exit: SandeshaModule::init");
	}

	public void engageNotify(AxisDescription axisDescription) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Entry: SandeshaModule::engageNotify, " + axisDescription);
		
		AxisDescription parent = axisDescription.getParent();
		SandeshaPolicyBean parentPropertyBean = null;
		if(parent != null) parentPropertyBean = SandeshaUtil.getPropertyBean(parent);
		
		SandeshaPolicyBean axisDescPropertyBean = PropertyManager.loadPropertiesFromAxisDescription(axisDescription,parentPropertyBean);
		
		if (axisDescPropertyBean!=null) {
			Parameter parameter = new Parameter ();
			parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
			parameter.setValue(axisDescPropertyBean);
			axisDescription.addParameter(parameter);
		}
		
		// When we engage Sandesha for a Service, we check to see if there are
		// any OUT_IN MEPs on it's operations. If there are then we record that,
		// so that we know we should send an Offer for WSRM 1.0 Sequences.
		// We check the operation names, as the RM operations are added in as
		// well, and and we only want to consider the user's operations.
		if(axisDescription instanceof AxisService) {
			AxisService service = (AxisService) axisDescription;
			Iterator ops = service.getOperations();
			while(ops.hasNext()) {
				AxisOperation op = (AxisOperation) ops.next();
				if (log.isDebugEnabled()) log.debug("Examining operation " + op.getName() + ", mep " + op.getMessageExchangePattern());
		
				String name = null;
				QName qName = op.getName();
				if(qName != null) name = qName.getLocalPart();
				
				//Check to see if the operation is a sandesha defined one or an axis2 defined one
				if((name != null) && (name.startsWith(Sandesha2Constants.SANDESHA_OP_PREFIX) || 
						qName.getNamespaceURI().equals(org.apache.axis2.namespace.Constants.AXIS2_NAMESPACE_URI)))
					continue;	

				// If we get to here then we must have one of the user's operations, so
				// check the MEP.
				if(op.getAxisSpecificMEPConstant() == WSDLConstants.MEP_CONSTANT_OUT_IN) {
					Parameter p = new Parameter(Sandesha2Constants.SERVICE_CONTAINS_OUT_IN_MEPS, Boolean.TRUE);
					service.addParameter(p);
					break;
				}
			}
		} else if(axisDescription instanceof AxisOperation) {
			AxisOperation op = (AxisOperation) axisDescription;
			if (log.isDebugEnabled()) log.debug("Examining operation " + op.getName() + ", mep " + op.getAxisSpecificMEPConstant());

			String name = null;
			QName qName = op.getName();
			if(qName != null) name = qName.getLocalPart();
			//Check to see if the operation is a sandesha defined one or an axis2 defined one
			if(name != null && (!name.startsWith(Sandesha2Constants.SANDESHA_OP_PREFIX) || 
					!qName.getNamespaceURI().equals(org.apache.axis2.namespace.Constants.AXIS2_NAMESPACE_URI))) {
				// If we get to here then we must have one of the user's operations, so
				// check the MEP.
				if(op.getAxisSpecificMEPConstant() == WSDLConstants.MEP_CONSTANT_OUT_IN) {
					Parameter p = new Parameter(Sandesha2Constants.SERVICE_CONTAINS_OUT_IN_MEPS, Boolean.TRUE);
					op.getParent().addParameter(p);
				}
			}
		} 


		if(log.isDebugEnabled()) log.debug("Exit: SandeshaModule::engageNotify");
	}

	public PolicyExtension getPolicyExtension() {
		return new RMPolicyExtension();
	}

	public void shutdown(ConfigurationContext configurationContext) throws AxisFault {
		if(log.isDebugEnabled()) log.debug("Entry: SandeshaModule::shutdown, " + configurationContext);
		StorageManager storageManager = SandeshaUtil.
			getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());

		if (storageManager!=null) {
			SandeshaThread sender = storageManager.getSender();
			SandeshaThread invoker = storageManager.getInvoker();
			SandeshaThread pollingManager = storageManager.getPollingManager();
			
			//stopping threads.
			if (sender!=null)
				sender.stopRunning();
			if (invoker!=null)
				invoker.stopRunning();
			if (pollingManager!=null)
				pollingManager.stopRunning();
			
			//shutting down the storage manager.
			storageManager.shutdown();
			
			//ensure the storage managers are nullified
			AxisConfiguration config = configurationContext.getAxisConfiguration();
			Parameter param = config.getParameter(Sandesha2Constants.INMEMORY_STORAGE_MANAGER);
			if(param!=null){
				config.removeParameter(param);
			}
			param = config.getParameter(Sandesha2Constants.PERMANENT_STORAGE_MANAGER);
			if(param!=null){
				config.removeParameter(param);
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: SandeshaModule::shutdown");
	}

    public void applyPolicy(Policy policy, AxisDescription axisDescription) {
        // TODO 
        
    }

    public boolean canSupportAssertion(Assertion assertion) {
        // TODO 
        return true;
    }
    
    

}
