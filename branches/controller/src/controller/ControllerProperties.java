package controller;

import java.io.*;
import java.util.*;

/**
 * Loads properties from the configuration file.
 * 
 * Types of properties:
 * <pre>
 * slavesNb			: start simulation of the user sessions
 * port				: stop simulation of the user sessions
 * usersNb			: retrieve statistics log
 * readWrite		: add more user sessions
 * logName			: create more user accounts
 * slavesAddresses	: exit program
 * </pre>
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a> 
 * @version 1.0
 */
public class ControllerProperties {
	private static Properties configuration = null;
	private int slavesNb;
	private int port;
	private int usersNb = 0;
	private double readWrite;
	private String logName;
	private String[] slavesAddresses;
	
	public ControllerProperties ( String propsFile ) {
		System.out.println("Loading properties from " + propsFile);
		
		try {
			configuration = new Properties();
			configuration.load(new FileReader(propsFile));
		}
		catch (IOException e) {
			e.printStackTrace();
			Runtime.getRuntime().exit(1);
		}
	}
	
	protected String getProperty ( String property ) {
		String s = configuration.getProperty(property);
	    return s;
	}
	
	public void checkPropertiesFile() throws Exception {
		String addresses = null;
		StringTokenizer st = null;
		int index = 0;
		try {
			slavesNb = Integer.parseInt(getProperty("slavesNb"));
			port = Integer.parseInt(getProperty("port"));
			usersNb = Integer.parseInt(getProperty("usersNb"));
			readWrite = Double.parseDouble(getProperty("readWrite"));
			logName = getProperty("logName");
			slavesAddresses = new String[slavesNb];
			addresses = getProperty("slavesAddresses");
			st = new StringTokenizer(addresses, " ");
			while (st.hasMoreTokens())
				slavesAddresses[index++] = st.nextToken();
			
		} catch (Exception e) {
			throw e;
		}
	}

	public int getSlavesNb() {
		return slavesNb;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getUsersNb() {
		return usersNb;
	}
	
	public double getReadWrite() {
		return readWrite;
	}
	
	public String getLogName() {
		return logName;
	}
	
	public String getAddress(int i) {
		if ( i < slavesNb )
			return slavesAddresses[i];
		else
			return "0";
	}
}
