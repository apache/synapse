/*
 * Copyright 2006 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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

import org.apache.axis2.client.Options;


public class MultiThreadedTest extends RMScenariosTest{

	
	public final static int NUMBER_OF_THREADS = 5;
	
	private int completeTests = 0;
	
	public void setUp()throws Exception{
		completeTests = 0;
		super.setUp();
	}

	
	public void _testPing() throws Exception  {
		final int NUMBER_OF_PING_MSGS_PER_THREAD = 5;
		//kick of a set of threads to run individual tests
		for(int i=0; i < NUMBER_OF_THREADS; i++){
			Thread th = new Thread(){
				public void run(){
					try{
						// Run a ping test with sync acks
						MultiThreadedTest.this.runPing(false, false, NUMBER_OF_PING_MSGS_PER_THREAD);
						// Run a ping test with async acks
						MultiThreadedTest.this.runPing(true, true, NUMBER_OF_PING_MSGS_PER_THREAD);
						MultiThreadedTest.this.completeTests ++ ; //mark another test as complete
					}
					catch(Exception e){	
					}
				}
			};
			th.start();
		}//end for
		
		int sleepCount = 0;
		while(completeTests < NUMBER_OF_THREADS){
			Thread.sleep(1000);
			sleepCount ++;
			if(sleepCount>45){
				fail("waited too long for all threads to complete");
			}
		}
		
	}
	
	public void _testAsyncEcho() throws Exception {
		
		//kick of a set of threads to run individual tests
		for(int i=0; i < NUMBER_OF_THREADS; i++){
			Thread th = new Thread(){
				public void run(){
					try{
						
						// Test async echo with sync acks
						Options clientOptions = new Options();
						runEcho(clientOptions, true, false, false,true,false);
						
						// Test async echo with async acks
						clientOptions = new Options();
						runEcho(clientOptions, true, true, false,true,false);
						MultiThreadedTest.this.completeTests ++ ; //mark another test as complete
						
					}
					catch(Exception e){	
					}
				}
			};
			th.start();
		}//end for
		
		int sleepCount = 0;
		while(completeTests < NUMBER_OF_THREADS){
			Thread.sleep(1000);
			sleepCount ++;
			if(sleepCount>45){
				fail("waited too long for all threads to complete");
			}
		}
	}
	
    public void _testSyncEcho() throws Exception {
		
		//kick of a set of threads to run individual tests
		for(int i=0; i < NUMBER_OF_THREADS; i++){
			Thread th = new Thread(){
				public void run(){
					try{
						// Test sync echo
						MultiThreadedTest.super.testSyncEcho();
						MultiThreadedTest.this.completeTests ++ ; //mark another test as complete
					}
					catch(Exception e){	
					}
				}
			};
			th.start();
		}//end for
		
		int sleepCount = 0;
		while(completeTests < NUMBER_OF_THREADS){
			Thread.sleep(1000);
			sleepCount ++;
			if(sleepCount>40){
				fail("waited too long for all threads to complete");
			}
		}
	}
    
	public void testSyncEchoWithOffer() throws Exception {
		
		//there is an issue with this test in that sequences from other threads
		//cause confusion in the error checking.
//		//kick of a set of threads to run individual tests
//		for(int i=0; i < NUMBER_OF_THREADS; i++){
//			Thread th = new Thread(){
//				public void run(){
//					try{
//						// Test sync echo
//						MultiThreadedTest.super.testSyncEchoWithOffer();
//						MultiThreadedTest.this.completeTests ++ ; //mark another test as complete
//					}
//					catch(Exception e){	
//					}
//				}
//			};
//			th.start();
//		}//end for
//		
//		int sleepCount = 0;
//		while(completeTests < NUMBER_OF_THREADS){
//			Thread.sleep(1000);
//			sleepCount ++;
//			if(sleepCount>45){
//				fail("waited too long for all threads to complete");
//			}
//		}
    }  

}
