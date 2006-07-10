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

import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaPropertyBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Iterator;


public class CreateSeqBeanMgrTest extends SandeshaTestCase {
    private CreateSeqBeanMgr mgr;
    Transaction transaction;
    
    public CreateSeqBeanMgrTest() {
        super("CreateSeqBeanMgrTest");
    }

    public void setUp() throws Exception {
    	
        AxisConfiguration axisConfig =  new AxisConfiguration();
        SandeshaPropertyBean propertyBean = PropertyManager.loadPropertiesFromDefaultValues();
        Parameter parameter = new Parameter ();
        parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
        parameter.setValue(propertyBean);
        axisConfig.addParameter(parameter);
        
        ConfigurationContext configCtx = new ConfigurationContext(axisConfig);

        ClassLoader classLoader = getClass().getClassLoader();
        configCtx.setProperty(Sandesha2Constants.MODULE_CLASS_LOADER,classLoader);
        
        StorageManager storageManager = SandeshaUtil.getInMemoryStorageManager(configCtx);
        transaction = storageManager.getTransaction();
        mgr = storageManager.getCreateSeqBeanMgr();
    }
    
    public void tearDown() throws Exception {
    	transaction.commit();
    }

    public void testDelete() throws SandeshaStorageException {
        mgr.insert(new CreateSeqBean("TmpSeqId1", "CreateSeqMsgId1", "SeqId1"));
        mgr.delete("CreateSeqMsgId1");
        assertNull(mgr.retrieve("CreateSeqMsgId1"));
    }

    public void testFind() throws SandeshaStorageException {
        mgr.insert(new CreateSeqBean("TmpSeqId2", "CreateSeqMsgId2", "SeqId2"));
        mgr.insert(new CreateSeqBean("TmpSeqId2", "CreateSeqMsgId3", "SeqId3"));

        CreateSeqBean target = new CreateSeqBean();
        target.setInternalSequenceID("TmpSeqId2");

        Iterator iter = mgr.find(target).iterator();
        CreateSeqBean tmp = (CreateSeqBean) iter.next();
        if (tmp.getCreateSeqMsgID().equals("CreateSeqMsgId1")) {
            tmp = (CreateSeqBean) iter.next();
            assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId2"));

        }   else {
            tmp = (CreateSeqBean) iter.next();
            assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId3"));
        }
    }

    public void testInsert() throws SandeshaStorageException{
        CreateSeqBean bean = new CreateSeqBean("TmpSeqId4", "CreateSeqMsgId4", "SeqId4");
        mgr.insert(bean);
        CreateSeqBean tmpbean = mgr.retrieve("CreateSeqMsgId4");
        assertTrue(tmpbean.getCreateSeqMsgID().equals("CreateSeqMsgId4"));
        assertTrue(tmpbean.getSequenceID().equals("SeqId4"));
        assertTrue(tmpbean.getInternalSequenceID().equals("TmpSeqId4"));
    }


    public void testRetrieve() throws SandeshaStorageException{
        assertNull(mgr.retrieve("CreateSeqMsgId5"));

        CreateSeqBean bean = new CreateSeqBean("TmpSeqId5", "CreateSeqMsgId5", "SeqId5");
        mgr.insert(bean);
        CreateSeqBean tmp = mgr.retrieve("CreateSeqMsgId5");
        assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId5"));
    }

    public void testUpdate() throws SandeshaStorageException {

        CreateSeqBean bean = new CreateSeqBean("TmpSeqId6", "CreateSeqMsgId6", "SeqId6");
        mgr.insert(bean);
        bean.setInternalSequenceID("TmpSeqId7");
        mgr.update(bean);
        CreateSeqBean tmp = mgr.retrieve("CreateSeqMsgId6");
        assertTrue(tmp.getInternalSequenceID().equals("TmpSeqId7"));
    }
}
