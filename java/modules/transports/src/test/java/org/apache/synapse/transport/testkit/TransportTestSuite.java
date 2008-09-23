/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.testkit;

import java.text.ParseException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.lang.StringUtils;
import org.apache.synapse.transport.testkit.filter.FilterExpression;
import org.apache.synapse.transport.testkit.filter.FilterExpressionParser;
import org.apache.synapse.transport.testkit.tests.TestResourceSet;
import org.apache.synapse.transport.testkit.tests.TestResourceSetTransition;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;
import org.apache.synapse.transport.testkit.util.LogManager;

public class TransportTestSuite extends TestSuite {
    private final Class<?> testClass;
    private final List<FilterExpression> excludes = new LinkedList<FilterExpression>();
    private final boolean reuseResources;
    private boolean invertExcludes;
    private int nextId = 1;
    
    public TransportTestSuite(Class<?> testClass, boolean reuseResources) {
        this.testClass = testClass;
        this.reuseResources = reuseResources;
    }
    
    public TransportTestSuite(Class<?> testClass) {
        this(testClass, true);
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public void addExclude(String filter) throws ParseException {
        excludes.add(FilterExpressionParser.parse(filter));
    }

    public void setInvertExcludes(boolean invertExcludes) {
        this.invertExcludes = invertExcludes;
    }

    @Override
    public void addTest(Test test) {
        if (test instanceof TransportTestCase) {
            TransportTestCase ttest = (TransportTestCase)test;
            Map<String,String> map = ttest.getNameComponents();
            boolean excluded = false;
            for (FilterExpression exclude : excludes) {
                if (exclude.matches(map)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded != invertExcludes) {
                return;
            }
            ttest.init(StringUtils.leftPad(String.valueOf(nextId++), 4, '0'),
                       reuseResources, testClass);
            ttest.getResourceSet().resolve();
        }
        super.addTest(test);
    }

    @Override
    public void run(TestResult result) {
        LogManager logManager = LogManager.INSTANCE;
        if (!reuseResources) {
            super.run(result);
        } else {
            TestResourceSet resourceSet = null;
            for (Enumeration<?> e = tests(); e.hasMoreElements(); ) {
                Test test = (Test)e.nextElement();
                if (test instanceof TransportTestCase) {
                    TransportTestCase ttest = (TransportTestCase)test;
                    TestResourceSet newResourceSet = ttest.getResourceSet();
                    try {
                        if (resourceSet == null) {
                            logManager.setTestCase(ttest);
                            newResourceSet.setUp();
                        } else {
                            TestResourceSetTransition transition = new TestResourceSetTransition(resourceSet, newResourceSet);
                            transition.tearDown();
                            logManager.setTestCase(ttest);
                            transition.setUp();
                        }
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                    resourceSet = newResourceSet;
                }
                runTest(test, result);
            }
            if (resourceSet != null) {
                try {
                    resourceSet.tearDown();
                    logManager.setTestCase(null);
                } catch (Throwable t) {
                    result.addError(this, t);
                    return;
                }
            }
        }
    }
}
