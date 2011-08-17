package controller;

/**
 * Generates formatted messages to be sent to the Workload Generators.
 * Types of messages:
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
public class MessageGenerator {
	public final int COMMAND_START_SIMULATION = 0;
	public final int COMMAND_END_SIMULATION = 1;
	public final int COMMAND_GET_LOG = 2;
	public final int COMMAND_ADD_USERS = 3;
	public final int COMMAND_CREATE_USERS = 4;
	public final int COMMAND_EXIT = 5;
	
	public MessageGenerator() {
		super();
	}
	
	public String startSimulation(int usersNbMin, int usersNbMax, double readWrite) {
		return COMMAND_START_SIMULATION + " " + usersNbMin + " " + usersNbMax + " " + readWrite;
	}
	
	public String endSimulation() {
		return COMMAND_END_SIMULATION + "";
	}
	
	public String getLog() {
		return COMMAND_GET_LOG + "";
	}
	
	public String addUsers(int usersNbMin, int usersNbMax) {
		return COMMAND_ADD_USERS + " " + usersNbMin + " " + usersNbMax;
	}
	
	public String createUsers(int newUsersMin, int newUsersMax) {
		return COMMAND_CREATE_USERS + " " + newUsersMin + " " + newUsersMax;
	}
	
	public String exit() {
		return COMMAND_EXIT + "";
	}
}
