package worker;

import java.io.*;
import java.net.*;

/**
 * Receives new messages from controller and sends acknowledgement or logfile back.
 * 
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a>
 */

public class ConnectionDaemon {
	ServerSocket welcomeSocket = null;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	PrintWriter fileToClient = null;
	Socket connectionSocket = null;
	private int port;
	
	/**
	 * Creates a new <code>ConnectionDaemon</code> instance.
	 * A new socket is created for waiting new connections.
	 *
	 * @param port waiting port
	 */
	public ConnectionDaemon(int port) {
		super();
		this.port = port;
		try {
			welcomeSocket = new ServerSocket(this.port);
		}
		catch (IOException e) {
			System.out.println("IOException in SlaveDaemon() constructor!");
		}
	}
	
	/**
	 * Waits connections from the controller.
	 */
	protected void waitForConnection () {
		try {
			if(welcomeSocket != null) {
				connectionSocket =  welcomeSocket.accept();
				inFromClient =
	               new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				fileToClient = new PrintWriter(connectionSocket.getOutputStream(),true);
			}
		}
		catch (IOException e) {
			System.out.println("IOException in waitForConnection() method!");
		}
	}
	
	/**
	 * Closes the connection with the controller.
	 */
	protected void closeConnection () {
		try {
			if(connectionSocket != null) {
				connectionSocket.close();
			}
		}
		catch (IOException e) {
			System.out.println("IOException in closeConnection() method!");
		}
	}
	
	/**
	 * Handles the connection from the controller.
	 * 
	 * @return message received from controller
	 */
	protected String getMessage () {
		String message = null;
		try {
			if(welcomeSocket != null && connectionSocket != null)
				message = inFromClient.readLine();
		}
		catch (IOException e) {
			System.out.println("IOException in getMessage() method!");
			System.out.println(e.getMessage());
			System.out.println(e.getStackTrace());
		}
		catch (NullPointerException e) {
			System.out.println("NullPointerException in getMessage() method!");
			System.out.println(e.getMessage());
			System.out.println(e.getStackTrace());
		}
		return message;
	}
	
	/**
	 * Sends response message to the controller.
	 * 
	 * @param message message to be sent
	 * 
	 */
	protected void sendMessage (String message) {
		try {
			if(welcomeSocket != null && connectionSocket != null)
				outToClient.writeBytes(message + '\n');
		}
		catch (IOException e) {
			System.out.println("IOException in sendMessage() method!");
		}
	}
	
	/**
	 * Sends logfile to the controller.
	 * 
	 * @param fileName logfile name
	 * 
	 */
	protected void sendFile (String fileName) {
		try {
			if(welcomeSocket != null && connectionSocket != null) {
				String line = "";

				File f=new File(fileName);
                if(f.exists()) { 
                    BufferedReader fileHandle=new BufferedReader(new FileReader(fileName));
                    while((line=fileHandle.readLine())!=null)
                    {
                    	fileToClient.write(line+"\n");
                        fileToClient.flush();
                    }
                    fileHandle.close();
                }
                else
                	System.out.println("Error: No log file!");
			}
		}
		catch (IOException e) {
			System.out.println("IOException in sendFile() method!");
		}
	}
}
