/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.xml.eventing.EventPublisherMediatorFactory;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 *
 *
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class MediatorFactoryFinder implements XMLToObjectMapper {

	private static final Log log = LogFactory.getLog(MediatorFactoryFinder.class);

	private static final Class[] mediatorFactories = {
        SequenceMediatorFactory.class,
        LogMediatorFactory.class,
        SendMediatorFactory.class,
        FilterMediatorFactory.class,
        SynapseMediatorFactory.class,
        DropMediatorFactory.class,
        HeaderMediatorFactory.class,
        FaultMediatorFactory.class,
        PropertyMediatorFactory.class,
        SwitchMediatorFactory.class,
        InMediatorFactory.class,
        OutMediatorFactory.class,
        ClassMediatorFactory.class,
        ValidateMediatorFactory.class,
        XSLTMediatorFactory.class,
        AnnotatedCommandMediatorFactory.class,
        POJOCommandMediatorFactory.class,
        CloneMediatorFactory.class,
        IterateMediatorFactory.class,
        AggregateMediatorFactory.class,
        DBReportMediatorFactory.class,
        DBLookupMediatorFactory.class,
        CacheMediatorFactory.class,
        CalloutMediatorFactory.class,
        EventPublisherMediatorFactory.class,
        TransactionMediatorFactory.class,
        EnqueueMediatorFactory.class,
        ConditionalRouterMediatorFactory.class,
        SamplingThrottleMediatorFactory.class,
        URLRewriteMediatorFactory.class,
        EnrichMediatorFactory.class,
        MessageStoreMediatorFactory.class,
        TemplateMediatorFactory.class,
        InvokeMediatorFactory.class,
        PayloadFactoryMediatorFactory.class,
        BeanMediatorFactory.class,
        EJBMediatorFactory.class,
        RespondMediatorFactory.class,
        LoopbackMediatorFactory.class
    };

    private final static MediatorFactoryFinder instance  = new MediatorFactoryFinder();

    /**
     * A map of mediator QNames to implementation class
     */
    private static Map<QName, Class> factoryMap = new HashMap<QName, Class>();

    private static boolean initialized = false;

    public static synchronized MediatorFactoryFinder getInstance() {
        if (!initialized) {
            loadMediatorFactories();
        }
        return instance;
    }

    /**
     * Force re initialization next time
     */
    public static synchronized void reset() {
        factoryMap.clear();
        initialized = false;
    }

    private MediatorFactoryFinder() {
    }

    private static void loadMediatorFactories() {
        for (Class c : mediatorFactories) {
            try {
                MediatorFactory fac = (MediatorFactory) c.newInstance();
                factoryMap.put(fac.getTagQName(), c);
            } catch (Exception e) {
                throw new SynapseException("Error instantiating " + c.getName(), e);
            }
        }
        // now iterate through the available pluggable mediator factories
        registerExtensions();
        initialized = true;
    }

    /**
     * Register pluggable mediator factories from the classpath
     *
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
     */
    private static void registerExtensions() {

        // register MediatorFactory extensions
        Iterator<MediatorFactory> factories = ServiceLoader.load(MediatorFactory.class).iterator();
        while (factories.hasNext()) {
            MediatorFactory factory = factories.next();
            QName tag = factory.getTagQName();
            factoryMap.put(tag, factory.getClass());
            if (log.isDebugEnabled()) {
                log.debug("Added MediatorFactory " + factory.getClass() + " to handle " + tag);
            }
        }
    }

    /**
	 * This method returns a Processor given an OMElement. This will be used
	 * recursively by the elements which contain processor elements themselves
	 * (e.g. rules)
	 *
	 * @param element XML representation of a mediator
     * @param properties bag of properties to pass in any information to the factory
     * @return Processor
	 */
	public Mediator getMediator(OMElement element, Properties properties) {

        String localName = element.getLocalName();
        QName qName;
        if (element.getNamespace() != null) {
            qName = new QName(element.getNamespace().getNamespaceURI(), localName);
        } else {
            qName = new QName(localName);
        }
        if (log.isDebugEnabled()) {
            log.debug("getMediator(" + qName + ")");
        }
        Class cls = factoryMap.get(qName);

        if (cls == null && localName.indexOf('.') > -1) {
            String newLocalName = localName.substring(0, localName.indexOf('.'));
            qName = new QName(element.getNamespace().getNamespaceURI(), newLocalName);
            if (log.isDebugEnabled()) {
                log.debug("getMediator.2(" + qName + ")");
            }
            cls = factoryMap.get(qName);
        }

        if (cls == null) {
            String msg = "Unknown mediator referenced by configuration element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
			MediatorFactory mf = (MediatorFactory) cls.newInstance();
			return mf.createMediator(element, properties);

        } catch (InstantiationException e) {
            String msg = "Error initializing mediator factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing mediator factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
		}
	}

    /**
     * This method exposes all the MediatorFactories and its Extensions
     * @return factoryMap
     */
    public Map<QName, Class> getFactoryMap() {
        return factoryMap;
    }

    /**
     * Allow the mediator factory finder to act as an XMLToObjectMapper for Mediators
     * (i.e. Sequence Mediator) loaded dynamically from a Registry
     * @param om node from which the object is expected
     * @return Object buit from the om node
     */
    public Object getObjectFromOMNode(OMNode om, Properties properties) {
        if (om instanceof OMElement) {
            return getMediator((OMElement) om, properties);
        } else {
            handleException("Invalid mediator configuration XML : " + om);
        }
        return null;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
