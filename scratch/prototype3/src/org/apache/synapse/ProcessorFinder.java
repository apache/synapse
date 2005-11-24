package org.apache.synapse;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.synapse.processors.AllProcessor;
import org.apache.synapse.processors.StageProcessor;
import org.apache.synapse.processors.SynapseProcessor;
import org.apache.synapse.processors.mediators.AddressingProcessor;
import org.apache.synapse.processors.mediators.ClassMediatorProcessor;
import org.apache.synapse.processors.mediators.FaultProcessor;
import org.apache.synapse.processors.mediators.HeaderProcessor;
import org.apache.synapse.processors.mediators.LogProcessor;
import org.apache.synapse.processors.mediators.SendProcessor;
import org.apache.synapse.processors.rules.RegexProcessor;
import org.apache.synapse.processors.rules.XPathProcessor;
import org.apache.synapse.spi.Processor;

/**
 * @author Paul Fremantle
 * 
 * This class is a temporary place holder for a dynamic model Probably will be
 * rebased on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ProcessorFinder {
	private static Map lookup = null;

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
