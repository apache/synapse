package com.infravio.broker.modules.routing;

import com.infravio.broker.parser.XMLParser;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class RoutingInHandler extends AbstractHandler implements Handler {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    static String ServiceName;

    XMLParser parser = null;

    public void invoke(MessageContext msgContext) {
        parser = new XMLParser();
        ServiceName = msgContext.getServiceContext().getServiceConfig()
                .getName().getLocalPart();
        HashMap urls = parser.getURLs(ServiceName);
        String operation = msgContext.getOperationContext().getAxisOperation()
                .getName().getLocalPart();
        Set urlSet = urls.keySet();
        if (operation.equals("add")) {
            String url = null;
            for (Iterator i = urlSet.iterator(); i.hasNext();) {
                url = (String) i.next();
                String EPR = ServiceName + ".RoutingInHandler.targetEPR";
                msgContext.setProperty(EPR, url);
                break;
            }
        } else {
            String url = null;
            for (Iterator i = urlSet.iterator(); i.hasNext();) {
                url = (String) i.next();
                String EPR = ServiceName + ".RoutingInHandler.targetEPR";
                msgContext.setProperty(EPR, url);
            }
        }
    }

    public void revoke(MessageContext msgContext) {
        System.out.println("Routing Handler revoked");
    }
}
