package org.apache.synapse.rules;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 11, 2005
 * Time: 4:42:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class RuleBean {
    private String condition; // this is the xpath of the messagte
    private String mediator; // is the mediator we need at this point 

    public void setCondition(String condition) {
        this.condition = condition;
    }
    public void setMediate(String mediatation) {
        this.mediator = mediatation;
    }

    public String getCondition(){
        return condition;
    }
    public String getMediator() {
        return mediator;
    }

}
