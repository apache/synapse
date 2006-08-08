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
package samples.mediation;

import samples.common.Util;

public class AdvancedQuoteClient {

    public static void main(String[] args) {

        String xurl    = "http://ws.invesbot.com/stockquotes.asmx";
        String turl    = "http://localhost:8080/StockQuote";
        String repo   = "client_repo";
        String sAction = "http://ws.invesbot.com/GetQuote";

        if (args.length > 0) xurl   = args[0];
        if (args.length > 1) turl   = args[1];
        if (args.length > 2) repo   = args[2];

        Util.testStandardQuote("IBM", sAction, xurl, turl, repo);
        Util.testAdvancedQuote("SUN", sAction, xurl, turl, repo);
        Util.testErroneousQuote("MSFT", sAction, xurl, turl, repo);
    }

}