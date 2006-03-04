package org.apache.synapse.spi.injection;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.extensions.utils.Axis2EnvSetup;
import org.apache.synapse.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import junit.framework.TestCase;
/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

public class SpringMediatorProcessorWithRuleTest extends TestCase {
    private MessageContext msgCtx;
        private SynapseEnvironment env;
        private String synapsexml =
                "<synapse xmlns=\"http://ws.apache.org/ns/synapse\" xmlns:spring=\"http://ws.apache.org/ns/synapse/spring\">\n" +
                        "<spring:springmediator bean=\"unit_test_spring_bean\" >\n" +
                        "    <beans xmlns=\"\">\n" +
                        "          <bean id=\"unit_test_spring_bean\" class=\"org.apache.synapse.extensions.utils.SimpleSpringBean\" >\n"+
                        "              <property name=\"epr\">\n"+
                        "                   <value>127.0.0.1:8008/axis2/services/MyService</value>\n"+
                        "              </property>\n"+
                        "              <property name=\"ip\">\n"+
                        "                   <value>192.168.1.245</value>\n"+
                        "              </property>\n"+
                        "           </bean>\n"+
                        "    </beans>\n" +
                        "</spring:springmediator>\n"+
                        "</synapse>";
        public void setUp() throws Exception {
            msgCtx = Axis2EnvSetup.axis2Deployment("target/synapse-repository");
            OMElement config =Axis2EnvSetup.getSynapseConfigElement(synapsexml);
            env = new Axis2SynapseEnvironment(config,
                    Thread.currentThread().getContextClassLoader());
        }

        public void testSpringProcessor() throws Exception {

            SynapseMessage smc = new Axis2SynapseMessage(msgCtx);
            env.injectMessage(smc);

        }

}
