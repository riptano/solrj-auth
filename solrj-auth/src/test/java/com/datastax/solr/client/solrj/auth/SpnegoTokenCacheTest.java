package com.datastax.solr.client.solrj.auth;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.solr.common.SolrException;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;
import com.cloudera.alfredo.client.AuthenticationException.AuthenticationExceptionCode;
import com.cloudera.alfredo.server.AuthenticationToken;
import com.cloudera.alfredo.server.KerberosAuthenticationHandler;

public class SpnegoTokenCacheTest
{

    URL url; 
    AuthenticatedURL.Token token;
    
    @Before
    public void setup() throws Exception
    {
        url = new URL("http://test.example.com/test/url");
        // generate a token string for testing using AuthenticationToken
        // which is how Alfredo's AuthenticationFilter handles it
        AuthenticationToken serverToken = new AuthenticationToken("foo", "foo/host@REALM", KerberosAuthenticationHandler.TYPE);
        serverToken.setExpires(System.currentTimeMillis() + 60000);
        token = new AuthenticatedURL.Token(serverToken.toString());
    }
        
    @Test
    public void whenNotCachedGetNewToken() throws Exception
    {
        AuthenticatedURL mockURL = createMock(AuthenticatedURL.class);
        expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(token);
        replay(mockURL);
        AuthenticatedURLProvider provider = getMockProvider(mockURL);
        
        SpnegoTokenCache cache = new SpnegoTokenCache(provider);
        AuthenticatedURL.Token t = cache.getToken(url);
        assertSame(token, t);
        verify(mockURL);
        verify(provider);
    }
    
    @Test
    public void returnAlreadyCachedTokens() throws Exception
    {
        AuthenticatedURL mockURL = createMock(AuthenticatedURL.class);
        expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(token);
        replay(mockURL);
        AuthenticatedURLProvider provider = getMockProvider(mockURL);
        
        SpnegoTokenCache cache = new SpnegoTokenCache(provider);
        assertSame(cache.getToken(url), cache.getToken(url));
        verify(mockURL);
        verify(provider);
    }
    
    @Test
    public void ifTokenIsExpiredRemoveFromCacheAndObtainNewToken() throws Exception
    {
        long now = System.currentTimeMillis();
        // use AuthenticationToken to construct a token string that has already expired
        AuthenticationToken serverToken = new AuthenticationToken("bar", "bar/host@REALM", "KERBEROS");
        serverToken.setExpires(now - 100); // already expired
        AuthenticatedURL.Token expiredToken = new AuthenticatedURL.Token(serverToken.toString());
        
        AuthenticatedURL mockURL = createMock(AuthenticatedURL.class);
        // first time around, return the expired token. This will be returned from the internal
        // guava cache. The token cache should then invalidate it & request a new token 
        expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(expiredToken);
        // next time, return an unexpired token
        expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(token);
        replay(mockURL);
                
        AuthenticatedURLProvider provider = createMock(AuthenticatedURLProvider.class);
        expect(provider.get()).andReturn(mockURL).times(2);
        replay(provider);
        
        SpnegoTokenCache cache = new SpnegoTokenCache(provider);
        assertSame(token, cache.getToken(url));
        verify(mockURL);
        verify(provider);
    }
    
    @Test
    public void cachedTokensRemovedFromCacheAfterTTL() throws Exception
    {
        System.setProperty(SpnegoTokenCache.CACHE_TTL_MS_PROPERTY, "" + 10);
        try
        {
            AuthenticationToken serverToken = new AuthenticationToken("bar", "bar/host@REALM", "KERBEROS");
            serverToken.setExpires(System.currentTimeMillis() + 60000); 
            AuthenticatedURL.Token secondToken = new AuthenticatedURL.Token(serverToken.toString());

            AuthenticatedURL mockURL = createMock(AuthenticatedURL.class);
            // first time around, return the initial token, this will be loaded into the internal
            // guava cache.  
            expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(token);

            // next time, return our second token
            expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject())).andReturn(secondToken);
            replay(mockURL);

            AuthenticatedURLProvider provider = createMock(AuthenticatedURLProvider.class);
            expect(provider.get()).andReturn(mockURL).times(2);
            replay(provider);

            SpnegoTokenCache cache = new SpnegoTokenCache(provider);
            assertSame(token, cache.getToken(url));
            // sleep to ensure the token gets expired from the cache
            TimeUnit.MILLISECONDS.sleep(100);
            // now we should get a different token instance
            assertSame(secondToken, cache.getToken(url));
            verify(mockURL);
            verify(provider);
        }
        finally
        {
            System.clearProperty(SpnegoTokenCache.CACHE_TTL_MS_PROPERTY);
        }
    }
    
    @Test (expected=SolrException.class)
    public void authenticationExceptionIsWrappedAndRethrown() throws Exception
    {
        AuthenticatedURL mockURL = createMock(AuthenticatedURL.class);
        expect(mockURL.authenticateWithToken(eq(url), (AuthenticatedURL.Token)anyObject()))
                .andThrow(new AuthenticationException("TEST EXCEPTION", 
                            AuthenticationExceptionCode.INVALID_TOKEN));
        
        replay(mockURL);
        AuthenticatedURLProvider provider = getMockProvider(mockURL);
        
        SpnegoTokenCache cache = new SpnegoTokenCache(provider);
        cache.getToken(url);
    }
    
    private AuthenticatedURLProvider getMockProvider(AuthenticatedURL mockURL)
    {
        AuthenticatedURLProvider provider = createMock(AuthenticatedURLProvider.class);
        expect(provider.get()).andReturn(mockURL);
        replay(provider);
        return provider;
    }
    
    
}
