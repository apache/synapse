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
package org.apache.synapse.format;

import javax.activation.DataSource;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;

/**
 * Message formatter with extended capabilities.
 * This interface adds new methods to the {@link MessageFormatter}
 * interface, allowing transport to optimize data transfers.
 */
public interface MessageFormatterEx extends MessageFormatter {
    /**
     * Get the formatted message as a {@link DataSource} object.
     * 
     * @param messageContext
     * @param format
     * @param soapAction
     * @return
     * @throws AxisFault
     */
    DataSource getDataSource(MessageContext messageContext, OMOutputFormat format, String soapAction) throws AxisFault;
}
