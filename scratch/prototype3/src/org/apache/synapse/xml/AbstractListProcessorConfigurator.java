package org.apache.synapse.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.processors.ListProcessor;

public abstract class AbstractListProcessorConfigurator extends AbstractProcessorConfigurator {

	Log log = LogFactory.getLog(getClass());
	
	public void compile(SynapseEnvironment se, OMElement el, ListProcessor p)
	{
		super.compile(se, el, p);

		Iterator it = el.getChildElements();
		List processors = new LinkedList();
		while (it.hasNext()) {
			OMElement child = (OMElement) it.next();
			Processor proc = Configurator.getProcessor(se, child);
			if (proc != null)
				processors.add(proc);
			else
				log.info("Unknown child of all" + child.getLocalName());
		}
		p.setList(processors);
		
	}

	public abstract QName getTagQName();

	public abstract Processor compile(SynapseEnvironment se, OMElement el);
		

}
