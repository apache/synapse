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
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Iterator;


public class RMSBeanMgrTest extends SandeshaTestCase {
    private RMSBeanMgr mgr;
    Transaction transaction;
    
    public RMSBeanMgrTest() {
        super("RMSBeanMgrTest");
    }

    public void setUp() throws Exception {
    	
        AxisConfiguration axisConfig =  new AxisConfiguration();
        SandeshaPolicyBean propertyBean = new SandeshaPolicyBean ();
        Parameter parameter = new Parameter ();
        parameter.setName(Sandesha2Constants.SANDESHA_PROPERTY_BEAN);
        parameter.setValue(propertyBean);
        axisConfig.addParameter(parameter);
        
        ConfigurationContext configCtx = new ConfigurationContext(axisConfig);

        ClassLoader classLoader = getClass().getClassLoader();
        parameter = new Parameter(Sandesha2Constants.MODULE_CLASS_LOADER,classLoader);
        axisConfig.addParameter(parameter);
        
        StorageManager storageManager = SandeshaUtil.getInMemoryStorageManager(configCtx);
        transaction = storageManager.getTransaction();
        mgr = storageManager.getRMSBeanMgr();
    }
    
    public void tearDown() throws Exception {
    	transaction.commit();
    }

    public void testDelete() throws SandeshaStorageException {
    	RMSBean rMSBean = new RMSBean ();
    	rMSBean.setInternalSequenceID("TmpSeqId1");
    	rMSBean.setCreateSeqMsgID("CreateSeqMsgId1");
    	rMSBean.setSequenceID("SeqId1");
        mgr.insert(rMSBean);
        mgr.delete("CreateSeqMsgId1");
        assertNull(mgr.retrieve("CreateSeqMsgId1"));
    }

    public void testFind() throws SandeshaStorageException {
    	RMSBean createSeqBean1 = new RMSBean ();
    	createSeqBean1.setInternalSequenceID("TmpSeqId1");
    	createSeqBean1.setCreateSeqMsgID("CreateSeqMsgId1");
    	createSeqBean1.setSequenceID("SeqId1");
    	
    	RMSBean createSeqBean2 = new RMSBean ();
    	createSeqBean2.setInternalSequenceID("TmpSeqId1");
    	createSeqBean2.setCreateSeqMsgID("CreateSeqMsgId2");
    	createSeqBean2.setSequenceID("SeqId2");
    	
        mgr.insert(createSeqBean1);
        mgr.insert(createSeqBean2);

        RMSBean target = new RMSBean();
        target.setInternalSequenceID("TmpSeqId1");

        Iterator iter = mgr.find(target).iterator();
        RMSBean tmp = (RMSBean) iter.next();
        if (tmp.getCreateSeqMsgID().equals("CreateSeqMsgId1")) {
            tmp = (RMSBean) iter.next();
            assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId2"));

        }   else {
            tmp = (RMSBean) iter.next();
            assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId1"));
        }
    }

    public void testInsert() throws SandeshaStorageException{
    	RMSBean rMSBean = new RMSBean ();
    	rMSBean.setInternalSequenceID("TmpSeqId4");
    	rMSBean.setCreateSeqMsgID("CreateSeqMsgId4");
    	rMSBean.setSequenceID("SeqId4");
        mgr.insert(rMSBean);
        RMSBean tmpbean = mgr.retrieve("CreateSeqMsgId4");
        assertTrue(tmpbean.getCreateSeqMsgID().equals("CreateSeqMsgId4"));
        assertTrue(tmpbean.getSequenceID().equals("SeqId4"));
        assertTrue(tmpbean.getInternalSequenceID().equals("TmpSeqId4"));
    }


    public void testRetrieve() throws SandeshaStorageException{
        assertNull(mgr.retrieve("CreateSeqMsgId5"));

    	RMSBean rMSBean = new RMSBean ();
    	rMSBean.setInternalSequenceID("TmpSeqId5");
    	rMSBean.setCreateSeqMsgID("CreateSeqMsgId5");
    	rMSBean.setSequenceID("SeqId5");
        mgr.insert(rMSBean);
        RMSBean tmp = mgr.retrieve("CreateSeqMsgId5");
        assertTrue(tmp.getCreateSeqMsgID().equals("CreateSeqMsgId5"));
    }

    public void testUpdate() throws SandeshaStorageException {
    	RMSBean rMSBean = new RMSBean ();
    	rMSBean.setInternalSequenceID("TmpSeqId6");
    	rMSBean.setCreateSeqMsgID("CreateSeqMsgId6");
    	rMSBean.setSequenceID("SeqId6");
        
        mgr.insert(rMSBean);
        rMSBean.setInternalSequenceID("TmpSeqId7");
        mgr.update(rMSBean);
        RMSBean tmp = mgr.retrieve("CreateSeqMsgId6");
        assertTrue(tmp.getInternalSequenceID().equals("TmpSeqId7"));
    }
}
