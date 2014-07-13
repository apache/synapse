package org.apache.synapse.samples.framework;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageCounter extends AbstractHandler {

    private Map<String,AtomicInteger> counter = new HashMap<String, AtomicInteger>();

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        String service = "_anon_";
        String operation = "_anon_";
        if (msgContext.getAxisService() != null) {
            service = msgContext.getAxisService().getName();
        }
        if (msgContext.getAxisOperation() != null) {
            operation = msgContext.getAxisOperation().getName().getLocalPart();
        }
        String key = getKey(service, operation);
        synchronized (this) {
            if (counter.containsKey(key)) {
                counter.get(key).incrementAndGet();
            } else {
                counter.put(key, new AtomicInteger(1));
            }
        }

        return InvocationResponse.CONTINUE;
    }

    public int getCount(String service, String operation) {
        String key = getKey(service, operation);
        synchronized (this) {
            if (counter.containsKey(key)) {
                return counter.get(key).get();
            }
        }
        return 0;
    }

    private String getKey(String service, String operation) {
        return service + ":" + operation;
    }

}
