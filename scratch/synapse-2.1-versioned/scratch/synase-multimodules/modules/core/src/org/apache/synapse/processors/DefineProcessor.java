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
package org.apache.synapse.processors;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

/**
 *
 */
public class DefineProcessor extends ListProcessor {

    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        return true;
    }

    /*
    Thre Processor.process() is common to every one. Define Processor is a unique
    mediator that is not run in-line. It should always reference by some entity. This
    Entity is "<refdefine/>. <refdefine/> has the @ref which will reference to the
    <define/>.

    The Following method processRef(SynapseEnvironment se, SynapseMessage sm); will
    allow to refernce the <define/> process and, will not allow the <define/> to be a inline
    process either.
    */

    public boolean processRef(SynapseEnvironment se, SynapseMessage sm) {
        return super.process(se,sm);
    }
}
