 package worker;

import java.io.IOException;
import java.lang.Runtime;
import java.util.*;

import org.apache.log4j.*;

/**
 * WordPressBench client emulator managing the user sessions.
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a> and 
 * 			<a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and 
 * 			<a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class ClientEmulator
{
	private WordpressProperties properties = null; // access to properties file
	private URLGenerator		urlGen = null;
	private TransitionTable	transitionTableLoggedOut = null;
	private TransitionTable	transitionTableLoggedIn = null;
	private TransitionTable	transitionTableReadOnly = null;
	private LogDaemon logDaemon = null;
	private Stats				stats = null;
	public static volatile boolean  endOfSimulation = false;
	public int nbOfUsers = 0;
	private int JOIN_TIMEOUT = 15000; 
	
	public Vector<String> usersCredentials;
	public Vector<WordpressUserSession>  sessions;

	public static final int COMMAND_START_SIMULATION = 0;
	public static final int COMMAND_END_SIMULATION = 1;
	public static final int COMMAND_GET_LOG = 2;
	public static final int COMMAND_ADD_USERS = 3;
	public static final int COMMAND_CREATE_USERS = 4;
	public static final int COMMAND_EXIT = 5;
	
	/**
	 * Creates a new <code>ClientEmulator</code> instance.
	 * It reads the configuration file, transition tables and opens log file. 
	 * The program is stopped on any error reading the configuration files.
	 * 
	 * @param propertiesFile configuration file name
	 */
	public ClientEmulator(String propertiesFile)
	{
		// Initialization, check that all files are ok
		properties = new WordpressProperties(propertiesFile);
		try {
			properties.checkPropertiesFile();
		}catch (Exception e) {
			System.exit(1);
		}
		urlGen = new WordpressURLGenerator(properties.getWebServerName(), properties.getWebsitePort(), properties.getScriptPath());
		if (urlGen == null)
			Runtime.getRuntime().exit(1);
		stats = new Stats(properties.getNumberOfStates(), properties.getLogFile());
		System.out.println("Loading transition tables...");
		transitionTableLoggedOut = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableLoggedIn = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableReadOnly = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
		transitionTableLoggedOut.ReadExcelTextFile(properties.getUserTransitionTableLoggedOut());
		transitionTableLoggedIn.ReadExcelTextFile(properties.getUserTransitionTableLoggedIn());
		transitionTableReadOnly.ReadExcelTextFile(properties.getUserTransitionTableReadOnly());
		System.out.println("Loading logfile...");
		logDaemon = new LogDaemon(properties.getLogFile());
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
	 * 
	 * @return  true if end of simulation is reached
	 */
	public static synchronized boolean isEndOfSimulation()
	{
		return endOfSimulation;
	}

	/**
	 * Main program receiving commands from Controller and managing the user sessions.
	 *
	 * @param args configuration file
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
		String commandString=null;
		StringTokenizer piece;
		int command=-1, nbOfUsersMin=0, nbOfUsersMax=0, readWritesCount; 
		double readWrites=0;
		boolean running = true;
		
		Vector<String> usersCredentials = new Vector<String>();
		Vector<WordpressUserSession>  sessions = new Vector<WordpressUserSession>();
		long initialTime = System.currentTimeMillis();
		WordpressUserSession  adminSession = new WordpressUserSession(client.urlGen, client.logDaemon, new Dictionary(dict), client.properties, client.transitionTableLoggedOut, client.transitionTableLoggedIn, client.stats, "admin", initialTime);
		
		System.out.println("Deleting database users...");
		adminSession.deleteWebsiteUsers();
				
		ConnectionDaemon connectionDaemon = new ConnectionDaemon(client.properties.getTcpPort());
		
		System.out.println("Waiting for commands from Controller...");
		
		while (running) {
			commandString = null;
			while (commandString==null) {
				connectionDaemon.waitForConnection();
				commandString = connectionDaemon.getMessage();
			}
			piece = new StringTokenizer(commandString);
			if (piece.hasMoreElements())
				command = Integer.parseInt(piece.nextToken());
			switch (command) {
				case ClientEmulator.COMMAND_START_SIMULATION:
					System.out.println("Received START SIMULATION command...");
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						readWrites = Double.parseDouble(piece.nextToken());
					
					if ( readWrites < 0.8 ) {
						System.out.println("Deleting database contents...");
						adminSession.deleteWebsiteData();
					}
					
					client.logDaemon.start();
					
					client.nbOfUsers = nbOfUsersMax - nbOfUsersMin;
					readWritesCount = (int)(client.nbOfUsers * readWrites);
					for (int i = nbOfUsersMin ; i < nbOfUsersMax; i++)
					{
						String username = "user"+i;
						
						if ( readWritesCount > 0 ) {
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, 
									new Dictionary(dict), client.properties, client.transitionTableReadOnly, 
									client.transitionTableLoggedIn, client.stats, username, initialTime ) );
							readWritesCount--;
						}
						else
							sessions.add(i, new WordpressUserSession(client.urlGen, client.logDaemon, 
									new Dictionary(dict), client.properties, client.transitionTableLoggedOut, 
									client.transitionTableLoggedIn, client.stats, username, initialTime ) );
						
						sessions.elementAt(i).start();
						usersCredentials.add(username);
					}
					
					connectionDaemon.sendMessage("START SIMULATION OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_END_SIMULATION:
					System.out.println("Received END SIMULATION command...");
					client.setEndOfSimulation();
					Logger.getLogger(Thread.currentThread().getName()).info(
							"ClientEmulator: Shutting down threads ...");
					
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
					
					connectionDaemon.sendMessage("END SIMULATION OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_GET_LOG:
					System.out.println("Received GET LOGFILE command...");
					client.stats.current_stats();
					connectionDaemon.sendFile("config/log.txt");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_ADD_USERS:
					System.out.println("Received ADD USERS command...");
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					
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

					client.nbOfUsers += (nbOfUsersMax - nbOfUsersMin);

					connectionDaemon.sendMessage("ADD USERS OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_CREATE_USERS:
					System.out.println("Received CREATE USERS command...");
					if (piece.hasMoreElements())
						nbOfUsersMin = Integer.parseInt(piece.nextToken());
					if (piece.hasMoreElements())
						nbOfUsersMax = Integer.parseInt(piece.nextToken());
					
					usersCredentials = adminSession.createNewUsers(nbOfUsersMin, nbOfUsersMax);
					
					connectionDaemon.sendMessage("CREATE USERS OK");
					connectionDaemon.closeConnection();
					break;
				case ClientEmulator.COMMAND_EXIT:
					System.out.println("Received EXIT command...");
					running = false;
					connectionDaemon.sendMessage("EXIT OK");
					connectionDaemon.closeConnection();
					System.exit(0);
					break;
			}
		}
  }
	
}
