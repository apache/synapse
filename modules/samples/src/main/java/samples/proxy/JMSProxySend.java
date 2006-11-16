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
package samples.proxy;

import samples.common.Util;

public class JMSProxySend {

    public static void main(String[] args) {
        String symbol  = "IBM";
        String xurl    = "http://ws.invesbot.com/stockquotes.asmx";
        String turl    = "jms:/JMSInvesbotSequenceProxy?transport.jms.ConnectionFactoryJNDIName=QueueConnectionFactory&java.naming.factory.initial=org.apache.activemq.jndi.ActiveMQInitialContextFactory&java.naming.provider.url=tcp://localhost:61616";
        String repo    = "client_repo";
        String sAction = "http://ws.invesbot.com/GetQuote";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) xurl   = args[1];
        if (args.length > 2) turl   = args[2];
        if (args.length > 3) repo   = args[3];

        Util.testStandardQuote(symbol, sAction, xurl, turl, repo);
    }

}
