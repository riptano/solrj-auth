package com.datastax.solr.client.solrj.auth;

import java.io.File;
import java.security.Principal;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.alfredo.client.AuthenticatedURL;

/**
 * Supplies AuthenticatedURLs, a class from the Alfredo library, which performs the 
 * actual SPNEGO protocol authentication. This class is used by SpnegoTokenCache, 
 * which obtains a new AuthenticatedURL instance and uses it to get a SPNEGO 
 * token if it doesn't already have one cached for a given host.
 * 
 * If *both* a keytab file and Principal are supplied, those credentials will be
 * used when performing GSSAPI authentication. If neither is supplied, the local
 * Kerberos ticket cache will be used. Supplying either one, but not the other 
 * is an error.
 * 
 * To use HTTPS for secure connections during SPNEGO auth, supply an SSLContext.  
 */
public class AuthenticatedURLProvider
{
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedURLProvider.class);
    
    private final File keytab; 
    private final Principal principal;
    private final SSLContext sslContext;
    private final HostnameVerifier hostVerifier;

    private AuthenticatedURLProvider(Builder builder)
    {
        this.keytab = builder.keytab;
        this.principal = builder.principal;
        this.sslContext = builder.sslContext;
        this.hostVerifier = builder.verifier;
    }

    public AuthenticatedURL get()
    {
        if (null == keytab)
        {
            logger.debug("Creating AuthenticatedURL to use credentials from ticket cache");
            if (null != sslContext)
            {   
                logger.debug("SSL is enabled, setting socketfactory & host name verifier");
                // null arg forces default (i.e. Kerberos) authenticator, but no keytab or principal
                return new AuthenticatedURL(null, sslContext.getSocketFactory(), hostVerifier);
            }
            else
            {
                return new AuthenticatedURL();
            }
        }
        else
        {
            logger.debug("Creating AuthenticatedURL to use credentials from DSE config");
            if (null != sslContext)
            {   
                logger.debug("SSL is enabled, setting socketfactory & host name verifier");
                return new AuthenticatedURL(keytab.getAbsolutePath(), principal.getName(), sslContext.getSocketFactory(), hostVerifier);
            }
            else
            {
                return new AuthenticatedURL(keytab.getAbsolutePath(), principal.getName());
            }
        }        
        
    }
    
    public static class Builder
    {
        private File keytab;
        private Principal principal;
        private SSLContext sslContext;
        private HostnameVerifier verifier;
        
        public Builder keytab(File keytab)
        {
            this.keytab = keytab;
            return this;
        }
        
        public Builder principal(Principal principal)
        {
            this.principal = principal;
            return this;
        }
        
        public Builder sslContext(SSLContext sslContext)
        {
            this.sslContext = sslContext;
            return this;
        }
        
        public Builder hostnameVerifier(HostnameVerifier verifier)
        {
            this.verifier = verifier;
            return this;
        }
        
        public AuthenticatedURLProvider build()
        {
            if ((keytab == null) != (principal == null))
            {
                throw new IllegalStateException("Please supply both a keytab and Principal");
            }
            return new AuthenticatedURLProvider(this);
        }
    }

}
