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

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The switch mediator implements the functionality of the "switch" contruct. It first
 * evaluates the given XPath expression into a String value, and performs a match against
 * the given list of cases. This is actually a list of sequences, and depending on the
 * selected case, the selected sequence gets executed.
 */
public class SwitchMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(SwitchMediator.class);

    private AXIOMXPath source = null;
    private List cases = new ArrayList();
    private SwitchCaseMediator defaultCase = null;

    /**
     * Iterate over switch cases and find match and execute selected sequence
     * @param synCtx current context
     * @return as per standard semantics
     */
    public boolean mediate(SynapseMessageContext synCtx) {

        String sourceText = Util.getStringValue(source, synCtx);
        Iterator iter = cases.iterator();

        while (iter.hasNext()) {
            SwitchCaseMediator swCase = (SwitchCaseMediator) iter.next();
            if (swCase.matches(sourceText)) {
                return swCase.mediate(synCtx);
            }
        }

        if (defaultCase != null) {
            return defaultCase.mediate(synCtx);
        }

        return true;
    }

    /**
     * Adds the given mediator (Should be a SwitchCaseMediator) to the list of cases
     * of this Switch mediator
     * @param m the SwitchCaseMediator instance to be added
     */
    public void addCase(SwitchCaseMediator m) {
        cases.add(m);
    }

    /**
     * Return the source XPath expression set
     * @return thje source XPath expression
     */
    public AXIOMXPath getSource() {
        return source;
    }

    /**
     * Sets the source XPath expression
     * @param source the XPath expression to be used as the source
     */
    public void setSource(AXIOMXPath source) {
        this.source = source;
    }
}
