package org.apache.axis2.mgmt.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:02:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ErrorEvent {
    String code, message;
    int type; // FATAL, FAULT, ALERT
    List log;
}
