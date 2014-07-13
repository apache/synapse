/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.mediators.deprecation;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.management.MediatorExecInfoObject;
import org.apache.synapse.management.impl.ManagementInformationExchange;
import org.apache.synapse.synapseobject.SynapseObject;
import org.apache.synapse.synapseobject.Utils;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class DeprecationMediator implements Mediator, EnvironmentAware {

    private ClassLoader classLoader;
    private SynapseEnvironment synapseEnvironment;
    SynapseObject deprecation;
    private Log log = LogFactory.getLog(getClass());
    private static long modifiedTime;

    public DeprecationMediator() {
    }

    public boolean mediate(SynapseMessage synapseMessageContext) {

        try {
            long start = System.currentTimeMillis();
            ManagementInformationExchange mix = new ManagementInformationExchange();
            MediatorExecInfoObject meio = new MediatorExecInfoObject();
            meio.setMediatorName("DEPRECATION");
            log.info("DEPRECATION MEDIATION");

            loadConfig(synapseMessageContext);

            SynapseObject contract = deprecation.findSynapseObjectByAttributeValue(synapseMessageContext.getTo().getAddress());
            SynapseObject[] services = contract.getChildren();
            int len = services.length;
            boolean deprecated = false;
            for (int i = 0; i < len; i++) {

                if (isDeprecated(services[i])) {

                    deprecated = true;
                }

            }

            synapseMessageContext.setProperty(DeprecationConstants.CFG_DEPRECATION_RESULT, Boolean.valueOf(deprecated));
            meio.setMediatorExecutionTime(System.currentTimeMillis() - start);
            mix.setMediatorExecutionTime(meio);
            MediatorExecInfoObject meio1 = new MediatorExecInfoObject();
            meio1.setMediatorName("EXECUTION");
            meio1.setMediatorExecutionTime(System.currentTimeMillis());
            mix.setClientIP("192.168.6.71");
            ((Axis2SynapseMessage) synapseMessageContext).getMessageContext().setProperty("MIX", mix);
            return !(deprecated);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadConfig(SynapseMessage synapseMessageContext) {
        boolean flag = false;
        InputStream deprecationInStream;
        String resource;
        AxisConfiguration ac = ((Axis2SynapseMessage) synapseMessageContext).getMessageContext().getConfigurationContext().getAxisConfiguration();
        Parameter param = ac.getParameter("PolicyRepo");
        if (param != null) {
            resource = param.getValue().toString().trim() + File.separator + "policy"+File.separator+"mediators" + File.separator + DeprecationConstants.CFG_DEPRECATION_XML;
            resource = resource.replace('\\','/');
            log.info("Resource from repo***" + resource);
            File file = new File(resource);
            try{

            flag = (file.lastModified() != modifiedTime);
            log.info("Flag***" + flag);
            modifiedTime = file.lastModified();
            }  catch(Exception e){
                e.printStackTrace();
            }
            log.info("Getting the stream***");
            try
            {
                deprecationInStream = new FileInputStream(file);
            }

            catch(Exception e)
            {
             log.info("ERROR HERE****"); e.printStackTrace();
             resource = DeprecationConstants.CFG_XML_FOLDER + "/" + DeprecationConstants.CFG_DEPRECATION_XML;
             log.info("Resource from AAR***" + resource);
             deprecationInStream = this.classLoader.getResourceAsStream(resource);
            }
        } else {
            resource = DeprecationConstants.CFG_XML_FOLDER + "/" + DeprecationConstants.CFG_DEPRECATION_XML;
            log.info("Resource from AAR***" + resource);
            deprecationInStream = this.classLoader.getResourceAsStream(resource);
       }
        log.info("Deprecation in stream " + resource);

        if ((!flag)&&(synapseEnvironment.getProperty("deprecation_synapseObject") != null))
        {
            deprecation = (SynapseObject) synapseEnvironment.getProperty("deprecation_synapseObject");
        } else {
            if(deprecationInStream!=null){
            deprecation = Utils.xmlToSynapseObject(deprecationInStream);
            synapseEnvironment.setProperty("deprecation_synapseObject", deprecation);
            }
            else
            {
                log.info("NULL STREAM");
                throw new SynapseException("No config found");
            }
        }
    }

    private boolean isDeprecated(SynapseObject service) {

        try {
            if (service.getBoolean("enabled").booleanValue()) {
                Calendar current = Calendar.getInstance();
                TimeZone tz = current.getTimeZone();
                int offset = tz.getRawOffset();
                Calendar calendar = new GregorianCalendar(tz);

                DateFormat df = new SimpleDateFormat("d/M/y:H:m");
                df.setTimeZone(tz);
                Date d1 = service.getDate(DeprecationConstants.CFG_DEPRECATION_FROM_DATE);
                Calendar fromCalendar = new GregorianCalendar(tz);
                d1.setTime(d1.getTime() + offset);
                fromCalendar.setTime(d1);
                String toDate = service.getDate(DeprecationConstants.CFG_DEPRECATION_TO_DATE).toString();
                if (toDate == null || (toDate.length() == 0)) {
                    return calendar.before(fromCalendar);
                }

                Date d2 = service.getDate("toDate");
                Calendar toCalendar = new GregorianCalendar(tz);
                d2.setTime(d2.getTime() + offset);
                toCalendar.setTime(d2);
                return (calendar.after(fromCalendar) && calendar.before(toCalendar));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public void setSynapseEnvironment(SynapseEnvironment se) {
        this.synapseEnvironment = se;
    }

    public void setClassLoader(ClassLoader cl) {
        this.classLoader = cl;
    }
}
