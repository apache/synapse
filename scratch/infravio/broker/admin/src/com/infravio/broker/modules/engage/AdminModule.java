package com.infravio.broker.modules.engage;

import com.infravio.broker.parser.OSBrokerConstants;
import com.infravio.broker.parser.XMLParser;
import noNamespace.BrokerConfiguratorDocument;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.ModuleDescription;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurationImpl;
import org.apache.axis2.modules.Module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class AdminModule implements Module {

    static AxisConfiguration axisSystem;
    static String serviceName, moduleName;

    XMLParser parser = null;
    Properties prop = new Properties();
    String file_location = "META-INF/module.properties";

    public void init(AxisConfiguration axisSystem) {

        System.out.println("INIT OF MODULE ENGAGE");
        try {
            // Engage AdminModule to AdminService first.
            AdminModule.axisSystem = axisSystem;
            serviceName = "AdminService";
            moduleName = "AdminModule";
            engageService();

            // module.properties file contains the location where axis2 is deployed.
            // This is used to locate the broker-configuration.xml file.
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file_location);
            try {
                prop.load(inputStream);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String path = prop.getProperty("deploy.dir");
            String broker_file_path = path + "/axis2/WEB-INF/broker-configuration.xml";

            File BrokerXmlFile = new File(broker_file_path);
            OSBrokerConstants.lastScanTime = System.currentTimeMillis();
            OSBrokerConstants.BrokerDoc = BrokerConfiguratorDocument.Factory.parse(BrokerXmlFile);
            parser = new XMLParser();
            get_collections_and_engage();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void get_collections_and_engage()
    {

        //Steps:
        // 1) Take all services present in broker-configuration.xml
        // 2) See if .aar files are present in services directory of axis2.war
        // 3) If present, lookout for modules that needs to be mapped for this service in broker-configuration.xml
        // 4) See if those modules are present in axis2.war.
        // 5) If presnt, engage the modules to the service.
        Collection serviceCollection = parser.getServices();
            for (Iterator i = serviceCollection.iterator(); i.hasNext();) {
                serviceName = (String) i.next();
                Collection moduleCollection = parser.getEngagedModules(serviceName);
                for (Iterator j = moduleCollection.iterator(); j.hasNext();) {
                    moduleName = (String) j.next();
                    engageService();
                }
            }
    }


    /**
     * Module & Service names are held in moduleName & serviceName fields.
     * These will be checked against services/modules physically present in axis2.war
     * If they are available, those modules are engaged to services.
     */
    private void engageService() {

        HashMap services = axisSystem.getServices();
        Collection serviceCol = services.values();
        try{
            for (Iterator serviceItr = serviceCol.iterator(); serviceItr.hasNext();) {
                ServiceDescription serviceDescription = (ServiceDescription) serviceItr.next();
                if (serviceDescription.getName().getLocalPart().equals(serviceName)) {
                    HashMap modules = ((AxisConfigurationImpl) axisSystem).getModules();
                    Collection moduleCol = modules.values();

                    for (Iterator moduleItr = moduleCol.iterator(); moduleItr.hasNext();) {
                        boolean isEngagedFlag = false;
                        ModuleDescription moduleDescription = (ModuleDescription) moduleItr.next();
                        Collection engagedModules = serviceDescription.getEngagedModules();

                        for (Iterator engagedItr = engagedModules.iterator(); engagedItr.hasNext();) {
                            ModuleDescription temp_md = (ModuleDescription) engagedItr.next();

                            if (temp_md.getName().getLocalPart().equals(moduleDescription.getName().getLocalPart())) {
                                isEngagedFlag = true;
                            }
                        }

                        if ((moduleDescription.getName().getLocalPart().equals(moduleName))
                                && (isEngagedFlag == false)) {
                            serviceDescription.engageModule(moduleDescription, axisSystem);
                            System.out.println("Module " + moduleName
                                    + " engaged to service " + serviceName);
                        }
                    }
                }
            }
        } catch (AxisFault axisFault) {
              axisFault.printStackTrace();
        }
    }


    // shutdown the module
    public void shutdown(AxisConfiguration axisSystem) throws AxisFault {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }
}
