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

import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Iterator;

public class StorageMapBeanMgrTest extends SandeshaTestCase {

    InvokerBeanMgr mgr;
    Transaction transaction;
    
    public StorageMapBeanMgrTest() {
        super ("StorageMapBeanMgrTest");
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
        mgr = storageManager.getStorageMapBeanMgr();
    }
    
    public void tearDown() throws Exception {
    	transaction.commit();
    }

    public void testDelete() throws SandeshaStorageException {
        mgr.insert(new InvokerBean("Key1", 1001, "SeqId1"));
        mgr.delete("Key1");
        assertNull(mgr.retrieve("Key1"));
    }

    public void testFind() throws SandeshaStorageException {
        mgr.insert(new InvokerBean("Key2", 1002, "SeqId2"));
        mgr.insert(new InvokerBean("Key3", 1003, "SeqId2"));

        InvokerBean bean = new InvokerBean();
        bean.setSequenceID("SeqId2");

        Iterator iter = mgr.find(bean).iterator();
        InvokerBean tmp = (InvokerBean) iter.next();

        if (tmp.getMessageContextRefKey().equals("Key2")) {
            tmp = (InvokerBean) iter.next();
            assertTrue(tmp.getMessageContextRefKey().equals("Key3"));
        } else {
            tmp = (InvokerBean) iter.next();
            assertTrue(tmp.getMessageContextRefKey().equals("Key2"));

        }
    }

    public void testInsert() throws SandeshaStorageException {
        mgr.insert(new InvokerBean("Key4", 1004, "SeqId4"));
        InvokerBean tmp = mgr.retrieve("Key4");
        assertTrue(tmp.getMessageContextRefKey().equals("Key4"));
    }

    public void testRetrieve() throws SandeshaStorageException {
        assertNull(mgr.retrieve("Key5"));
        mgr.insert(new InvokerBean("Key5", 1004, "SeqId5"));
        assertNotNull(mgr.retrieve("Key5"));
    }

    public void testUpdate() throws SandeshaStorageException {
        InvokerBean bean = new InvokerBean("Key6", 1006, "SeqId6");
        mgr.insert(bean);
        bean.setMsgNo(1007);
        mgr.update(bean);
        InvokerBean tmp = mgr.retrieve("Key6");
        assertTrue(tmp.getMsgNo() == 1007);
    }
}
