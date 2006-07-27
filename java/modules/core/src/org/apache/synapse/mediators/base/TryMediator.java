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
package org.apache.synapse.mediators.base;

import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.synapse.api.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a ListMediator which is similar to a Java try-catch-finally but with a catch-all
 *
 * If any of the child mediators throws an exception during execution, this mediator
 * invokes the specified error handler sequence
 */
public class TryMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(TryMediator.class);

    private List errorHandlerMediators = new ArrayList();

    private List finallyMediators = new ArrayList();

    public boolean mediate(MessageContext synCtx) {
        log.debug("Try mediator :: mediate()");
        boolean retVal = true;
        try {
            return super.mediate(synCtx);

        } catch (SynapseException e) {
            // set exception information to message context
            Util.setErrorInformation(synCtx, e);

            Iterator it = errorHandlerMediators.iterator();
            while (it.hasNext()) {
                Mediator m = (Mediator) it.next();
                if (!m.mediate(synCtx)) {
                    return false;
                }
            }
        } finally {
            Iterator it = finallyMediators.iterator();
            while (it.hasNext()) {
                Mediator m = (Mediator) it.next();
                if (!m.mediate(synCtx)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List getErrorHandlerMediators() {
        return errorHandlerMediators;
    }

    public void setErrorHandlerMediators(List errorHandlerMediators) {
        this.errorHandlerMediators = errorHandlerMediators;
    }

    public List getFinallyMediators() {
        return finallyMediators;
    }

    public void setFinallyMediators(List finallyMediators) {
        this.finallyMediators = finallyMediators;
    }
}
