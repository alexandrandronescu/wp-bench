package controller;

import java.io.*;
import java.util.*;

/**
 * Sends commands to the WOrkload Generators. The possible commands are the following:
 * <pre>
 * START_SIMULATION	: start simulation of the user sessions
 * END_SIMULATION	: stop simulation of the user sessions
 * GET_LOG			: retrieve statistics log
 * ADD_USERS		: add more user sessions
 * CREATE_USERS		: create more user accounts
 * EXIT				: exit program
 * </pre>
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a> 
 * @version 1.0
 */
public class Controller {
	private ControllerProperties properties = null;
	private int slavesNb;
	private int port;
	private int usersNb = 0;
	private double readWrite;
	private int userCredentials = 0;
	TaskDaemon[] taskPool = null;
	
	/**
	 * Creates a new <code>Controller</code> instance.
	 * Loads the properties and creates a thread for each Workload Generator slave.
	 * 
	 * @param 
	 */
	public Controller(String propertiesFile) {
		super();
		properties = new ControllerProperties(propertiesFile);
		try {
			properties.checkPropertiesFile();
		}catch (Exception e) {
			System.exit(1);
		}
		
		this.usersNb = properties.getUsersNb();
		this.slavesNb = properties.getSlavesNb();
		this.port = properties.getPort();
		
		MessageGenerator msgGen = new MessageGenerator(); 
		taskPool = new TaskDaemon[slavesNb];
		for(int i=0; i < slavesNb; i++) {
			taskPool[i] = new TaskDaemon(port, properties.getAddress(i), msgGen, i);
		}
	}
	
	/**
	 * Creates a new <code>Controller</code> instance.
	 * Adds users to the counter.
	 * 
	 * @param 
	 */
	private void addUsers(int usersNb) {
		this.usersNb += usersNb;
	}

	public int getUsersNumber() {
		return this.usersNb;
	}
	
	/**
	 * Computes the number of users for a Workload Generator slave. 
	 */
	public int getInterval() {
		return usersNb / slavesNb;
	}

	public int getInterval(int number) {
		return number / slavesNb;
	}
	
	/**
	 * Creates new user accounts on the WordPress website.
	 */
	public void createUsers() throws InterruptedException {
		if(userCredentials < usersNb) {
			taskPool[0].createNewUsers(0, usersNb);
			taskPool[0].run();
			taskPool[0].join();
			userCredentials = usersNb;
		}
	}
	
	/**
	 * Starts the simulation process.
	 */
	public void startSimulation () throws InterruptedException{
		int interval = getInterval();
		int i;
		for(i=0; i < slavesNb; i++) {
			taskPool[i].startSimulation(i*interval, (i+1)*interval, readWrite);
			taskPool[i].run();
		}
		
		for(i=0; i < slavesNb; i++)
			taskPool[i].join();
		
		try{
			Thread.sleep(200000);
		}catch (Exception e) {
			System.out.println("Error sleeping!");
		}
	}
	
	/**
	 * Creates new user accounts and starts more user sessions using the new accounts. 
	 * 
	 * @param moreUsers the number of users to be created and started
	 */
	public void addMoreUsers(int moreUsers) throws InterruptedException {
		int i, interval;
		if ( userCredentials < (usersNb+moreUsers) ) {
			System.out.println("Sending COMMAND_CREATE_USERS message!");
			taskPool[0].createNewUsers(usersNb, usersNb + moreUsers);
			taskPool[0].run();
			taskPool[0].join();
			userCredentials = usersNb;
		}
		
		interval = getInterval(moreUsers);
		for(i=0; i<slavesNb; i++) {
			if((i+1)*interval<=moreUsers)
				taskPool[i].addUsers(usersNb + i*interval, usersNb + (i+1)*interval);
			else
				taskPool[i].addUsers(usersNb + i*interval, usersNb + moreUsers);
			taskPool[i].run();
		}
		addUsers(moreUsers);
		
		for(i=0; i < slavesNb; i++)
			taskPool[i].join();

		try{
			Thread.sleep(200000);
		}catch (Exception e) {
			System.out.print("Error sleeping!");
		}
	}
	
	/**
	 * Ends all users sessions on all the Workload Generator machines.
	 */
	public void endSimulation () throws InterruptedException {
		int i;
		for(i=0; i < slavesNb; i++) {
			taskPool[i].endSimulation();
			taskPool[i].run();
		}
		
		for(i=0; i < slavesNb; i++)
			taskPool[i].join();
	}
	
	/**
	 * Requests log files from each Workload Generator and writes them into local files.
	 */
	public void getLog () throws InterruptedException {
		int i;
		for(i=0; i < slavesNb; i++) {
			taskPool[i].getLog();
			taskPool[i].run();
		}
		for(i=0; i < slavesNb; i++)
			taskPool[i].join();
		
		aggregateLogFiles();
	}
	
	/**
	 * Exists the program, including the Workload Generators and current Controller. 
	 */
	public void exitProgram () throws InterruptedException {
		int i;
		for(i=0; i < slavesNb; i++) {
			taskPool[i].exit();
			taskPool[i].run();
		}
		
		for(i=0; i < slavesNb; i++)
			taskPool[i].join();
	}
	
	/**
	 * Aggregates log files received from multiple Workload Generators and writes them 
	 * into a generic log file.
	 */
	public void aggregateLogFiles() {
		String line;
		BufferedReader fileHandle = null;
		BufferedWriter aggregatedFileHandle = null;
		Vector<Vector<Object>> secondsAverageTime = new Vector<Vector<Object>>();
		
		for(int i=0;i<slavesNb;i++) {
			String fileName = "config/log"+i+".txt";
			File f=new File(fileName);
			try {
		        if ( f.exists() ) { 
		            fileHandle = new BufferedReader(new FileReader(fileName));
		            
					long second = 0, currentSecond = -1, responseTime = 0, averageResponseTime = 0, requestCount = 0;

					while ( (line=fileHandle.readLine()) != null && !line.startsWith("WordPressBench  statistics")) {
	            		StringTokenizer st = new StringTokenizer(line);
	            		if ( st.hasMoreTokens() )
	            			second = Long.parseLong(st.nextToken());
	            		
	            		if ( st.hasMoreTokens() )
	            			responseTime = Long.parseLong(st.nextToken());
	            		
	            		if ( currentSecond >= 0 ) {
	            			if ( second == currentSecond ) {
	            				averageResponseTime += responseTime;
		            			requestCount++;
	            			}
	            			else {
	            				Vector<Object> point = new Vector<Object>(2);
	            				point.addElement(currentSecond);
	            				point.addElement((double)(averageResponseTime/requestCount));
	            				secondsAverageTime.add(point);
	            				
	            				currentSecond = second;
		            			averageResponseTime = responseTime;
		            			requestCount = 1;
	            			}
	            		}
	            		else {
	            			currentSecond = second;
	            			averageResponseTime += responseTime;
	            			requestCount = 1;
	            		}
	            		
	            		if ( secondsAverageTime.size() > 1024 ) {
	            			aggregatedFileHandle = new BufferedWriter(new FileWriter(properties.getLogName(), true));
	            			Vector<Object> pair = null;
	            			for (int index = 0; index<secondsAverageTime.size();index++) {
	                			pair = secondsAverageTime.get(index);
	            				aggregatedFileHandle.write(pair.get(0)+" "+pair.get(1));
	            				aggregatedFileHandle.newLine();
	            			}
	            			secondsAverageTime.clear();
	            			aggregatedFileHandle.close();
	            		}
		            }
		            	
		           	fileHandle.close();
		        }
			} 
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
        }
		
		try {
			aggregatedFileHandle = new BufferedWriter(new FileWriter(properties.getLogName(), true));
			Vector<Object> pair = null;
			for (int index = 0; index<secondsAverageTime.size();index++) {
    			pair = secondsAverageTime.get(index);
				aggregatedFileHandle.write(pair.get(0)+" "+pair.get(1));
				aggregatedFileHandle.newLine();
			}
			aggregatedFileHandle.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The sequence of commands to be sent to the Workload Generators.
	 */
	public static void main(String[] args) throws InterruptedException, IOException {		
		Controller master = new Controller(args[0]);
		
		master.createUsers();
		master.startSimulation();
		master.addMoreUsers(10);
		master.endSimulation();
		master.getLog();
		master.exitProgram();
	
	}
}
