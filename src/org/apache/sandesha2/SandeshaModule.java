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

package org.apache.sandesha2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.modules.Module;
import org.apache.axis2.modules.ModulePolicyExtension;
import org.apache.axis2.modules.PolicyExtension;
import org.apache.axis2.util.TargetResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.RMPolicyExtension;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * The Module class of Sandesha2.
 */

public class SandeshaModule implements Module, ModulePolicyExtension {
    
    private Log log = LogFactory.getLog(SandeshaModule.class);
    
	// initialize the module
	public void init(ConfigurationContext configContext,
			AxisModule module) throws AxisFault {

		//storing the Sadesha module as a property.
		configContext.setProperty(Sandesha2Constants.MODULE_CLASS_LOADER,module.getModuleClassLoader());

		//init the i18n messages
		SandeshaMessageHelper.innit();
		
		//storing the module as a static variable
		SandeshaUtil.setAxisModule(module);
		
		// continueUncompletedSequences (storageManager,configCtx);

		SandeshaPolicyBean constantPropertyBean = PropertyManager.loadPropertiesFromDefaultValues();
		SandeshaPolicyBean propertyBean = PropertyManager.loadPropertiesFromModuleDescPolicy(module,constantPropertyBean);
		
		if (propertyBean==null) {
			String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.couldNotLoadModulePolicies);
			log.error (message);
			
			propertyBean = PropertyManager.loadPropertiesFromDefaultValues();
		} else {
			if (log.isDebugEnabled()) {
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.modulePoliciesLoaded);
				log.info (message);
			}
		}
		
		Parameter parameter = new Parameter ();
		parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
		parameter.setValue(propertyBean);
		configContext.getAxisConfiguration().addParameter(parameter);;
		
		configContext.setProperty(Sandesha2Constants.INMEMORY_STORAGE_MANAGER,null);   // this must be resetted by the module settings.
		configContext.setProperty(Sandesha2Constants.PERMANENT_STORAGE_MANAGER,null);
		
		try {
			StorageManager inMemorytorageManager = SandeshaUtil.getInMemoryStorageManager(configContext);
			inMemorytorageManager.initStorage(module);
		} catch (SandeshaStorageException e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitInMemoryStorageManager,
					e.toString());
			log.debug(message,e);
		}
		
		try {
			StorageManager permanentStorageManager = SandeshaUtil.getPermanentStorageManager(configContext);
			permanentStorageManager.initStorage(module);
		} catch (SandeshaStorageException e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotInitPersistentStorageManager,
					e.toString());
			log.debug(message,e);
		}
		
		configContext.setProperty(Sandesha2Constants.SECURITY_MANAGER,null);
		SecurityManager util = SandeshaUtil.getSecurityManager(configContext);
		util.initSecurity(module);

		// Mark the config context so that we can run sync 2-way interations over
		// RM, but at the same time switch it off for unreliable messages.
		configContext.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.TRUE);
		configContext.getAxisConfiguration().addTargetResolver(
				new TargetResolver() {
					public void resolveTarget(MessageContext messageContext) {
						String unreliable = (String) messageContext.getProperty(SandeshaClientConstants.UNRELIABLE_MESSAGE);
						if("true".equals(unreliable)) {
							messageContext.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.FALSE);
						}
					}
				}
		);
	}

	public void engageNotify(AxisDescription axisDescription) throws AxisFault {
		
		SandeshaPolicyBean parentPropertyBean = SandeshaUtil.getPropertyBean(axisDescription);
		if (parentPropertyBean==null) 
			throw new AxisFault (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.defaultPropertyBeanNotSet));
		
		SandeshaPolicyBean axisDescPropertyBean = PropertyManager.loadPropertiesFromAxisDescription(axisDescription,parentPropertyBean);
		
		if (axisDescPropertyBean!=null) {
			Parameter parameter = new Parameter ();
			parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
			parameter.setValue(axisDescPropertyBean);
			axisDescription.addParameter(parameter);
		}
	}

	public PolicyExtension getPolicyExtension() {
		return new RMPolicyExtension();
	}

	public void shutdown(ConfigurationContext configurationContext) throws AxisFault {
		SandeshaUtil.
			getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration())
				.shutdown();
	}

    public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault {
        // TODO 
        
    }

    public boolean canSupportAssertion(Assertion assertion) {
        // TODO 
        return true;
    }
    
    

}
