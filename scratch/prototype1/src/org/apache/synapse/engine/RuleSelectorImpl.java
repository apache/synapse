package org.apache.synapse.engine;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAttribute;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.ArrayList;

public class RuleSelectorImpl implements RuleSelector {

    private  Rule rules [];


    /**
     * This will give a chance for the rules processor to compile or do whatever
     * stuff he needs to optimize rules processing.
     * Normally the user who inits the RuleSelector should pass the rules relevant to
     * a particular stage for a given direction
     * In other words, RuleSelector should be created per stage per direction
     */
    public void init(OMElement ruleSet) {
        ArrayList ruleslist = new ArrayList();
        Iterator itsruls = ruleSet.getChildrenWithName(new QName("rule"));
        while (itsruls.hasNext()) {
            OMElement ruleelement = (OMElement) itsruls.next();
            OMAttribute attributeName = ruleelement.getAttribute(new QName("name"));
            if(attributeName != null){
                String rulename = attributeName.getAttributeValue();
                Rule rule = new Rule();
                Iterator mediatoes =  ruleelement.getChildrenWithName(new QName("mediator"));
                while (mediatoes.hasNext()) {
                    OMElement mediatorElement = (OMElement) mediatoes.next();
                    OMAttribute mediatorName = mediatorElement.getAttribute(new QName("name"));
                    //todo: Deepal mediator has to create and add the rule
                }
                rule.setName(rulename);
                ruleslist.add(rule);
            }
        }
        rules =   (Rule[])ruleslist.toArray(new Rule[ruleslist.size()]);
    }

    public Rule [] getRules() {
        return rules;
    }

    /**
     * This will give a chance for the rules processor to compile or do whatever
     * stuff he needs to optimize rules processing.
     * Normally the user who passes the rules should pass the rules relevant to
     * a particular stage for a given direction.
     */


    public Iterator match(MessageContext messageContext) {
        return null;
    }

    public Rule getBestMatch(MessageContext messageContext) {
        return null;
    }
}
