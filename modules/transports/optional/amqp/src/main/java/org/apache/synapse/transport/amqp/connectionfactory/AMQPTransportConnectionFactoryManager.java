/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.transport.amqp.connectionfactory;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.AMQPTransportException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Holds a list of {@link AMQPTransportConnectionFactory}.
 */
public class AMQPTransportConnectionFactoryManager {

    private static Log log = LogFactory.getLog(AMQPTransportConnectionFactoryManager.class);

    /**
     * Keeps the list of connection factories defined.
     */
    private ConcurrentHashMap<String, AMQPTransportConnectionFactory> factories =
            new ConcurrentHashMap<String, AMQPTransportConnectionFactory>();

    /**
     * Add the list of defined connection factories definition.
     *
     * @param transportInDescription The connection factory definition in axis2.xml
     * @param es                     An instance of java.util.concurrent.ExecutorService to use with AMQP connection
     *                               factory
     */
    public void addConnectionFactories(ParameterInclude transportInDescription, ExecutorService es) {
        for (Parameter p : transportInDescription.getParameters()) {
            try {
                addConnectionFactory(p, es);
            } catch (AMQPTransportException e) {
                log.error("Error whiling adding the connection factory with name '" + p.getName() +
                        "'. ", e);
            }
        }
    }

    /**
     * Add a connection factory definition into store.
     */
    public void addConnectionFactory(Parameter parameter, ExecutorService es)
            throws AMQPTransportException {
        factories.put(parameter.getName(), new AMQPTransportConnectionFactory(parameter, es));
    }

    public void addConnectionFactory(String name, AMQPTransportConnectionFactory cf) {
        factories.put(name, cf);
    }

    /**
     * Get the connection factory with this name.
     *
     * @param name connection factory name.
     * @return the connection factory with this name.
     * @throws AMQPTransportException throws in case of an error.
     */
    public AMQPTransportConnectionFactory getConnectionFactory(final String name)
            throws AMQPTransportException {
        if (factories.containsKey(name)) {
            return factories.get(name);
        }
        throw new AMQPTransportException("No connection factory found with the name '" + name + "'");
    }

    /**
     * Remove and return the connection factory with this name.
     *
     * @param name connection factory name.
     * @throws AMQPTransportException throws in case of an error.
     */
    public void removeConnectionFactory(final String name) throws AMQPTransportException {
        if (factories.containsKey(name)) {
            try {
                // shutdown and remove
                AMQPTransportConnectionFactory factory = factories.remove(name);
                factory.shutDownChannel();
                factory.shutDownConnection();
            } catch (IOException e) {
                throw new AMQPTransportException("Could not remove the connection '" + name + "'", e);
            }
        } else {
            throw new AMQPTransportException("No connection factory found with the name '"
                    + name + "'");
        }
    }

    /**
     * Shutdown the open connections to the broker via the connection factory
     */
    public void shutDownConnectionFactories() throws AMQPTransportException {
        try {
            for (Map.Entry<String, AMQPTransportConnectionFactory> entry : factories.entrySet()) {
                AMQPTransportConnectionFactory connectionFactory = entry.getValue();
                connectionFactory.shutDownChannel();
                connectionFactory.shutDownConnection();
            }
        } catch (IOException e) {
            throw new AMQPTransportException("Error occurred whiling shutting down connections", e);
        }
    }

    public ConcurrentHashMap<String, AMQPTransportConnectionFactory> getAllFactories() {
        return factories;
    }

}
