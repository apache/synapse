/*
 * Copyright 2007 The Apache Software Foundation.
 * Copyright 2007 International Business Machines Corp.
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

package org.apache.sandesha2.scenarios;

import java.io.Serializable;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.sandesha2.context.ContextManager;

public class UnitTestContextManager implements ContextManager {

	private static final String id = "UNIT_TEST_CONTEXT";
	
	public UnitTestContextManager(ConfigurationContext context) {
		
	}
	
	public Serializable storeContext() {
		return id;
	}

	public Runnable wrapWithContext(final Runnable work,
									final Serializable context) {
		
		Runnable result = new Runnable() {
			public void run() {
				if(!id.equals(context)) throw new RuntimeException("Unexpected context " + context);
				System.out.println("Switching to " + context);
				work.run();
			}
		};

		return result;
	}

}
