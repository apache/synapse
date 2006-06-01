package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.config.SpringConfigExtension;
import org.apache.synapse.mediators.ext.spring.SpringMediator;

import javax.xml.namespace.QName;

/**
 * Creates an instance of a Spring mediator that refers to the given Spring
 * configuration and bean. Optionally, one could specify an inlined Spring
 * configuration as opposed to a globally defined Spring configuration
 * <p/>
 * <spring bean="exampleBean1" (config="spring1" | src="spring.xml)"/>
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
        OMAttribute bean = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "bean"));
        OMAttribute cfg = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "config"));
        OMAttribute src = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "src"));

        if (bean == null) {
            handleException("The 'bean' attribute is required for a Spring mediator definition");
        } else if (cfg == null && src == null) {
            handleException("A 'config' or 'src' attribute is required for a Spring mediator definition");

        } else {
            sm.setBeanName(bean.getAttributeValue());
            if (cfg != null) {
                log.debug("Creating a Spring mediator using configuration named : " + cfg.getAttributeValue());
                sm.setConfigName(cfg.getAttributeValue());

            } else {
                log.debug("Creating an inline Spring configuration using source : " + src.getAttributeValue());
                SpringConfigExtension sce = new SpringConfigExtension("inline", src.getAttributeValue());
                sm.setAppContext(sce.getAppContext());
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
