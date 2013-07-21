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

package samples.util;

import java.util.LinkedHashMap;
import java.util.Map;
import samples.bean.Location;
import samples.bean.Store;

/**
 * Provides utility methods to be used by the EJB implementations.
 */
public class StoreRegisterUtil {

    private static Map<String, Store> stores =
            new LinkedHashMap<String, Store>();

    static {
        stores.put("1010", new Store("1010", "Majestic City",
                "403, Station Road, Colombo 4.", "0112352354"));
        stores.put("2020", new Store("2020", "Dehiwala",
                "67, Galle Road, Dehiwala.", "0114789056"));
        stores.put("3030", new Store("3030", "Kadawatha",
                "253, Kandy Road, Kadawatha", "0112990789"));
        stores.put("4040", new Store("4040", "Moratuwa",
                "33, Galle Road, Rawathawatte", "0117564902"));
    }

    public static String getClosestStore(Location loc) {
        int index = (int)(loc.getLongitude() + loc.getLatitude()) %
                stores.size();
        return stores.keySet().toArray()[index].toString();
    }

    public static Store getStoreById(String id) {
        return stores.get(id);
    }
    
    private static String getStoreNameByLocation(Location loc) {
       return getStoreById(getClosestStore(loc)).getName();
    }

}
