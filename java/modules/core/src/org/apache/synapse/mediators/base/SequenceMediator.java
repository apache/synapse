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

import org.apache.synapse.SynapseException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Util;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Sequence mediator either refers to a named Sequence mediator instance
 * or is a *Named* list/sequence of other (child) Mediators
 *
 * If this instance defines a sequence mediator, then the name is required, and
 * an errorHandler sequence name optional. If this instance refers to another (defined)
 * sequence mediator, the errorHandler will not have a meaning, and if an error in
 * encountered in the reffered sequence, its errorHandler would execute.
 */
public class SequenceMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(SequenceMediator.class);
    private String name = null;
    private String ref = null;
    private String errorHandler = null;

    /**
     * If this mediator refers to another named Sequence, execute that. Else
     * execute the list of mediators (children) contained within this. If a referenced
     * named sequence mediator instance cannot be found at runtime, an exception is
     * thrown. This may occur due to invalid configuration of an erroneous runtime
     * change of the synapse configuration. It is the responsibility of the
     * SynapseConfiguration builder to ensure that dead references are not present.
     *
     * @param synCtx the synapse message
     * @return as per standard mediator result
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Sequence mediator <" + (name == null? "anonymous" : name ) +"> :: mediate()");
        if (ref == null) {
            try {
                return super.mediate(synCtx);

            } catch (SynapseException e) {

                if (errorHandler != null) {
                    // set exception information to message context
                    Util.setErrorInformation(synCtx, e);

                    Mediator errHandler = synCtx.getConfiguration().getNamedMediator(errorHandler);
                    if (errHandler == null) {
                        handleException("Error handler sequence mediator instance named " +
                            errorHandler + " cannot be found");
                    } else {
                        return errHandler.mediate(synCtx);
                    }
                
                } else {
                    throw e;
                }
            }

        } else {
            Mediator m = synCtx.getConfiguration().getNamedMediator(ref);
            if (m == null) {
                handleException("Sequence mediator instance named " + ref + " cannot be found.");
            } else {
                return m.mediate(synCtx);
            }
        }
        return false;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(String errorHandler) {
        this.errorHandler = errorHandler;
    }
}
