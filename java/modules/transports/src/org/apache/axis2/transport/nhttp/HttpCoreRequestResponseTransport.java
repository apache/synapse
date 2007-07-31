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

package org.apache.axis2.transport.nhttp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.RequestResponseTransport;

/**
 * This interface is a point of control for Axis2 (and Sandesha2 in particular) to control
 * the behaviour of a Request-Response transport such as HTTP/s
 *
 * For nhttp, this does not make much of a difference, as we are capable of keeping a socket open
 * and writing to it from a different thread, while letting the initial thread that read the request
 * go free. However, it seems like Sandesha2 is looking for this interface, and it is not going to
 * create much of an issue anyway
 */
public class HttpCoreRequestResponseTransport implements RequestResponseTransport {

    private static final Log log = LogFactory.getLog(HttpCoreRequestResponseTransport.class);
    private RequestResponseTransportStatus status = RequestResponseTransportStatus.INITIAL;
    private MessageContext msgContext = null;

    HttpCoreRequestResponseTransport(MessageContext msgContext) {
        this.msgContext = msgContext;
    }

    public void acknowledgeMessage(MessageContext msgContext) throws AxisFault {
        log.debug("Acking one-way request");
    }

    public void awaitResponse() throws InterruptedException, AxisFault {
        log.debug("Returning thread but keeping socket open -- awaiting response");
        status = RequestResponseTransportStatus.WAITING;
        msgContext.getOperationContext().setProperty(Constants.RESPONSE_WRITTEN, "SKIP");
    }

    public void signalResponseReady() {
        log.debug("Signal response available");
        status = RequestResponseTransportStatus.SIGNALLED;
    }

    public RequestResponseTransportStatus getStatus() {
        return status;
    }

    public void signalFaultReady(AxisFault fault) {
    }
}
