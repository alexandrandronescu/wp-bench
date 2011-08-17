package controller;

import java.io.*;
import java.net.*;

/**
 * Emulates a generic TCP connection session between a client and a server.
 *
 * @author <a href="mailto:a.andronescu@student.vu.nl">Alexandra Andronescu</a> 
 * @version 1.0
 */
public abstract class Daemon extends Thread {
	private Socket socket = null;
	BufferedReader inFromServer = null;
	DataInputStream fileFromServer = null;
	DataOutputStream outToServer = null;
	private int threadId;
	private int port;
	private String address;
	
	public Daemon(int port, String address, int threadId) {
		super();
		this.port = port;
		this.address = address;
		this.threadId = threadId;
	}
	
	/**
	 * Connects to the server.
	 */
	protected void connectToSlave() {
		try {
			if(socket == null) {
				socket = new Socket(address, port); //localhost -> IP
				inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				fileFromServer = new DataInputStream(socket.getInputStream());
				outToServer = new DataOutputStream(socket.getOutputStream());
			}
		}
		catch (IOException e) {
			System.out.println("IOException in connectToSlave() method!");
		}
	}
	
	/**
	 * Disconnects to the server.
	 */
	protected void disconnectFromSlave() {
		try {
			socket.close();
		}
		catch (IOException e) {
			System.out.println("IOException in disconnectToSlave() method!");
		}
		socket = null;
	}
	
	/**
	 * Receives a message from the server.
	 * 
	 * @return the message received from a Workload Generator  
	 */
	protected String getMessage() {
		String message = null;
		try {
			if ( socket != null ) {
				message = inFromServer.readLine();
				if ( message == null )
					System.out.println("Error: Received message NULL.");
			}
			else
				System.out.println("Error: socket NULL");
		}
		catch (IOException e) {
			System.out.println("IOException in getMessage() method!");
		}
		return message;
	}
	
	/**
	 * Received a file from the server.
	 */
	protected boolean getFile() {
		try {
			String line, newLine;
			FileOutputStream bufWriter = new FileOutputStream(new File("config/log" + threadId + ".txt"));
			byte bytes[] = new byte[512];
			while((line=inFromServer.readLine())!=null) {
				newLine = line + '\n';
				bytes=newLine.getBytes();
				bufWriter.write(bytes);
			}  
			bufWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Sends a command to the server.
	 * 
	 * @param message command to be sent
	 */
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
