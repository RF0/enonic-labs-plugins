package com.enonic.cms.plugin.esocial;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Facebook userstore
 * <config>
 * <user-fields>
 * <first-name readonly="false" remote="false" required="false" />
 * <gender readonly="false" remote="false" required="false" />
 * <home-page readonly="false" remote="false" required="false" />
 * <last-name readonly="false" remote="false" required="false" />
 * <locale readonly="false" remote="false" required="false" />
 * <member-id readonly="false" remote="false" required="false" />
 * <nick-name readonly="false" remote="false" required="false" />
 * <personal-id readonly="false" remote="false" required="true" />
 * <photo readonly="false" remote="false" required="false" />
 * <time-zone readonly="false" remote="false" required="false" />
 * </user-fields>
 * </config>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FacebookUser
{
    private String id;

    private String name;

    private String firstName;

    private String lastName;

    private String userName;

    private String link;

    private String gender;

    private String timeZone;

    private String locale;

    private String verified;

    private String updatedTime;

    private String bio;

    private String quotes;

    @JsonProperty("quotes")
    public String getQuotes()
    {
        return quotes;
    }

    public void setQuotes( final String quotes )
    {
        this.quotes = quotes;
    }

    @JsonProperty("bio")
    public String getBio()
    {
        return bio;
    }

    public void setBio( final String bio )
    {
        this.bio = bio;
    }

    @JsonProperty("updated_time")
    public String getUpdatedTime()
    {
        return updatedTime;
    }

    public void setUpdatedTime( final String updatedTime )
    {
        this.updatedTime = updatedTime;
    }

    @JsonProperty("link")
    public String getLink()
    {
        return link;
    }

    public void setLink( final String link )
    {
        this.link = link;
    }

    @JsonProperty("gender")
    public String getGender()
    {
        return gender;
    }

    public void setGender( final String gender )
    {
        this.gender = gender;
    }

    @JsonProperty("timezone")
    public String getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone( final String timeZone )
    {
        this.timeZone = timeZone;
    }

    @JsonProperty("locale")
    public String getLocale()
    {
        return locale;
    }

    public void setLocale( final String locale )
    {
        this.locale = locale;
    }

    @JsonProperty("verified")
    public String getVerified()
    {
        return verified;
    }

    public void setVerified( final String verified )
    {
        this.verified = verified;
    }

    @JsonProperty("id")
    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    @JsonProperty("first_name")
    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName( final String firstName )
    {
        this.firstName = firstName;
    }

    @JsonProperty("last_name")
    public String getLastName()
    {
        return lastName;
    }

    public void setLastName( final String lastName )
    {
        this.lastName = lastName;
    }

    @JsonProperty("username")
    public String getUserName()
    {
        return userName;
    }

    public void setUserName( final String userName )
    {
        this.userName = userName;
    }
}
