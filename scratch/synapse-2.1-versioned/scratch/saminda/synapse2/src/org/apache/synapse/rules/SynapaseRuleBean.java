package org.apache.synapse.rules;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapaseRuleBean {
    private String condition; // this is the xpath of the messagte
    private String mediator; // is the mediator we need at this point

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setMediate(String mediatation) {
        this.mediator = mediatation;
    }

    public String getCondition() {
        return condition;
    }

    public String getMediator() {
        return mediator;
    }

}
