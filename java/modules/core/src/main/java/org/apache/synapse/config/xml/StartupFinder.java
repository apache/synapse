package org.apache.synapse.config.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Startup;

import sun.misc.Service;

public class StartupFinder {
	private static final Log log = LogFactory
			.getLog(ConfigurationFactoryAndSerializerFinder.class);

	private static StartupFinder instance = null;

	/**
	 * A map of mediator QNames to implementation class
	 */
	private static Map factoryMap = new HashMap(),
			serializerMap = new HashMap();

	public static synchronized StartupFinder getInstance() {
		if (instance == null) {
			instance = new StartupFinder();
		}
		return instance;
	}

	/**
	 * Force re initialization next time
	 */
	public synchronized void reset() {
		factoryMap.clear();
		instance = null;
	}

	private StartupFinder() {
		factoryMap = new HashMap();
		registerExtensions();
	}

	private void handleException(String msg, Exception e) {
		log.error(msg, e);
		throw new SynapseException(msg, e);
	}

	private void handleException(String msg) {
		log.error(msg);
		throw new SynapseException(msg);
	}

	/**
	 * Register pluggable mediator factories from the classpath
	 * 
	 * This looks for JAR files containing a META-INF/services that adheres to
	 * the following
	 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
	 */
	private void registerExtensions() {

		// log.debug("Registering mediator extensions found in the classpath : "
		// + System.getResource("java.class.path"));

		// register MediatorFactory extensions
		Iterator it = Service.providers(StartupFactory.class);
		while (it.hasNext()) {
			StartupFactory sf = (StartupFactory) it.next();
			QName tag = sf.getTagQName();
			factoryMap.put(tag, sf.getClass());
			serializerMap.put(tag, sf.getSerializerClass());
			if (log.isDebugEnabled()) {
				log.debug("Added StartupFactory " + sf.getClass()
						+ " to handle " + tag);
			}
		}
	}

	/**
	 * This method returns a Processor given an OMElement. This will be used
	 * recursively by the elements which contain processor elements themselves
	 * (e.g. rules)
	 * 
	 * @param element
	 * @return Processor
	 */
	public Startup getStartup(OMElement element) {

		String localName = element.getLocalName();
		QName qName = null;
		if (element.getNamespace() != null) {
			qName = new QName(element.getNamespace().getNamespaceURI(),
					localName);
		} else {
			qName = new QName(localName);
		}
		if (log.isDebugEnabled()) {
			log.debug("getStartup(" + qName + ")");
		}
		Class cls = (Class) factoryMap.get(qName);

		if (cls == null) {
			String msg = "Unknown Startup type referenced by startup element : "
					+ qName;
			log.error(msg);
			throw new SynapseException(msg);
		}

		try {
			StartupFactory sf = (StartupFactory) cls.newInstance();
			return sf.createStartup(element);

		} catch (InstantiationException e) {
			String msg = "Error initializing configuration factory : " + cls;
			log.error(msg);
			throw new SynapseException(msg, e);

		} catch (IllegalAccessException e) {
			String msg = "Error initializing configuration factory : " + cls;
			log.error(msg);
			throw new SynapseException(msg, e);
		}
	}

    /**
     * This method will serialize the config using the supplied QName (looking
	 * up the right class to do it)
     * 
     * @param parent - Parent OMElement to which the created element will be added if not null
     * @param startup - Startup to be serialized
     * @throws XMLStreamException if the serialization encounter an error
     */
    public void serializeStartup(OMElement parent, Startup startup) throws XMLStreamException {

		Class cls = (Class) serializerMap.get(startup.getTagQName());
		if (cls == null) {
			String msg = "Unknown startup type referenced by startup element : "
					+ startup.getTagQName();
			log.error(msg);
			throw new SynapseException(msg);
		}

		try {
			StartupSerializer ss = (StartupSerializer) cls.newInstance();
			ss.serializeStartup(parent,  startup);

		} catch (InstantiationException e) {
			String msg = "Error initializing startup serializer: " + cls;
			log.error(msg);
			throw new SynapseException(msg, e);

		} catch (IllegalAccessException e) {
			String msg = "Error initializing startup ser: " + cls;
			log.error(msg);
			throw new SynapseException(msg, e);
		}
	}

	/*
	 * This method exposes all the MediatorFactories and its Extensions
	 */
	public Map getFactoryMap() {
		return factoryMap;
	}

	/**
	 * Allow the mediator factory finder to act as an XMLToObjectMapper for
	 * Mediators (i.e. Sequence Mediator) loaded dynamically from a Registry
	 * 
	 * @param om
	 * @return
	 */
	public Object getObjectFromOMNode(OMNode om) {
		if (om instanceof OMElement) {
			return getStartup((OMElement) om);
		} else {
			handleException("Invalid configuration XML : " + om);
		}
		return null;
	}

}
