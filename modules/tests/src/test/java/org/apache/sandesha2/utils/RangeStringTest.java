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
package org.apache.sandesha2.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.sandesha2.SandeshaTestCase;
import org.apache.sandesha2.util.Range;
import org.apache.sandesha2.util.RangeString;

public class RangeStringTest extends SandeshaTestCase{

	
	public RangeStringTest(String s){
		super(s);
	}
	
	public void testStringToRangeAndBack(){
	  
		//our expected range is missing 6, 9, 10 and 11 and ends at 12
		String finalRangeString = "[0,5][7,8][12,12]";
		
		RangeString rString = new RangeString(finalRangeString);
		assertTrue(rString.isMessageNumberInRanges(0));
		assertTrue(rString.isMessageNumberInRanges(1));
		assertTrue(rString.isMessageNumberInRanges(2));
		assertTrue(rString.isMessageNumberInRanges(3));
		assertTrue(rString.isMessageNumberInRanges(4));
		assertTrue(rString.isMessageNumberInRanges(5));
		
		assertFalse(rString.isMessageNumberInRanges(6));
		
		assertTrue(rString.isMessageNumberInRanges(7));
		assertTrue(rString.isMessageNumberInRanges(8));
		
		assertFalse(rString.isMessageNumberInRanges(9));
		assertFalse(rString.isMessageNumberInRanges(10));
		assertFalse(rString.isMessageNumberInRanges(11));
		
		assertTrue(rString.isMessageNumberInRanges(12));
		
		//now just check some boundary conditions
		assertFalse(rString.isMessageNumberInRanges(13));
		assertFalse(rString.isMessageNumberInRanges(-1));
		
		//now we get the string representation back
		assertEquals(finalRangeString, rString.toString());
		
	}
	
	
	public void testGrowingRange(){
		//start of missing msgs 2-9
		String msgs = "[1,1][10,10]";
		
		RangeString rString = new RangeString(msgs);
		
		//msg 2 arrives
		{
			Range ackedMsgRange = new Range(1,2);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,1);
			assertEquals(newRanges[0],new Range(2,2));
		}
		
		//msgs 8 and 9 arrive
		{
			Range ackedMsgRange = new Range(8,9);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,1);
			assertEquals(newRanges[0],ackedMsgRange);
		}
		
		// msg 6 arrives
		{
			Range ackedMsgRange = new Range(6,6);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,1);
			assertEquals(newRanges[0],ackedMsgRange);
		}
		
		//msgs 3,4 and 5 arrive
		{
			Range ackedMsgRange = new Range(1,5);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,1);
			assertEquals(newRanges[0],new Range(3,5));
		}
		
		//msgs 3,4 are duplicated
		{
			Range ackedMsgRange = new Range(3,4);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,0); //no new information
		}
		
		//finally msg 7
		{
			Range ackedMsgRange = new Range(7,7);
			Range[] newRanges = rString.addRange(ackedMsgRange).getRanges();
			assertEquals(newRanges.length,1);
			assertEquals(newRanges[0],ackedMsgRange);
		}
		
		//all msgs have now arrived
		assertEquals("[1,10]", rString.toString());
		
		//all messages are duplicated - ensure this is detected
		assertEquals(rString.addRange(new Range(1,10)).getRanges().length, 0);  
		//check we handle duplicates i.e. the string is still correct
		assertEquals("[1,10]", rString.toString());
	}
	
	public void testSerialize()throws Exception{
		String msgRange = "[1,100]";
		RangeString r = new RangeString(msgRange);
		//serialize
		ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(memoryBuffer);
		serializer.writeObject(r);
		serializer.flush();
		//deserialize
		ObjectInputStream inStrm = new ObjectInputStream(new ByteArrayInputStream(memoryBuffer.toByteArray()));
		RangeString newRangeString = (RangeString)inStrm.readObject();
		assertEquals(msgRange, newRangeString.toString());
	}
	
	
}
