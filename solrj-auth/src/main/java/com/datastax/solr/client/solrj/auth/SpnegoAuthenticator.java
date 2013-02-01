package com.datastax.solr.client.solrj.auth;

import java.net.URL;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.solr.client.solrj.impl.auth.HttpRequestAuthenticator;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.alfredo.client.AuthenticatedURL;

public class SpnegoAuthenticator implements HttpRequestAuthenticator
{
    private static final Logger logger = LoggerFactory.getLogger(SpnegoAuthenticator.class);

    private final SpnegoTokenCache tokenCache;
    
    public SpnegoAuthenticator(SpnegoTokenCache tokenCache)
    {
        this.tokenCache = tokenCache;
    }
    
    @Override
    public HttpRequestBase setAuthenticationOptions(HttpRequestBase method) throws SolrException
    {
        
       logger.debug("Using Alfredo to pre-authenticate SolrJ request with Kerberos");
        try 
        {
            URL url = method.getURI().toURL();
            AuthenticatedURL.Token token = tokenCache.getToken(url);
            // this is the HTTP Client equivalent of what Alfredo does for java.net.HttpUrlConnection
            method.addHeader("Cookie", AuthenticatedURL.AUTH_COOKIE + "=" + token);
        }
        catch (Exception e)
        {
            logger.error("Error performing HTTP Authentication for Solr client request", e);
            throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, e.getMessage(), e);
        }
        return method;
    }

}
