 package worker;

import java.io.IOException;
import java.lang.Runtime;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import worker.Dictionary;
import worker.Stats;
import worker.TransitionTable;


/**
 * RUBBoS client emulator. 
 * This class plays random user sessions emulating a Web browser.
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class ClientEmulator
{
	private WordpressProperties properties = null;        // access to rubbos.properties file
	private URLGenerator		urlGen = null;        // URL generator corresponding to the version to be used (PHP, EJB or Servlets)
	private TransitionTable	transitionTableLoggedOut = null;
	private TransitionTable	transitionTableLoggedIn = null;
	private TransitionTable	transitionTableReadOnly = null;
	private LogDaemon logDaemon = null;
	private Stats				stats = null;
	private static float		slowdownFactor = 0;
	public static volatile boolean  endOfSimulation = false;
	public int nbOfUsers = 0;
	private int JOIN_TIMEOUT = 15000; 
	
	public Vector<String> usersCredentials;
	//public Vector<String> runningUsers;
	public Vector<WordpressUserSession>  sessions;

	public static final int COMMAND_START_SIMULATION = 0;
	public static final int COMMAND_END_SIMULATION = 1;
	public static final int COMMAND_GET_LOG = 2;
	public static final int COMMAND_ADD_USERS = 3;
	public static final int COMMAND_CREATE_USERS = 4;
	public static final int COMMAND_EXIT = 5;
	/**
	 * Creates a new <code>ClientEmulator</code> instance.
	 * The program is stopped on any error reading the configuration files.
	 */
	public ClientEmulator(String propsFile)
	{
		// Initialization, check that all files are ok
		properties = new WordpressProperties(propsFile);
		try {
			properties.checkPropertiesFile();
		}catch (Exception e) {
			System.exit(1);
		}
		urlGen = new WordpressURLGenerator(properties.getWebServerName(), properties.getWebsitePort(), properties.getScriptPath());
		if (urlGen == null)
			Runtime.getRuntime().exit(1);
		stats = new Stats(properties.getNumberOfStates(), properties.getLogFile());
		// Check that the transition table is ok and print it
		transitionTableLoggedOut = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableLoggedIn = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableReadOnly = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableLoggedOut.ReadExcelTextFile(properties.getUserTransitionTableLoggedOut());
		transitionTableLoggedIn.ReadExcelTextFile(properties.getUserTransitionTableLoggedIn());
		transitionTableReadOnly.ReadExcelTextFile(properties.getUserTransitionTableReadOnly());
		logDaemon = new LogDaemon(properties.getLogFile());
	}

	/**
	 * Updates the slowdown factor.
	 *
	 * @param newValue new slowdown value
	 */
	private synchronized void setSlowDownFactor(float newValue)
	{
		slowdownFactor = newValue;
	}
	
	
	/**
	 * Get the slowdown factor corresponding to current ramp (up, session or down).
	 */
	public static synchronized float getSlowDownFactor()
	{
		return slowdownFactor;
	}
	
	
	/**
	 * Set the end of the current simulation
	 */
	private synchronized void setEndOfSimulation()
	{
		endOfSimulation = true;
	}


	/**
	 * True if end of simulation has been reached.
	 */
	public static synchronized boolean isEndOfSimulation()
	{
		return endOfSimulation;
	}


	/**
	 * Main program take an optional output file argument only 
	 * if it is run on as a remote client.
	 *
	 * @param args optional output file if run as remote client
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException
	{
		if(args.length < 2) {
			System.err.println("Error: No config files supplied.");
			System.err.println("Parameter order: config/wordpress.properties config/log4j.properties");
			System.exit(0);
		}
	
		PropertyConfigurator.configure(args[1]);
		ClientEmulator client = new ClientEmulator(args[0]); // Get wordpress.properties
		Dictionary dict = new Dictionary(client.properties.getStoryDictionary());
		//Dictionary keyDict = new Dictionary(client.properties.getKeyDictionary());
		String commandString=null;
		StringTokenizer piece;
		int command=-1, nbOfUsersMin=0, nbOfUsersMax=0, readWritesCount; 
		double readWrites=0;
		boolean running = true;
		
		Vector<String> usersCredentials = new Vector<String>();
		Vector<WordpressUserSession>  sessions = new Vector<WordpressUserSession>();
		long initialTime = System.currentTimeMillis();
		WordpressUserSession  adminSession = new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, "admin", initialTime);
		
		adminSession.deleteWebsiteData();
		
		// Run user sessions
		Logger.getLogger(Thread.currentThread().getName()).info("ClientEmulator: Starting "+client.properties.getNumberOfClients()+" session threads");
		
		ConnectionDaemon connectionDaemon = new ConnectionDaemon(client.properties.getTcpPort());
		//ConstantUsers keepConstantUsers = null;
		
		while (running) {
			commandString = null;
			while (commandString==null) {
				connectionDaemon.waitForConnection();
				commandString = connectionDaemon.getMessage();
				System.out.println("NEW COMMAND:" + commandString);
			}
			piece = new StringTokenizer(commandString);
			if (piece.hasMoreElements())
				command = Integer.parseInt(piece.nextToken());
			switch (command) {
				case ClientEmulator.COMMAND_START_SIMULATION:
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						readWrites = Double.parseDouble(piece.nextToken());
					System.out.println("COMMAND_START_SIMULATION-"+nbOfUsersMin+"-"+nbOfUsersMax+"-"+readWrites);
					
					client.logDaemon.start();
					
					client.nbOfUsers = nbOfUsersMax - nbOfUsersMin;
					System.out.println("nbOfUsers "+client.nbOfUsers);
					readWritesCount = (int)(client.nbOfUsers * readWrites);
					for (int i = nbOfUsersMin ; i < nbOfUsersMax; i++)
					{
						String username = "user"+i;
						
						if ( readWritesCount > 0 ) {
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableReadOnly, client.transitionTableLoggedIn, client.stats, username, initialTime ) );
							readWritesCount--;
						}
						else
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, username, initialTime ) );
						
						sessions.elementAt(i).start();
						usersCredentials.add(username);
					}
					
					try{
						//Thread.sleep(client.properties.getTestLength() * 100000);
					}catch (Exception e) {
						// TODO: handle exception
					}
					
					System.out.println("start constantUsers daemon "+sessions.size());
					//keepConstantUsers = new ConstantUsers(client);
					System.out.println("* ");
					//keepConstantUsers.run();
					System.out.println("sending OK message");
					connectionDaemon.sendMessage("OK");
					connectionDaemon.closeConnection();
					System.out.println("exit COMMAND_START_SIMULATION");
					break;
				case ClientEmulator.COMMAND_END_SIMULATION:
					System.out.println("COMMAND_END_SIMULATION! "+sessions.size()+" = "+client.nbOfUsers);
					client.setEndOfSimulation();
					Logger.getLogger(Thread.currentThread().getName()).info("ClientEmulator: Shutting down threads ...");
					
					for (int i = 0 ; i < client.nbOfUsers ; i++)
					{
					      try
					      {
					        sessions.elementAt(i).join(client.JOIN_TIMEOUT);
					      }
					      catch (java.lang.InterruptedException ie)
					      {
					    	  Logger.getLogger(Thread.currentThread().getName()).error("ClientEmulator: Thread "+i+" has been interrupted.");
					      }
					}
					
					Logger.getLogger(Thread.currentThread().getName()).info("Done");
					client.stats.display_stats("WordPressBench ");
					client.stats.log_stats("WordPressBench ");
					
					connectionDaemon.sendMessage("OK");
					connectionDaemon.closeConnection();
					System.out.println("STOP SIMULATION");
					break;
				case ClientEmulator.COMMAND_GET_LOG:
					client.stats.current_stats();
					connectionDaemon.sendFile("config/log.txt");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_ADD_USERS:
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					System.out.println("COMMAND_ADD_USERS-"+nbOfUsersMin+"-"+nbOfUsersMax);
					
					readWritesCount = (int)((nbOfUsersMax - nbOfUsersMin) * readWrites);
					for (int i = nbOfUsersMin ; i < nbOfUsersMax; i++)
					{
						String username = "user"+i;
						
						if ( readWritesCount > 0 ) {
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableReadOnly, client.transitionTableLoggedIn, client.stats, username, initialTime ) );
							readWritesCount--;
						}
						else
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, username, initialTime ) );
						
						sessions.lastElement().start();
						usersCredentials.add(username);
					}
					try{
						//Thread.sleep(client.properties.getTestLength() * 100000);
					}catch (Exception e) {
						// TODO: handle exception
					}

					client.nbOfUsers += (nbOfUsersMax - nbOfUsersMin);

					connectionDaemon.sendMessage("OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_CREATE_USERS:
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					System.out.println("COMMAND_CREATE_USERS-("+nbOfUsersMin+","+nbOfUsersMax+")");
					
					usersCredentials = adminSession.createNewUsers(nbOfUsersMin, nbOfUsersMax);
					
					connectionDaemon.sendMessage("OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_EXIT:
					running = false;
					connectionDaemon.sendMessage("OK");
					connectionDaemon.closeConnection();
					System.out.println("EXIT");
					System.exit(0);
					break;
			}
		}
		
/*
		SlaveDaemon connectionDaemon = new SlaveDaemon(client.properties.getTcpPort());
		while (commandString==null) {
			connectionDaemon.waitForConnection();
			commandString = connectionDaemon.getMessage();
		}
		System.out.println("Received following message: " + commandString);
		piece = new StringTokenizer(commandString);
		if (piece.hasMoreElements())
			command = Integer.parseInt(piece.nextToken());
		if (piece.hasMoreElements())
			nbOfUsers = Integer.parseInt(piece.nextToken());
		System.out.println(command+"-"+nbOfUsers);

		
		//WordpressUserSession  adminSession = null;
		//adminSession = new WordpressUserSession("AdminSession", client.urlGen, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, "admin");
		//usersCredentials = adminSession.createNewUsers(0, nbOfClients);
		connectionDaemon.sendMessage("OK");
		connectionDaemon.closeConnection();
		
		
		commandString = null;
		while (commandString==null) {
			connectionDaemon.waitForConnection();
			commandString = connectionDaemon.getMessage();
		}
		System.out.println("Received following message: " + commandString);
		piece = new StringTokenizer(commandString);
		if (piece.hasMoreElements())
			command = Integer.parseInt(piece.nextToken());
		if (piece.hasMoreElements())
			nbOfUsersMin = Integer.parseInt(piece.nextToken());
		if (piece.hasMoreElements())
			nbOfUsersMax = Integer.parseInt(piece.nextToken());
		if (piece.hasMoreElements())
			readWrites = Integer.parseInt(piece.nextToken());
		System.out.println(command+"-"+nbOfUsersMin+"-"+nbOfUsersMax+"-"+readWrites);
		connectionDaemon.sendMessage("OK");
		connectionDaemon.closeConnection();
		
		
		commandString = null;
		while (commandString==null) {
			connectionDaemon.waitForConnection();
			commandString = connectionDaemon.getMessage();
		}
		System.out.println("Received following message: " + commandString);
		connectionDaemon.sendMessage("OK");
		connectionDaemon.closeConnection();
*/
////////////////////////////////////////////////////////////////////////////////////
/*			
		Vector<WordpressUserSession>  sessions = new Vector<WordpressUserSession>();
		for (int i = 0 ; i < client.properties.getNumberOfClients(); i++)
		{
			sessions.elementAt(i)= new WordpressUserSession("UserSession"+i, client.urlGen, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, usersCredentials.get(i));
			sessions.elementAt(i).start();
		}
		try{
			Thread.sleep(client.properties.getTestLength() * 100000);
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		client.setEndOfSimulation();
		///////////// END /////////////
		///////////// END /////////////
		///////////// END /////////////
		Logger.getLogger(Thread.currentThread().getName()).info("ClientEmulator: Shutting down threads ...");
		for (int i = 0 ; i < client.properties.getNumberOfClients() ; i++)
		{
		//      try
		//      {
		//        sessions.elementAt(i).join();
		//      }
		//      catch (java.lang.InterruptedException ie)
		//      {
		//    	  Logger.getLogger(Thread.currentThread().getName()).error("ClientEmulator: Thread "+i+" has been interrupted.");
		//      }
		}
		
		
		Logger.getLogger(Thread.currentThread().getName()).info("Done");
		client.stats.display_stats("Wordpress statistics");

		adminSession.deleteWebsiteData();
*/	
		//System.exit(0);
  }
  
  
/*  public static void main(String[] args) throws InterruptedException, IOException
	{
		if(args.length < 2) {
			System.err.println("Error: No config files supplied.");
			System.err.println("Parameter order: config/wordpress.properties config/log4j.properties");
			System.exit(0);
		}
		
		PropertyConfigurator.configure(args[1]);//"log4j.properties");
		ClientEmulator client = new ClientEmulator(args[0]); // Get wordpress.properties
		Vector<String> usersCredentials = new Vector<String>(); 
		    
		WordpressUserSession[]  sessions = new WordpressUserSession[client.properties.getNumberOfClients()];
		WordpressUserSession  adminSession = null;
		// #############################
		// ### TEST TRACE BEGIN HERE ###
		// #############################
		   
		// Run user sessions
		Logger.getLogger(Thread.currentThread().getName()).info("ClientEmulator: Starting "+client.properties.getNumberOfClients()+" session threads");
		Dictionary dict = new Dictionary(client.properties.getStoryDictionary());
		//Dictionary keyDict = new Dictionary(client.properties.getKeyDictionary());
		    
		adminSession = new WordpressUserSession("AdminSession", client.urlGen, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, "admin");
		usersCredentials = adminSession.initWebsiteData();
		    
		for (int i = 0 ; i < client.properties.getNumberOfClients(); i++)
		{
			sessions[i] = new WordpressUserSession("UserSession"+i, client.urlGen, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, usersCredentials.get(i));
			sessions[i].start();
		}
		try{
			Thread.sleep(client.properties.getTestLength() * 100000);
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		client.setEndOfSimulation();
		///////////// END /////////////
		///////////// END /////////////
		///////////// END /////////////
		Logger.getLogger(Thread.currentThread().getName()).info("ClientEmulator: Shutting down threads ...");
		for (int i = 0 ; i < client.properties.getNumberOfClients() ; i++)
		{
		//      try
		//      {
		//        sessions[i].join();
		//      }
		//      catch (java.lang.InterruptedException ie)
		//      {
		//    	  Logger.getLogger(Thread.currentThread().getName()).error("ClientEmulator: Thread "+i+" has been interrupted.");
		//      }
		}
		Logger.getLogger(Thread.currentThread().getName()).info("Done");
		client.stats.display_stats("Wordpress statistics");
		    
		adminSession.deleteWebsiteData();
		    
		System.exit(0);
	}
*/
	
}
