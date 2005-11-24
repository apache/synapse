package org.apache.synapse.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.AllProcessor;
import org.apache.synapse.processors.StageProcessor;
import org.apache.synapse.processors.SynapseProcessor;

import org.apache.synapse.processors.builtin.FaultProcessor;
import org.apache.synapse.processors.builtin.HeaderProcessor;
import org.apache.synapse.processors.builtin.LogProcessor;
import org.apache.synapse.processors.builtin.SendProcessor;
import org.apache.synapse.processors.builtin.axis2.AddressingProcessor;
import org.apache.synapse.processors.mediatortypes.ClassMediatorProcessor;
import org.apache.synapse.processors.rules.RegexProcessor;
import org.apache.synapse.processors.rules.XPathProcessor;

import sun.misc.Service;

/**
 * @author Paul Fremantle
 * 
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ProcessorFinder {
	
	private static Map lookup = null;
	private static Log log = LogFactory.getLog(ProcessorFinder.class);
	private static Class[] processors = { SynapseProcessor.class,
			AllProcessor.class, AddressingProcessor.class,
			ClassMediatorProcessor.class, HeaderProcessor.class,
			LogProcessor.class, SendProcessor.class, XPathProcessor.class,
			RegexProcessor.class, StageProcessor.class, FaultProcessor.class };

	private static void config() {
		
		if (lookup != null)
			return;
		lookup = new HashMap();

		for (int i = 0; i < processors.length; i++) {
			Class c = processors[i];
			try {
				lookup.put(((Processor) c.newInstance()).getTagQName(), c);
			} catch (Exception e) {
				throw new SynapseException(e);
			}
		}
		// now try additional processors
		Iterator it = Service.providers(Processor.class);
		while (it.hasNext()) {
			Processor p = (Processor)it.next();
			QName tag = p.getTagQName();
			lookup.put(tag, p.getClass());
			log.debug("added Processor "+p.getClass()+" to handle "+tag);
		}
	}

	/**
	 * @param QName
	 * @return the class which implements the Processor for the given QName
	 */
	public static Class find(QName qn) {
		config();
		return (Class) lookup.get(qn);
	}
}
