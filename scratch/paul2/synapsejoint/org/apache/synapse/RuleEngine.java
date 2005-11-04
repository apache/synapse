package org.apache.synapse;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.axis2.Expression;
import org.apache.synapse.axis2.MediatorExecutor;


// This class does the hard work, relying on OM/MC for the input
// and using Expression to match, MedEx to dispatch, and Sender to call on
public class RuleEngine {
	private RuleList rl = null;
	
	public RuleEngine(String ruleFile) {
		
		ClassLoader cl = getClass().getClassLoader();
		
		InputStream ruleIS =null;
		try {
			ruleIS = new FileInputStream(ruleFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (ruleIS==null) throw new SynapseException("Can't locate rule file: "+ruleFile);
		rl = new RuleList(ruleIS, cl);
	}
	
	public void process(MessageContext messageContext) {
		
		
		Iterator iterator = rl.iterator();
		while (iterator.hasNext()) {
				Rule r = (Rule) iterator.next();
				Expression e = r.getExpression();
				if (e.match(messageContext))
				{
					
					boolean cont = MediatorExecutor.execute(r, messageContext); 
					if (!cont) return;
				}
		}
		// send now
			
		//Sender.send(messageContext);
	}

	

}
