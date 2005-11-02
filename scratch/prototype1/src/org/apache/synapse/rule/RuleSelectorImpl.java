package org.apache.synapse.rule;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAttribute;
import org.apache.synapse.rule.Rule;
import org.apache.synapse.rule.RuleSelector;
import org.apache.synapse.mediator.Mediator;
import org.apache.synapse.SynapseException;

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
    public void init(OMElement ruleSet) throws SynapseException {
        ArrayList ruleslist = new ArrayList();
        Iterator itsruls = ruleSet.getChildrenWithName(new QName("rule"));
        while (itsruls.hasNext()) {
            OMElement ruleElement = (OMElement) itsruls.next();
            OMAttribute attributeName = ruleElement.getAttribute(new QName("name"));
            if(attributeName != null){
                String rulename = attributeName.getAttributeValue();
                Rule rule = new Rule();
                Iterator mediatoes =  ruleElement.getChildrenWithName(new QName("mediator"));
                ArrayList mediatorList = new ArrayList();
                while (mediatoes.hasNext()) {
                    OMElement mediatorElement = (OMElement) mediatoes.next();
                    OMAttribute mediatorName = mediatorElement.getAttribute(new QName("name")); //what is the use of this
                    OMAttribute mediatorImplClass = mediatorElement.getAttribute(new QName("class"));

                    try {
                        Mediator mediator = (Mediator) Class.forName(mediatorImplClass.getAttributeValue()).newInstance();
                        mediatorList.add(mediator);
                    } catch (Exception e){
                        throw new SynapseException(e);
                    }

                    //todo: Deepal mediator has to create and add the rule
                }
                rule.setMediators((Mediator[])mediatorList.toArray(new Mediator[mediatorList.size()]));
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
