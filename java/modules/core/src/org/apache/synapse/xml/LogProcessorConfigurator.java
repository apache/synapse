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

package org.apache.synapse.xml;

import javax.xml.namespace.QName;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.builtin.LogProcessor;
import org.apache.axiom.om.OMElement;

/**
 *
 * 
 * <p>
 * Logs messages using Commons-logging. 
 * 
 * <xmp><log name="optional"/></xmp>
 * TODO add support for simple one-line log entry (doesn't cause body parsing)
 *
 */
public class LogProcessorConfigurator extends AbstractProcessorConfigurator {
    private static final QName LOG_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "log");


    public QName getTagQName() {
        return LOG_Q;
    }


    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        LogProcessor lp = new LogProcessor();
        super.setNameOnProcessor(se,el,lp);
        return lp;
    }

}
