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

package org.apache.sandesha2.storage;

import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Iterator;


public class SenderBeanMgrTest extends SandeshaTestCase {
    
	private SenderBeanMgr mgr;
	Transaction transaction;
	
    public SenderBeanMgrTest() {
        super("SenderBeanMgrTest");
    }

    public void setUp() throws Exception {
        AxisConfiguration axisConfig = new AxisConfiguration();
        SandeshaPolicyBean propertyBean = PropertyManager.loadPropertiesFromDefaultValues();
        Parameter parameter = new Parameter ();
        parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
        parameter.setValue(propertyBean);
        axisConfig.addParameter(parameter);
        
        ConfigurationContext configCtx = new ConfigurationContext(axisConfig);
        
        ClassLoader classLoader = getClass().getClassLoader();
        configCtx.setProperty(Sandesha2Constants.MODULE_CLASS_LOADER,classLoader);
        
        StorageManager storageManager = SandeshaUtil.getInMemoryStorageManager(configCtx);
        transaction = storageManager.getTransaction();
        mgr = storageManager.getSenderBeanMgr();
    }
    
    public void tearDown() throws Exception {
    	transaction.commit();
    }

    public void testDelete() throws SandeshaStorageException {
        assertNull(mgr.retrieve(""));
        try {
            mgr.insert(new SenderBean("MsgId1", "Key1", false , 1001 , "TmpSeqId1", 1001));
        } catch (Exception ex) {
            fail("should not throw an exception");
        }
        assertNotNull(mgr.retrieve("MsgId1"));
    }

    public void testFind() throws SandeshaStorageException {
        try {
            mgr.insert(new SenderBean("MsgId2", "Key2", false , 1001 , "TmpSeqId2", 1002));
            mgr.insert(new SenderBean("MsgId3", "Key3", false , 1001 , "TmpSeqId2", 1003));

            SenderBean target = new SenderBean();
            target.setInternalSequenceID("TmpSeqId2");

            Iterator iterator = mgr.find(target,true).iterator();
            SenderBean tmp = (SenderBean) iterator.next();

            if (tmp.getMessageID().equals("MsgId2")) {
                tmp = (SenderBean) iterator.next();
                assertTrue(tmp.getMessageID().equals("MsgId3"));
            } else {
                tmp = (SenderBean) iterator.next();
                assertTrue(tmp.getMessageID().equals("MsgId2"));
            }


        } catch (SandeshaException e) {
            fail("should not throw an exception");
        }


    }

    public void testInsert() throws SandeshaStorageException {
//        try {
//            mgr.insert(new SenderBean());
//            fail("should throw an exception");
//
//        } catch (SandeshaException ex) {
//        }

        try {
            mgr.insert(new SenderBean("MsgId4","Key4", false , 1001 , "TmpSeqId4", 1004));
            SenderBean tmp = mgr.retrieve("MsgId4");
            assertTrue(tmp.getMessageContextRefKey().equals("Key4"));


        } catch (SandeshaException e) {
            fail("should not throw an exception");
        }

    }

    public void testRetrieve() throws SandeshaStorageException {
        assertNull(mgr.retrieve("MsgId5"));
        try {
            mgr.insert(new SenderBean("MsgId5", "Key5", false , 1001 , "TmpSeqId5", 1005));
        } catch (SandeshaException e) {
            fail("this should not throw an exception");
        }
        assertNotNull(mgr.retrieve("MsgId5"));
    }

    public void testUpdate() throws SandeshaStorageException {
        SenderBean bean = new SenderBean("MsgId6", "Key6", false , 1001 , "TmpSeqId6", 1006);
        try {
            mgr.insert(bean);
        } catch (SandeshaException e) {
            fail("should not throw an exception");
        }
        bean.setSend(true);
        mgr.update(bean);

        SenderBean tmp = mgr.retrieve("MsgId6");
        assertTrue(tmp.isSend());
    }
}
