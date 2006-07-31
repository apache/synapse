/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.policy;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.processors.AcknowledgementIntervalProcessor;
import org.apache.sandesha2.policy.processors.ExponentialBackoffProcessor;
import org.apache.sandesha2.policy.processors.InactivityTimeoutMeasureProcessor;
import org.apache.sandesha2.policy.processors.InactivityTimeoutProcessor;
import org.apache.sandesha2.policy.processors.InvokeInOrderProcessor;
import org.apache.sandesha2.policy.processors.MaximumRetransmissionCountProcessor;
import org.apache.sandesha2.policy.processors.MessageTypesToDropProcessor;
import org.apache.sandesha2.policy.processors.RetransmissionItervalProcessor;
import org.apache.sandesha2.policy.processors.SecurityManagerProcessor;
import org.apache.sandesha2.policy.processors.StorageManagersProcessor;
import org.apache.ws.policy.All;
import org.apache.ws.policy.Assertion;
import org.apache.ws.policy.ExactlyOne;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.PrimitiveAssertion;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyReader;

public class RMPolicyProcessor {

	private static final Log logger = LogFactory.getLog(RMPolicyProcessor.class);

	PolicyReader prdr = null;

	RMPolicyToken topLevel = new RMPolicyToken("_TopLevel_",
			RMPolicyToken.COMPLEX_TOKEN, null);

	RMProcessorContext rmProcessorContext = null;

	public boolean setup() throws NoSuchMethodException {
		prdr = PolicyFactory.getPolicyReader(PolicyFactory.OM_POLICY_READER);

		RMPolicyToken rpt = null;

		RetransmissionItervalProcessor rip = new RetransmissionItervalProcessor();
		rpt = RMPolicy.retransmissionIterval.copy();
		rpt.setProcessTokenMethod(rip);
		topLevel.setChildToken(rpt);

		AcknowledgementIntervalProcessor aip = new AcknowledgementIntervalProcessor();
		rpt = RMPolicy.acknowledgementInterval.copy();
		rpt.setProcessTokenMethod(aip);
		topLevel.setChildToken(rpt);

		MaximumRetransmissionCountProcessor mrip = new MaximumRetransmissionCountProcessor();
		rpt = RMPolicy.maximumRetransmissionCount.copy();
		rpt.setProcessTokenMethod(mrip);
		topLevel.setChildToken(rpt);
		
		ExponentialBackoffProcessor ebp = new ExponentialBackoffProcessor();
		rpt = RMPolicy.exponentialBackoff.copy();
		rpt.setProcessTokenMethod(ebp);
		topLevel.setChildToken(rpt);

		InactivityTimeoutMeasureProcessor itmp = new InactivityTimeoutMeasureProcessor();
		rpt = RMPolicy.inactiveTimeoutMeasure.copy();
		rpt.setProcessTokenMethod(itmp);
		topLevel.setChildToken(rpt);

		InactivityTimeoutProcessor itp = new InactivityTimeoutProcessor();
		rpt = RMPolicy.inactiveTimeout.copy();
		rpt.setProcessTokenMethod(itp);
		topLevel.setChildToken(rpt);

		InvokeInOrderProcessor iiop = new InvokeInOrderProcessor();
		rpt = RMPolicy.invokeInOrder.copy();
		rpt.setProcessTokenMethod(iiop);
		topLevel.setChildToken(rpt);

		MessageTypesToDropProcessor mttdp = new MessageTypesToDropProcessor();
		rpt = RMPolicy.messageTypeToDrop.copy();
		rpt.setProcessTokenMethod(mttdp);
		topLevel.setChildToken(rpt);

		StorageManagersProcessor smp = new StorageManagersProcessor();
		rpt = RMPolicy.storageManagers.copy();
		rpt.setProcessTokenMethod(smp);
		topLevel.setChildToken(rpt);

		SecurityManagerProcessor secmp = new SecurityManagerProcessor();
		rpt = RMPolicy.securityManager.copy();
		rpt.setProcessTokenMethod(secmp);
		topLevel.setChildToken(rpt);
		
		/*
		 * Now get the initial PolicyEngineData, initialize it and put it onto
		 * the PED stack.
		 */
		PolicyEngineData ped = new PolicyEngineData();
		ped.initializeWithDefaults();

		/*
		 * Now get a context and push the top level token onto the token stack.
		 * The top level token is a special token that acts as anchor to start
		 * parsing.
		 */
		rmProcessorContext = new RMProcessorContext();
		rmProcessorContext.pushRMToken(topLevel);
		rmProcessorContext.pushPolicyEngineData(ped);

		return true;
	}

	/**
	 * This method takes a normalized policy object, processes it and returns
	 * true if all assertion can be fulfilled.
	 * 
	 * Each policy must be nromalized accordig to the WS Policy framework
	 * specification. Therefore a policy has one child (wsp:ExactlyOne) that is
	 * a XorCompositeAssertion. This child may contain one or more other terms
	 * (alternatives). To match the policy one of these terms (alternatives)
	 * must match. If none of the contained terms match this policy cannot be
	 * enforced.
	 * 
	 * @param policy
	 *            The policy to process
	 * @return True if this policy can be enforced by the policy enforcement
	 *         implmentation
	 */
	public boolean processPolicy(Policy policy) {
		if (!policy.isNormalized()) {
			policy = (Policy) policy.normalize();
		}

		ExactlyOne xor = (ExactlyOne) policy.getTerms()
				.get(0);
		List listOfPolicyAlternatives = xor.getTerms();

		boolean success = false;
		int numberOfAlternatives = listOfPolicyAlternatives.size();

		for (int i = 0; !success && i < numberOfAlternatives; i++) {
			All aPolicyAlternative = (All) listOfPolicyAlternatives
					.get(i);

			List listOfAssertions = aPolicyAlternative.getTerms();

			Iterator iterator = listOfAssertions.iterator();
			/*
			 * Loop over all assertions in this alternative. If all assertions
			 * can be fulfilled then we choose this alternative and signal a
			 * success.
			 */
			boolean all = true;
			while (all && iterator.hasNext()) {
				Assertion assertion = (Assertion) iterator.next();

				/*
				 * At this point we expect PrimitiveAssertions only.
				 */
				if (!(assertion instanceof PrimitiveAssertion)) {
					logger.debug("Got a unexpected assertion type: "
							+ assertion.getClass().getName());
					continue;
				}
				/*
				 * We need to pick only the primitive assertions which contain a
				 * v1_0 policy assertion. For that we'll check the namespace of
				 * the primitive assertion
				 */
				PrimitiveAssertion pa = (PrimitiveAssertion) assertion;
				if (!(pa.getName().getNamespaceURI()
						.equals("http://ws.apache.org/sandesha2/policy"))) {
					logger.debug("Got a unexpected assertion: "
							+ pa.getName().getLocalPart());
					continue;
				}
				all = processPrimitiveAssertion((PrimitiveAssertion) assertion);
			}
			/*
			 * copy the status of assertion processing. If all is true then this
			 * alternative is "success"ful
			 */
			success = all;
		}
		return success;
	}

	boolean processPrimitiveAssertion(PrimitiveAssertion pa) {
		boolean commit = true;

		commit = startPolicyTransaction(pa);

		List terms = pa.getTerms();
		if (commit && terms.size() > 0) {
			for (int i = 0; commit && i < terms.size(); i++) {
				Assertion assertion = (Assertion) pa.getTerms().get(i);
				if (assertion instanceof Policy) {
					commit = processPolicy((Policy) assertion);
				} else if (assertion instanceof PrimitiveAssertion) {
					commit = processPrimitiveAssertion((PrimitiveAssertion) assertion);
				}
			}
		}
		if (commit) {
			commitPolicyTransaction(pa);
		} else {
			abortPolicyTransaction(pa);
		}
		return commit;
	}

	public boolean startPolicyTransaction(PrimitiveAssertion pa) {
		String tokenName = pa.getName().getLocalPart();

		RMPolicyToken rmpt = null;

		/*
		 * Get the current rm policy token from the context and check if the
		 * current token supports/contains this assertion as token. If yes set
		 * this token as current token (push onto stack), set the assertion into
		 * context and call the processing method for this token.
		 */
		RMPolicyToken currentToken = rmProcessorContext
				.readCurrentRMToken();
		if (currentToken == null) {
			logger.error(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.errorOnTokenStack));
			System.exit(1); 
		}
		rmpt = currentToken.getChildToken(tokenName);
		rmProcessorContext.pushRMToken(rmpt);
		rmProcessorContext.setAssertion(pa);
		rmProcessorContext.setAction(RMProcessorContext.START);

		/*
		 * Get the current state of the PolicyEngineData, make a copy of it and
		 * push the copy onto the PED stack. The token method works on this
		 * copy, adding its data.
		 */
		PolicyEngineData ped = rmProcessorContext.readCurrentPolicyEngineData();
		ped = ped.copy();
		rmProcessorContext.pushPolicyEngineData(ped);
		if (rmpt == null) {
			logger
					.debug("RM token: '" + tokenName
							+ "' unknown in context of '"
							+ currentToken.getTokenName());
			return false;
		}
		boolean ret = false;

		try {
			ret = rmpt.invokeProcessTokenMethod(rmProcessorContext);
		} catch (Exception ex) {
			logger.error(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.generalError, ex.toString()),
					ex);
		} finally {
			rmProcessorContext.setAction(RMProcessorContext.NONE);
		}
		return ret;
	}

	public void abortPolicyTransaction(PrimitiveAssertion pa) {
		RMPolicyToken currentToken = rmProcessorContext
				.readCurrentRMToken();
		if (currentToken == null) {
			logger.debug(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownTokenAbortTran,
					pa.getName().getLocalPart()));

			rmProcessorContext.popRMToken();
			return;
		}

		rmProcessorContext.setAssertion(pa);
		rmProcessorContext.setAction(RMProcessorContext.ABORT);
		try {
			currentToken.invokeProcessTokenMethod(rmProcessorContext);

		} catch (Exception ex) {
			logger.error(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.processTokenMethodError, ex.toString()),
					ex);

		} finally {
			rmProcessorContext.setAction(RMProcessorContext.NONE);
			rmProcessorContext.popRMToken();
			rmProcessorContext.popPolicyEngineData();

		}
	}

	public void commitPolicyTransaction(PrimitiveAssertion pa) {
		RMPolicyToken currentToken = rmProcessorContext
				.readCurrentRMToken();
		if (currentToken == null) {
			logger.error(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.commitingUnknownToken, 
					pa.getName().getLocalPart()));
			System.exit(1);
		}
		rmProcessorContext.setAssertion(pa);
		rmProcessorContext.setAction(RMProcessorContext.COMMIT);
		try {
			currentToken.invokeProcessTokenMethod(rmProcessorContext);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			rmProcessorContext.setAction(RMProcessorContext.NONE);
			rmProcessorContext.popRMToken();
			rmProcessorContext.commitPolicyEngineData();
		}
	}

	public RMProcessorContext getContext() {
		return rmProcessorContext;
	}

	public void setContext(RMProcessorContext rmProcessorContext) {
		this.rmProcessorContext = rmProcessorContext;
	}
}
