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
import org.apache.synapse.mediators.builtin.LogMediator;

/**
 * <pre>
 * &lt;log [level="simple|headers|full|custom"] [separator="string"]&gt;
 *      &lt;property&gt; *
 * &lt;/log&gt;
 * </pre>
 */
public class LogMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof LogMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        LogMediator mediator = (LogMediator) m;
        OMElement log = fac.createOMElement("log", synNS);
        saveTracingState(log,mediator);

        if (mediator.getLogLevel() != LogMediator.SIMPLE) {
            log.addAttribute(fac.createOMAttribute(
                "level", nullNS,
                    mediator.getLogLevel() == LogMediator.HEADERS ? "headers" :
                    mediator.getLogLevel() == LogMediator.FULL ? "full" :
                    mediator.getLogLevel() == LogMediator.CUSTOM ? "custom" : "simple"
                ));
        }

        if (mediator.getSeparator() != LogMediator.DEFAULT_SEP) {
            log.addAttribute(fac.createOMAttribute(
                "separator", nullNS, mediator.getSeparator()));
        }

        super.serializeProperties(log, mediator.getProperties());

        if (parent != null) {
            parent.addChild(log);
        }
        return log;
    }

    public String getMediatorClassName() {
        return LogMediator.class.getName();
    }
}
