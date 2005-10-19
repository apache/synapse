package org.apache.synapse.rules;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseConstants;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:12:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleReader {
    public static final String RULE_XML = "synapse.xml";
    private ArrayList ruleList;

    private HashMap namespaceMap;

    public SynapseRuleReader() {
        ruleList = new ArrayList();
        namespaceMap = new HashMap();
    }

    public OMElement readRules()
            throws AxisFault {

        try {
            File absolutePath = new File(".");
            File inFile = new File(absolutePath.getAbsolutePath(), RULE_XML);
            InputStream in = new FileInputStream(inFile);
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
        } catch (FileNotFoundException e) {
            throw new AxisFault(e);
        }
    }

    public void populateRules() throws
            AxisFault {

        OMElement rules = readRules();
        if (rules != null) {
            this.fillBean(rules);
            this.fillNamespaces(rules);
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
            SynapaseRuleBean ruleBean = new SynapaseRuleBean();
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

    public HashMap getNamespaceMap() {
        return namespaceMap;
    }

    private void fillNamespaces(OMElement ele) {
        Iterator ite = ele.getChildElements();
        while (ite.hasNext()) {
            OMElement ruleEle = (OMElement) ite.next();
            Iterator nsIte = ruleEle.getChildElements();
            while (nsIte.hasNext()) {
                OMElement nsEle = (OMElement) nsIte.next();
                if (nsEle.getLocalName().equalsIgnoreCase(
                        SynapseConstants.SynapseRuleReader.NAMESPACE)) {
                    Iterator attIte = nsEle.getAllAttributes();
                    String prifix = null;
                    String uri = null;
                    while (attIte.hasNext()) {
                        OMAttribute att = (OMAttribute) attIte.next();
                        if (att.getLocalName().equalsIgnoreCase(
                                SynapseConstants.SynapseRuleReader.PRIFIX)) {
                            prifix = att.getValue();
                        }
                        if (att.getLocalName().equalsIgnoreCase(
                                SynapseConstants.SynapseRuleReader.URI)) {
                            uri = att.getValue();
                        }

                    }
                    namespaceMap.put(prifix, uri);
                }
            }
        }
    }

}
