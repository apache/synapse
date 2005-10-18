package org.apache.synapse.rules;

import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.synapse.SynapseConstants;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 11, 2005
 * Time: 3:31:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleReader {
    public static final String RULE_XML = "META-INF/synapse.xml";
    private ArrayList ruleList;

    public SynapseRuleReader() {
        ruleList = new ArrayList();
    }

    private OMElement readRules(ServiceDescription serviceDescription)
            throws AxisFault {

        try {
            InputStream in = serviceDescription.getClassLoader()
                    .getResourceAsStream(RULE_XML);
            if (in == null) {
                return null;
            } else {
                XMLStreamReader parser = XMLInputFactory.newInstance()
                        .createXMLStreamReader(in);
                OMFactory fac = OMAbstractFactory.getOMFactory();
                StAXOMBuilder staxOMBuilder = new StAXOMBuilder(fac, parser);
                OMElement element = staxOMBuilder.getDocumentElement();
                element.build();
                return element;
            }
//

        } catch (XMLStreamException e) {
            throw new AxisFault(e);
        }
    }

    public void populateRules(AxisConfiguration registry) throws
            AxisFault {
        boolean beanReader = false;
        HashMap serviceMap = registry.getServices();
        try {
            Iterator ite = serviceMap.entrySet().iterator();
            Map.Entry entry = null;
            String service = null;
            while (ite.hasNext()) {
                entry = (Map.Entry) ite.next();
                service = (String) entry.getKey();
                if (!beanReader) {
                    ServiceDescription serviceDes = registry
                            .getService(service);
                    OMElement rules = readRules(serviceDes);
                    if (rules != null) {
                        beanReader = this.fillBean(rules);
                    }
                }
            }
        } catch (AxisFault axisFault) {
            throw new AxisFault(axisFault);
        }
    }

    private boolean fillBean(OMElement ele) {
        Iterator children = ele.getChildElements();
        while (children.hasNext()) {
            OMElement rule = (OMElement) children.next();
            OMElement condition = null;
            OMElement mediation = null;
            Iterator childite = rule.getChildElements();
            while (childite.hasNext()) {
                OMElement childele = (OMElement) childite.next();
                if (childele.getLocalName().equalsIgnoreCase(
                        SynapseConstants.SynapseRuleReader.CONDITION)) {
                    condition = childele;
                }
                if (childele.getLocalName().equalsIgnoreCase(
                        SynapseConstants.SynapseRuleReader.MEDIATOR)) {
                    mediation = childele;
                }
            }
            RuleBean ruleBean = new RuleBean();
            if (condition != null && mediation != null) {
                ruleBean.setCondition(condition.getText());
                ruleBean.setMediate(mediation.getText());
                ruleList.add(ruleBean);
            }
        }
        return true;
    }

    public Iterator getRulesIterator() {
        return ruleList.iterator();
    }

}
