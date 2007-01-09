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

package org.apache.sandesha2;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.soap.SOAPFactory;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.AcknowledgementRange;

import junit.framework.TestCase;

public class SandeshaUtilTest extends TestCase {

	public void testUUIDGen () {
		String UUID1 = SandeshaUtil.getUUID();
		String UUID2 = SandeshaUtil.getUUID();
		
		assertTrue(UUID1!=null);
		assertTrue(UUID2!=null);
		assertTrue(!UUID1.equals(UUID2));
		
		assertTrue(UUID1.startsWith("urn:uuid:"));
		assertTrue(UUID2.startsWith("urn:uuid:"));
	}
	
	public void testInternalSequenceIDToSequenceKeyConversion()throws SandeshaException{
		String toEPR = "http://127.0.0.1:1111/some_random_uri";
		String sequenceKey = "1234abcd";
		
		String internalSequenceID = SandeshaUtil.getInternalSequenceID(toEPR, sequenceKey);
		
		//check that we can parse out the sequence key
		assertEquals(sequenceKey, SandeshaUtil.getSequenceKeyFromInternalSequenceID(internalSequenceID, toEPR));
		
		//try an internal sequenceID without a sequenceKey - should get null
		internalSequenceID = SandeshaUtil.getSequenceKeyFromInternalSequenceID(toEPR, null);
		assertNull(SandeshaUtil.getSequenceKeyFromInternalSequenceID(internalSequenceID, toEPR));
		
		//for badly formed sequences, or for server-side response sequences, check 
		//we just get null
		String outgoingSequenceID = SandeshaUtil.getOutgoingSideInternalSequenceID(SandeshaUtil.getUUID());
		assertNull(SandeshaUtil.getSequenceKeyFromInternalSequenceID(outgoingSequenceID, toEPR));
		
	}
	
	public void testGetAckRangeArrayList () throws SandeshaException {
		ArrayList msgNumberArr = new ArrayList();
		msgNumberArr.add(new Long(3));
		msgNumberArr.add(new Long(6));
		msgNumberArr.add(new Long(1));
		msgNumberArr.add(new Long(5));
		msgNumberArr.add(new Long(8));
		msgNumberArr.add(new Long(2));	
		
		ArrayList list = SandeshaUtil.getAckRangeArrayList(msgNumberArr,Sandesha2Constants.SPEC_2005_02.NS_URI);
		assertNotNull(list);
		assertEquals(list.size(),3);
		
		Iterator it = list.iterator();
		AcknowledgementRange ackRange = null;
		
		ackRange = (AcknowledgementRange) it.next();
		assertNotNull(ackRange);
		assertEquals(ackRange.getLowerValue(),1);
		assertEquals(ackRange.getUpperValue(),3);
		
		ackRange = null;
		ackRange = (AcknowledgementRange) it.next();
		assertNotNull(ackRange);
		assertEquals(ackRange.getLowerValue(),5);
		assertEquals(ackRange.getUpperValue(),6);
		
		ackRange = null;
		ackRange = (AcknowledgementRange) it.next();
		assertNotNull(ackRange);
		assertEquals(ackRange.getLowerValue(),8);
		assertEquals(ackRange.getUpperValue(),8);
		
		assertFalse(it.hasNext());
	}
	
	
	
	
	
}
