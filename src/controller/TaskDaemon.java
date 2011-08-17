package controller;

/**
 * Defines ad implements all the possible actions available for the controller:
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
public class TaskDaemon extends Daemon {
	int usersNbMin, usersNbMax, command;
	double readWrite;
	MessageGenerator msgGen;
	String logFile, responseMessage;
	
	public final int COMMAND_START_SIMULATION = 0;
	public final int COMMAND_END_SIMULATION = 1;
	public final int COMMAND_GET_LOG = 2;
	public final int COMMAND_ADD_USERS = 3;
	public final int COMMAND_CREATE_USERS = 4;
	public final int COMMAND_EXIT = 5;
	
	public TaskDaemon(int port, String address, MessageGenerator msgGen, int threadId) {
		super(port, address, threadId);
		this.msgGen = msgGen;
		this.command = COMMAND_START_SIMULATION;
	}
	
	public void startSimulation(int usersNbMin, int usersNbMax, double readWrite) {
		this.usersNbMin = usersNbMin;
		this.usersNbMax = usersNbMax;
		this.readWrite = readWrite;
		this.command = COMMAND_START_SIMULATION;
	}
	
	public void endSimulation() {
		this.command = COMMAND_END_SIMULATION;
		
	}
	
	public void getLog() {
		this.command = COMMAND_GET_LOG;
	}

	public void addUsers(int usersNbMin, int usersNbMax) {
		this.command = COMMAND_ADD_USERS;
		this.usersNbMin = usersNbMin;
		this.usersNbMax = usersNbMax;
	}
	
	public void createNewUsers(int newUsersNbMin, int newUsersNbMax) {
		this.command = COMMAND_CREATE_USERS;
		this.usersNbMin = newUsersNbMin;
		this.usersNbMax = newUsersNbMax;
	}
	
	public void exit() {
		this.command = COMMAND_EXIT;
	}
	
	public void setUsersNb(int usersNbMin, int usersNbMax) {
		this.usersNbMin = usersNbMin;
		this.usersNbMax = usersNbMax;
	}
	
	public void setCommand(int command) {
		this.command = command;
	}
	
	public String getLogFile() {
		return this.logFile;
	}
	
	/**
	 * Creates new connections, runs the command, and disconnects.
	 */
	public void run() {
		connectToSlave();
		switch(command) {
			case COMMAND_START_SIMULATION:
				System.out.format("Sending START SIMULATION command (usersNbMin=%d, usersNbMax=%d, readWrite=%f)...%n", usersNbMin, usersNbMax, readWrite);
				sendMessage(msgGen.startSimulation(usersNbMin, usersNbMax, readWrite));
				responseMessage = getMessage();
				System.out.println("Received message: " + responseMessage);
			break;
			case COMMAND_END_SIMULATION:
				System.out.println("Sending END SIMULATION command...");
				sendMessage(msgGen.endSimulation());
				responseMessage = getMessage();
				System.out.println("Received message: " + responseMessage);
				break;
			case COMMAND_GET_LOG:
				System.out.println("Sending GET LOG command...");
				sendMessage(msgGen.getLog());
				getFile();
				System.out.println("Received log file...");
			break;
			case COMMAND_ADD_USERS:
				System.out.format("Sending ADD USERS command (usersNbMin=%d, usersNbMax=%d)...%n", usersNbMin, usersNbMax);
				sendMessage(msgGen.addUsers(usersNbMin, usersNbMax));
				responseMessage = getMessage();
				System.out.println("Received message: " + responseMessage);
			break;
			case COMMAND_CREATE_USERS:
				System.out.format("Sending CREATE USERS command (usersNbMin=%d, usersNbMax=%d)...%n", usersNbMin, usersNbMax);
				sendMessage(msgGen.createUsers(usersNbMin, usersNbMax));
				responseMessage = getMessage();
				System.out.println("Received message: " + responseMessage);
			break;
			case COMMAND_EXIT:
				System.out.println("Sending EXIT command...");
				sendMessage(msgGen.exit());
				responseMessage = getMessage();
				System.out.println("Received message: " + responseMessage);
			break;
		}
		disconnectFromSlave();
	}
}
