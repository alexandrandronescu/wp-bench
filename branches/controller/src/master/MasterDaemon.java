package master;

import java.io.*;
import java.net.*;

public abstract class MasterDaemon extends Thread {
	private Socket socket = null;
	BufferedReader inFromServer = null;
	DataOutputStream outToServer = null;
	private int port;
	
	public MasterDaemon(int port) {
		super();
		this.port = port;
	}
	
	protected void connectToSlave() {
		try {
			if(socket == null) {
				socket = new Socket("localhost", port);
				inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				outToServer = new DataOutputStream(socket.getOutputStream());
			}
		}
		catch (IOException e) {
			System.out.println("IOException in connectToSlave() method!");
		}
	}
	
	protected void disconnectFromSlave() {
		try {
			socket.close();
		}
		catch (IOException e) {
			System.out.println("IOException in disconnectToSlave() method!");
		}
		socket = null;
	}
	
	protected String getMessage() {
		String message = null;
		try {
			if ( socket != null ) {
				message = inFromServer.readLine();
				if(message!=null)
					System.out.println("Message:"+message);
				else
					System.out.println("message var is NULL");
			}
			else
				System.out.println("socket var is NULL");
		}
		catch (IOException e) {
			System.out.println("IOException in getMessage() method!");
		}
		return message;
	}
	
	protected void sendMessage(String message) {
		try {
			if ( socket != null )
				outToServer.writeBytes(message + '\n');		
		}
		catch (IOException e) {
			System.out.println("IOException in sendMessage() method!");
			System.out.println(e.getMessage());
			System.out.println(e.getStackTrace());
		}
	}
}
