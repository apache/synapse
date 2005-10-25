package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.InOutOperationDescrition;
import org.apache.axis2.description.ModuleDescription;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.synapse.receiver.SynapseMessageReceiver;

import javax.xml.namespace.QName;
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

        ModuleDescription mpdule = axisConfiguration.getModule(
                new QName(SynapseConstant.SYNAPSE_MODULE));
        ServiceDescription service = new ServiceDescription(
                new QName(SynapseConstant.SYNAPSE_SERVICE));
        service.setClassLoader(mpdule.getModuleClassLoader());
        OperationDescription axisOp = new InOutOperationDescrition(
                new QName(SynapseConstant.SYNAPSE_OPERATION));
        axisOp.setMessageReceiver(new SynapseMessageReceiver());
        service.addOperation(axisOp);
        axisConfiguration.addService(service);

        //Starting SynapseEngine
        new SynapseEngine("../webapps/Synapse/WEB-INF/synapse.xml", axisConfiguration);

        //todo have to process syanpse.xml
    }

    public void shutdown(AxisConfiguration axisConfiguration) throws AxisFault {
    }

}
