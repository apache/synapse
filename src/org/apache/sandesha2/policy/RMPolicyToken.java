package org.apache.sandesha2.policy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;


public class RMPolicyToken {
	/**
	 * The following values describe the type of the rm pocliy token. A complex
	 * token starts a transaction because it contains nested tokens. A simple
	 * token does not contain nested tokens but stands allone an defines a
	 * simple assertion or property.
	 * 
	 * If Content is set then this token contains additional text content, e.g.
	 * XPath expressions.
	 */
	public static final int COMPLEX_TOKEN = 1;

	public static final int SIMPLE_TOKEN = 2;

	public static final int WITH_CONTENT = 100;

	private String tokenName;

	private int tokenType = 0;

	// private boolean supported = false;

	private String[] attributes = null;

	private Object handler = null;

	private Method processTokenMethod = null;

	private ArrayList childTokens = null;

	private static final Log logger = LogFactory.getLog(RMPolicyToken.class);
	
	public RMPolicyToken(String token, int type, String[] attribs,
			Object h) throws SandeshaException, NoSuchMethodException {
		this(token, type, attribs);
		setProcessTokenMethod(h);
	}

	public RMPolicyToken(String token, int type, String[] attribs) {
		tokenName = token;
		tokenType = type;
		attributes = attribs;

		if (tokenType == COMPLEX_TOKEN) {
			childTokens = new ArrayList();
		}
	}

	/**
	 * @return Returns the attributes.
	 */
	public String[] getAttributes() {
		return attributes;
	}

	public void setProcessTokenMethod(Object h) throws NoSuchMethodException {
		
		if (h == null) {
			return;
		}
		handler = h;
		Class handlerCls = h.getClass();
		Class[] parameters = new Class[] { RMProcessorContext.class };

		processTokenMethod = handlerCls.getDeclaredMethod("do" + tokenName,
				parameters);
	}

	public boolean invokeProcessTokenMethod(RMProcessorContext spc)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		if (processTokenMethod == null) {
			return false;
		}
		Object[] parameter = new Object[] { spc };
		Object ret = processTokenMethod.invoke(handler, parameter);
		Boolean bool;
		if (ret instanceof Boolean) {
			bool = (Boolean) ret;
			return bool.booleanValue();
		}
		return false;
	}

	public String getTokenName() {
		return tokenName;
	}

	public void setChildToken(RMPolicyToken spt) {
		childTokens.add(spt);
	}

	public RMPolicyToken getChildToken(String sptName) {
		Iterator it = childTokens.iterator();
		while (it.hasNext()) {
			RMPolicyToken tmpSpt = (RMPolicyToken) it.next();
			if (sptName.equals(tmpSpt.getTokenName())) {
				return tmpSpt;
			}
		}
		return null;
	}


	public void removeChildToken(String sptName) {
		Iterator it = childTokens.iterator();
		while (it.hasNext()) {
			RMPolicyToken tmpSpt = (RMPolicyToken) it.next();
			if (sptName.equals(tmpSpt.getTokenName())) {
				childTokens.remove(tmpSpt);
				return;
			}
		}
	}

	public RMPolicyToken copy() {
		RMPolicyToken spt = new RMPolicyToken(tokenName, tokenType,
				attributes);
		if (childTokens != null) {
			Iterator it = childTokens.iterator();
			while (it.hasNext()) {
				RMPolicyToken tmpSpt = (RMPolicyToken) it.next();
				spt.setChildToken(tmpSpt);
			}
		}
		return spt;
	}
}
