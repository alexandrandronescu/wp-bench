package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Stack;

import org.apache.log4j.Logger;


public class TransitionTable
{
  private int    nbColumns;
  private int    nbRows;
  private float  transitions[][];
  private int    transitionsTime[];
  private String tableName = null;
  private Random rand = new Random();
  private Stack<Integer>  previousStates = new Stack<Integer>();
  private int currentState = 0;
  private Stats  stats;
  private boolean useTPCWThinkTime;
  private static String[] stateNames;
  
  /**
   * Creates a new <code>TransitionTable</code> instance.
   */
  public TransitionTable(int columns, int rows, Stats statistics, boolean UseTPCWThinkTime)
  {
    nbColumns = columns;
    nbRows = rows;
    stats = statistics;
    transitions = new float[nbColumns][nbRows];
    transitionsTime = new int[nbRows];
    useTPCWThinkTime = UseTPCWThinkTime;
  }
  
  public TransitionTable(TransitionTable t)
  {
    nbColumns = t.nbColumns;
    nbRows = t.nbRows;
    stats = t.stats;
    transitions = new float[nbColumns][nbRows];
    for (int i = 0; i < nbRows; i++) {
		for (int j = 0; j < nbColumns; j++) {
			transitions[i][j] = t.transitions[i][j];
		}
	}
    
    transitionsTime = new int[nbRows];
    useTPCWThinkTime = t.useTPCWThinkTime;
  }

  /**
   * Get the name of the transition table as defined in file.
   *
   * @return name of the transition table.
   */
  public String getTableName() 
  {
    return tableName;
  }


  /**
   * Resets the current state to initial state (home page).
   */
  public void resetToInitialState()
  {
    currentState = 0;
    stats.incrementCount(currentState);
  }


  /**
   * Return the current state value (row index). 
   *
   * @return current state value (0 means initial state)
   */
  public int getCurrentState()
  {
    return currentState;
  }


  /**
   * Return the previous state value (row index). 
   *
   * @return previous state value (-1 means no previous state)
   */
  public int getPreviousState()
  {
    if (previousStates.empty())
      return -1;
    else
    {
      Integer state = (Integer)previousStates.peek();
      return state.intValue();
    }
  }


  /**
   * Go back to the previous state and return the value of the new state
   *
   * @return new state value (-1 means no previous state)
   */
  public int backToPreviousState()
  {
    if (previousStates.empty())
      return -1;
    else
    {
      Integer state = (Integer)previousStates.pop();
      currentState = state.intValue();
      return currentState;
    }
  }


  /**
   * Returns true if the 'End of Session' state has been reached
   *
   * @return true if current state is 'End of Session'
   */
  public boolean isEndOfSession()
  {
    return currentState == (nbRows-1);
  }
  

  /**
   * Return the current state name
   *
   * @return current state name
   */
  public String getCurrentStateName()
  {
    return stateNames[currentState];
  }
  

  /**
   * Return a state name
   *
   * @return current state name
   */
  public static String getStateName(int state)
  {
    return stateNames[state];
  }
  

  /**
   * Compute a next state from current state according to transition matrix.
   *
   * @return value of the next state
   */
  public int nextState()
  {
    int   beforeStep = currentState;
    float step = rand.nextFloat();
    float cumul = 0;
    int   i;

    for (i = 0 ; i < nbRows ; i++)
    {
      cumul = cumul + transitions[i][currentState];
      if (step < cumul)
      {
        currentState = i;
        break;
      }
    }
    
    previousStates.add(beforeStep);
    return currentState;
  }

  
  /**
   * Read the matrix transition from a file conforming to the
   * format described in the class description.
   *
   * @param filename name of the file to read the matrix from
   * @return true upon success else false
   */
  public boolean ReadExcelTextFile(String filename)
  { 
    BufferedReader reader;
    int            i = 0;
    int            j = 0;
    // Try to open the file
    try
    {	
      reader = new BufferedReader(new FileReader(filename));
      System.out.println("Reading transitions from " + (new File(filename)).getAbsolutePath());
    }
    catch (FileNotFoundException f)
    {
      Logger.getLogger(Thread.currentThread().getName()).error("File "+filename+" not found.");
      return false;
    }

    // Now read the file using tab (\t) as field delimiter
    try
    {
      // Header
      StringTokenizer st = new StringTokenizer(reader.readLine(), "\t");
      st.nextToken(); // Should be 'RUBBoS Transition Table'
      tableName = st.nextToken();
      //System.out.println("Reading "+tableName+" from "+filename);
      reader.readLine(); // Empty line
      reader.readLine(); // Column headers

      stateNames = new String[nbRows];
      // Read the matrix
      for (i = 0 ; i < nbRows ; i++)
      {
        st = new StringTokenizer(reader.readLine(), "\t");
        stateNames[i] = st.nextToken();
        System.out.println(stateNames[i]);
        for (j = 0 ; j < nbColumns ; j++)
        {
        	String ss = st.nextToken();
        	try {
        		Float f = new Float(ss);
       			transitions[i][j] = f.floatValue();
        	}catch(NumberFormatException e) {
        		System.out.println("bad number format i=" + i + " j=" + j + "token = " + ss);
        	}
        }
        // Last column is transition_waiting_time
//        Integer t = new Integer(st.nextToken());
        transitionsTime[i] = 1; // TBF: t.intValue(); 
      }
      reader.close();
    }
    catch (IOException ioe)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("An error occured while reading "+filename+". ("+ioe.getMessage()+")");
    	return false;
    }
    catch (NoSuchElementException nsu)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("File format error in file "+filename+" when reading line "+i+", column "+j+". ("+nsu.getMessage()+")");
      return false;
    }
    catch (NumberFormatException ne)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("Number format error in file "+filename+" when reading line "+i+", column "+j+". ("+ne.getMessage()+")");
      return false;
    }
//    System.out.println("Transition matrix successfully build.");
    return true;
  }


  /**
   * Display the transition matrix on the standard output.
   *
   * @param title transition table name to be displayed in title
   */
  public void displayMatrix(String title)
  {
    int i,j;

    for (j = 0 ; j < nbColumns ; j++)
    for (i = 0 ; i < nbRows ; i++)
    {
      for (j = 0 ; j < nbColumns ; j++){
    	  System.out.print(Float.toString(transitions[i][j]));
      		System.out.print("\t");
      }
      System.out.println();
    }
    System.out.println();
  }

  // Negative exponential distribution used by
  //  TPC-W spec for Think Time (Clause 5.3.2.1) and USMD (Clause 6.1.9.2)
  public long TPCWthinkTime()
  {
    double r = rand.nextDouble();
    if (r < (double)4.54e-5)
      return ((long) (r+0.5));
    return  ((long) ((((double)-7000.0)*Math.log(r))+0.5));
  }

}
