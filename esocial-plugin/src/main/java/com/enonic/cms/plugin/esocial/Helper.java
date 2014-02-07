package com.enonic.cms.plugin.esocial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Common used helper methods.
 */
public final class Helper
{
    /**
     * Pretty print document.
     */
    public static void prettyPrint( final Document doc )
        throws IOException
    {
        prettyPrint( doc, System.out );
    }

    /**
     * Pretty print document.
     */
    public static void prettyPrint( final Document doc, final OutputStream out )
        throws IOException
    {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat( Format.getPrettyFormat() );
        outputter.output( doc, out );
    }

    /**
     * Copy input to output. Return number of bytes copied.
     */
    public static int copy( final InputStream in, final OutputStream out )
        throws IOException
    {
        int copied = 0;
        byte[] buffer = new byte[1024];

        while ( true )
        {
            int num = in.read( buffer );
            if ( num > 0 )
            {
                out.write( buffer, 0, num );
                copied += num;
            }
            else
            {
                break;
            }
        }

        return copied;
    }

    /**
     * Parse xml document.
     */
    public static Document parseXml( final InputStream in )
        throws JDOMException, IOException
    {
        SAXBuilder builder = new SAXBuilder();
        return builder.build( in );
    }

    /**
     * Copy to bytes.
     */
    public static byte[] copyToBytes( final InputStream in )
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy( in, out );
        return out.toByteArray();
    }

    /**
     * Load text file.
     */
    public static String copyToString( final InputStream in )
        throws IOException
    {
        byte[] bytes = copyToBytes( in );
        return new String( bytes, "UTF-8" );
    }
}
