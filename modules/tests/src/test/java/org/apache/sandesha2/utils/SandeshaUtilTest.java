/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.apache.sandesha2.utils;

import java.util.ArrayList;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.transport.Sandesha2TransportOutDesc;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.SandeshaThread;

public class SandeshaUtilTest extends SandeshaTestCase{

	
	public SandeshaUtilTest(String s){
		super(s);
	}
	
	private class DummyStorageManager extends StorageManager{

		public DummyStorageManager(ConfigurationContext context) {
			super(context);
		}

		public SandeshaThread getInvoker() {
			return null;
		}

		public InvokerBeanMgr getInvokerBeanMgr() {
			return null;
		}

		public PollingManager getPollingManager() {
			return null;
		}

		public RMDBeanMgr getRMDBeanMgr() {
			return null;
		}

		public RMSBeanMgr getRMSBeanMgr() {
			return null;
		}

		public SandeshaThread getSender() {
			return null;
		}

		public SenderBeanMgr getSenderBeanMgr() {return null;
		}

		public Transaction getTransaction() {
			return null;
		}

		public boolean hasUserTransaction(MessageContext message) throws SandeshaStorageException {
			return false;
		}

		public void initStorage(AxisModule moduleDesc) throws SandeshaStorageException {}

		public void removeMessageContext(String storageKey) throws SandeshaStorageException {}

		public boolean requiresMessageSerialization() {
			return false;
		}

		public MessageContext retrieveMessageContext(String storageKey, ConfigurationContext configContext) throws SandeshaStorageException {
			return null;
		}

		public void storeMessageContext(String storageKey, MessageContext msgContext) throws SandeshaStorageException {}

		public void updateMessageContext(String storageKey, MessageContext msgContext) throws SandeshaStorageException {}
		
	}
	
	public void testModifyExecutionChainForStoring() throws Exception{
		ArrayList executionChain = new ArrayList();
		executionChain.add(new Phase("one"));
		executionChain.add(new Phase("two"));
		executionChain.add(new Phase("MessageOut"));
		executionChain.add(new Phase("Security"));
		
		MessageContext mc = new MessageContext();
		mc.setExecutionChain(executionChain);
		mc.setTransportOut(new Sandesha2TransportOutDesc());
		StorageManager sm = new DummyStorageManager(null);
		SandeshaUtil.modifyExecutionChainForStoring(mc, sm);
		
		ArrayList retransmittablePhases = (ArrayList) mc.getProperty(Sandesha2Constants.RETRANSMITTABLE_PHASES);
		
		assertEquals(2, mc.getExecutionChain().size());
		assertEquals(2, retransmittablePhases.size());
		assertEquals("MessageOut", ((Handler)retransmittablePhases.get(0)).getName());
		assertEquals("Security", ((Handler)retransmittablePhases.get(1)).getName());
	}
}

