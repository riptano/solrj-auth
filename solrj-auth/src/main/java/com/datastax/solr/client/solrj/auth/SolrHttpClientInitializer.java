package com.datastax.solr.client.solrj.auth;

import java.io.File;
import java.security.Principal;

import javax.net.ssl.SSLContext;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.auth.HttpRequestAuthenticatorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Handles initializing Solr with custom helpers to support Kerberos authentication
 * & SSL encryption. These helpers must set before Solr creates any HTTPClient objects, 
 * so one of the init methods should be called as early as possible in the application 
 * initialization. Additionally, once Solr has been initialized, subsequent calls to 
 * any init method will have no effect.</p>
 * 
 * <p>There are methods provided by this class are broken down into those responsible 
 * for setting up SPNEGO authentication for SolrJ clients, and one which initializes
 * SSL encryption for requests made by those clients.</p>
 * 
 * <p>Options for configuring Kerberos authentication via SPNEGO</p>
 * 
 * <p>Use the AuthenticationInitializer class to configure SolrJ with a SpnegoAuthenticator,
 * a plugin which is used internally by every instance of HttpSolrServer. It performs
 * authentication using SPNEGO/GSSAPI/Kerberos and additionally caches authentication
 * tokens on a per-host basis. Optionally, all HTTP requests performed as part of the
 * SPNEGO protocol can be carried out using secure connections if an SSLContext is 
 * supplied. To use the Kerberos credentials a keytab file, both the file and the 
 * Principal must be supplied. If neither is supplied, then credentials from the local 
 * Kerberos ticket cache will be used. Supplying either a keytab or Principal, but not
 * both is not supported and will result in an error.</p>
 * 
 * <p>Examples:</p>
 * <p>Enable Kerberos authentication using credentials from local ticket cache</p>
 * <pre>
 * {@code
 *      SolrHttpClientInitializer.initAuthentication(new AuthenticationOptions());
 * }
 * </pre>
 * 
 * <p>Enable Kerberos authentication using a specified Principal and a keytab file</p>
 * <pre>
 * {@code
 *       SolrHttpClientInitializer.initAuthentication(
 *                  new AuthenticationOptions()
 *                      .withPrincipal(new KerberosPrincipal("user@REALM"))
 *                      .withKeytab(new File("/path/to/keytab")));
 * }
 * </pre>
 * 
 * <p>Enable Kerberos authentication using credentials from local ticket cache. HTTP 
 * requests during the SPNEGO protocol negotiation will be encrypted and use a specific
 * X509HostnameVerifier - the very lax version supplied by HTTPClient</p>
 * <pre>
 * {@code
 *       SolrHttpClientInitializer.initAuthentication(
 *                  new AuthenticationOptions()
 *                      .withPrincipal(new KerberosPrincipal("user@REALM"))
 *                      .withKeytab(new File("/path/to/keytab"))
 *                      .withSSLContext(SSLContext.getDefault())
 *                      .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
 * }
 * </pre>
 *
 *<p>Turn on SSL encryption for all client -> server HTTP requests. Hostnames will be verified
 *using the system default X509HostnameVerifier</p>
 * <pre>
 * {@code
 *      SolrHttpClientInitializer.initEncryption(
 *                 new EncryptionOptions()
 *                     .withSSLContext(SSLContext.getDefault()));
 * }
 * </pre>
 * 
 *<p>Turn on SSL encryption for all client -> server HTTP requests. Hostnames will be verified
 *using the specified X509HostnameVerifier</p>
 * <pre>
 * {@code
 *      SolrHttpClientInitializer.initEncryption(
 *                 new EncryptionOptions()
 *                     .withSSLContext(SSLContext.getDefault())
 *                     .withHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER));
 * }
 * </pre>
 *
 */
public class SolrHttpClientInitializer
{
    private static final Logger logger = LoggerFactory.getLogger(SolrHttpClientInitializer.class);
    
    /**
     * Configure Solrj to use Kerberos authentication via SPNEGO/GSSAPI for
     * client requests. Client Kerberos credentials for the supplied Principal
     * will be retrieved from specified keytab file. 
     * Additionally, all HTTP requests performed as part of the authentication 
     * negotiation will use SSL encryption and use the supplied SSLContext to 
     * obtain secure sockets. If you have no special SSL requirements, other 
     * than to enable it, use SSLContext.getDefault(). Optionally, an
     * X509HostVerifier may also be supplied in AuthenticationOptions.
     * 
     * @param options properties to configure the Kerberos connection & SPNEGO 
     * protocol negotiation
     */
    public static void initAuthentication(AuthenticationOptions options)
    {
        logger.info("Registering custom HTTPClient authentication with Solr");
        SpnegoAuthenticatorFactory authenticatorFactory = 
                    new SpnegoAuthenticatorFactory.Builder()
                        .keytab(options.keytab)
                        .principal(options.principal)
                        .sslContext(options.ctx)
                        .hostnameVerifier(options.verifier)
                        .build();
      HttpRequestAuthenticatorProvider.registerFactory(authenticatorFactory);
    }
    
    /**
     * Enable SSL encryption between SolrJ clients and Solr servers. The supplied
     * EncryptionOptions object is used to specify an SSLContext and optionally
     * an X509HostnameVerifier. If you have no special SSL requirements, other than to
     * enable it, you can just use SSLContext.getDefault() and leave the hostname
     * verifier unspecified. 
     * 
     * @param options 
     */
    public static void initEncryption(EncryptionOptions options)
    {    
        logger.info("Registering custom HTTPClient SSL configuration with Solr");
        SSLSocketFactory socketFactory = 
                (options.verifier == null) ?
                        new SSLSocketFactory(options.ctx) :
                        new SSLSocketFactory(options.ctx, options.verifier);
        HttpClientUtil.setConfigurer(new SSLHttpClientConfigurer(socketFactory));  
    }
    
    
    public static class AuthenticationOptions
    {
        private Principal principal;
        private File keytab;
        private SSLContext ctx;
        private X509HostnameVerifier verifier;
        
        public AuthenticationOptions withKeytab(File keytab)
        {
            this.keytab = keytab;
            return this;
        }
        
        public AuthenticationOptions withPrincipal(Principal principal)
        {
            this.principal = principal;
            return this;
        }
        
        public AuthenticationOptions withSSLContext(SSLContext ctx)
        {
            this.ctx = ctx;
            return this;
        }
        
        public AuthenticationOptions withHostnameVerifier(X509HostnameVerifier verifier)
        {
            this.verifier = verifier;
            return this;
        }
    }
            
    public static class EncryptionOptions
    {
        private SSLContext ctx;
        private X509HostnameVerifier verifier;
        
        public EncryptionOptions withSSLContext(SSLContext ctx)
        {
            this.ctx = ctx;
            return this;
        }
        
        public EncryptionOptions withHostnameVerifier(X509HostnameVerifier verifier)
        {
            this.verifier = verifier;
            return this;
        }
    }
}
