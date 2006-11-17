/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.RMMediator;

import javax.xml.namespace.QName;
/*
 * 
 */

public class RMMediatorFactory implements MediatorFactory {

    private static Log log = LogFactory.getLog(RMMediatorFactory.class);

    private static final QName RM_Q    = new QName(Constants.SYNAPSE_NAMESPACE, "enableRM");

    public Mediator createMediator(OMElement elem) {
        log.info("RMMediatorFactory  :: createMediator()");
        //TODO: Fill properties if needed
        return new RMMediator();
    }

    public QName getTagQName() {
        return RM_Q;
    }
}
