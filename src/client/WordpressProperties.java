package client;

import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This program check and get all information for the rubbos.properties file
 * found in the classpath.
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class WordpressProperties
{
  private static Properties configuration = null;

  // Information about web server
  private String webSiteName;
  private int    webSitePort;
  private String scriptPath;

  // Information about Workload
  private int     numberOfClients;
  private int     numberOfStates;
  private int     maxNumberOfTransitions;
  private int     testLength;
  
  private String  transitionTableLoggedOut;
  private String  transitionTableLoggedIn;

  // Policy to generate database information
  private Integer numberOfUsers;

  private String  textDictionary;
  private String  keyDictionary;
  private int commentMaxLength;
  private int actionMaxLength;
  private int affectedMaxLength;
  private int scopeMaxLength;

  /**
   * Creates a new <code>RUBBoSProperties</code> instance.
   * If the rubbos.properties file is not found in the classpath,
   * the current thread is killed.
   */
  public WordpressProperties(String propsFile)
  {
    // Get and check database.properties
    System.out.println("Looking for wordpress.properties in classpath ("+System.getProperty("java.class.path",".")+")");
    try
    {
    	configuration = new Properties();
    	configuration.load(new FileReader(propsFile));//"wordpress.properties"));
//      configuration = ResourceBundle.getBundle("wordpress.properties");
    }
    catch (IOException e)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("No wordpress.properties file has been found in your classpath.");
      e.printStackTrace();
      Runtime.getRuntime().exit(1);
    }
  }

  
  /**
   * Returns the value corresponding to a property in the rubbos.properties file.
   *
   * @param property the property name
   * @return a <code>String</code> value
   */
  protected String getProperty(String property)
  {
    String s = configuration.getProperty(property);
    return s;
  }


  /**
   * Check for all needed fields in rubbos.properties and inialize corresponding values.
   * This function returns the corresponding URLGenerator on success.
   *
   * @return returns null on any error or the URLGenerator corresponding to the configuration if everything was ok.
   */
	public void checkPropertiesFile() throws Exception {
		try {
			webSiteName = getProperty("webSiteName");
			webSitePort = Integer.parseInt(getProperty("webSitePort"));
			scriptPath = getProperty("scriptPath");
			numberOfClients = Integer.parseInt(getProperty("numberOfClients"));
			numberOfStates = Integer.parseInt(getProperty("numberOfStates"));
			maxNumberOfTransitions = Integer.parseInt(getProperty("maxNumberOfTransitions"));
			testLength = Integer.parseInt(getProperty("testLength"));
			transitionTableLoggedOut = getProperty("transitionTableLoggedOut");
			transitionTableLoggedIn = getProperty("transitionTableLoggedIn");
			numberOfUsers = Integer.parseInt(getProperty("numberOfUsers"));
			textDictionary = getProperty("textDictionary");
			keyDictionary = getProperty("keyDictionary");
			commentMaxLength = Integer.parseInt(getProperty("commentMaxLength"));
			actionMaxLength = Integer.parseInt(getProperty("actionMaxLength"));
			affectedMaxLength = Integer.parseInt(getProperty("affectedMaxLength"));
			scopeMaxLength = Integer.parseInt(getProperty("scopeMaxLength"));
			try {
				RandomAccessFile f = new RandomAccessFile(textDictionary, "r");
				f.readLine();
				f.close();
			} catch (Exception e) {
				Logger.getLogger(Thread.currentThread().getName()).error("Unable to read dictionary file '"
						+ textDictionary + "' (got exception: "
						+ e.getMessage() + ")");
				throw e;
			}
		} catch (Exception e) {
			Logger.getLogger(Thread.currentThread().getName()).error("Error while checking database.properties: "
					+ e.getMessage());
			throw e;
		}
	}


  /**
   * Get the web server name
   *
   * @return web server name
   */
  public String getWebServerName()
  {
    return webSiteName;
  }

  
  public String getScriptPath() {
	  return scriptPath;
  }
  
  public int getPort() {
	  return webSitePort;
  }

  /**
   * Get the total number of users given in the database_number_of_users field
   *
   * @return total number of users
   */
  public int getNumberOfUsers()
  {
    return numberOfUsers.intValue();
  }


  /**
   * Get the dictionary file used to build the stories.
   * This file is a plain text file with one word per line.
   *
   * @return dictionary file used to build the stories
   */
  public String getStoryDictionary()
  {
    return textDictionary;
  }
  
  public String getKeyDictionary()
  {
    return keyDictionary;
  }

  /**
   * Get the maximum story length given in the database_story_maximum_length field
   *
   * @return maximum story length
   */
  public int getCommentMaximumLength()
  {
    return commentMaxLength;
  }
  
  public int getActionMaximumLength()
  {
    return actionMaxLength;
  }
  
  public int getAffectedMaximumLength()
  {
    return affectedMaxLength;
  }
  
  public int getScopeMaximumLength()
  {
    return scopeMaxLength;
  }

  /**
   * Get the user transition table file name given in the workload_user_transition_table field
   *
   * @return user transition table file name
   */
  public String getUserTransitionTableLoggedOut()
  {
    return transitionTableLoggedOut;
  }
  
  /**
   * Get the user transition table file name given in the workload_user_transition_table field
   *
   * @return user transition table file name
   */
  public String getUserTransitionTableLoggedIn()
  {
    return transitionTableLoggedIn;
  }

  /**
   * Get the number of columns in the transition table
   *
   * @return number of columns
   */
  public int getNumberOfStates()
  {
    return numberOfStates;
  }

  /**
   * Get the total number of clients user sessions to launch in parallel
   *
   * @return total number of clients
   */
  public int getNumberOfClients()
  {
    return numberOfClients;
  }

  /**
   * Get the maximum number of transitions a client may perform
   *
   * @return maximum number of transitions
   */
  public int getMaxNumberOfTransitions()
  {
    return maxNumberOfTransitions;
  }
  
  public int getTestLength() {
	  return testLength;
  }
}
