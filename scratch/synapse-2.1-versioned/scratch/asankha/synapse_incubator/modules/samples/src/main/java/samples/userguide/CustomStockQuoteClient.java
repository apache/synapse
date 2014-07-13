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

package samples.userguide;

import samples.common.Util;

public class CustomStockQuoteClient {

    public static void main(String[] args) {

        String symbol = "IBM";
        String xurl   = "http://localhost:9000/axis2/services/SimpleStockQuoteService";
        String turl   = "http://localhost:8080";
        String repo   = "client_repo";
        String sAction= "urn:getQuote";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) xurl   = args[1];
        if (args.length > 2) turl   = args[2];
        if (args.length > 3) repo   = args[3];

        Util.testCustomQuote(symbol, sAction, xurl, turl, repo);
    }
}