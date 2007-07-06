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
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.PropertyManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaTestCase;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Iterator;

public class RMDBeanMgrTest extends SandeshaTestCase {
    
	private RMDBeanMgr mgr;
	Transaction transaction;
	
    public RMDBeanMgrTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        AxisConfiguration axisConfig = new AxisConfiguration();
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
        mgr = storageManager.getRMDBeanMgr();

    }
    
    public void tearDown() throws Exception {
    	transaction.commit();
    }

    public void testDelete() throws SandeshaStorageException{
        mgr.insert(new RMDBean("SeqId1", 1001));
        mgr.delete("SeqId1");
        assertNull(mgr.retrieve("SeqId1"));
    }

    public void testFind() throws SandeshaStorageException {
        mgr.insert(new RMDBean("SeqId2", 1002));
        mgr.insert(new RMDBean("SeqId3", 1002));

        RMDBean target = new RMDBean();
        target.setNextMsgNoToProcess(1002);

        Iterator iterator = mgr.find(target).iterator();
        RMDBean tmp = (RMDBean) iterator.next();

        if (tmp.getSequenceID().equals("SeqId2")) {
            tmp = (RMDBean) iterator.next();
            tmp.getSequenceID().equals("SeqId3");
        } else {
            tmp = (RMDBean) iterator.next();
            tmp.getSequenceID().equals("SeqId2");
        }

    }

    public void testInsert() throws SandeshaStorageException {
        RMDBean bean = new RMDBean("SeqId4", 1004);
        mgr.insert(bean);
        RMDBean tmp = mgr.retrieve("SeqId4");
        assertTrue(tmp.getNextMsgNoToProcess() == 1004);
    }

    public void testRetrieve() throws SandeshaStorageException {
        assertNull(mgr.retrieve("SeqId5"));
        mgr.insert(new RMDBean("SeqId5", 1005));

        RMDBean tmp = mgr.retrieve("SeqId5");
        assertTrue(tmp.getNextMsgNoToProcess() == 1005);
    }

    public void testUpdate() throws SandeshaStorageException {
        RMDBean bean = new RMDBean("SeqId6", 1006);
        mgr.insert(bean);
        bean.setNextMsgNoToProcess(1007);
        mgr.update(bean);
        RMDBean tmp = mgr.retrieve("SeqId6");
        assertTrue(tmp.getNextMsgNoToProcess() ==1007);
    }

}
