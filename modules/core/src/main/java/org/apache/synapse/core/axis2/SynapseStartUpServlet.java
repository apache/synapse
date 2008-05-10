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
package org.apache.synapse.core.axis2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.ServerManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * When Synapse is deployed on a WAR container, this is the init servlet that kicks off the
 * Synapse instance, calling on the ServerManager
 */

public class SynapseStartUpServlet extends HttpServlet {

    private static Log log = LogFactory.getLog(SynapseStartUpServlet.class);
    private ServletContext servletContext = null;
    private static final String ALREADY_INITED = "synapseAlreadyInited";

    public void init() throws ServletException {
        super.init();
    }

    public void init(ServletConfig servletConfig) throws ServletException {

        servletContext = servletConfig.getServletContext();
        if (Boolean.TRUE.equals(servletContext.getAttribute(ALREADY_INITED))) {
            return;
        }

        ServerManager serverManager = ServerManager.getInstance();
        serverManager.setSynapseHome(loadParameter(servletConfig, SynapseConstants.SYNAPSE_HOME));
        serverManager.setSynapseXMLPath(loadParameter(servletConfig, SynapseConstants.SYNAPSE_XML));
        serverManager.setResolveRoot(loadParameter(servletConfig, SynapseConstants.RESOLVE_ROOT));
        serverManager.setAxis2Repolocation(loadParameter(servletConfig, org.apache.axis2.Constants.AXIS2_REPO));
        serverManager.setAxis2Xml(loadParameter(servletConfig, org.apache.axis2.Constants.AXIS2_CONF));

        serverManager.start();
        servletContext.setAttribute(ALREADY_INITED, Boolean.TRUE);
    }


    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
    }

    public void destroy() {
        try {
            ServerManager serverManager = ServerManager.getInstance();
            serverManager.stop();
            servletContext.removeAttribute(ALREADY_INITED);
        } catch (Exception e) {
            log.error("Error stopping the Synapse listener manager", e);
        }
    }

    private String loadParameter(ServletConfig servletConfig, String name)
        throws ServletException {

        if (System.getProperty(name) == null) {

            String value = servletConfig.getInitParameter(name);
            log.debug("Init parameter '" + name + "' : " + value);

            if (value == null || value.trim().length() == 0) {
                handleException("A valid system property or init parameter '" + name + "' is required");
            } else {
                return value;
            }
        } else {
            return System.getProperty(name);
        }
        return null;
    }

    private void handleException(String message) throws ServletException {
        log.error(message);
        log(message);
        throw new ServletException(message);
    }
}
