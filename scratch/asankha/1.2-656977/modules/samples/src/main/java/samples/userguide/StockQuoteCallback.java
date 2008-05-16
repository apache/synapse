package samples.userguide;

import org.apache.axis2.client.async.AxisCallback;

/**
 * 
 */
public class StockQuoteCallback implements AxisCallback {

    public void onMessage(org.apache.axis2.context.MessageContext messageContext) {
        System.out.println("Response received to the callback");
        StockQuoteClient.InnerStruct.RESULT
                = messageContext.getEnvelope().getBody().getFirstElement();
    }

    public void onFault(org.apache.axis2.context.MessageContext messageContext) {
        System.out.println("Fault received to the callback : " + messageContext.getEnvelope().getBody().getFault());
    }

    public void onError(Exception e) {
        System.out.println("Error inside callback : " + e);
    }

    public void onComplete() {
        StockQuoteClient.InnerStruct.COMPLETED = true;
    }
}
