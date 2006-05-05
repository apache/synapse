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

package org.apache.synapse.config.xml;

import javax.xml.namespace.QName;

import org.apache.synapse.SynapseContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.om.OMElement;

/**
 * A mediator factory capable of creating an instance of a mediator through a given
 * XML should implement this interface
 */
public interface MediatorFactory {
    /**
     * Creates an instance of the mediator using the OMElement
     * @param el
     * @return the created mediator
     */
    public Mediator createMediator(OMElement el);

    /**
     * The QName of this mediator element in the XML config
     * @return QName of the mediator element
     */
    public QName getTagQName();
}
