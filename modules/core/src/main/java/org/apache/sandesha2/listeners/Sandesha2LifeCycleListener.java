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
package org.apache.sandesha2.listeners;

import org.apache.axis2.deployment.DeploymentLifeCycleListener;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.SandeshaUtil;

import java.util.List;
import java.util.Iterator;


public class Sandesha2LifeCycleListener implements DeploymentLifeCycleListener {

	public void preDeploy(AxisConfiguration axisConfig) throws AxisFault {

	}

	public void postDeploy(ConfigurationContext configurationContext) throws AxisFault {
		// find the persistance storage RMS and RMD beans and start workers
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(
				configurationContext, configurationContext.getAxisConfiguration());
		List<RMDBean> rmdBeans = storageManager.getRMDBeanMgr().find(null);

		for (Iterator<RMDBean> iter = rmdBeans.iterator(); iter.hasNext();){
			SandeshaUtil.startWorkersForSequence(configurationContext, iter.next());
		}

		List<RMSBean> rmsBeans = storageManager.getRMSBeanMgr().find(null);

		for (Iterator<RMSBean> iter = rmsBeans.iterator(); iter.hasNext();){
			SandeshaUtil.startWorkersForSequence(configurationContext, iter.next());
		}
	}
}
