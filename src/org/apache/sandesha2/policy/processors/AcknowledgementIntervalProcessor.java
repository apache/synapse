package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class AcknowledgementIntervalProcessor {

	private boolean initializedAcknowledgementInterval = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeAcknowledgementIterval(RMPolicyToken rmpt)
			throws NoSuchMethodException {

	}

	public Object doAcknowledgementInterval(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();

		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedAcknowledgementInterval) {
				try {
					initializeAcknowledgementIterval(rmpt);
					initializedAcknowledgementInterval = true;
				} catch (NoSuchMethodException e) {
					logger.error("AcknowledgementIntervalProcessor:doAcknowledgementInterval", e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());

		case RMProcessorContext.COMMIT:

			// //////////

			PolicyEngineData engineData = rmpc.readCurrentPolicyEngineData();
			String txt = rmpc.getAssertion().getStrValue();
			engineData.setAcknowledgementInterval(Long.parseLong(txt.trim()));

			// /////////////////////////////////

			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}
}
