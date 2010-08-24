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

import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class RewriteAction {

    private static final Log log = LogFactory.getLog(RewriteAction.class);

    public static final int ACTION_SET      = 0;
    public static final int ACTION_APPEND   = 1;
    public static final int ACTION_PREPEND  = 2;
    public static final int ACTION_REPLACE  = 3;
    public static final int ACTION_REMOVE   = 4;

    private String value;
    private SynapseXPath xpath;
    private String regex;
    private int fragmentIndex = URLRewriteMediator.FULL_URI;
    private int actionType = ACTION_SET;

    public void execute(Object[] fragments, MessageContext messageContext) {
        String result;
        if (xpath != null) {
            result = xpath.stringValueOf(messageContext);
        } else {
            result = value;
        }

        if (fragmentIndex == URLRewriteMediator.FULL_URI) {
            try {
                URI uri;
                if (result != null) {
                    uri = new URI(result);
                    if (log.isTraceEnabled()) {
                        log.trace("Setting the URI to: " + result);
                    }
                } else {
                    uri = new URI("");
                }

                fragments[URLRewriteMediator.PROTOCOL] = uri.getScheme();
                fragments[URLRewriteMediator.USER_INFO] = uri.getUserInfo();
                fragments[URLRewriteMediator.HOST] = uri.getHost();
                fragments[URLRewriteMediator.PORT] = uri.getPort();
                // The uri.getPath() return the empty string for empty URIs
                // We are better off setting it to null
                fragments[URLRewriteMediator.PATH] = "".equals(uri.getPath()) ? null : uri.getPath();
                fragments[URLRewriteMediator.QUERY] = uri.getQuery();
                fragments[URLRewriteMediator.REF] = uri.getFragment();

            } catch (URISyntaxException e) {
                String msg = "Error while setting the URL to: " + result;
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
        } else if (fragmentIndex == URLRewriteMediator.PORT) {
            // When setting the port we must first convert the value into an integer
            if (result != null) {
                fragments[fragmentIndex] = Integer.parseInt(result);
            } else {
                fragments[fragmentIndex] = -1;
            }
        } else {
            String str;
            switch (actionType) {
                case ACTION_PREPEND:
                    str = result +
                            (fragments[fragmentIndex] != null ? fragments[fragmentIndex] : "");
                    break;

                case ACTION_APPEND:
                    str = (fragments[fragmentIndex] != null ? fragments[fragmentIndex] : "") +
                                    result;
                    break;

                case ACTION_REPLACE:
                    str = (fragments[fragmentIndex] != null ? (String) fragments[fragmentIndex] : "");
                    str = str.replaceAll(regex, result);
                    break;

                case ACTION_REMOVE:
                    str = null;
                    break;

                default:
                    str = result;
            }            
            fragments[fragmentIndex] = str;
        }
    }

    public int getFragmentIndex() {
        return fragmentIndex;
    }

    public void setFragmentIndex(int fragmentIndex) {
        this.fragmentIndex = fragmentIndex;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SynapseXPath getXpath() {
        return xpath;
    }

    public void setXpath(SynapseXPath xpath) {
        this.xpath = xpath;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }
}
