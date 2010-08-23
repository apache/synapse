/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.mediators;

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.axis2.addressing.EndpointReference;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class URLRewriteMediator extends AbstractMediator {

    public static final int FULL_URI    = -1;
    public static final int PROTOCOL    = 0;
    public static final int USER_INFO   = 1;
    public static final int HOST        = 2;
    public static final int PORT        = 3;
    public static final int PATH        = 4;
    public static final int QUERY       = 5;
    public static final int REF         = 6;

    private List<RewriteRule> rules = new ArrayList<RewriteRule>();
    private String inputProperty;
    private String outputProperty;

    public boolean mediate(MessageContext messageContext) {
        Object[] fragments = newFragmentSet();
        URI uri;

        String address = getInputAddress(messageContext);
        if (address != null) {
            try {
                uri = new URI(address);
                fragments[0] = uri.getScheme();
                fragments[1] = uri.getUserInfo();
                fragments[2] = uri.getHost();
                fragments[3] = uri.getPort();
                fragments[4] = uri.getPath();
                fragments[5] = uri.getQuery();
                fragments[6] = uri.getFragment();

            } catch (URISyntaxException e) {
                handleException("Malformed URI in the input address field", e, messageContext);
                return false;
            }
        } else {
            uri = getURI(fragments, messageContext);
        }

        Map<String, String> headers = getHeaders(messageContext);
        for (RewriteRule r : rules) {
            r.rewrite(fragments, messageContext, uri.toString(), headers);
            uri = getURI(fragments, messageContext);
        }

        if (outputProperty != null) {
            messageContext.setProperty(outputProperty, uri.toString());
        } else {
            messageContext.setTo(new EndpointReference(uri.toString()));
        }
        return true;
    }

    private String getInputAddress(MessageContext messageContext) {
        if (inputProperty != null) {
            Object prop = messageContext.getProperty(inputProperty);
            if (prop != null && prop instanceof String) {
                return (String) prop;
            }
        } else if (messageContext.getTo() != null) {
            return messageContext.getTo().getAddress();
        }
        return null;
    }

    private Object[] newFragmentSet() {
        return new Object[] {
            null, null, null, -1, null, null, null
        };
    }

    private Map<String, String> getHeaders(MessageContext synCtx) {
        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx =
                axis2smc.getAxis2MessageContext();
        Object headers = axis2MessageCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map<String, String> evaluatorHeaders = new HashMap<String, String>();

        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            for (Object key : headersMap.keySet()) {
                if (key instanceof String && headersMap.get(key) instanceof String) {
                    evaluatorHeaders.put((String) key, (String) headersMap.get(key));
                }
            }
        }
        return evaluatorHeaders;
    }

    private URI getURI(Object[] fragments, MessageContext messageContext) {
        try {
            return new URI(
                    (String) fragments[0],
                    (String) fragments[1],
                    (String) fragments[2],
                    (Integer) fragments[3],
                    (String) fragments[4],
                    (String) fragments[5],
                    (String) fragments[6]);
        } catch (URISyntaxException e) {
            handleException("Error while constructing the URI from fragments", e, messageContext);
        }
        return null;
    }

    public void addRule(RewriteRule rule) {
        rules.add(rule);
    }

    public String getInputProperty() {
        return inputProperty;
    }

    public void setInputProperty(String inputProperty) {
        this.inputProperty = inputProperty;
    }

    public String getOutputProperty() {
        return outputProperty;
    }

    public void setOutputProperty(String outputProperty) {
        this.outputProperty = outputProperty;
    }
}
