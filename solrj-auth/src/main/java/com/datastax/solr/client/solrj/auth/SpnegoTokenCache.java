package com.datastax.solr.client.solrj.auth;

import java.io.IOException;

import java.net.URL;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticatedURL.Token;
import com.cloudera.alfredo.client.AuthenticationException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SpnegoTokenCache
{
    private static final Logger logger = LoggerFactory.getLogger(SpnegoTokenCache.class);
    
    private final Cache<String, Token> cache;
    private final AuthenticatedURLProvider urlProvider;

    public static final long CACHE_TTL_MS_DEFAULT = 60 * 60 * 1000;
    public static final String CACHE_TTL_MS_PROPERTY = "spnego.token.cache.ttl";
    
    private static final String ATTR_SEPARATOR = "&";
    private static final String EXPIRES_ATTR = "e";
    
    public SpnegoTokenCache(AuthenticatedURLProvider urlProvider)
    {
        this.urlProvider = urlProvider;
        long ttl = Long.getLong(CACHE_TTL_MS_PROPERTY,  CACHE_TTL_MS_DEFAULT);
        logger.info(String.format("Initialized SPNEGO token cache with TTL of %s ms", ttl));
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
                .build();
    }
    
    public Token getToken(URL url)
    {
        logger.debug("Checking for cached token for host : " + url.getHost());
        Token token = getFromCache(url);
        if (isExpired(token))
        {
            cache.invalidate(url.getHost());
            return getFromCache(url);
        }
        else
        {
            return token;
        }
    }
    
    private boolean isExpired(Token token)
    {
        StringTokenizer st = new StringTokenizer(token.toString(), ATTR_SEPARATOR);
        while (st.hasMoreTokens()) {
            String part = st.nextToken();
            int separator = part.indexOf('=');
            if (separator == -1) {
                // if the token string is invalid, return true so we invalidate from 
                // cache and try to get a new token
                return true;
            }
            if ( part.substring(0, separator).equals(EXPIRES_ATTR) )
            {
                long expires = Long.parseLong(part.substring(separator + 1));
                return expires != -1 && System.currentTimeMillis() > expires;
            }
        }
        // strange, the token appeared to contain no expiry information
        // this should not happen, so something is up. lets return true, 
        // expire this token and try to grab a new one
        return true;
    }

    private Token getFromCache(final URL url)
    {
        try
        {
            Token token = cache.get(url.getHost(), new Callable<Token>(){
                @Override
                public Token call() throws IOException, AuthenticationException
                {
                    logger.debug("No token found for host, obtaining new one via AuthenticatedURL");
                    AuthenticatedURL authenticatedUrl = urlProvider.get();
                    Token token = new AuthenticatedURL.Token();
                    return authenticatedUrl.authenticateWithToken(url, token);
                }
            });
            return token;
        }
        catch(ExecutionException e)
        {
            logger.debug("Error performing HTTP Authentication for Solr client request", e.getCause());
            throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, e.getCause().getMessage(), e.getCause());
        }
    }
    
}
