package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

//This is has to be improved, current impl only used to load synpase.xml , and this allow hot changing
//of synapse.xml

public class SynapseEngine extends Thread{

    private RuleEngine ruleEngine;
    private int refreshTime ;
    private long lastUpdate;
    private String location;
    private AxisConfiguration axisConfig;

    public SynapseEngine(String location,AxisConfiguration axisConfig) throws AxisFault {
        this.location = location;
        this.axisConfig = axisConfig;
        ruleEngine = populeteRuleEngine(null);
        Parameter par= new ParameterImpl();
        par.setName(SynapseConstant.RULE_ENGINE);
        par.setValue(ruleEngine);
        axisConfig.addParameter(par);
        this.start();
    }

    private RuleEngine populeteRuleEngine(RuleEngine ruleeng) throws AxisFault {
        try {
            File syanpseFile = new File(location);
            lastUpdate = syanpseFile.lastModified();
            InputStream in = new FileInputStream(syanpseFile);
            XMLStreamReader xmlReader =
                    XMLInputFactory.newInstance().createXMLStreamReader(in);
            OMFactory fac = OMAbstractFactory.getOMFactory();
            StAXOMBuilder staxOMBuilder = new StAXOMBuilder(fac, xmlReader);
            OMElement element = staxOMBuilder.getDocumentElement();
            element.build();

            OMElement time = element.getFirstChildWithName(new QName("searchTime"));
            OMAttribute value = time.getAttribute(new QName("value"));
            refreshTime = Integer.parseInt(value.getAttributeValue());

            OMElement ruleengine = element.getFirstChildWithName(new QName("ruleengine"));
            if (ruleengine == null) {
                throw new AxisFault("no rule engine found");
            } else {
                OMAttribute attr = ruleengine.getAttribute(new QName("class"));
                if (attr == null) {
                    throw new AxisFault("no rule engine found");
                } else {
                    String className = attr.getAttributeValue();
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class engineClass = Class.forName(className,
                            true, loader);
                    if(ruleeng == null){
                        ruleeng = (RuleEngine) engineClass.newInstance();
                    }
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
        } catch (FileNotFoundException e) {
            throw new AxisFault(e);
        }
    }

    private void processMediators(OMElement ele, ClassLoader loader, RuleEngine engine) throws AxisFault {
        Iterator child = ele.getChildrenWithName(new QName("rule"));
        while (child.hasNext()) {
            OMElement omElement = (OMElement) child.next();
            OMAttribute id = omElement.getAttribute(new QName("id"));
            OMAttribute clazz = omElement.getAttribute(new QName("mediator"));
            System.out.println("ID:" + id.getAttributeValue() + " mediator: " + clazz.getAttributeValue());
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

    public void run() {
        while(true){
            try {
                File  syanpseFile = new File(location);
                if(lastUpdate != syanpseFile.lastModified()){
                    ruleEngine = populeteRuleEngine(ruleEngine);
                    Parameter par= new ParameterImpl();
                    par.setName(SynapseConstant.RULE_ENGINE);
                    par.setValue(ruleEngine);
                    axisConfig.addParameter(par);
                }
                sleep(refreshTime);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                break;
            }
        }
    }

}
