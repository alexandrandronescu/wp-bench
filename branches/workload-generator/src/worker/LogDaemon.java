package worker;

import java.io.*;
import java.util.concurrent.*;

public class LogDaemon extends Thread {
	private BlockingQueue<Entry> queue;
	private PrintStream logFile;
	
	class Entry {
		String time;	
		String responseTime;
		String stateName;

		Entry(long time, long responseTime, String stateName) {
			this.time = Long.toString(time);
			this.responseTime = Long.toString(responseTime);
			this.stateName = stateName;
		}
	}
	
	public LogDaemon (String logFileName) {
		try {
			logFile = new PrintStream(new FileOutputStream (logFileName));
		}
		catch (FileNotFoundException e) {
			System.out.println("No logfile!");
		}
		queue = new LinkedBlockingQueue<Entry>(10);
	}
	
	public void log(long time, long responseTime, String stateName) {
		try {
			queue.put(new Entry(time, responseTime, stateName));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void writeData(int size) {
		try {
			Entry element;
			for (int i=0; i<size;i++) {
				element = queue.poll(365, TimeUnit.DAYS);
				logFile.println(element.time + " " + element.responseTime + " " + element.stateName);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
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
