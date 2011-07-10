 package client;

import java.io.IOException;
import java.lang.Runtime;
import java.util.Collections;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import client.Dictionary;
import client.Stats;
import client.TransitionTable;

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
  private TransitionTable	transitionLoggedOut = null;
  private TransitionTable	transitionLoggedIn = null;
  private Stats				stats = null;
  private static float		slowdownFactor = 0;
  private static volatile boolean  endOfSimulation = false;

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
    urlGen = new WordpressURLGenerator(properties.getWebServerName(), properties.getPort(), properties.getScriptPath());
    if (urlGen == null)
      Runtime.getRuntime().exit(1);
    stats = new Stats(properties.getNumberOfStates());
    // Check that the transition table is ok and print it
    transitionLoggedOut = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
    transitionLoggedIn = new TransitionTable(properties.getNumberOfStates(), properties.getNumberOfStates(), stats, true);
    transitionLoggedOut.ReadExcelTextFile(properties.getUserTransitionTableLoggedOut());
    transitionLoggedIn.ReadExcelTextFile(properties.getUserTransitionTableLoggedIn());
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
    Dictionary keyDict = new Dictionary(client.properties.getKeyDictionary());
    
    adminSession = new WordpressUserSession("AdminSession", client.urlGen, new Dictionary(dict), new Dictionary(keyDict), client.properties, client.transitionLoggedOut, client.transitionLoggedIn, client.stats, "admin");
    usersCredentials = adminSession.initWebsiteData();
    
    for (int i = 0 ; i < client.properties.getNumberOfClients(); i++)
    {
        sessions[i] = new WordpressUserSession("UserSession"+i, client.urlGen, new Dictionary(dict), new Dictionary(keyDict), client.properties, client.transitionLoggedOut, client.transitionLoggedIn, client.stats, usersCredentials.get(i));
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
}
