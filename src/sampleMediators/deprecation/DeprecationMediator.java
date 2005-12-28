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

package sampleMediators.deprecation;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.EnvironmentAware;

import java.util.Map;
import java.io.InputStream;

public class DeprecationMediator implements Mediator, EnvironmentAware {

    DeprecationConfiguration configuration;
    private ClassLoader classLoader;

    public DeprecationMediator() {
    }

    public boolean mediate(SynapseMessage synapseMessageContext) {

        try {

            String resource = DeprecationConstants.CFG_XML_FOLDER+"/"+DeprecationConstants.CFG_DEPRECATION_XML;
            InputStream inStream = classLoader.getResourceAsStream(resource);
            final DeprecationConfigurator deprecationConfigurator =
                    new DeprecationConfigurator(inStream);
            Map mediatorConfig = deprecationConfigurator
                    .getConfig(synapseMessageContext.getTo());
            loadConfiguration(mediatorConfig);
            DeprecationRule rules[] = configuration.getRules();
            boolean deprecated = false;

            for (int i = 0, len = rules.length; i < len; i++) {

                if (rules[i].isDeprecated()) {

                    deprecated = true;
                }

            }

            synapseMessageContext.setProperty(
                    DeprecationConstants.CFG_DEPRECATION_RESULT,
                    Boolean.valueOf(deprecated));

            return !(deprecated);

        } catch (Exception e) {

            return false;
        }
    }

    private void loadConfiguration(Map mediatorConfig) {
        configuration = new DeprecationConfiguration();

        for (int i = 0; true; i++) {

            String serviceKey = DeprecationConstants.CFG_DEPRECATION_SERVICE +
                    "[" + i + "]";
            String fromDateKey = DeprecationConstants
                    .CFG_DEPRECATION_FROM_DATE + "[" + i + "]";
            String toDateKey = DeprecationConstants.CFG_DEPRECATION_TO_DATE +
                    "[" + i + "]";
            String enabledKey = DeprecationConstants.CFG_DEPRECATION_ENABLED +
                    "[" + i + "]";

            if (mediatorConfig.get(serviceKey) == null) {
                break;
            }


            DeprecationRule rule = new DeprecationRule();
            rule.setService((String) mediatorConfig.get(serviceKey));
            rule.setFromDate((String) mediatorConfig.get(fromDateKey));
            rule.setToDate((String) mediatorConfig.get(toDateKey));
            rule.setEnabled((String) mediatorConfig.get(enabledKey));
            configuration.addRule(rule);
        }

    }

    public void setSynapseEnvironment(SynapseEnvironment se) {
        //not used for now
    }

    public void setClassLoader(ClassLoader cl) {
        this.classLoader = cl;
    }
}
