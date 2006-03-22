package org.apache.synapse.resources;


import org.apache.synapse.SynapseException;
import org.apache.synapse.resources.xml.ResourceMediator;
import org.apache.synapse.resources.xml.PropertyMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

public class ResourceHelperFactory {
    private Log log = LogFactory.getLog(getClass());

    private static ResourceHelperFactory fac;

    private static boolean created = false;

    private HashMap resourcesProcessorsMap;

    private ResourceHelperFactory() {
    }

    public static ResourceHelperFactory newInstance() {
        if (created) {
            return fac;
        } else {
            fac = new ResourceHelperFactory();
        }
        return fac;
    }

    public ResourceHelper createResourceHelper() {
        log.debug("Creating resources helper");
        ResourceHelperImpl helper = new ResourceHelperImpl();

        for (Iterator ite = resourcesProcessorsMap.keySet().iterator();
             ite.hasNext();) {

            String key = (String) ite.next();

            Object obj = resourcesProcessorsMap.get(key);

            if (obj instanceof ResourceMediator) {
                ResourceMediator rp = (ResourceMediator) obj;
                // getting the resource handler
                ResourceHandler rh = getResourceHandler(rp.getType());
                //filling the propertybags of ResourceHanlder
                fillResourceHandler(rp,rh);
                helper.registerResourceHandler(rh,rp.getURIRoot());
            } else {
                throw new SynapseException(
                        "Should be found only ResourceHandler implementaions");
            }

        }
        return helper;
    }

    private void fillResourceHandler(ResourceMediator rp, ResourceHandler rh) {
        //filling the ResourcesHandlers properties
        List rms = rp.getList();

        for (Iterator ite = rms.iterator();ite.hasNext();) {
            Object obj = ite.next();
            if (obj instanceof PropertyMediator) {
                PropertyMediator pp = (PropertyMediator)obj;
                rh.setProperty(pp.getName(),pp.getValue());
            } else {
                throw new SynapseException("Support only PropertyMediator, found :" + obj.getClass().getName());
            }
        }



    }

    public void setResourceProcessorsMap(HashMap resourceProcessorsMap) {
        this.resourcesProcessorsMap = resourceProcessorsMap;
    }

    private ResourceHandler getResourceHandler(String clazzName) {
        ResourceHandler handler;
        try {
            Class clazz = Class.forName(clazzName);
            Constructor ct = clazz.getConstructor(new Class[]{});
            Object obj = ct.newInstance(new Object[]{});

            if (obj instanceof ResourceHandler) {
                handler = (ResourceHandler) obj;
            } else {
                throw new SynapseException(
                        "Only Objects implements ResourceHandler should be available");
            }
            return handler;
        } catch (ClassNotFoundException e) {
            throw new SynapseException(e);
        } catch (IllegalAccessException e) {
            throw new SynapseException(e);
        } catch (NoSuchMethodException e) {
            throw new SynapseException(e);
        } catch (InvocationTargetException e) {
            throw new SynapseException(e);
        } catch (InstantiationException e) {
            throw new SynapseException(e);
        }

    }



}
