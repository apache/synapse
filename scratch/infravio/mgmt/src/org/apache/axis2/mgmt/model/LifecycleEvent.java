package org.apache.axis2.mgmt.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:02:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class LifecycleEvent {
    int type; // DEPLOY, CONFIG, START, STOP, PAUSE, UNDEPLOY
    List log;
}
