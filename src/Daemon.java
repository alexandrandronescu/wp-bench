package controller;

import java.io.*;
import java.net.*;

public abstract class Daemon extends Thread {
	private Socket socket = null;
	BufferedReader inFromServer = null;
	DataInputStream fileFromServer = null;
	DataOutputStream outToServer = null;
	private int threadId;
	private int port;
	
	public Daemon(int port, int threadId) {
		super();
		this.port = port;
		this.threadId = threadId;
	}
	
	protected void connectToSlave() {
		try {
			if(socket == null) {
				socket = new Socket("localhost", port);
				inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				fileFromServer = new DataInputStream(socket.getInputStream());
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
	
	protected boolean getFile() {
		try {
			String line, newLine;
			FileOutputStream bufWriter = new FileOutputStream(new File("log" + threadId + ".txt"));
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
