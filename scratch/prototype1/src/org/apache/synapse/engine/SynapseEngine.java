package org.apache.synapse.engine;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurationImpl;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMAttribute;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.synapse.SynapseException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;


public class SynapseEngine {
    private boolean proceed = true;

    private RuleSelector incomingPreStageRuleSelector;
    private RuleSelector incomingProcessingStageRuleSelector;
    private RuleSelector incomingPostStageRuleSelector;
    private RuleSelector outgoingPreStageRuleSelector;
    private RuleSelector outgoingProcessingStageRuleSelector;
    private RuleSelector outgoingPostStageRuleSelector;

    private AxisConfiguration axisConfig;

    /**
     *
     */
    public void init(SynapseConfiguration synapseConfiguration)
            throws SynapseException {
        axisConfig = new AxisConfigurationImpl();
        incomingPreStageRuleSelector = createRuleSelector(
                synapseConfiguration.getIncomingPreStageRuleSet());
        incomingProcessingStageRuleSelector = createRuleSelector(
                synapseConfiguration.getIncomingProcessingStageRuleSet());
        incomingPostStageRuleSelector = createRuleSelector(
                synapseConfiguration.getIncomingPostStageRuleSet());

        outgoingPreStageRuleSelector = createRuleSelector(
                synapseConfiguration.getOutgoingPreStageRuleSet());
        outgoingProcessingStageRuleSelector = createRuleSelector(
                synapseConfiguration.getOutgoingProcessingStageRuleSet());
        outgoingPostStageRuleSelector = createRuleSelector(
                synapseConfiguration.getOutgoingPostStageRuleSet());
    }

    public void processIncoming(MessageContext messageContext) {

        try {
            // first get the incoming IN stage Rule Engine, run them sequentially. You will get
            // the mediators as an ArrayList
            // before runninng a new mediator, check whether we should proceed
            // taking the result from the last mediation

            invokeMediators(messageContext, incomingPreStageRuleSelector);

            // get the mediator by mdeiator for the Processing phase and execute them
            // remember you have to call this method in the Rule Processor after each and
            // every mediation

            invokeProcessingStage(messageContext,
                    incomingProcessingStageRuleSelector);

            // Now get the incoming OUT stage Rule Engine, run them sequentially. You will get
            // the mediators as an ArrayList
            // before runninng a new mediator, check whether we should proceed
            // taking the result from the last mediation
            invokeMediators(messageContext, incomingPostStageRuleSelector);

            if (proceed) {
                TransportSender tSender = getSenderTransport(messageContext);
                tSender.invoke(messageContext);
            }


        } catch (SynapseException e) {
            // log your error
            // do whatever with this
            e.printStackTrace();
        } catch (AxisFault axisFault) {
            // log your error
            // do whatever with this
            axisFault.printStackTrace();
        }

    }

    /**
     * Find out the transport sender for me to send the message to the service
     * which this message was destined
     *
     * @param messageContext
     */
    private TransportSender getSenderTransport(MessageContext messageContext) {
        // TODO implement this so that the proper transport sender will be picked up and
        // initialized
        return new CommonsHTTPTransportSender();
    }

    public void processOutgoing(MessageContext messageContext) {

        try {
            // first get the Out going IN stage Rule Engine, run them sequentially. You will get
            // the mediators as an ArrayList
            // before runninng a new mediator, check whether we should proceed
            // taking the result from the last mediation

            invokeMediators(messageContext, outgoingPreStageRuleSelector);

            // get the mediator by mediator for the Processing phase and execute them
            // remember you have to call this method in the Rule Processor after each and
            // every mediation

            invokeProcessingStage(messageContext,
                    outgoingProcessingStageRuleSelector);

            // Now get the out going OUT stage Rule Engine, run them sequentially. You will get
            // the mediators as an ArrayList
            // before runninng a new mediator, check whether we should proceed
            // taking the result from the last mediation
            invokeMediators(messageContext, outgoingPostStageRuleSelector);

            // now send the message to the service which it was destined
            if (proceed) {
                TransportSender tSender = getSenderTransport(messageContext);
                tSender.invoke(messageContext);
            }


        } catch (SynapseException e) {
            // log your error
            // do whatever with this
            e.printStackTrace();
        } catch (AxisFault axisFault) {
            // log your error
            // do whatever with this
            axisFault.printStackTrace();
        }
    }

    private void invokeMediators(MessageContext messageContext,
                                 RuleSelector ruleSelector)
            throws SynapseException {
        Rule rule;
        Iterator inStageRulesIter = ruleSelector.match(messageContext);
        while (inStageRulesIter.hasNext() && proceed) {
            rule = (Rule) inStageRulesIter.next();
            proceed = RuleExecutor.execute(rule, messageContext);
        }
    }

    private RuleSelector createRuleSelector(
            OMElement ruleSet)
            throws
            SynapseException {
        OMAttribute classAttribute = ruleSet.getAttribute(new QName("class"));
        if (classAttribute == null) {
            throw new SynapseException("RuleSelector not found");
        }
        RuleSelector ruleSelector = null;
        try {
            String ruleClass = classAttribute.getAttributeValue();
            ruleSelector = (RuleSelector) Class.forName(ruleClass)
                    .newInstance();
            ruleSelector.init(ruleSet);


            Rule []   rules = ruleSelector.getRules();
            for (int i = 0; i < rules.length; i++) {
                Rule rule = rules[i];
                AxisService service = new AxisService(new QName(rule.getName()));
                List qoslist = rule.getQosModules();
                for (int j = 0; j < qoslist.size(); j++) {
                    String moduleName = (String) qoslist.get(j);
                    //todo engage all the mdoule to the service
                }
                axisConfig.addService(service);
//                rule.setAxisConfig();
            }
            return ruleSelector;
        } catch (Exception e) {
            throw new SynapseException(e);
        }
    }


    private void invokeProcessingStage(MessageContext messageContext,
                                       RuleSelector ruleSelector) throws
            SynapseException {
        Rule rule;
        while (proceed && (rule = ruleSelector
                .getBestMatch(messageContext)) != null) {
            proceed = RuleExecutor.execute(rule, messageContext);
        }
    }


}
