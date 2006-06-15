package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;


public class InactivityTimeoutProcessor {
	private boolean initializedInactivityTimeout = false;
	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeInactivityTimeout(RMPolicyToken spt)
			throws NoSuchMethodException {

	}

	public Object doInactivityTimeout(RMProcessorContext rmpc) {

		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedInactivityTimeout) {
				try {
					initializeInactivityTimeout(rmpt);
					initializedInactivityTimeout = true;
				} catch (NoSuchMethodException e) {
					logger.error("Exception occured in initializeInactivityTimeout", e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());
			
		case RMProcessorContext.COMMIT:
			
			///////////////
			PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
			String text = rmpc.getAssertion().getStrValue();
			ped.setInactivityTimeout(Long.parseLong(text));
			///////////////
			
			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}

}
