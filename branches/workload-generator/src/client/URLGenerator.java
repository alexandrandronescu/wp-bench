package client;

import java.net.URL;

public abstract class URLGenerator {
	protected static final String protocol = "http";
	protected String webSiteName;
	protected int webSitePort;
	protected String scriptPath;

	public URLGenerator(String host, int port, String ScriptFilesPath) {
		webSiteName = host;
		scriptPath = ScriptFilesPath;
		webSitePort = port;
	}

	// /////////////// SETTERS ///////////////////
	public void setWebSiteName(String host) {
		webSiteName = host;
	}

	public void setScriptPath(String p) {
		scriptPath = p;
	}

	public void setWebSitePort(int p) {
		webSitePort = p;
	}

	// /////////////// END SETTERS ///////////////////
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
