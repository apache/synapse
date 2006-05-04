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
package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.api.Mediator;

/**
 * This class is an abstract Mediator, that defines the logging and debugging
 * elements of a mediator class.
 */
public abstract class AbstractMediator implements Mediator {

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Returns the class name of the mediator
     * @return the class name of the mediator
     */
    public String getType() {
        return getClass().getSimpleName();
    }
}
