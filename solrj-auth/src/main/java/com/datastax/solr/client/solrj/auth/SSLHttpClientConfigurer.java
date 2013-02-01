package com.datastax.solr.client.solrj.auth;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.common.params.SolrParams;

public class SSLHttpClientConfigurer extends HttpClientConfigurer
{
    private final SSLSocketFactory socketFactory;
    
    public SSLHttpClientConfigurer(SSLSocketFactory socketFactory)
    {
        this.socketFactory = socketFactory;
    }
    
    @Override
    protected void configure(DefaultHttpClient httpClient, SolrParams config)
    {
        super.configure(httpClient, config);
        Scheme httpsScheme = new Scheme("https", 443, socketFactory);
        httpClient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
    }

}
