package com.datastax.solr.client.solrj.auth;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Test;

import com.cloudera.alfredo.client.AuthenticatedURL;

public class SpnegoAuthenticatorTest
{

    @Test
    public void authenticateAndInjectHttpHeader() throws Exception
    {
        String url = "http://test.example.com/test/url";
        String tokenString = "this is a test token";
        
        AuthenticatedURL.Token token = new AuthenticatedURL.Token(tokenString);
        SpnegoTokenCache mockCache = createMock(SpnegoTokenCache.class);
        expect(mockCache.getToken(new URL(url))).andReturn(token);
        replay(mockCache);
        
        SpnegoAuthenticator authenticator = new SpnegoAuthenticator(mockCache);
        HttpRequestBase method = new HttpGet(url);
        method = authenticator.setAuthenticationOptions(method);
        
        assertEquals(method.getFirstHeader("Cookie").getValue(), AuthenticatedURL.AUTH_COOKIE + "=" + tokenString);
        verify(mockCache);
    }
}
