package org.apache.synapse.processors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.xml.Configurator;


/**
 * @author Paul Fremantle This class simply runs the message through all rules,
 *         stages, mediations that it has as subelements It is a way of grouping
 *         stuff.
 * 
 */
public class AllProcessor extends AbstractProcessor {
	public static final QName tag = new QName(Constants.SYNAPSE_NAMESPACE,
			"all");

	private Log log = LogFactory.getLog(getClass());

	List processors = null;

	public void compile(SynapseEnvironment se, OMElement el) {
		super.compile(se, el);

		// no special children
		Iterator it = el.getChildElements();
		processors = new LinkedList();
		while (it.hasNext()) {
			OMElement child = (OMElement) it.next();
			Processor p = Configurator.getProcessor(se, child);
			if (p != null)
				processors.add(p);
			else
				log.info("Unknown child of all" + child.getLocalName());
		}

	}

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {
		if (processors == null) {
			log.info("process called on empty processor list");
			return true;
		}
		Iterator it = processors.iterator();
		while (it.hasNext()) {
			Processor p = (Processor) it.next();
			if (!p.process(se,smc))
				return false;
		}
		return true;
	}

	public QName getTagQName() {
		return tag;
	}

}
