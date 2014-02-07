package com.enonic.cms.plugin.esocial;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import com.enonic.cms.api.plugin.ext.http.HttpAutoLogin;


/**
 * 1) When setting fb_uid, calculate it as follows:
 * a) concat "seed" + "uid" where "seed" is configured in properties (a secret key, eg. "myfacebook123").
 * b) create a hash of the result (eg. sha256) and store it in fb_hash cookie.
 * c) store uid (user name) in plain text in fb_uid cookie.
 * d) example: fb_uid = 123, fb_hash = sha256("myfacebook123_123")
 * <p/>
 * 2) in FacebookAutoLogin do the following:
 * a) Read fb_uid (example, 123) from cookie.
 * b) Read fb_hash (example, 432y4872dsiuyfidyi4y23i7y42) from cookie.
 * c) Calcluate fb_hash from fb_uid as we did in 1 a) and b).
 * d) if the result of c) is identical to fb_hash, then login using fb_uid
 */


public final class FacebookAutoLogin
    extends HttpAutoLogin
{
    private final static Logger LOG = LoggerFactory.getLogger( FacebookAutoLogin.class );

    private String fb_uid;

    private String fb_hash;

    private String hashseed;

    public void setHashseed( String hashseed )
    {
        this.hashseed = hashseed;
    }

    //TODO: Implement security so user cannot just set other facebook user cookie id and log in as them!
    public String getAuthenticatedUser( final HttpServletRequest req )
        throws Exception
    {

        //When this header is set we already made sure user exists in facebook userstore
        try
        {
            final Cookie[] cookies = req.getCookies();
            if ( cookies != null )
            {
                for ( final Cookie cookie : cookies )
                {
                    if ( "fb_uid".equals( cookie.getName() ) )
                    {
                        fb_uid = cookie.getValue();
                    }
                    if ( "fb_hash".equals( cookie.getName() ) )
                    {
                        fb_hash = cookie.getValue();
                    }
                    if ( !Strings.isNullOrEmpty( fb_uid ) && !Strings.isNullOrEmpty( fb_hash ) )
                    {
                        break;
                    }
                }
            }
        }
        catch ( final Exception e )
        {
            LOG.info( "Exception while doing unsafe operations to get code and sessionState", e );
        }

        if ( Strings.isNullOrEmpty( fb_uid ) || Strings.isNullOrEmpty( fb_hash ) )
        {
            return null;
        }

        if ( isSecurityCheckOk( fb_uid, fb_hash ) )

        {
            LOG.info( "FacebookAutoLogin. Logging in as #2:{}", fb_uid );
        }
        return "#2:" + fb_uid;
    }

    private boolean isSecurityCheckOk( String uid, String hashedUid )
    {
        String controlUid = Hashing.sha256().hashString( uid + "_" + hashseed ).toString();
        if ( controlUid.equals( hashedUid ) )
        {
            return true;
        }
        else
        {
            LOG.info( "Security check failed {} != {}", controlUid, hashedUid );
        }

        return false;
    }
}
