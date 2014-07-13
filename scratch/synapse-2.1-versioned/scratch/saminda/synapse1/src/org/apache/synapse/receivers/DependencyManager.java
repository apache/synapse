package org.apache.synapse.receivers;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 5:44:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class DependencyManager {
     private final static String MESSAGE_CONTEXT_MEDIATE_INJECTION_METHOD = "mediate";

    public static Boolean mediatorBusinessLogicProvider(Object obj,
                                                      MessageContext msgctx)
            throws AxisFault {
        Boolean returnValue = null;
        try {
            Class classToLoad = obj.getClass();
            Method[] methods = classToLoad.getMethods();

            for (int i = 0; i < methods.length; i++) {
                if (MESSAGE_CONTEXT_MEDIATE_INJECTION_METHOD.equals(
                        methods[i].getName()) &&
                        methods[i].getParameterTypes().length == 1 &&
                        methods[i].getParameterTypes()[0] ==
                                MessageContext.class) {
                    returnValue = (Boolean)methods[i].invoke(obj, new Object[]{msgctx});
                }
            }
            /**
             * return the mediator state
             */
            return returnValue;
        } catch (SecurityException e) {
            throw new AxisFault(e);
        } catch (IllegalArgumentException e) {
            throw new AxisFault(e);
        } catch (IllegalAccessException e) {
            throw new AxisFault(e);
        } catch (InvocationTargetException e) {
            throw new AxisFault(e);
        }
    }
}
