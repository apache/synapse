package com.infravio.broker.modules.engage;

import com.infravio.broker.parser.OSBrokerConstants;
import com.infravio.broker.parser.XMLParser;
import noNamespace.BrokerConfiguratorDocument;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.ModuleDescription;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurationImpl;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.xmlbeans.XmlException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class AdminInHandler extends AbstractHandler implements Handler {

    static AxisConfiguration axisSystem;
    static String serviceName, moduleName;

    Properties prop = new Properties();
    String file_location = "META-INF/module.properties";

    XMLParser parser = null;
    private QName name;

    public QName getName() {
        return name;
    }

    public void invoke(MessageContext msgContext) {

        System.out.println("Admin In handler - invoke method called");

        ConfigurationContext configContext = msgContext.getOperationContext().getEngineContext();
        AdminInHandler.axisSystem = configContext.getAxisConfiguration();

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
        try {
            System.out.println("BrokerXmlFile.lastModified()->"+BrokerXmlFile.lastModified());
            System.out.println("OSBrokerConstants.lastScanTime->"+OSBrokerConstants.lastScanTime);

            if (BrokerXmlFile.lastModified() > OSBrokerConstants.lastScanTime) {
                System.out.println("AdminInHandler Invoked!");
                OSBrokerConstants.BrokerDoc = BrokerConfiguratorDocument.Factory.parse(BrokerXmlFile);
                OSBrokerConstants.lastScanTime = BrokerXmlFile.lastModified();
                parser = new XMLParser();
                AdminModule adminModule = new AdminModule();
                adminModule.get_collections_and_engage();
                }
        } catch (AxisFault axisFault) {
                            axisFault.printStackTrace();
        } catch (XmlException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void revoke(MessageContext msgContext) {
        System.out.println("AdminInHandler revoked!");
    }

    public void setName(QName name) {
        this.name = name;
    }
}
