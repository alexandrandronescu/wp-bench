package master;

public class MessageGenerator {
	public final int COMMAND_START_SIMULATION = 0;
	public final int COMMAND_END_SIMULATION = 1;
	public final int COMMAND_GET_LOG = 2;
	public final int COMMAND_ADD_USERS = 3;
	public final int COMMAND_CREATE_USERS = 4;
	
	public MessageGenerator() {
		super();
	}
	
	public String startSimulation(int usersNbMin, int usersNbMax, int readWrite) {
		return COMMAND_START_SIMULATION + usersNbMin + " " + usersNbMax + " " + readWrite;
	}
	
	public String endSimulation() {
		return COMMAND_END_SIMULATION + "";
	}
	
	public String getLog() {
		return COMMAND_GET_LOG + "";
	}
	
	public String addUsers(int usersNbMin, int usersNbMax) {
		return COMMAND_ADD_USERS + usersNbMin + " " + usersNbMax;
	}
	
	public String createUsers(int newUsers) {
		return COMMAND_CREATE_USERS + " " + newUsers;
	}
}
