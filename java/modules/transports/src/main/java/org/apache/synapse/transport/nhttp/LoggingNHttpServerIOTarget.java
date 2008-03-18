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

package org.apache.synapse.transport.nhttp;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.protocol.HttpContext;

public class LoggingNHttpServerIOTarget 
    implements NHttpServerIOTarget, HttpInetConnection {

    private final NHttpServerIOTarget target;
    private final Log log;
    private final Log headerlog;
    
    public LoggingNHttpServerIOTarget(
            final Log log, 
            final Log headerlog, 
            final NHttpServerIOTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("IO target may not be null");
        }
        this.target = target;
        if (log != null) {
            this.log = log;
        } else {
            this.log = LogFactory.getLog(target.getClass());
        }
        if (log != null) {
            this.headerlog = headerlog;
        } else {
            this.headerlog = LogFactory.getLog(LoggingUtils.HEADER_LOG_ID);
        }
    }

    public void requestInput() {
        this.target.requestInput();
    }

    public void requestOutput() {
        this.target.requestOutput();
    }

    public void suspendInput() {
        this.target.suspendInput();
    }

    public void suspendOutput() {
        this.target.suspendOutput();
    }

    public void close() throws IOException {
        this.log.debug("Close connection");        
        this.target.close();
    }

    public HttpConnectionMetrics getMetrics() {
        return this.target.getMetrics();
    }

    public int getSocketTimeout() {
        return this.target.getSocketTimeout();
    }

    public boolean isOpen() {
        return this.target.isOpen();
    }

    public boolean isStale() {
        return this.target.isStale();
    }

    public void setSocketTimeout(int timeout) {
        this.target.setSocketTimeout(timeout);
    }

    public void shutdown() throws IOException {
        this.log.debug("Shutdown connection");        
        this.target.shutdown();
    }
    
    public HttpContext getContext() {
        return this.target.getContext();
    }

    public HttpRequest getHttpRequest() {
        return this.target.getHttpRequest();
    }

    public HttpResponse getHttpResponse() {
        return this.target.getHttpResponse();
    }

    public int getStatus() {
        return this.target.getStatus();
    }

    public boolean isResponseSubmitted() {
        return this.target.isResponseSubmitted();
    }

    public void resetInput() {
        this.target.requestInput();
    }

    public void resetOutput() {
        this.target.requestOutput();
    }

    public void submitResponse(final HttpResponse response) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + this + ": "  + response.getStatusLine().toString());
        }
        this.target.submitResponse(response);
        if (this.headerlog.isDebugEnabled()) {
            this.headerlog.debug("<< " + response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                this.headerlog.debug("<< " + headers[i].toString());
            }
        }
    }

    public void consumeInput(final NHttpServiceHandler handler) {
        this.log.debug("Consume input");        
        this.target.consumeInput(handler);
    }

    public void produceOutput(final NHttpServiceHandler handler) {
        this.log.debug("Produce output");        
        this.target.produceOutput(handler);
    }
    
    public InetAddress getLocalAddress() {
      if (this.target instanceof HttpInetConnection) {
          return ((HttpInetConnection) this.target).getLocalAddress();
      } else {
          return null;
      }
  }

  public int getLocalPort() {
      if (this.target instanceof HttpInetConnection) {
          return ((HttpInetConnection) this.target).getLocalPort();
      } else {
          return -1;
      }
  }

  public InetAddress getRemoteAddress() {
      if (this.target instanceof HttpInetConnection) {
          return ((HttpInetConnection) this.target).getRemoteAddress();
      } else {
          return null;
      }
  }

  public int getRemotePort() {
      if (this.target instanceof HttpInetConnection) {
          return ((HttpInetConnection) this.target).getRemotePort();
      } else {
          return -1;
      }
  }
  
  @Override
  public String toString() {
      return this.target.toString();
  }
  
}