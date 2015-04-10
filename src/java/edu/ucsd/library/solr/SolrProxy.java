package edu.ucsd.library.solr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

// servlet
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// httpclient
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

// logging
import org.apache.log4j.Logger;


/**
 * Servlet to proxy Lucene/Solr searches for client-side Javascript.
 * @author escowles
 * @author vchu 
**/
public class SolrProxy extends HttpServlet
{
	private static Logger log = Logger.getLogger( SolrProxy.class );
	private static final String defaultEncoding = "UTF-8";

	private String solrBase = null;

	public void init( ServletConfig config ) throws ServletException
	{
		try
		{   
			InitialContext ctx = new InitialContext();
			String prefix = "java:comp/env/";			
			solrBase = (String)ctx.lookup( prefix + "solrBase" );		
		}
		catch ( Exception ex )
		{
			log.error( "Error looking up Solr base from JDNI", ex );
		}
		super.init( config );
	}
	public void doGet( HttpServletRequest req, HttpServletResponse res )
	{
		long start = System.currentTimeMillis();

		// make sure char encoding is unicode by default
		if ( req.getCharacterEncoding() == null )
		{
			try
			{
				req.setCharacterEncoding( defaultEncoding );
				log.debug("Setting character encoding: " + defaultEncoding );
			}
			catch ( UnsupportedEncodingException ex )
			{
				log.warn("Unable to set chararacter encoding");
			}
		}
		else
		{
			log.debug("Browser specified character encoding: " + req.getCharacterEncoding() );
		}
		
		// build URL
		String url = solrBase + "select?" + req.getQueryString();
		
		System.out.println("hey ======= url:"+url);
		log.info("url: " + url);

		// perform search
		try
		{
			// IOException here with message like "Http error connecting to ... 400 Bad Request" when malformed search (like single quote)
			String output = null;
			GetMethod method = null;
			String contentType = null;
			try
			{
				StringBuffer response = new StringBuffer();
				HttpClient client = new HttpClient();
				method = new GetMethod(url);
				int status = client.executeMethod(method);
				if ( status == HttpStatus.SC_OK )
				{
					InputStream is = method.getResponseBodyAsStream();
					if ( is != null )
					{
						BufferedReader buf = new BufferedReader(
							new InputStreamReader(is)
						);
						String line = null;
						while ( (line=buf.readLine()) != null )
						{
							response.append( line + "\n" ); // ZZZ: add cr
						}
					}
				}
				output = response.toString();
				contentType = method.getResponseHeader("Content-Type").toString();
				System.out.println("contentTYpe:"+contentType);
				if ( contentType == null )
				{
					contentType = "text/html";
				}
			}
			catch ( IOException ex )
			{
				log.warn( "Error performing Solr search, url: " + url, ex );
				String err = ex.getMessage();
				if ( err.endsWith("400 Bad Request") )
				{
					output = "{\"error\":\"Parsing error, please revise your query and try again.\"}";
					contentType = "application/json";
				}
			}
			finally
			{
				if ( method != null )
				{
					method.releaseConnection(); // ZZZ: release connection
				}
			}

			// check for null xml
			if ( output == null )
			{
				output = "{\"error\":\"Processing error, please revise your query and try again\"}";
				contentType = "application/json";
			}

			if ( contentType.indexOf("; charset=") == -1 )
			{
				contentType += "; charset=" + defaultEncoding;
			}

			// override content type
			if ( req.getParameter("contentType") != null )
			{
				contentType= req.getParameter("contentType");
			}

			// send output to client
			res.setContentType( contentType );
			PrintWriter out = res.getWriter();
			out.println( output );
			out.flush();
			out.close();
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
			try
			{
				res.sendError( res.SC_INTERNAL_SERVER_ERROR, ex.toString() );
			}
			catch ( Exception e2 )
			{
			}
		}
		long dur = System.currentTimeMillis() - start;
		log.info("SolrProxy dur: " + dur + ", params: " + req.getQueryString());
	}

	
}
