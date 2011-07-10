package client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

//import org.apache.log4j.Logger;

import sun.net.www.protocol.http.HttpURLConnection;

public abstract class UserSession extends Thread {
	// session
	protected String lastHTMLReply = null; // last HTML reply received from
	protected Random rand = new Random(); // random number generator
	protected URL lastURL = null; // Last accessed URL
	protected int state;
	
	protected URLGenerator urlGen = null; // URL generator corresponding to the
	
	protected int userId; // User id for the current session
	protected String username = null; // User name for the current session
	protected String password = null; // User password for the current session
	
	protected Hashtable<String, HttpCookie> cookies;
	
	public UserSession(String name) {
		super(name);
	}
	
	/**
	 * Call the HTTP Server according to the given URL and get the reply
	 * 
	 * @param url
	 *            URL to access
	 * @return <code>String</code> containing the web server reply (HTML file)
	 * @throws UnsupportedEncodingException 
	 */
	protected String callHTTPServer(URL url) throws UnsupportedEncodingException {
		return callHTTPServer(url, null);
	}
	
	protected void resetCookies() {
		cookies = null;
	}

	protected String callHTTPServer(URL url, Hashtable<String, String> params) throws UnsupportedEncodingException {
//		if(params != null)
//			System.out.println("Going to request " + url.toString() + ":" + params.toString());
//		else
//			System.out.println(url.toString() + ":{}");
		String HTMLReply = "";
		String postData = "";
		HttpURLConnection connection = null;
		BufferedInputStream in = null;
		PrintWriter out = null;
		int retry = 0;
		boolean first = true;
		
		// Set POST parameters if present
		if (params != null) {
			Enumeration<String> keys = params.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				postData += (first ? (key) : "&" + key) + "="
						+ URLEncoder.encode(params.get(key), "UTF-8");
				if (first)
					first = false;
			}
		}

		while (retry < 5) {
			try {
				// Open the connection
				HttpURLConnection.setFollowRedirects(false);
				connection = (HttpURLConnection) url.openConnection();
				if(params != null)
					connection.setRequestMethod("POST");
				else
					connection.setRequestMethod("GET");
				
				// Send cookies if present
				if(cookies != null) {
					String cookie = "";
					Set<Map.Entry<String,HttpCookie>> cookieSet = cookies.entrySet();
					Iterator<Map.Entry<String,HttpCookie>> j = cookieSet.iterator();
					int size = cookieSet.size();
					while(j.hasNext()) {
						Map.Entry<String,HttpCookie> mapping = j.next();
						cookie += mapping.getKey() + "=" + mapping.getValue().getValue();
						size--;
						if(size != 0)
							cookie += "; ";
					}
					connection.setRequestProperty("Cookie", cookie);
					System.out.println("Sending COOKIE: " + cookie);
				}
				connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
				connection.setRequestProperty("Accept","*/*");
				// Send Request
				if (params != null) {
					connection.setDoOutput(true);
					connection.setDoInput(true);
					out = new PrintWriter(connection.getOutputStream());
					out.write(postData);
					out.close();
				} else {
					connection.setDoOutput(false);
					connection.setDoInput(true);
				}
				
				// Response received! 
				// Get headers
				for(int i = 0; i < connection.getHeaderFields().size(); i++) {
					String header = connection.getHeaderField(i);
					if(connection.getHeaderFieldKey(i) == null || !connection.getHeaderFieldKey(i).equalsIgnoreCase("Set-Cookie"))
						continue;
					if(header != null) {
						header = header.replaceAll(" httponly", "");
					}
					if(header != null && cookies != null) {
						for(HttpCookie cookie : HttpCookie.parse(header))
							if(!cookies.containsKey(cookie.getName()))
								cookies.put(cookie.getName(), cookie);
					}
					else if(header != null) {
						 List<HttpCookie> cookiesList = HttpCookie.parse(header);
						 Iterator<HttpCookie> j = cookiesList.iterator();
						 if(cookies==null)
							 cookies = new Hashtable<String,HttpCookie>();
						 while(j!=null && j.hasNext()) {
							 HttpCookie cookie = j.next();
							 if(cookie!=null && !cookies.containsKey(cookie.getName()))
									cookies.put(cookie.getName(), cookie);
						 }
					}
				}
				//System.out.println("Receiving COOKIEs: " + cookies);
//				if(connection.getHeaderFields().size() != 0)
//					System.out.println(connection.getHeaderFields());
				
				System.out.println("Response code: " + connection.getResponseCode());
				if(connection.getResponseCode() == 403) {
					try {
						System.out.println("Response Message:" + connection.getResponseMessage());
						Thread.sleep(8000);
					} catch (InterruptedException e) {
					}
				}
				if(connection.getResponseCode() == 404) {
						System.out.println("Response Message:" + connection.getResponseMessage());
				}
				if(connection.getResponseCode() == 302) {
					System.out.println("Response Message:" + connection.getResponseMessage());
					System.out.println("Redirect From: " + url.toString());
					System.out.print("Redirect To: " + connection.getHeaderField("Location"));
					
					if((url.toString()).compareTo(connection.getHeaderField("Location")) != 0) {
						System.out.println(" ------ Redirect!");
						try {
							Thread.sleep(8000);
						} catch (InterruptedException e) {
						}
						return callHTTPServer(new URL(connection.getHeaderField("Location")), params);
					}
				}
				in = new BufferedInputStream(connection.getInputStream(), 4096);
				
			} catch (IOException ioe) {
				retry++;
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException i) {
					return null;
				}
				continue;
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}

			// Get the data
			try {
				byte[] buffer = new byte[4096];
				int read;
				
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					if (read > 0)
						HTMLReply = HTMLReply + new String(buffer, 0, read);
				}
			} catch (IOException ioe) {
				return null;
			}
			// No retry at this point
			break;
		}
		try {
			if (in != null)
				in.close();
		} catch (IOException ioe) {
		}
		if (retry == 5)
			return null;

		// Look for any image to download
		Vector<String> images = new Vector<String>();
		int index = HTMLReply.indexOf("<IMG SRC=\"");
		while (index != -1) {
			int startQuote = index + 10; // 10 equals to length of <IMG SRC"
			int endQuote = HTMLReply.indexOf("\"", startQuote + 1);
			images.add(HTMLReply.substring(startQuote, endQuote));
			index = HTMLReply.indexOf("<IMG SRC=\"", endQuote);
		}

		// Download all images
		byte[] buffer = new byte[4096];
		while (images.size() > 0) {
			URL imageURL = urlGen.getScriptPath((String) images.elementAt(0));
			try {
				BufferedInputStream inImage = new BufferedInputStream(imageURL
						.openStream(), 4096);
				while (inImage.read(buffer, 0, buffer.length) != -1)
					; // Just download, skip data
				inImage.close();
			} catch (IOException ioe) {
			}
			images.removeElementAt(0);
		}
		
//		System.out.println(HTMLReply);
		index = HTMLReply.indexOf("MASSIVE FAILURE");
		if(index != -1) {
			System.err.println("ERROR in " + url + " " + HTMLReply.substring(index, index + "MASSIVE FAILURE".length() + 2));
			return null;
		}
		return HTMLReply;
	}
}
