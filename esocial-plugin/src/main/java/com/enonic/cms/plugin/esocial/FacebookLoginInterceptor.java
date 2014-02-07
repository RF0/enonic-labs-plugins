package com.enonic.cms.plugin.esocial;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;

import com.enonic.cms.api.client.Client;
import com.enonic.cms.api.client.ClientException;
import com.enonic.cms.api.client.model.CreateUserParams;
import com.enonic.cms.api.client.model.GetUserParams;
import com.enonic.cms.api.client.model.JoinGroupsParams;
import com.enonic.cms.api.client.model.user.UserInfo;
import com.enonic.cms.api.plugin.PluginConfig;
import com.enonic.cms.api.plugin.ext.http.HttpInterceptor;

/**
 * User: rfo
 * Date: 12/17/12
 * Time: 3:55 PM
 */

//TODO:Review best practices: https://developers.facebook.com/docs/facebook-login/checklist/
//TODO: Add error handling, see error codes here: https://developers.facebook.com/docs/graph-api/using-graph-api/#errors
public final class FacebookLoginInterceptor
    extends HttpInterceptor
{
    private final static Logger LOG = LoggerFactory.getLogger( FacebookLoginInterceptor.class );

    private Client client;

    private String fbOauthUrl;

    private String fbGraphUrl;

    private String fbAccessTokenUrl;

    private String fbDeleteAccessTokenUrl;

    private String fbAppId;

    private String fbAppSecret;

    private String fbLoginUrl;

    private String fbRedirectAfterLogin;

    private String fbRedirectAfterLogout;

    private String hashseed;

    public void setConfig( final PluginConfig config )
    {
        fbOauthUrl = config.getString( "fbOauthUrl" );
        fbGraphUrl = config.getString( "fbGraphUrl" );
        fbDeleteAccessTokenUrl = config.getString( "fbDeleteAccessTokenUrl" );
        fbAccessTokenUrl = config.getString( "fbAccessTokenUrl" );
        hashseed = config.getString( "hashseed" );
    }

    public void setClient( final Client client )
    {
        this.client = client;
    }

    public final void setFbLoginUrl( final String value )
    {
        this.fbLoginUrl = value;
    }

    public final void setFbRedirectAfterLogin( final String value )
    {
        this.fbRedirectAfterLogin = value;
    }

    public final void setFbRedirectAfterLogout( final String value )
    {
        this.fbRedirectAfterLogout = value;
    }

    public final void setFbAppId( final String value )
    {
        this.fbAppId = value;
    }

    public final void setFbAppSecret( final String value )
    {
        this.fbAppSecret = value;
    }

    //https://developers.facebook.com/docs/howtos/login/server-side-login/#step1
    @Override
    public boolean preHandle( final HttpServletRequest req, final HttpServletResponse res )
        throws Exception
    {
        final String code = getParamOrCookieValue( req, "code", "fb_code" );
        if ( Strings.isNullOrEmpty( code ) )
        {
            redirectToLoginDialog( res );
            return false;
        }

        final String sessionState = getParamOrCookieValue( req, "state", "fb_sessionState" );
        if ( Strings.isNullOrEmpty( sessionState ) )
        {
            return false;
        }

        if ( req.getParameter( "signout" ) != null )
        {
            this.client.logout();
            deleteSessionCookies( res );
            res.sendRedirect( this.fbRedirectAfterLogout );
            return false;
        }

        String accessToken = readAccessToken( code );
        if ( Strings.isNullOrEmpty( accessToken ) )
        {
            redirectToLoginDialog( res );
            return false;
        }

        final String basicUserInfo = getBasicUserInfo( accessToken );
        LOG.info( basicUserInfo );

        final FacebookUser fbUser = deserializeFacebookUser( basicUserInfo );

        final String userName = fbUser.getId();
        final boolean userExists = doesUserExist( userName );
        if ( userExists )
        {
            setAuthenticationCookies( res, code, sessionState, userName );
            res.sendRedirect( this.fbRedirectAfterLogin );
            return false;
        }

        // TODO: Remove "#1:" since it's probably in default userstore
        this.client.impersonate( "#1:esocial" );

        final CreateUserParams createUserParams = new CreateUserParams();
        UserInfo userInfo = new UserInfo();
        createUserParams.username = fbUser.getId();
        createUserParams.displayName = fbUser.getName();
        createUserParams.password = createPasswordHash( fbUser );
        createUserParams.userstore = "facebook";
        createUserParams.email = fbUser.getUserName() + "@facebook.com";

        userInfo.setFirstName( fbUser.getFirstName() );
        userInfo.setLastName( fbUser.getLastName() );
        userInfo.setNickName( fbUser.getUserName() );
        userInfo.setPersonalId( fbUser.getId() );

        createUserParams.userInfo = userInfo;

        this.client.createUser( createUserParams );

        final JoinGroupsParams joinGroupsParams = new JoinGroupsParams();
        //joinGroupsParams.group = "#2:Facebook";
        joinGroupsParams.groupsToJoin = new String[]{"#2:Facebook"};
        joinGroupsParams.user = "#2:" + userName;

        this.client.joinGroups( joinGroupsParams );
        setAuthenticationCookies( res, code, sessionState, userName );
        res.sendRedirect( this.fbRedirectAfterLogin );

        return false;
    }

    private void setAuthenticationCookies( final HttpServletResponse res, final String code, final String sessionState,
                                           final String userName )
    {
        setCookie( res, "fb_sessionState", sessionState, -1 );
        setCookie( res, "fb_code", code, -1 );
        setCookie( res, "fb_uid", userName, -1 );
        setCookie( res, "fb_hash", createSeedHash( userName ), -1 );
    }

    private String createSeedHash( String stringToHash )
    {
        return Hashing.sha256().hashString( stringToHash + "_" + hashseed ).toString();
    }

    private FacebookUser deserializeFacebookUser( final String info )
        throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        return mapper.readValue( info, FacebookUser.class );
    }

    private String createPasswordHash( final FacebookUser user )
    {
        final String id = user.getId();
        return Hashing.sha256().hashString( id ).toString();
    }

    private String readAccessToken( final String code )
    {
        final String url = fbAccessTokenUrl +
            "?client_id=" + fbAppId +
            "&redirect_uri=" + fbLoginUrl +
            "&client_secret=" + fbAppSecret +
            "&code=" + code;

        LOG.info( "read string from url " + url );

        String accessToken = null;

        try
        {
            final String stringFromUrl = readStringFromUrl( url );
            final String split1 = stringFromUrl.split( "access_token=" )[1];
            accessToken = split1.split( "&expires=" )[0];

            // TODO: Add handling of token expiration
            // https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/#token
            // expires = split1.split( "&expires=" )[1];
        }
        catch ( Exception e )
        {
            LOG.warn( "Exception when reading response from oauth api", e );
        }

        return accessToken;
    }

    private String getParamOrCookieValue( final HttpServletRequest req, final String paramName, final String cookieName )
    {
        if ( req.getParameter( paramName ) != null )
        {
            return req.getParameter( paramName );
        }

        return getCookieValue( req, cookieName );
    }

    private String getCookieValue( final HttpServletRequest req, final String name )
    {
        final Cookie[] cookies = req.getCookies();
        if ( cookies == null )
        {
            return null;
        }

        for ( final Cookie cookie : cookies )
        {
            if ( cookie.getName().equals( name ) )
            {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void deleteSessionCookies( final HttpServletResponse res )
    {
        setCookie( res, "fb_sessionState", "", 0 );
        setCookie( res, "fb_code", "", 0 );
        setCookie( res, "fb_uid", "", 0 );
    }

    //This will delete access token in users fb profile, we usually don't want to do this.
    private void deleteAccessToken( final String access_token )
        throws Exception
    {
        readStringFromUrl( this.fbDeleteAccessTokenUrl + access_token );
    }

    private void setCookie( final HttpServletResponse res, final String name, final String value, final int maxAge )
    {
        final Cookie cookie = new Cookie( name, value );
        cookie.setMaxAge( maxAge );
        res.addCookie( cookie );
    }

    private boolean doesUserExist( final String userName )
    {
        try
        {
            final GetUserParams getUserParams = new GetUserParams();
            getUserParams.includeCustomUserFields = false;
            getUserParams.includeMemberships = false;
            getUserParams.normalizeGroups = false;

            // TODO: Should replce "2:" with "facebook:" (name of userstore)
            getUserParams.user = "2:" + userName;

            Document userDocument = this.client.getUser( getUserParams );
            Helper.prettyPrint( userDocument );
            LOG.info( "User {} exists!", userName );

            return true;
        }
        catch ( ClientException ce )
        {
            LOG.warn( "ClientException while fetching fb user, user may not exist {}", ce );
        }
        catch ( IOException ioe )
        {
            LOG.warn( "IOException while printing out user document {}", ioe );
        }

        return false;
    }

    private void redirectToLoginDialog( final HttpServletResponse res )
        throws Exception
    {
        final String hash = Hashing.md5().hashString( "aWl0cnVzdGl0" ).toString();
        res.sendRedirect( this.fbOauthUrl + "?client_id=" + this.fbAppId +
                              "&redirect_uri=" + this.fbLoginUrl +
                              "&state=" + hash );
    }

    //TODO: Add support for reading facebook graph api error messages, see fb_messages_examples.txt
    private String readStringFromUrl( final String url )
        throws IOException
    {
        return Resources.toString( new URL( url ), Charsets.UTF_8 );
    }

    private String getBasicUserInfo( final String access_token )
        throws Exception
    {
        final String url = this.fbGraphUrl + "?access_token=" + access_token;
        return readStringFromUrl( url );
    }

    @Override
    public void postHandle( final HttpServletRequest req, final HttpServletResponse res )
        throws Exception
    {
        // Do nothing
    }
}

