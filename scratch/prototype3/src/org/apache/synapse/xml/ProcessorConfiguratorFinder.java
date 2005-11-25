package org.apache.synapse.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.SynapseException;

import sun.misc.Service;

/**
 * @author Paul Fremantle
 * 
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class ProcessorConfiguratorFinder {

	private static Map lookup = null;

	private static Log log = LogFactory
			.getLog(ProcessorConfiguratorFinder.class);

	private static Class[] processorConfigurators = {
			SynapseProcessorConfigurator.class,
			StageProcessorConfigurator.class, RegexProcessorConfigurator.class,
			XPathProcessorConfigurator.class,
			HeaderProcessorConfigurator.class,
			ClassMediatorProcessorConfigurator.class,
			ServiceMediatorProcessorConfigurator.class,
			LogProcessorConfigurator.class, SendProcessorConfigurator.class,
			FaultProcessorConfigurator.class, AddressingProcessorConfigurator.class };

	private static void initialise() {

		if (lookup != null)
			return;
		lookup = new HashMap();

		for (int i = 0; i < processorConfigurators.length; i++) {
			Class c = processorConfigurators[i];
			try {
				lookup.put(((ProcessorConfigurator) c.newInstance())
						.getTagQName(), c);
			} catch (Exception e) {
				throw new SynapseException(e);
			}
		}
		// now try additional processors
		Iterator it = Service.providers(ProcessorConfigurator.class);
		while (it.hasNext()) {
			ProcessorConfigurator p = (ProcessorConfigurator) it.next();
			QName tag = p.getTagQName();
			lookup.put(tag, p.getClass());
			log.debug("added Processor " + p.getClass() + " to handle " + tag);
		}
	}

	/**
	 * @param QName
	 * @return the class which implements the Processor for the given QName
	 */
	public static Class find(QName qn) {
		initialise();
		return (Class) lookup.get(qn);
	}
}
