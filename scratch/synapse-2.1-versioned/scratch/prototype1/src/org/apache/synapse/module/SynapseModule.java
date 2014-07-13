package org.apache.synapse.module;

import org.apache.axis2.modules.Module;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.synapse.engine.SynapseConfiguration;
import org.apache.synapse.engine.SynapseDeployer;
import org.apache.synapse.engine.SynapseEngine;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.messagereceiver.SynapseMessageReceiver;

import javax.xml.namespace.QName;
import java.io.File;
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
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynapseModule implements Module {

    public void init(AxisConfiguration axisConfiguration) throws AxisFault {
        try {
            File syncongiFile = new File("./synapse.xml");
            SynapseConfiguration config = new SynapseDeployer(syncongiFile
            ).populteConfig();

            SynapseEngine engine = new SynapseEngine();
            engine.init(config);
            Parameter syPara = new ParameterImpl();
            syPara.setName(SynapseConstants.SYNAPSE_ENGINE);
            syPara.setValue(engine);
            axisConfiguration.addParameter(syPara);

             //adding a dumy service to the axisConfig
            AxisService service = new AxisService(
                    new QName(SynapseConstants.SYNAPSE_SERVICE));
            service.setClassLoader(axisConfiguration.getServiceClassLoader());
            AxisOperation axisOp = new InOutAxisOperation(
                    new QName(SynapseConstants.SYNAPSE_OPERATION));
            axisOp.setMessageReceiver(new SynapseMessageReceiver());
            service.addOperation(axisOp);
            axisConfiguration.addService(service);
        } catch (SynapseException e) {
            throw new AxisFault(e);
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void shutdown(AxisConfiguration axisConfiguration) throws AxisFault {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
