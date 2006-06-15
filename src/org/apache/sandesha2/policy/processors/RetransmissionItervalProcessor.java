package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class RetransmissionItervalProcessor {
	private boolean initializedRetransmissionInterval = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeRetranmissionIterval(RMPolicyToken spt)
			throws NoSuchMethodException {
		logger.debug("RetransmissionIntervalProcessor:initializeRetransmissionInterval");;

	}

	public Object doRetransmissionInterval(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedRetransmissionInterval) {
				try {
					initializeRetranmissionIterval(rmpt);
					initializedRetransmissionInterval = true;
				} catch (NoSuchMethodException e) {
					logger.error("Exception in initializeRetransmissionInterval", e);
					return new Boolean(false);
				}
			}

		case RMProcessorContext.COMMIT:

			// //////////////////
			PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
			String text = rmpc.getAssertion().getStrValue();
			ped.setRetransmissionInterval(Long.parseLong(text));
			// /////////////////

			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}
}
