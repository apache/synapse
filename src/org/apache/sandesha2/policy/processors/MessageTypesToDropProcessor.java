package org.apache.sandesha2.policy.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.policy.PolicyEngineData;
import org.apache.sandesha2.policy.RMPolicyToken;
import org.apache.sandesha2.policy.RMProcessorContext;

public class MessageTypesToDropProcessor {
	private boolean initializedMessageTypesToDrop = false;

	private Log logger = LogFactory.getLog(this.getClass().getName());

	public void initializeMessageTypesToDrop(RMPolicyToken spt)
			throws NoSuchMethodException {

	}

	public Object doMessageTypesToDrop(RMProcessorContext rmpc) {
		RMPolicyToken rmpt = rmpc.readCurrentRMToken();
		switch (rmpc.getAction()) {

		case RMProcessorContext.START:
			if (!initializedMessageTypesToDrop) {
				try {
					initializeMessageTypesToDrop(rmpt);
					initializedMessageTypesToDrop = true;
				} catch (NoSuchMethodException e) {
					logger
							.error(
									"Exception occured in initializeMessageTypesToDrop",
									e);
					return new Boolean(false);
				}
			}
			logger.debug(rmpt.getTokenName());

		case RMProcessorContext.COMMIT:

			// ////////////////////
			PolicyEngineData ped = rmpc.readCurrentPolicyEngineData();
			String text = rmpc.getAssertion().getStrValue();
			ped.setMessageTypesToDrop(text);
			// ////////////////////

			break;
		case RMProcessorContext.ABORT:
			break;
		}
		return new Boolean(true);
	}
}
