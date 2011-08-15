package worker;

import java.io.*;
import java.net.*;

public class ConnectionDaemon {
	ServerSocket welcomeSocket = null;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	PrintWriter fileToClient = null;
	Socket connectionSocket = null;
	private int port;
	
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
	
	protected void sendMessage (String message) {
		try {
			if(welcomeSocket != null && connectionSocket != null)
				outToClient.writeBytes(message + '\n');
		}
		catch (IOException e) {
			System.out.println("IOException in sendMessage() method!");
		}
	}
	
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
