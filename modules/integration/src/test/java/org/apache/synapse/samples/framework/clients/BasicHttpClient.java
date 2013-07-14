package org.apache.synapse.samples.framework.clients;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;

public class BasicHttpClient {

    public HttpResponse doGet(String url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(url);
            return new HttpResponse(client.execute(get));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public HttpResponse doPost(String url, byte[] payload, String contentType) throws Exception {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(url);
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType(contentType);
            entity.setContent(new ByteArrayInputStream(payload));
            post.setEntity(entity);
            return new HttpResponse(client.execute(post));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

}
