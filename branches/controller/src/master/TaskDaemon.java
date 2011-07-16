package master;

public class TaskDaemon extends MasterDaemon{
	int usersNbMin, usersNbMax, readWrite, command;
	MessageGenerator msgGen;
	String logFile, responseCode;
	
	public final int COMMAND_START_SIMULATION = 0;
	public final int COMMAND_END_SIMULATION = 1;
	public final int COMMAND_GET_LOG = 2;
	public final int COMMAND_ADD_USERS = 3;
	public final int COMMAND_CREATE_USERS = 4;
	
	public TaskDaemon(int port, int usersNbMin, int usersNbMax, int readWrite, MessageGenerator msgGen) {
		super(port);
		setUsersNb(usersNbMin, usersNbMax);
		this.msgGen = msgGen;
		this.readWrite = readWrite;
		this.command = COMMAND_START_SIMULATION;
	}
	
	public void startSimulation() {
		this.command = COMMAND_START_SIMULATION;
	}
	
	public void endSimulation() {
		this.command = COMMAND_GET_LOG;
		
	}
	
	public void getLog() {
		this.command = COMMAND_END_SIMULATION;
	}

	public void addUsers(int usersNbMin, int usersNbMax) {
		this.command = COMMAND_ADD_USERS;
		this.usersNbMin = usersNbMin;
		this.usersNbMax = usersNbMax;
	}
	
	public void createNewUsers(int newUsers) {
		this.command = COMMAND_CREATE_USERS;
		this.usersNbMax = newUsers;	
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
	
	public void run() {
		connectToSlave();
		switch(command) {
			case COMMAND_START_SIMULATION:
				sendMessage(msgGen.startSimulation(usersNbMin, usersNbMax, readWrite));
				responseCode = getMessage();
			break;
			case COMMAND_END_SIMULATION:
				sendMessage(msgGen.endSimulation());
				responseCode = getMessage();
				break;
			case COMMAND_GET_LOG:
				sendMessage(msgGen.getLog());
				logFile = getMessage();
			break;
			case COMMAND_ADD_USERS:
				sendMessage(msgGen.addUsers(usersNbMin, usersNbMax));
				responseCode = getMessage();
			break;
			case COMMAND_CREATE_USERS:
				sendMessage(msgGen.createUsers(usersNbMax));
				responseCode = getMessage();
			break;
			default:
			break;
		}
		disconnectFromSlave();
	}
}
