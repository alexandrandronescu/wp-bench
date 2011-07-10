package master;

import java.io.IOException;

public class Master{
	private int slavesNb;
	private int port;
	private int usersNbMin;
	private int usersNbMax;
	private int readWrite;	
	
	public Master(int slavesNb, int port) {
		super();
		this.slavesNb = slavesNb;
		this.port = port;
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		int slavesNb = 1;
		int port = 6700;
		int interval;
		int i;
		int users, activeUsersMin = 6, activeUsersMax = 10;
		users = activeUsersMax;
		
		Master master = new Master(slavesNb, port);
		MessageGenerator msgGen = new MessageGenerator(); 
		TaskDaemon[] taskPool = new TaskDaemon[master.slavesNb];
	  
		interval = ( activeUsersMax - activeUsersMin ) / slavesNb;
		for(i=0; i < master.slavesNb-1; i++) {
			taskPool[i] = new TaskDaemon(master.port, interval*i, interval*(i+1), master.readWrite, msgGen);
		}
		taskPool[i] = new TaskDaemon(master.port, interval*i, activeUsersMax, master.readWrite, msgGen);
		
		taskPool[0].createNewUsers(users);
		taskPool[0].run();
		
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].startSimulation();
			taskPool[i].run();
		}
		
		activeUsersMin = 10;
		activeUsersMax = 20;
		interval = ( activeUsersMax - activeUsersMin ) / slavesNb;
		for(i=0; i < master.slavesNb-1; i++) {
			taskPool[i].addUsers(interval*i, interval*(i+1));
			taskPool[i].run();
		}
		taskPool[i].addUsers(interval*i, activeUsersMax);
		taskPool[i].run();
		
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].endSimulation();
			taskPool[i].run();
		}
	}
}
