package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class InactivityTimeoutMeasureProcessor {
	private boolean initializedInactivityTimeoutMeasure = false;
	private Log logger = LogFactory.getLog(this.getClass().getName()); 
		
	public void initializeInactivityTimeoutMeasure(RMPolicyToken rmpt)
			throws NoSuchMethodException {

	}

	public Object doInactivityTimeoutMeasure(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedInactivityTimeoutMeasure) {
				try {
					initializeInactivityTimeoutMeasure(rmpt);
					initializedInactivityTimeoutMeasure = true;
				} catch (NoSuchMethodException e) {
					logger.error("Exception occured when initializeInactivityTimeoutMeasure", e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());
			
		case RMProcessorContext.COMMIT:
			
			//////////////
			
			PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
			String value = rmpc.getAssertion().getStrValue();
			ped.setInactivityTimeoutMeassure(value);
			
			//////////////
			
			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}

}
