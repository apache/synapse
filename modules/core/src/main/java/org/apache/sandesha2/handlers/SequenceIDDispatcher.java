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

package org.apache.sandesha2.handlers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.Handler;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;

public class SequenceIDDispatcher extends AbstractDispatcher {

	private final String NAME = "SequenceIDDIspatcher";
	
	public AxisOperation findOperation(AxisService service, MessageContext messageContext) throws AxisFault {
		// TODO Auto-generated method stub
		return null;
	}

	public void initDispatcher() {
		  init(new HandlerDescription(NAME));
	}

	public AxisService findService(MessageContext msgContext) throws AxisFault {
		// TODO Auto-generated method stub
		
		
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		RMMsgContext rmmsgContext = MsgInitializer.initializeMessage(msgContext);
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext, configurationContext.getAxisConfiguration());
		
		Transaction transaction = storageManager.getTransaction();
		
		AxisService service;
		try {
			String sequenceID = (String) rmmsgContext
					.getProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID);
			service = null;
			if (sequenceID != null) {

				//If this is the RMD of the sequence 
				RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
				RMDBean rmdFindBean = new RMDBean();
				rmdFindBean.setSequenceID(sequenceID);

				RMDBean rmdBean = rmdBeanMgr.findUnique(rmdFindBean);
				String serviceName = rmdBean.getServiceName();
				if (serviceName != null) {
					service = configurationContext.getAxisConfiguration()
							.getService(serviceName);
				}

				if (service == null && rmdBean == null) {
					//If this is the RMD of the sequence 
					RMSBeanMgr rmsBeanMgr = storageManager.getRMSBeanMgr();
					RMSBean rmsfindBean = new RMSBean();
					rmsfindBean.setSequenceID(sequenceID);

					RMSBean rmsBean = rmsBeanMgr.findUnique(rmsfindBean);

					serviceName = rmsBean.getServiceName();
					if (serviceName != null) {
						service = configurationContext.getAxisConfiguration()
								.getService(serviceName);
					}
				}

			}
		} finally  {
			transaction.commit();
		}		
		
		return service;
	}


}
