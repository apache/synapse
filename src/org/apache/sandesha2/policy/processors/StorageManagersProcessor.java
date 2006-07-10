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

package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicy;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class StorageManagersProcessor {
	private boolean initializedStorageManager = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeStorageManager(RMPolicyToken rmpt)
			throws NoSuchMethodException {
//		RMPolicyToken tmpRpt = RMPolicy.storageManager.copy();
//		tmpRpt.setProcessTokenMethod(this);
//		rmpt.setChildToken(tmpRpt);

		RMPolicyToken tmpRpt = RMPolicy.inMemoryStorageManager.copy();
		tmpRpt.setProcessTokenMethod(this);
		rmpt.setChildToken(tmpRpt);
		
		tmpRpt = RMPolicy.permanentStorageManager.copy();
		tmpRpt.setProcessTokenMethod(this);
		rmpt.setChildToken(tmpRpt);

	}

	public Object doStorageManagers(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedStorageManager) {
				try {
					initializeStorageManager(rmpt);
					initializedStorageManager = true;
				} catch (NoSuchMethodException e) {
					logger.error(
							"Exception occured in initializeStorageManager", e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());

		case RMProcessorContext.COMMIT:
			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}

//	public Object doStorageManager(RMProcessorContext rmpc) {
//		PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
//		String cls = rmpc.getAssertion().getStrValue();
//
//		if (cls != null && !cls.trim().equals("")) {
//			ped.setStorageManager(cls.trim());
//		}
//
//		return new Boolean(true);
//	}
	
	public Object doInMemoryStorageManager(RMProcessorContext rmpc) {
		PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
		String cls = rmpc.getAssertion().getStrValue();

		if (cls != null && !cls.trim().equals("")) {
			ped.setInMemoryStorageManager(cls.trim());
		}

		return new Boolean(true);
	}

	public Object doPermanentStorageManager(RMProcessorContext spc) {
		PolicyEngineData ped = spc.readCurrentPolicyEngineData();
		String cls = spc.getAssertion().getStrValue();

		if (cls != null && !cls.trim().equals("")) {
			ped.setPermanentStorageManager(cls.trim());
		}

		return new Boolean(true);
	}
}
