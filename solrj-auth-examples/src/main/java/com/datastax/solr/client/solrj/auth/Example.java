package com.datastax.solr.client.solrj.auth;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

import com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.AuthenticationOptions;
import com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.EncryptionOptions;

public class Example
{
    
    public static void usage()
    {
        System.err.println("usage: Example <url> <kerberos_principal> <keytab> <truststore> <truststore_password> <keystore> <keystore_password>");
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 7)
        {
            usage();
            System.exit(1);
        }
        
        String url = args[0];
        String kerberosPrincipal = args[1];
        String keytabPath = args[2];
        
        String truststorePath = args[3];
        String truststorePassword = args[4];
        String keystorePath = args[5];
        String keystorePassword = args[6];
                
        SSLContext context = getSSLContext(truststorePath, truststorePassword, keystorePath, keystorePassword);
        SolrHttpClientInitializer.initAuthentication(
                    new AuthenticationOptions()
                        .withPrincipal(new KerberosPrincipal(kerberosPrincipal))
                        .withKeytab(new File(keytabPath))
                        .withSSLContext(context)
                        .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
        SolrHttpClientInitializer.initEncryption(
                    new EncryptionOptions()
                        .withSSLContext(context)
                        .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
        
        SolrServer server = new HttpSolrServer(url);
        
        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.addField( "id", "id1", 1.0f );
        doc1.addField( "name", "doc1", 1.0f );
        doc1.addField( "title", "this is doc1");
        
        SolrInputDocument doc2 = new SolrInputDocument();
        doc2.addField( "id", "id2", 1.0f );
        doc2.addField( "name", "doc2", 1.0f );
        doc2.addField( "title", "this is doc2" );
        
        Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        docs.add( doc1 );
        docs.add( doc2 );
        
        server.add( docs );
        server.commit();
        
        SolrQuery query = new SolrQuery();
        query.setQuery( "name:doc2" );
        
        QueryResponse rsp = server.query( query );
        System.out.println(rsp.toString());
    }
    
    public static SSLContext getSSLContext(String truststorePath, String truststorePassword,
                                            String keystorePath, String keystorePassword)
    throws Exception {
        FileInputStream tsf = new FileInputStream(truststorePath);
        FileInputStream ksf = new FileInputStream(keystorePath);
        SSLContext ctx = SSLContext.getInstance("SSL");

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(tsf, truststorePassword.toCharArray());
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(ksf, keystorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keystorePassword.toCharArray());
        
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return ctx;
    }

    
    
}
