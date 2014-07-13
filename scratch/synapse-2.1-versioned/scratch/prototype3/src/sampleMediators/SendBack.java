package sampleMediators;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseMessage;
import org.apache.axis2.addressing.EndpointReference;
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

public class SendBack implements Mediator {
    public boolean mediate(SynapseMessage smc) {
        /**
         * Since addressing is not engaged before running this test case 
         * To give the right behavior for this test-case
         * setTo is set for anonymous url.
         */
        smc.setTo(new EndpointReference("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous"));
        smc.setResponse(true);
        return true;
    }
}
