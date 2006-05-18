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
package org.apache.synapse.spring;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.SynapseConfigurationBuilder;
import org.apache.synapse.TestUtils;

import java.io.FileInputStream;

/**
 * This unit test is a different 'type' of a unit test, such that it tests end-to-end
 * like scenario of using Spring extensions! First it tests that the configuration
 * builder properly looks up specified named and anonymous spring configurations
 * and mediates properly to Spring mediator beans. The public static invokeCounter field
 * though ugly, serves the purpose to test that the Spring beans were properly created
 * and invoked
 */
public class SpringMediatorTest extends TestCase {

    public void testSpringBean() throws Exception {

        SynapseConfigurationBuilder synCfgBuilder = new SynapseConfigurationBuilder();
        synCfgBuilder.setConfiguration(
            new FileInputStream("./../../repository/conf/sample/synapse_sample_3.xml"));

        MessageContext msgCtx = TestUtils.getTestContext("<dummy/>");
        msgCtx.setConfiguration(synCfgBuilder.getConfig());
        msgCtx.getConfiguration().getMainMediator().mediate(msgCtx);

        assertEquals(TestMediateHandlerImpl.invokeCount, 202);
    }

}
