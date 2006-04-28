package org.apache.synapse.extensions.utils;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseMessage;
import junit.framework.TestCase;
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

public class SimpleSpringBean implements Mediator {
    public String getEpr() {
        return epr;
    }

    public void setEpr(String epr) {
        this.epr = epr;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    private String epr;
    private String ip;

    public boolean mediate(SynapseMessage smc) {
        TestCase.assertNotNull(getIp());
        TestCase.assertNotNull(getEpr());
        return true;
    }
}
