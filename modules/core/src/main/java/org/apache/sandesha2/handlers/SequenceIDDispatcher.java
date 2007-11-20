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

package org.apache.sandesha2.handlers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

public class SequenceIDDispatcher extends AbstractDispatcher {

	private static final String NAME = "SequenceIDDIspatcher";
	private static final Log log = LogFactory.getLog(SequenceIDDispatcher.class);
	
	public AxisOperation findOperation(AxisService service, MessageContext messageContext) {
		return null;
	}

	public void initDispatcher() {
		  init(new HandlerDescription(NAME));
	}

	public AxisService findService(MessageContext msgContext) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: SequenceIDDispatcher::findService, " + msgContext.getEnvelope().getHeader());
		// look at the service to see if RM is totally disabled. This allows the user to disable RM using
		// a property on the service, even when Sandesha is engaged.
		if (msgContext.getAxisService() != null) {
			Parameter unreliableParam = msgContext.getAxisService().getParameter(SandeshaClientConstants.UNRELIABLE_MESSAGE);
			if (null != unreliableParam && "true".equals(unreliableParam.getValue())) {
				if (log.isDebugEnabled())
					log.debug("Exit: SequenceIDDispatcher::findService, Service has disabled RM ");
				return null;
			}
		} 
		
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		RMMsgContext rmmsgContext = MsgInitializer.initializeMessage(msgContext);
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
		
		Transaction transaction = storageManager.getTransaction();
		
		AxisService service = null;
		try {
			String sequenceID = (String) rmmsgContext
					.getProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID);
			service = null;
			if (sequenceID != null) {

				//If this is the RMD of the sequence 				
				RMDBean rmdBean = SandeshaUtil.getRMDBeanFromSequenceId(storageManager, sequenceID);
                String serviceName = null;
                if (rmdBean != null ) {
                    serviceName = rmdBean.getServiceName();
                }
				if (serviceName != null) {
					service = configurationContext.getAxisConfiguration()
							.getService(serviceName);
				}

				if (service == null && rmdBean == null) {
					//If this is the RMS of the sequence 
					RMSBean rmsBean = SandeshaUtil.getRMSBeanFromSequenceId(storageManager, sequenceID);

					serviceName = rmsBean.getServiceName();
					if (serviceName != null) {
						service = configurationContext.getAxisConfiguration()
								.getService(serviceName);
					}
				}

			}
		} finally  {
			if (transaction != null && transaction.isActive())
				transaction.commit();
		}		
		
		if (log.isDebugEnabled())
			log.debug("Exit: SequenceIDDispatcher::findService, " + service);
		return service;
	}


}
