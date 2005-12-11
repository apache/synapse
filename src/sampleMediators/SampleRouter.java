package sampleMediators;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
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
*
*/

public class SampleRouter implements Mediator {
    public boolean mediate(SynapseMessage smc) {
        smc.setWSAAction("urn:synapse/sendon");
        smc.setTo(new EndpointReference(
                "http://localhost:8090/axis2/services/npe"));
        return true;
    }
}
