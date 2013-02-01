package com.datastax.solr.client.solrj.auth;

import java.io.File;
import java.security.Principal;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.solr.client.solrj.impl.auth.HttpRequestAuthenticator;
import org.apache.solr.client.solrj.impl.auth.HttpRequestAuthenticatorFactory;

public class SpnegoAuthenticatorFactory implements HttpRequestAuthenticatorFactory
{
    private final SpnegoAuthenticator authenticator; 

    private SpnegoAuthenticatorFactory(Builder builder)
    {
        AuthenticatedURLProvider urlProvider = 
                   new AuthenticatedURLProvider.Builder()
                       .keytab(builder.keytab)
                       .principal(builder.principal)
                       .sslContext(builder.sslContext)
                       .hostnameVerifier(builder.hostnameVerifier)
                       .build();
        authenticator = new SpnegoAuthenticator(new SpnegoTokenCache(urlProvider));
    }
        
    @Override
    public HttpRequestAuthenticator getAuthenticator()
    {
        return authenticator;
    }
        
    public static class Builder
    {
        private Principal principal;
        private File keytab;
        private SSLContext sslContext;
        private HostnameVerifier hostnameVerifier;
        
        public Builder principal(Principal principal)
        {
            this.principal = principal;
            return this;
        }
        
        public Builder keytab(File keytab)
        {
            this.keytab = keytab;
            return this;
        }
        
        public Builder sslContext(SSLContext sslContext)
        {
            this.sslContext = sslContext;
            return this;
        }
        
        public Builder hostnameVerifier(HostnameVerifier verifier)
        {
            this.hostnameVerifier = verifier;
            return this;
        }
        
        public SpnegoAuthenticatorFactory build()
        {
            if ((keytab == null) != (principal == null))
            {
                throw new IllegalStateException("Please supply both a keytab and Principal");
            }
            
            return new SpnegoAuthenticatorFactory(this);
        }
    }
}
