package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
public class StockQuoteCallback implements AxisCallback {

    private static final Log log = LogFactory.getLog(StockQuoteCallback.class);

    StockQuoteSampleClient client;

    public StockQuoteCallback(StockQuoteSampleClient client) {
        this.client=client;
    }

    public void onMessage(org.apache.axis2.context.MessageContext messageContext) {
        log.info("Response received to the callback");
        OMElement result
                = messageContext.getEnvelope().getBody().getFirstElement();
        // Detach the result to make sure that the element we return to the sample client
        // is completely built
        result.detach();
        client.setResponse(result);
    }

    public void onFault(org.apache.axis2.context.MessageContext messageContext) {
        log.warn("Fault received to the callback : " + messageContext.getEnvelope().
                getBody().getFault());
    }

    public void onError(Exception e) {
        log.warn("Error inside callback : " + e);
    }

    public void onComplete() {
        client.setCompleted(true);
    }
}
