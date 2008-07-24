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
package org.apache.synapse.transport.jms;

import java.io.IOException;
import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;

import org.apache.commons.io.IOExceptionWithCause;

/**
 * Input stream that reads data from a JMS {@link BytesMessage}.
 */
public class BytesMessageInputStream extends InputStream {
    private final BytesMessage message;

    public BytesMessageInputStream(BytesMessage message) {
        this.message = message;
    }

    @Override
    public int read() throws IOException {
        try {
            return message.readByte() & 0xFF;
        } catch (MessageEOFException ex) {
            return -1;
        } catch (JMSException ex) {
            throw new IOExceptionWithCause(ex);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (off == 0) {
            try {
                return message.readBytes(b, len);
            } catch (JMSException ex) {
                throw new IOExceptionWithCause(ex);
            }
        } else {
            byte[] b2 = new byte[len];
            int c = read(b2);
            if (c > 0) {
                System.arraycopy(b2, 0, b, off, c); 
            }
            return c;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return message.readBytes(b);
        } catch (JMSException ex) {
            throw new IOExceptionWithCause(ex);
        }
    }
}
