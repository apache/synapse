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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContextConstants;
import samples.common.InvesbotHandler;
import samples.common.Util;

public class ProxyStockQuoteClient {

    public static void main(String[] args) {

        String symbol   = "IBM";
        String fwdProxy = "http://localhost:8080/axis2/services/InvesbotForwardProxy";
        String defProxy = "http://localhost:8080/axis2/services/InvesbotDefaultProxy";
        String seqProxy = "http://localhost:8080/axis2/services/InvesbotSequenceProxy";
        String sAction  = "http://ws.invesbot.com/GetQuote";

        if (args.length > 0) symbol   = args[0];
        if (args.length > 1) fwdProxy = args[1];
        if (args.length > 2) defProxy = args[2];
        if (args.length > 2) seqProxy = args[3];

        Util.testStandardQuote(symbol, sAction, null, fwdProxy);
        Util.testStandardQuote(symbol, sAction, null, defProxy);
        Util.testStandardQuote(symbol, sAction, null, seqProxy);
    }
}
