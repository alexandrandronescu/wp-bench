package worker;

import java.net.URL;

/**
 * Class for creating basic URLs.
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a>
 */
public abstract class URLGenerator {
	protected static final String protocol = "http";
	protected String webSiteName;
	protected int webSitePort;
	protected String scriptPath;

	/**
	 * Creates a new <code>URLGenerator</code> instance.
	 *
	 * @param host WordPress Web server name
	 * @param port WordPress Web server connection port
	 * @param ScriptFilesPath WordPress Web server path
	 */
	public URLGenerator(String host, int port, String ScriptFilesPath) {
		webSiteName = host;
		scriptPath = ScriptFilesPath;
		webSitePort = port;
	}

	public void setWebSiteName(String host) {
		webSiteName = host;
	}

	public void setScriptPath(String p) {
		scriptPath = p;
	}

	public void setWebSitePort(int p) {
		webSitePort = p;
	}

	public URL getScriptPath(String s) {
		return createURL(s);
	}
	
	public URL createURL(String path) {
		try {
			URL url = new URL(protocol, webSiteName, webSitePort, scriptPath
					+ "/" + path);
			return url;
		} catch (java.net.MalformedURLException e) {
			System.out.println("Error while generating script path: "
					+ e.getMessage());
			return null;
		}
	}
}
