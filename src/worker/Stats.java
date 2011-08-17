package worker;

import java.io.*;
import org.apache.log4j.Logger;

 
/**
 * This class provides thread-safe statistics. Each statistic entry is composed as follow:
 * <pre>
 * count     : statistic counter
 * error     : statistic error counter
 * minTime   : minimum time for this entry (automatically computed)
 * maxTime   : maximum time for this entry (automatically computed)
 * totalTime : total time for this entry
 * </pre>
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class Stats
{
  private int nbOfStats;
  private int count[];
  private int error[];
  private long minTime[];
  private long maxTime[];
  private long totalTime[];
  private int countAboveLimit[];
  private int  nbSessions;   // Number of sessions succesfully ended
  private long sessionsTime; // Sessions total duration
  private String logFile;
  int LIMIT = 500;//millis


  /**
   * Creates a new <code>Stats</code> instance.
   * The entries are reset to 0.
   *
   * @param NbOfStats number of entries to create
   */
  public Stats(int NbOfStats, String logFile)
  {
    nbOfStats = NbOfStats;
    count = new int[nbOfStats];
    error = new int[nbOfStats];
    minTime = new long[nbOfStats];
    maxTime = new long[nbOfStats];
    totalTime = new long[nbOfStats];
    countAboveLimit = new int[nbOfStats];
    this.logFile = logFile;
    
    reset();
  }

  /**
   * Resets all entries to 0
   */
  public synchronized void reset()
  {
    int i;

    for (i = 0 ; i < nbOfStats ; i++)
    {
      count[i] = 0;
      error[i] = 0;
      minTime[i] = Long.MAX_VALUE;
      maxTime[i] = 0;
      totalTime[i] = 0;
      countAboveLimit[i] = 0;
    }
    nbSessions = 0;
    sessionsTime = 0;
  }

  /**
   * Add a session duration to the total sessions duration and
   * increase the number of succesfully ended sessions.
   *
   * @param time duration of the session
   */
  public synchronized void addSessionTime(long time)
  {
    nbSessions++;
    if (time < 0)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("Negative time received in Stats.addSessionTime("+time+")<br>\n");
      return ;
    }
    sessionsTime = sessionsTime + time;
  }

 /**
   * Increment the number of succesfully ended sessions.
   */
  public synchronized void addSession()
  {
    nbSessions++;
  }


  /**
   * Increment an entry count by one.
   *
   * @param index index of the entry
   */
  public synchronized void incrementCount(int index)
  {
    count[index]++;
  }


  /**
   * Increment an entry error by one.
   *
   * @param index index of the entry
   */
  public synchronized void incrementError(int index)
  {
    error[index]++;
  }


  /**
   * Add a new time sample for this entry. <code>time</code> is added to total time
   * and both minTime and maxTime are updated if needed.
   *
   * @param index index of the entry
   * @param time time to add to this entry
   */
  public synchronized void updateTime(int index, long time)
  {
    if (time < 0)
    {
    	Logger.getLogger(Thread.currentThread().getName()).error("Negative time received in Stats.updateTime("+time+")<br>\n");
      return ;
    }
    totalTime[index] += time;
    if (time > maxTime[index])
      maxTime[index] = time;
    if (time < minTime[index])
      minTime[index] = time;
    if(time > LIMIT)
    	countAboveLimit[index]++;
  }


  /**
   * Get current count of an entry
   *
   * @param index index of the entry
   *
   * @return entry count value
   */
  public synchronized int getCount(int index)
  {
    return count[index];
  }


  /**
   * Get current error count of an entry
   *
   * @param index index of the entry
   *
   * @return entry error value
   */
  public synchronized int getError(int index)
  {
    return error[index];
  }


  /**
   * Get the minimum time of an entry
   *
   * @param index index of the entry
   *
   * @return entry minimum time
   */
  public synchronized long getMinTime(int index)
  {
    return minTime[index];
  }


  /**
   * Get the maximum time of an entry
   *
   * @param index index of the entry
   *
   * @return entry maximum time
   */
  public synchronized long getMaxTime(int index)
  {
    return maxTime[index];
  }


  /**
   * Get the total time of an entry
   *
   * @param index index of the entry
   *
   * @return entry total time
   */
  public synchronized long getTotalTime(int index)
  {
    return totalTime[index];
  }


  /**
   * Get the total number of entries that are collected
   *
   * @return total number of entries
   */
  public int getNbOfStats()
  {
    return nbOfStats;
  }

  public long current_stats()
  {
    int counts = 0;
    int errors = 0;
    long time = 0;
    int countAbove = 0;
    long averageTime = 0;

    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      counts += count[i];
      errors += error[i];
      time += totalTime[i];
      countAbove += countAboveLimit[i];
    }

    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      if (count[i] != 0)
      {
    	  double success = count[i] - error[i];
    	  if (success > 0)
    		  success = totalTime[i]/success;
    	  else
    		  success = 0;
    	averageTime += success;
      }
    }

    return (averageTime/getNbOfStats());
  }

  /**
   * Display an HTML table containing the stats for each state.
   * Also compute the totals and average throughput
   *
   * @param title table title
   * @param sessionTime total time for this session
   * @param exclude0Stat true if you want to exclude the stat with a 0 value from the output
   */
  public void display_stats(String title)
  {
    int counts = 0;
    int errors = 0;
    long time = 0;
    int countAbove = 0;
    double averageTime = 0;

    String format = "%-20s %10s %10s %10s %10sms %10sms %10sms";
    System.out.println(String.format(format, "State name", "% of total", "Count", "Errors", "Minimum", "Maximum", "Average Time"));
    // Display statistics for each state
    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      counts += count[i];
      errors += error[i];
      time += totalTime[i];
      countAbove += countAboveLimit[i];
    }

    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      if (count[i] != 0)
      {
    	  double percent = 0;
    	  if ((counts > 0) && (count[i] > 0))
              percent = 100.0*count[i]/counts;
    	  double success = count[i] - error[i];
    	  if (success > 0)
    		  success = totalTime[i]/success;
    	  else
    		  success = 0;
    	System.out.println(String.format(
						format, TransitionTable.getStateName(i),
						percent, count[i], error[i], minTime[i], maxTime[i],
						success));
    	averageTime += success;
      }
    }
    System.out.println("% above latency limit = " + (countAbove*100.0/counts));
    System.out.println("% error = " + (errors*100.0/(counts+errors)));

    System.out.println("% average time = " + (averageTime/getNbOfStats()) );
  }
  
  /**
   * Display an HTML table containing the stats for each state.
   * Also compute the totals and average throughput
   *
   * @param title table title
   * @param sessionTime total time for this session
   * @param exclude0Stat true if you want to exclude the stat with a 0 value from the output
   */
  public void log_stats(String title)
  {
    int counts = 0;
    int errors = 0;
    long time = 0;
    int countAbove = 0;
    double averageTime = 0;
    PrintStream logFileHandle = null;
    
    try {
    	logFileHandle = new PrintStream(new FileOutputStream (logFile, true));
    }
    catch (FileNotFoundException e) {
    	e.printStackTrace();
    }

    logFileHandle.println(title+" statistics");
    String format = "%-20s %10s %10s %10s %10sms %10sms %10sms";
    logFileHandle.println(String.format(format, "State name", "% of total", "Count", "Errors", "Minimum", "Maximum", "Average Time"));
    // Display statistics for each state
    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      counts += count[i];
      errors += error[i];
      time += totalTime[i];
      countAbove += countAboveLimit[i];
    }

    for (int i = 0 ; i < getNbOfStats() ; i++)
    {
      if (count[i] != 0)
      {
    	  double percent = 0;
    	  if ((counts > 0) && (count[i] > 0))
              percent = 100.0*count[i]/counts;
    	  double success = count[i] - error[i];
    	  if (success > 0)
    		  success = totalTime[i]/success;
    	  else
    		  success = 0;
    	  logFileHandle.println(String.format(
						format, TransitionTable.getStateName(i),
						percent, count[i], error[i], minTime[i], maxTime[i],
						success));
    	averageTime += success;
      }
    }
    logFileHandle.println("% above latency limit = " + (countAbove*100.0/counts));
    logFileHandle.println("% error = " + (errors*100.0/(counts+errors)));

    logFileHandle.println("% average time = " + (averageTime/getNbOfStats()) );
  }
}
