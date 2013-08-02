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
package org.apache.synapse.transport.amqp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.base.AbstractTransportListenerEx;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactory;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactoryManager;
import org.apache.synapse.transport.amqp.ha.AMQPTransportReconnectHandler;
import org.apache.synapse.transport.amqp.pollingtask.AMQPTransportPollingTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The AMQP transport receiver implementation.
 */
public class AMQPTransportListener extends AbstractTransportListenerEx<AMQPTransportEndpoint> {

    /**
     * The worker pool for polling tasks
     */
    private ScheduledExecutorService workerPool;

    /**
     * The connection factories defined in axis2.xml for transport receiver section.
     */
    private AMQPTransportConnectionFactoryManager connectionFactoryManager;

    private ExecutorService connectionFactoryES;

    private AMQPTransportReconnectHandler haHandlerTask;

    @Override
    protected void doInit() throws AxisFault {

        // pass a custom executor service instance into the AMQP connection factory for better
        // control see http://www.rabbitmq.com/api-guide.html
        connectionFactoryES = Executors.newFixedThreadPool(
                AMQPTransportUtils.getIntProperty(
                        AMQPTransportConstant.PARAM_CONNECTION_FACTORY_POOL_SIZE,
                        AMQPTransportConstant.CONNECTION_FACTORY_POOL_DEFAULT));

        connectionFactoryManager = new AMQPTransportConnectionFactoryManager();
        connectionFactoryManager.addConnectionFactories(
                getTransportInDescription(), connectionFactoryES);

        workerPool = Executors.newScheduledThreadPool(
                AMQPTransportUtils.getIntProperty(AMQPTransportConstant.PARAM_WORKER_POOL_SIZE,
                        AMQPTransportConstant.WORKER_POOL_DEFAULT));


        int initialReconnectDuration = AMQPTransportUtils.getIntProperty(
                AMQPTransportConstant.PARAM_INITIAL_RE_CONNECTION_DURATION, 1000);

        double reconnectionProgressionFactor = AMQPTransportUtils.getDoubleProperty(
                AMQPTransportConstant.PARAM_RE_CONNECTION_PROGRESSION_FACTOR, 2.0);

        int maxReconnectionDuration = AMQPTransportUtils.getIntProperty(
                AMQPTransportConstant.PARAM_MAX_RE_CONNECTION_DURATION, 1000 * 60 * 10);

        haHandlerTask = new AMQPTransportReconnectHandler(
                connectionFactoryES,
                maxReconnectionDuration,
                reconnectionProgressionFactor,
                initialReconnectDuration,
                connectionFactoryManager);

        new Thread(haHandlerTask, "AMQP-HA-handler-task").start();

        log.info("AMQP transport listener initializing..");
    }

    @Override
    protected AMQPTransportEndpoint createEndpoint() {
        return new AMQPTransportEndpoint(workerPool, this);
    }

    @Override
    protected void startEndpoint(AMQPTransportEndpoint endpoint) throws AxisFault {
        AMQPTransportPollingTask ptm = endpoint.getPollingTask();
        try {
            ptm.start();
        } catch (AMQPTransportException e) {
            throw new AxisFault(e.getMessage(), e);
        }

        log.info("AMQP transport polling task started listen for service '" +
                ptm.getServiceName() + "'");
    }

    @Override
    protected void stopEndpoint(AMQPTransportEndpoint endpoint) {
        AMQPTransportPollingTask ptm = endpoint.getPollingTask();
        ptm.stop();

        log.info("AMQP transport polling task stopped listen for service '" +
                ptm.getServiceName() + "'");
    }

    @Override
    public void stop() throws AxisFault {
        super.stop();
        workerPool.shutdown();
        try {
            connectionFactoryManager.shutDownConnectionFactories();
        } catch (AMQPTransportException e) {
            log.error("Error while shutting down connection factories, continue anyway...", e);
        }
        connectionFactoryES.shutdown();
    }

    /**
     * Returns the connection factory with this name.
     *
     * @param name Name of the connection factory.
     * @return The connection factory with this name.
     * @throws AMQPTransportException throws in case of an error.
     */
    public AMQPTransportConnectionFactory getConnectionFactory(final String name)
            throws AMQPTransportException {
        if (connectionFactoryManager.getConnectionFactory(name) != null) {
            return connectionFactoryManager.getConnectionFactory(name);
        }
        return connectionFactoryManager.getConnectionFactory(
                AMQPTransportConstant.DEFAULT_CONNECTION_FACTORY_NAME);
    }

    public AMQPTransportReconnectHandler getHaHandler(){
        return haHandlerTask;
    }
}
