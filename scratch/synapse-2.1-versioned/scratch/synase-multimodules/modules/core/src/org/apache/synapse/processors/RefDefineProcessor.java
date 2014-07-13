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

package org.apache.synapse.processors;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processor that reference to <refdefine/>
 */
public class RefDefineProcessor extends AbstractProcessor{
    private Log log = LogFactory.getLog(getClass());
	private String refDefine = null;

    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        log.debug("RefDfineProcessor-Process");
        Processor p = se.lookupProcessor(getRefDefine());
		if (p==null) log.debug("processor with name "+this.getRefDefine()+" not found");
		else if (p instanceof DefineProcessor) {
            DefineProcessor defp = (DefineProcessor)p;
            return defp.processRef(se,sm);
        }
		return true;
    }

    public void setRefDefine(String refDefine) {
        this.refDefine = refDefine;
    }

    public String getRefDefine() {
        return this.refDefine;
    }

}
