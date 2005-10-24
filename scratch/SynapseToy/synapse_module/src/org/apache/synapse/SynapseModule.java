package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.receiver.SynapseMessageReceiver;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
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
        File syanpseFile = new File("../webapps/Synapse/WEB-INF/synapse.xml");
        if (!syanpseFile.exists()) {
            throw new AxisFault("synapse.xml does not exist");
        } else {
            try {
                InputStream in = new FileInputStream(syanpseFile);
                RuleEngine re= populeteRuleEngine(in, mpdule.getModuleClassLoader());
                Parameter par= new ParameterImpl();
                par.setName(SynapseConstant.RULE_ENGINE);
                par.setValue(re);
                axisConfiguration.addParameter(par);
            } catch (FileNotFoundException e) {
                throw new AxisFault(e);
            }
        }

        ServiceDescription service = new ServiceDescription(
                new QName(SynapseConstant.SYNAPSE_SERVICE));
        service.setClassLoader(mpdule.getModuleClassLoader());
        OperationDescription axisOp = new InOutOperationDescrition(
                new QName(SynapseConstant.SYNAPSE_OPERATION));
        axisOp.setMessageReceiver(new SynapseMessageReceiver());
        service.addOperation(axisOp);
        axisConfiguration.addService(service);

        //todo have to process syanpse.xml
    }

    public void shutdown(AxisConfiguration axisConfiguration) throws AxisFault {
    }

    private RuleEngine populeteRuleEngine(InputStream in, ClassLoader loader) throws AxisFault {
        try {
            XMLStreamReader xmlReader =
                    XMLInputFactory.newInstance().createXMLStreamReader(in);
            OMFactory fac = OMAbstractFactory.getOMFactory();
            StAXOMBuilder staxOMBuilder = new StAXOMBuilder(fac, xmlReader);
            OMElement element = staxOMBuilder.getDocumentElement();
            element.build();

            OMElement ruleengine = element.getFirstChildWithName(new QName("ruleengine"));
            if (ruleengine == null) {
                throw new AxisFault("no rule engine found");
            } else {
                OMAttribute attr = ruleengine.getAttribute(new QName("class"));
                if (attr == null) {
                    throw new AxisFault("no rule engine found");
                } else {
                    String className = attr.getAttributeValue();
                    Class engineClass = Class.forName(className,
                            true, loader);
                    RuleEngine ruleeng = (RuleEngine) engineClass.newInstance();
                    processMediators(element, loader, ruleeng);
                    return ruleeng;
                }
            }
        } catch (XMLStreamException e) {
            throw new AxisFault(e);
        } catch (ClassNotFoundException e) {
            throw new AxisFault(e);
        } catch (IllegalAccessException e) {
            throw new AxisFault(e);
        } catch (InstantiationException e) {
            throw new AxisFault(e);
        }
    }

    private void processMediators(OMElement ele, ClassLoader loader, RuleEngine engine) throws AxisFault {
        Iterator child = ele.getChildrenWithName(new QName("rule"));
        while (child.hasNext()) {
            OMElement omElement = (OMElement) child.next();
            OMAttribute id = omElement.getAttribute(new QName("id"));
            OMAttribute clazz = omElement.getAttribute(new QName("mediator"));
            try {
                String className = clazz.getAttributeValue();
                Class engineClass = Class.forName(className,
                        true, loader);
                Mediators medi = (Mediators) engineClass.newInstance();
                engine.addRules(id.getAttributeValue(), medi);
            } catch (ClassNotFoundException e) {
                throw new AxisFault(e);
            } catch (IllegalAccessException e) {
                throw new AxisFault(e);
            } catch (InstantiationException e) {
                throw new AxisFault(e);
            }
        }
    }
}
