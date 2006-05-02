/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.mediators.filters;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.AbstractListMediator;
import org.jaxen.JaxenException;

import java.util.regex.Pattern;


/**
 * The filter mediator combines the regex and xpath filtering functionality. If an xpath
 * is set, it is evaluated; else the given regex is evaluated against the source xpath.
 */
public class FilterMediator extends AbstractListMediator {

    private String source = null;
    private String regex = null;
    private String xpath = null;

    public boolean mediate(SynapseMessage synMsg) {
        if (test(synMsg)) {
            return super.mediate(synMsg);
        } else {
            return true;
        }
    }

    public boolean test(SynapseMessage synMsg) {
        try {
            if (xpath != null) {
                AXIOMXPath xp = new AXIOMXPath(xpath);
                return xp.booleanValueOf(synMsg.getEnvelope());

            } else if (source != null && regex != null) {
                Pattern pattern = Pattern.compile(regex);
                AXIOMXPath xp = new AXIOMXPath(source);
                Object result = xp.evaluate(synMsg.getEnvelope());
                return pattern.matcher(result.toString()).matches();

            } else {
                log.error("Invalid configuration specified");
                return false;
            }

        } catch (JaxenException e) {
            log.error("XPath error : " + e.getMessage());
            return false;
        }
    }


    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    //TODO name space addition support for xpath
    // i.e. xp.addNamespace(prefix, uri);
}
