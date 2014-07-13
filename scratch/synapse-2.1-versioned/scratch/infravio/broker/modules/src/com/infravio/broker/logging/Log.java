package com.infravio.broker.modules.logging;

import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;

import java.io.IOException;

public class Log implements Runnable {

    public MessageContext msgContext = null;

    public Log(MessageContext msgContext) {

        this.msgContext = msgContext;
    }

    public void run() {

        // System.out.println(msgContext.getEnvelope().toString());
        String serviceName = msgContext.getServiceContext().getServiceConfig().getName().getLocalPart();
        String logFileName = serviceName + ".log";
        Appender app = null;
        SimpleLayout layout = new SimpleLayout();
        try {
            app = new FileAppender(layout, logFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Logger logger = Logger.getLogger(this.getClass());
        logger.addAppender(app);
        logger.setLevel(Level.ALL);
        System.out.println(msgContext.getEnvelope().toString());
        String message = "Message - " + msgContext.hashCode() + " at time " + System.currentTimeMillis();
        logger.log(Priority.INFO, message);
    }
}
