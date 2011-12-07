/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.samples.framework;

import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.tests.endpoint.*;
import org.apache.synapse.samples.framework.tests.message.*;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * This is executed by maven and handles which samples to run
 */
public class TestSamplesHandlerSuite extends TestSuite {

    private static final Log log = LogFactory.getLog(TestSamplesHandlerSuite.class);
    private static HashMap<String, Object> sampleClassRepo
            = new HashMap<String, Object>();

    public static TestSuite suite() {

        //Adding all samples available
        populateSamplesMap();

        ArrayList<Class> suiteClassesList = new ArrayList<Class>();
        TestSuite suite = new TestSuite();

        String inputSuiteName = System.getProperty("suite");
        String tests = System.getProperty("tests");
        String suiteName = "SamplesSuite";

        //preparing suites, if specified
        if (inputSuiteName != null) {
            if (inputSuiteName.equalsIgnoreCase("message")) {
                suiteName = "MessageMediationSamplesSuite";
                for (int i = 0; i <= 15; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("endpoint")) {
                suiteName = "EndpointSamplesSuite";
                for (int i = 50; i <= 60; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("qos")) {
                suiteName = "QoSSamplesSuite";
                for (int i = 100; i <= 110; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("proxy")) {
                suiteName = "ProxySamplesSuite";
                for (int i = 150; i <= 170; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("transport")) {
                suiteName = "TransportSamplesSuite";
                for (int i = 250; i <= 280; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("tasks")) {
                suiteName = "TasksSamplesSuite";
                for (int i = 300; i <= 310; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("advanced")) {
                suiteName = "AdvancedSamplesSuite";
                for (int i = 350; i <= 490; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("eventing")) {
                suiteName = "EventingSamplesSuite";
                for (int i = 500; i <= 510; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
        } else if (tests != null) {
            String[] testArray = tests.split(",");
            suiteName = "SelectedSamplesSuite";
            for (String testNum : testArray) {
                Class testClass = (Class) sampleClassRepo.get(testNum);
                if (testClass != null) {
                    suiteClassesList.add(testClass);
                }
            }
        } else {
            suiteName = "AllSamplesSuite";
            for (int i = 0; i <= 600; i++) {
                Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                if (testClass != null) {
                    suiteClassesList.add(testClass);
                }
            }
        }

        for (Class testClass : suiteClassesList) {
            suite.addTestSuite(testClass);
            log.info("Adding Sample:" + testClass);
        }
        suite.setName(suiteName);

        return suite;
    }

    private static void populateSamplesMap() {

        //Message Mediation
        sampleClassRepo.put("0", Sample0.class);
        sampleClassRepo.put("1", Sample1.class);
        sampleClassRepo.put("2", Sample2.class);
        sampleClassRepo.put("3", Sample3.class);
        sampleClassRepo.put("4", Sample4.class);
        sampleClassRepo.put("5", Sample5.class);
        sampleClassRepo.put("6", Sample6.class);
        sampleClassRepo.put("7", Sample7.class);
        sampleClassRepo.put("8", Sample8.class);
        sampleClassRepo.put("9", Sample9.class);
        sampleClassRepo.put("10", Sample10.class);
        sampleClassRepo.put("11", Sample11.class);
        sampleClassRepo.put("12", Sample12.class);
        sampleClassRepo.put("13", Sample13.class);
        sampleClassRepo.put("15", Sample15.class);
        sampleClassRepo.put("16", Sample16.class);

        //Endpoint
        sampleClassRepo.put("50", Sample50.class);
        sampleClassRepo.put("51", Sample51.class);
        sampleClassRepo.put("52", Sample52.class);
        sampleClassRepo.put("53", Sample53.class);
        sampleClassRepo.put("54", Sample54.class);
        sampleClassRepo.put("55", Sample55.class);
        sampleClassRepo.put("56", Sample56.class);
        //sampleClassRepo.put("57", Sample57.class);  //intermittently fail
        sampleClassRepo.put("58", Sample58.class);
        sampleClassRepo.put("59", Sample59.class);
    }
}