package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SpringConfiguration;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.ext.spring.SpringMediator;

import javax.xml.namespace.QName;

/**
 * Creates an instance of a Spring mediator that refers to the given Spring
 * configuration and bean.
 * <p/>
 * <spring ref_bean="exampleBean1" (config_name="spring1" | config_src="spring.xml)"/>
 */
public class SpringMediatorFactory implements MediatorFactory {

    private static final Log log = LogFactory.getLog(SpringMediatorFactory.class);

    private static final QName tagName = new QName(Constants.SYNAPSE_NAMESPACE, "spring");

    /**
     * Create a Spring mediator instance referring to the bean and configuration given
     * by the OMElement declaration
     *
     * @param elem the OMElement that specifies the Spring mediator configuration
     * @return the Spring mediator instance created
     */
    public Mediator createMediator(OMElement elem) {

        SpringMediator sm = new SpringMediator();
        OMAttribute ref = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "ref_bean"));
        OMAttribute cfg = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "config_name"));
        OMAttribute src = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "config_src"));

        if (ref == null) {
            handleException("The 'ref_bean' attribute is required for a Spring mediator definition");
        } else if (cfg == null && src == null) {
            handleException("A 'config_name' or 'config_src' attribute is required for a Spring mediator definition");

        } else {
            sm.setBeanName(ref.getAttributeValue());
            if (cfg != null) {
                sm.setConfigName(cfg.getAttributeValue());

            } else {
                log.debug("Creating an anonymous Spring configuration using source : " + src.getAttributeValue());
                SpringConfiguration sc = new SpringConfiguration("anonymous", src.getAttributeValue());
                sm.setAppContext(sc.getAppContext());
            }
            return sm;
        }
        return null;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return tagName;
    }

}
