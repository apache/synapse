/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.transport;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.TransportSender;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.SandeshaUtil;

public class Sandesha2TransportSender implements TransportSender  {

	public void cleanup(MessageContext msgContext) throws AxisFault {

	}

	public void stop() {

	}

	public void invoke(MessageContext msgContext) throws AxisFault {
		
		//setting the correct transport sender.
		TransportOutDescription transportOut = (TransportOutDescription) msgContext.getProperty(Sandesha2Constants.ORIGINAL_TRANSPORT_OUT_DESC);
		
		if (transportOut==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.transportOutNotPresent));

		msgContext.setTransportOut(transportOut);
		
		String key =  (String) msgContext.getProperty(Sandesha2Constants.MESSAGE_STORE_KEY);
		
		if (key==null)
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotGetStorageKey));
		
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
		AxisConfiguration axisConfiguration = configurationContext.getAxisConfiguration();
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,axisConfiguration);
		
		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING,Sandesha2Constants.VALUE_TRUE);
		
		storageManager.updateMessageContext(key,msgContext);

	}

	//Below methods are not used
	public void cleanUp(MessageContext msgContext) throws AxisFault {}

	public void init(ConfigurationContext confContext, TransportOutDescription transportOut) throws AxisFault {}

	public void cleanup() {}

	public HandlerDescription getHandlerDesc() {return null;}

	public String getName() { return null;}

	public Parameter getParameter(String name) {  return null; }

	public void init(HandlerDescription handlerdesc) {}

}
