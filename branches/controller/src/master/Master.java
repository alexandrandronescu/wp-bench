package master;

import java.io.*;

public class Master{
	private int slavesNb;
	private int port;
	private int usersNb;
	private int readWrite;	
	
	public Master(int slavesNb, int port) {
		super();
		this.slavesNb = slavesNb;
		this.port = port;
	}
	
	public void setUsers(int usersNb) {
		this.usersNb = usersNb;
	}
	
	public int getInterval() {
		return usersNb / slavesNb;
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		int slavesNb = 1;
		int port = 6791;
		int i, interval;
		
		Master master = new Master(slavesNb, port);
		master.setUsers(1);
		MessageGenerator msgGen = new MessageGenerator(); 
		TaskDaemon[] taskPool = new TaskDaemon[master.slavesNb];
	  
		interval = master.getInterval();
		for(i=0; i < master.slavesNb-1; i++) {
			taskPool[i] = new TaskDaemon(master.port, interval*i, interval*(i+1), master.readWrite, msgGen);
		}
		taskPool[i] = new TaskDaemon(master.port, interval*i, master.usersNb, master.readWrite, msgGen);
		
		System.out.println("Sending CREATE_USERS message!");
		taskPool[0].createNewUsers(master.usersNb);
		taskPool[0].run();

		System.out.println("Sending START_SIMULATION message!");
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].startSimulation();
			taskPool[i].run();
		}
	/*	
		master.setUsersRange(10, 20);
		interval = master.getInterval();
		for(i=0; i < master.slavesNb-1; i++) {
			taskPool[i].addUsers(interval*i, interval*(i+1));
			taskPool[i].run();
		}
		taskPool[i].addUsers(interval*i, master.usersNbMax);
		taskPool[i].run();
	*/	
		for(i=0; i < master.slavesNb; i++) {
			taskPool[i].endSimulation();
			taskPool[i].run();
		}
	
	}
}
