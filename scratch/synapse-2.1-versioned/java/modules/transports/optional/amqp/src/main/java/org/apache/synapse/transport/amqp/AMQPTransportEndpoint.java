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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.ProtocolEndpoint;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactory;
import org.apache.synapse.transport.amqp.pollingtask.AMQPTransportPollingTask;
import org.apache.synapse.transport.amqp.pollingtask.AMQPTransportPollingTaskFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Represent and endpoint in AMQP transport implementation.
 */
public class AMQPTransportEndpoint extends ProtocolEndpoint {

    private Set<EndpointReference> endpointReferences = new HashSet<EndpointReference>();

    private ScheduledExecutorService workerPool;

    private AMQPTransportPollingTask pollingTask;

    private AMQPTransportListener transportReceiver;

    public AMQPTransportEndpoint(ScheduledExecutorService workerPool,
                                 AMQPTransportListener receiver) {
        this.workerPool = workerPool;
        this.transportReceiver = receiver;
    }

    public AMQPTransportPollingTask getPollingTask() {
        return pollingTask;
    }

    @Override
    public boolean loadConfiguration(ParameterInclude params) throws AxisFault {
        if (!(params instanceof AxisService)) {
            return false;
        }
        try {
            AxisService service = (AxisService) params;

            String conFacName;
            if (service.getParameter(AMQPTransportConstant.PARAMETER_CONNECTION_FACTORY_NAME) != null) {
                conFacName = (String) service.getParameter(
                        AMQPTransportConstant.PARAMETER_CONNECTION_FACTORY_NAME).getValue();

            } else {
                conFacName = AMQPTransportConstant.DEFAULT_CONNECTION_FACTORY_NAME;
            }

            AMQPTransportConnectionFactory conFac =
                    transportReceiver.getConnectionFactory(conFacName);
            if (conFac == null) {
                throw new AxisFault("No connection factory definition found");
            }

            pollingTask = AMQPTransportPollingTaskFactory.createPollingTaskForService(
                    service,
                    workerPool,
                    this,
                    conFac,
                    transportReceiver.getHaHandler());

        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not load the AMQP endpoint configuration, " + e.getMessage(), e);
        }
        return true;
    }

    @Override
    public EndpointReference[] getEndpointReferences(AxisService service, String ip)
            throws AxisFault {
        return endpointReferences.toArray(new EndpointReference[endpointReferences.size()]);
    }
}