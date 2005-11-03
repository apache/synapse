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
package org.apache.synapse;

import org.apache.synapse.axis2.Expression;



public class Rule {
	private Expression expr= null;
	private String mediatorName = null;
	private boolean secure = false;
	private boolean reliable = false;
	private boolean transactional = false;
	public void setExpression(Expression expr) {
		this.expr = expr;
	}
	public Expression getExpression() {
		return expr;
	}
	public void setMediatorName(String mediatorName) {
		this.mediatorName = mediatorName;
	}
	public  String getMediatorName() {
		return mediatorName;
	}
	public void setSecure(boolean secure) {
		this.secure = secure;
	}
	public  boolean isSecure() {
		return secure;
	}
	public  void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	public boolean isReliable() {
		return reliable;
	}
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}
	public boolean isTransactional() {
		return transactional;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		sb.append('\'');
		sb.append(expr);
		sb.append('\'');
		sb.append('}');
		sb.append(' ');
		sb.append('-');
		sb.append('>');
		sb.append(' ');
		sb.append(mediatorName);
		if (isReliable()) { sb.append(','); sb.append("reliable"); }
		if (isSecure()) { sb.append(','); sb.append("secure"); }
		if (isTransactional()) { sb.append(','); sb.append("transactional"); }
		sb.append(';');
		
		return sb.toString();
	}
}
