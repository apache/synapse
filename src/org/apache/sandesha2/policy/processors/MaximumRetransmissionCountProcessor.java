package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class MaximumRetransmissionCountProcessor {

	private boolean initializedMaximumRetransmissionCount = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeMaximumRetransmissionCount(RMPolicyToken rmpt)
			throws NoSuchMethodException {
	}

	public Object doMaximumRetransmissionCount(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();

		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedMaximumRetransmissionCount) {
				try {
					initializeMaximumRetransmissionCount(rmpt);
					initializedMaximumRetransmissionCount = true;
				} catch (NoSuchMethodException e) {
					logger.error("MaximumRetransmissionCountProcessor:doAcknowledgementInterval", e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());

		case RMProcessorContext.COMMIT:

			// //////////

			PolicyEngineData engineData = rmpc.readCurrentPolicyEngineData();
			String txt = rmpc.getAssertion().getStrValue();
			engineData.setMaximumRetransmissionCount(Integer.parseInt(txt.trim()));

			// /////////////////////////////////

			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}
}
