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

package org.apache.synapse.mediators.sla;

import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import org.apache.synapse.api.Mediator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

public class SLAMediator implements Mediator {

    private ClassLoader classLoader;

    private SynapseConfiguration synCfg;

    private Log log = LogFactory.getLog(getClass());

    public SLAMediator() {
    }

    public boolean mediate(MessageContext synCtx) {

        try {
            log.info("SLA Mediator!");
            // MessageContext mc =
            // ((Axis2SynapseMessage)synapseMessageContext).getMessageContext();
            String resource = SLAConstants.CFG_XML_FOLDER + "/"
                + SLAConstants.CFG_SLA_XML;
            InputStream inStream = classLoader.getResourceAsStream(resource);
            final SLAConfigurator slaConfigurator = new SLAConfigurator(
                inStream);
            SLAStack slaStack = null;
            try {
                if (synCfg.getProperty("PRIORITY_STACK") != null) {
                    slaStack = (SLAStack) synCfg.getProperty("PRIORITY_STACK");
                } else {
                    slaStack = new SLAStack();
                    synCfg.addProperty("PRIORITY_STACK", slaStack);
                }
            } catch (Exception ex) {
                log.info(ex);

            }
            String fromAddress = (String) synCtx.getFrom()
                .getAddress();
            int priority = slaConfigurator.getPriority(fromAddress,
                synCtx.getTo());
            SLAObject slaObject = new SLAObject(priority, System
                .currentTimeMillis(), fromAddress);
            try {
                slaStack.addRequest(slaObject);
            } catch (Exception ex) {
                log.info(ex);
            }

            while (true) {
                if (!slaStack.isEmpty()) {
                    SLAObject slaObjectStack = (SLAObject) slaStack.get(0);
                    if (slaObjectStack.equals(slaObject)) {
                        slaStack.remove(0);
                        break;
                    }
                } else {
                    break;
                }
            }

            return true;

        } catch (Exception e) {
            log.info(e);
            return false;
        }
    }

    public void setSynapseConfiguration(SynapseConfiguration se) {

        this.synCfg = se;
        if (se != null) {
            log.info("ENVIRONMENT NOT NULL IN SLA");
        }
    }

    public void setClassLoader(ClassLoader cl) {
        this.classLoader = cl;
    }

    public String getType() {
        return null;
    }
}
