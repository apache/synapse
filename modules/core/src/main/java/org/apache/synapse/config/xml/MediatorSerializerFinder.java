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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.ValidateMediatorSerializer;
import org.apache.synapse.config.xml.XSLTMediatorSerializer;
import sun.misc.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MediatorSerializerFinder {

    private static final Log log = LogFactory.getLog(MediatorSerializerFinder.class);

    private static final Class[] mediatorSerializers = {
        SequenceMediatorSerializer.class,
        LogMediatorSerializer.class,
        SendMediatorSerializer.class,
        FilterMediatorSerializer.class,
        SynapseMediatorSerializer.class,
        DropMediatorSerializer.class,
        HeaderMediatorSerializer.class,
        FaultMediatorSerializer.class,
        PropertyMediatorSerializer.class,
        SwitchMediatorSerializer.class,
        InMediatorSerializer.class,
        OutMediatorSerializer.class,
        RMSequenceMediatorSerializer.class,     
        ClassMediatorSerializer.class,
        ValidateMediatorSerializer.class,
        XSLTMediatorSerializer.class        
    };

    private static MediatorSerializerFinder instance = null;

    /**
     * A map of mediator QNames to implementation class
     */
    private static Map serializerMap = new HashMap();

    public static synchronized MediatorSerializerFinder getInstance() {
        if (instance == null) {
            instance = new MediatorSerializerFinder();
        }
        return instance;
    }

    public MediatorSerializer getSerializer(Mediator mediator) {
        return (MediatorSerializer) serializerMap.get(mediator.getClass().getName());
    }

    private MediatorSerializerFinder() {

        serializerMap = new HashMap();

        for (int i = 0; i < mediatorSerializers.length; i++) {
            Class c = mediatorSerializers[i];
            try {
                MediatorSerializer ser = (MediatorSerializer) c.newInstance();
                serializerMap.put(ser.getMediatorClassName(), ser);
            } catch (Exception e) {
                throw new SynapseException("Error instantiating " + c.getName(), e);
            }
        }
        // now iterate through the available pluggable mediator factories
        registerExtensions();
    }

    /**
     * Register pluggable mediator serializers from the classpath
     *
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
     */
    private void registerExtensions() {

        log.debug("Registering mediator extensions found in the classpath : " + System.getProperty("java.class.path"));

        // register MediatorSerializer extensions
        Iterator it = Service.providers(MediatorSerializer.class);
        while (it.hasNext()) {
            MediatorSerializer ms = (MediatorSerializer) it.next();
            String name = ms.getMediatorClassName();
            try {
                serializerMap.put(name, ms.getClass().newInstance());
            } catch (InstantiationException e) {
                handleException("Error instantiating mediator serializer : " + ms);
            } catch (IllegalAccessException e) {
                handleException("Error instantiating mediator serializer : " + ms);
            }
            log.debug("Added MediatorSerializer " + ms.getClass().getName() + " to handle " + name);
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
