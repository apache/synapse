package org.apache.synapse;

import org.apache.axis2.context.SOAPMessageContext;

public class ResponseIdentifier {

	public static boolean isResponse(SOAPMessageContext smc) {
		Boolean bool = (Boolean)smc.getProperty(Constants.ISRESPONSE_PROPERTY);
		if (bool==null) {
			// TODO figure it out
			return false;
		}
		else
			return bool.booleanValue();
	}
	public static void setResponse(SOAPMessageContext smc, boolean b) {
		smc.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(b));
	}

}
