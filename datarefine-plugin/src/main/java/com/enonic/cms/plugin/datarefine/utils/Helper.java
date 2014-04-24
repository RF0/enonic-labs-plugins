package com.enonic.cms.plugin.datarefine.utils;

import com.enonic.cms.plugin.datarefine.DatarefineControllerPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * User: rfo
 * Date: 10/10/13
 * Time: 3:32 PM
 */
public class Helper {

    public static void serveCss(String css, HttpServletResponse response) throws Exception{
        InputStream in = DatarefineControllerPlugin.class.getResourceAsStream(css);
        response.setContentType("text/css");
        Helper.stream(in,response.getOutputStream());
    }


    public static long stream(InputStream input, OutputStream output) throws IOException {
        ReadableByteChannel inputChannel = null;
        WritableByteChannel outputChannel = null;

        try {
            inputChannel = Channels.newChannel(input);
            outputChannel = Channels.newChannel(output);
            ByteBuffer buffer = ByteBuffer.allocate(10240);
            long size = 0;

            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                size += outputChannel.write(buffer);
                buffer.clear();
            }

            return size;
        }
        finally {
            if (outputChannel != null) try { outputChannel.close(); } catch (IOException ignore) { /**/ }
            if (inputChannel != null) try { inputChannel.close(); } catch (IOException ignore) { /**/ }
        }
    }

    /**
    	 * Pretty print document.
    	 */
    	public static void prettyPrint(Document doc)
    		throws IOException
    	{
    		prettyPrint(doc, System.out);
    	}

    	/**
    	 * Pretty print document.
    	 */
    	public static void prettyPrint(Document doc, OutputStream out)
    		throws IOException
    	{
    		XMLOutputter outputter = new XMLOutputter();
    		outputter.setFormat(Format.getPrettyFormat());
    		outputter.output(doc, out);
    	}

        public static void prettyPrint(Element el) throws IOException {
            prettyPrint(el, System.out);
        }

        public static void prettyPrint(Element el, OutputStream out) throws IOException {
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            outputter.output(el, out);
        }

    	/**
    	 * Copy input to output. Return number of bytes copied.
    	 */
    	public static int copy(InputStream in, OutputStream out)
    		throws IOException
    	{
    		int copied = 0;
    		byte[] buffer = new byte[1024];

    		while (true) {
    			int num = in.read(buffer);
    			if (num > 0) {
    				out.write(buffer, 0, num);
    				copied += num;
    			} else {
    				break;
    			}
    		}

    		return copied;
    	}

    	/**
    	 * Parse xml document.
    	 */
    	public static Document parseXml(InputStream in)
    		throws JDOMException, IOException
    	{
    		SAXBuilder builder = new SAXBuilder();
    		return builder.build(in);
    	}

    	/**
    	 * Copy to bytes.
    	 */
    	public static byte[] copyToBytes(InputStream in)
    		throws IOException
    	{
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		copy(in, out);
    		return out.toByteArray();
    	}

    	/**
    	 * Load text file.
    	 */
    	public static String copyToString(InputStream in)
    		throws IOException
    	{
    		byte[] bytes = copyToBytes(in);
    		return new String(bytes, "UTF-8");
    	}

}
