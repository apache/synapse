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
package org.apache.synapse.mediator;

import org.apache.synapse.SynapseException;

/**
 * Unchecked Synapse Mediator exception
 */
public class MediatorException extends SynapseException {


	private static final long serialVersionUID = 4523165601281538518L;

	public MediatorException(Throwable e) {
        super(e);
    }

    public MediatorException(String msg, Throwable e) {
        super(msg, e);
    }

    public MediatorException(String msg) {
        super(msg);
    }

}