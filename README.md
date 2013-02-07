SolrJ-Auth for DSE Search
================

Handles initializing Solr with custom helpers to support Kerberos authentication & SSL encryption. These helpers must be setup before Solr creates any HTTPClient objects, so the ```initXXX``` methods should be called as early as possible during application initialization. Additionally, once Solr has been initialized, subsequent calls to either ```initXXX``` method will have no effect.

This library must be used with the DSE specific SolrJ component. Both jars are shipped as part of the DSE distro, and the SolrJ library should be used as a drop in replacement for the regular Apache library in client applications which want to access secured DSE search services using SolrJ.

Options for configuring Kerberos authentication via SPNEGO
=====================================

Use the SolrHttpClientInitializer class to configure SolrJ with a SpnegoAuthenticator, a plugin which is used internally by every instance of HttpSolrServer. It performs authentication using SPNEGO/GSSAPI/Kerberos and additionally caches authentication tokens on a per-host basis. Optionally, all HTTP requests performed as part of the SPNEGO protocol can be carried out using secure connections if an SSLContext is supplied. To use the Kerberos credentials a keytab file, both the file and the Principal must be supplied. If neither is supplied, then credentials from the local Kerberos ticket cache will be used. Supplying either a keytab or Principal, but not both is not supported and will result in an error.

Enable Kerberos authentication using credentials from local ticket cache
-----------------------------------------------------------------------------------------------------------

```java
SolrHttpClientInitializer.initAuthentication(new AuthenticationOptions());
```
  
Enable Kerberos authentication using a specified Principal and a keytab file
---------------------------------------------------------------------------------------------------------------

```java
SolrHttpClientInitializer.initAuthentication(
           new AuthenticationOptions()
                .withPrincipal(new KerberosPrincipal("user@REALM"))
                .withKeytab(new File("/path/to/keytab")));
```
 
Enable Kerberos authentication using credentials from local ticket cache and SSL encryption
------------------------------------------------------------------------------------------------------------
HTTP requests during the SPNEGO protocol negotiation will be encrypted 
and use a specific X509HostnameVerifier - the very lax version supplied 
by HTTPClient

```java
SolrHttpClientInitializer.initAuthentication(
           new AuthenticationOptions()
                .withPrincipal(new KerberosPrincipal("user@REALM"))
                .withKeytab(new File("/path/to/keytab"))
                .withSSLContext(SSLContext.getDefault())
                .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
```
 
Turn on SSL encryption for all client / server HTTP requests
---------------------------------------------------------------------------------------
Hostnames will be verified using the system default X509HostnameVerifier

```java
SolrHttpClientInitializer.initEncryption(
           new EncryptionOptions()
                .withSSLContext(SSLContext.getDefault()));
```
 
Turn on SSL encryption for all client / server HTTP requests 
------------------------------------------------------------------------------------------
Hostnames will be verified using the specified X509HostnameVerifier

```java
SolrHttpClientInitializer.initEncryption(
           new EncryptionOptions()
                .withSSLContext(SSLContext.getDefault())
                .withHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER));
```

Code Sample
========

The solrj-auth-examples subproject contains a working demo which can be run from the command line using maven. It assumes a secured running DSE search node is running.

```
solrj-auth-examples$ mvn exec:java -Durl=https://example.host:8983/solr/wiki.solr -Dprincipal=user@REALM -Dkeytab=/home/user/test.keytab -Dtruststore=/home/user/truststore.jks -Dtruststore_pwd=cassandra -Dkeystore=/home/user/keystore.jks -Dkeystore_pwd=cassandra
```
