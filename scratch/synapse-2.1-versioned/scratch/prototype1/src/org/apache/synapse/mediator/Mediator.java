package org.apache.synapse.mediator;

import org.apache.axis2.context.MessageContext;

public interface Mediator  {
    public boolean mediate(MessageContext messageContext);

    public void addParameter(String key, Object value);

    public Object getParameter(String key);

}

