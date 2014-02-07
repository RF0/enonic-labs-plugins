package com.enonic.cms.plugin.esocial;

import java.io.IOException;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.api.client.Client;
import com.enonic.cms.api.client.ClientException;
import com.enonic.cms.api.client.model.GetUserParams;
import com.enonic.cms.api.plugin.ext.FunctionLibrary;

/**
 * User: rfo
 * Date: 11/22/13
 * Time: 12:21 AM
 */
public final class FacebookFunctionLibrary
    extends FunctionLibrary
{
    private final static Logger LOG = LoggerFactory.getLogger( FacebookLoginInterceptor.class );

    private Client client;

    public void setClient( Client client )
    {
        this.client = client;
    }

    public Document getUser()
    {
        try
        {
            final String username = client.getUserName();

            // TODO: Should try to use client.getUserContext()
            final GetUserParams getUserParams = new GetUserParams();
            getUserParams.includeCustomUserFields = true;
            getUserParams.includeMemberships = true;
            getUserParams.normalizeGroups = false;

            // TODO: Replace "2:" with "facebook:"
            getUserParams.user = "2:" + username;

            final Document userDocument = client.getUser( getUserParams );
            Helper.prettyPrint( userDocument );

            return userDocument;
        }
        catch ( ClientException ce )
        {
            LOG.warn( "ClientException while fetching fb user, user may not exist {}", ce );
        }
        catch ( IOException ioe )
        {
            LOG.warn( "IOException while printing out user document {}", ioe );
        }

        return null;
    }
}
