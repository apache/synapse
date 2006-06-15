package org.apache.sandesha2.policy;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.policy.PrimitiveAssertion;


public class RMProcessorContext {
	public static final int NONE = 0;
	public static final int START = 1;
	public static final int COMMIT = 2;
	public static final int ABORT = 3;

	public static final String[] ACTION_NAMES = new String[]{"NONE", "START", "COMMIT", "ABORT"};
	
	private ArrayList tokenStack = new ArrayList();

	private int tokenStackPointer = 0;
	
    private ArrayList pedStack = new ArrayList();

    private int pedStackPointer = 0;

    private PrimitiveAssertion assertion = null;
	
	private int action = NONE;
	
	private static final Log logger = LogFactory.getLog(RMProcessorContext.class);

	public RMProcessorContext() {
	}

	public int getAction() {
		return action;
	}

	public void setAction(int act) {
		this.action = act;
	}

	public PrimitiveAssertion getAssertion() {
		return assertion;
	}

	public void setAssertion(PrimitiveAssertion asrt) {
		this.assertion = asrt;
	}

	public void pushRMToken(RMPolicyToken spt) {
		tokenStack.add(tokenStackPointer, spt);
		tokenStackPointer++;
	}

	public RMPolicyToken popRMToken() {
		if (tokenStackPointer > 0) {
			tokenStackPointer--;
			return (RMPolicyToken) tokenStack.get(tokenStackPointer);
		} else {
			return null;
		}
	}

	public RMPolicyToken readCurrentRMToken() {
		if (tokenStackPointer > 0) {
			return (RMPolicyToken) tokenStack.get(tokenStackPointer - 1);
		} else {
			return null;
		}
	}

    public void pushPolicyEngineData(PolicyEngineData ped) {
        pedStack.add(pedStackPointer, ped);
        pedStackPointer++;
    }

    public PolicyEngineData popPolicyEngineData() {
        if (pedStackPointer > 0) {
            pedStackPointer--;
            return (PolicyEngineData) pedStack.get(pedStackPointer);
        } else {
            return null;
        }
    }

    public PolicyEngineData readCurrentPolicyEngineData() {
        if (pedStackPointer > 0) {
            return (PolicyEngineData) pedStack.get(pedStackPointer - 1);
        } else {
            return null;
        }
    }
    
    public PolicyEngineData commitPolicyEngineData() {	
        if (pedStackPointer > 1) {
            pedStackPointer--;
            PolicyEngineData ped = (PolicyEngineData) pedStack.get(pedStackPointer);
            pedStackPointer--;
            pedStack.add(pedStackPointer, ped);
            pedStackPointer++;
            return ped;
        } else {
            return null;
        }
    }
}
