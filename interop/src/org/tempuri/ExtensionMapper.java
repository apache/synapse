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

package org.tempuri;

/**
 * ExtensionMapper class
 */

public class ExtensionMapper {

	public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
			java.lang.String typeName, javax.xml.stream.XMLStreamReader reader)
			throws java.lang.Exception {

		if ("http://tempuri.org/".equals(namespaceURI)
				&& "EchoStringResponse.BodyType".equals(typeName)) {

			return org.tempuri.EchoStringResponseBodyType.Factory.parse(reader);

		}

		if ("http://tempuri.org/".equals(namespaceURI)
				&& "EchoStringRequest.BodyType".equals(typeName)) {

			return org.tempuri.EchoStringRequestBodyType.Factory.parse(reader);

		}

		throw new java.lang.RuntimeException("Unsupported type " + namespaceURI
				+ " " + typeName);
	}

}
