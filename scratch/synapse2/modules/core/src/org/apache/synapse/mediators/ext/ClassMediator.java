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

package org.apache.synapse.mediators.ext;


import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This executes the "mediate" operation on a new instance of the specified class
 * <p/>
 * TODO add support for simple properties to be set
 * TODO Requires refactoring and cleanup - revisit later
 */
public class ClassMediator extends AbstractMediator {

    private Class clazz = null;

    private static final Log log = LogFactory.getLog(ClassMediator.class);

    public boolean mediate(SynapseMessage smc) {
        log.debug(getType() + " mediate()");
        Mediator m = null;

        try {
            m = (Mediator) getClazz().newInstance();
        } catch (Exception e) {
            throw new SynapseException(e);
        }
        /*if (EnvironmentAware.class.isAssignableFrom(m.getClass())) {
              ((EnvironmentAware) m).setSynapseContext(se);
          }*/
        return m.mediate(smc);

    }


    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

}
