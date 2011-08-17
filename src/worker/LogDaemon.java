package worker;

import java.io.*;
import java.util.concurrent.*;

/**
 * Class for logging info into blocking queue and writing data periodically into the log file.
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a>
 */

public class LogDaemon extends Thread {
	private BlockingQueue<LogEntry> queue;
	private PrintStream logFile;
	private int QUEUE_SIZE = 10;
	
	/**
	 * Class which contains a log file entry.
	 * 
	 * Each entry is composed as follow:
	 * <pre>
	 * time     	: current time relative to the beginning of the simulation
	 * responseTime	: WordPress Web server response time
	 * stateName	: type of the request issued to the WordPress Web server
	 * </pre>
	 * 
	 */
	class LogEntry {
		String time;	
		String responseTime;
		String stateName;

		
		LogEntry(long time, long responseTime, String stateName) {
			this.time = Long.toString(time);
			this.responseTime = Long.toString(responseTime);
			this.stateName = stateName;
		}
	}
	
	/**
	 * Creates a new <code>LogDaemon</code> instance.
	 * Loads the log file and creates new temporary buffer for thread-safety (blocking queue).
	 *
	 * @param logFileName log file name
	 */
	public LogDaemon (String logFileName) {
		try {
			logFile = new PrintStream(new FileOutputStream (logFileName));
		}
		catch (FileNotFoundException e) {
			System.out.println("No logfile!");
		}
		queue = new LinkedBlockingQueue<LogEntry>(QUEUE_SIZE);
	}
	
	/**
	 * Adds new log entry to the queue.
	 * 
	 * @param time current time relative to the beginning of the simulation
	 * @param responseTime WordPress Web server response time
	 * @param stateName	type of the request issued to the WordPress Web server
	 */
	public void log(long time, long responseTime, String stateName) {
		try {
			queue.put(new LogEntry(time, responseTime, stateName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes log queue content.
	 * 
	 * @param size queue length to be written in the log file 
	 */
	private void writeData(int size) {
		try {
			LogEntry element;
			for (int i=0; i<size;i++) {
				element = queue.poll(365, TimeUnit.DAYS);
				logFile.println(element.time + " " + element.responseTime + " " + element.stateName);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes queue content into the logfile once it reaches a certain threshold.
	 */
	public void run() {
		while (!ClientEmulator.isEndOfSimulation()) {
			if ( queue.remainingCapacity() == 0 ) {
				writeData(queue.size());
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(ClientEmulator.isEndOfSimulation()) {
			writeData(queue.size());
		}
	}
}
