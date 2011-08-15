package controller;

import java.io.*;
import java.util.*;

public class Controller {
	private int slavesNb;
	private int port;
	private int usersNb = 0;
	private int readWrite;
	private int userCredentials = 0;
	private int JOIN_TIMEOUT = 20000; 
	
	public Controller(int slavesNb, int port) {
		super();
		this.slavesNb = slavesNb;
		this.port = port;
	}
	
	public void addUsers(int usersNb) {
		this.usersNb += usersNb;
	}

	public int getUsersNumber() {
		return this.usersNb;
	}
	
	public int getInterval() {
		return usersNb / slavesNb;
	}

	public int getInterval(int number) {
		return number / slavesNb;
	}
	
	public void aggregateLogFiles() {
		String line;
		BufferedReader fileHandle = null;
		BufferedWriter aggregatedFileHandle = null;
		Vector<Vector<Object>> secondsAverageTime = new Vector<Vector<Object>>();
		
		for(int i=0;i<slavesNb;i++) {
			String fileName = "log"+i+".txt";
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
	            				Vector point = new Vector(2);
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
	            			aggregatedFileHandle = new BufferedWriter(new FileWriter("log.txt", true));
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
			aggregatedFileHandle = new BufferedWriter(new FileWriter("log.txt", true));
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
	
	public static void main(String[] args) throws InterruptedException, IOException {
		int slavesNb = 1;
		int port = 6781;
		int i, interval;
		
		Controller master = new Controller(slavesNb, port);
		master.addUsers(200);
		MessageGenerator msgGen = new MessageGenerator(); 
		TaskDaemon[] taskPool = new TaskDaemon[master.slavesNb];
	  
		interval = master.getInterval();
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i] = new TaskDaemon(master.port, msgGen, i);
		}

////////////////////////////////////////////////////////////////////////////////////////////
		System.out.println("Sending CREATE_USERS message! USERS:"+master.usersNb);
		if(master.userCredentials < master.usersNb) {
			taskPool[0].createNewUsers(0, master.usersNb);
			taskPool[0].run();
			taskPool[0].join();
			master.userCredentials = master.usersNb;
		}
////////////////////////////////////////////////////////////////////////////////////////////
		System.out.println("Sending START_SIMULATION message!");
		interval = master.getInterval();
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].startSimulation(i*interval, (i+1)*interval, 0.4);
			taskPool[i].run();
		}
		
		for(i=0; i < master.slavesNb; i++)
			taskPool[i].join();
		
		try{
			Thread.sleep(900000);
		}catch (Exception e) {
			System.out.print("Not sleeping");
		}
//////////////////////////////////////////////////////////////////////////////////////////////
//		System.out.println("Sending ADD_USERS commands!");
//		int moreUsers = 30;
//		if ( master.userCredentials < (master.usersNb+moreUsers) ) {
//			System.out.println("Sending COMMAND_CREATE_USERS message!");
//			taskPool[0].createNewUsers(master.usersNb, master.usersNb + moreUsers);
//			taskPool[0].run();
//			taskPool[0].join();
//			master.userCredentials = master.usersNb;
//		}
//		
//		System.out.println("Sending COMMAND_ADD_USERS message!");
//		interval = master.getInterval(moreUsers);
//		for(i=0; i<master.slavesNb; i++) {
//			if((i+1)*interval<=moreUsers)
//				taskPool[i].addUsers(master.usersNb + i*interval, master.usersNb + (i+1)*interval);
//			else
//				taskPool[i].addUsers(master.usersNb + i*interval, master.usersNb + moreUsers);
//			taskPool[i].run();
//		}
//		master.addUsers(moreUsers);
//		
//		for(i=0; i < master.slavesNb; i++)
//			taskPool[i].join();
//
//		try{
//			Thread.sleep(400000);
//		}catch (Exception e) {
//			System.out.print("Not sleeping");
//		}
////////////////////////////////////////////////////////////////////////////////////////////
		System.out.println("Sending END_SIMULATION message!");
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].endSimulation();
			taskPool[i].run();
		}
		
		for(i=0; i < master.slavesNb; i++)
			taskPool[i].join();
		
////////////////////////////////////////////////////////////////////////////////////////////
		System.out.println("Sending COMMAND_GET_LOG message!");
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].getLog();
			taskPool[i].run();
		}
		for(i=0; i < master.slavesNb; i++)
			taskPool[i].join();
		
		master.aggregateLogFiles();
		
////////////////////////////////////////////////////////////////////////////////////////////
		System.out.println("Sending EXIT message!");
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].exit();
			taskPool[i].run();
		}
		
		for(i=0; i < master.slavesNb; i++)
			taskPool[i].join();
	
	}
}
