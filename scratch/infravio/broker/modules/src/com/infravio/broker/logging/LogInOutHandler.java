package com.infravio.broker.modules.logging;

import java.util.ArrayList;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.handlers.AbstractHandler;
import sjt.mgmt.SimpleWorkManager;
import sjt.mgmt.WorkManager;

import javax.xml.namespace.QName;

public class LogInOutHandler extends AbstractHandler implements Handler {

    private static int ACTIVE_THREADS = 1;

    private static int MAXIMUM_CAPACITY = 10;

    private static final long serialVersionUID = 1L;

    private QName name;

    WorkManager manager = null;

    public QName getName() {
        return name;
    }

    public void invoke(final MessageContext msgContext) throws AxisFault {

        Log loggerClass = new Log(msgContext);
        manager.addWork(loggerClass);
    }

    public void revoke(MessageContext msgContext) {

        System.out.println("LogInOutHandler Revoked!");
    }

    public void setName(QName name) {
        this.name = name;
    }

    public void init(HandlerDescription handlerdesc) {
        manager = new SimpleWorkManager("LogThread", ACTIVE_THREADS, MAXIMUM_CAPACITY, WorkManager.BLOCK);

    }
}
