package master;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public abstract class Daemon extends Thread {
	private Socket socket = null;
	private int port;
	
	public Daemon(int port) {
		super();
		this.port = port;
	}
	
	protected void connectToSlave() {
		this.port = port;
		try {
			if(socket == null)
				socket = new Socket("localhost", port);
		}
		catch (IOException e) {}
	}
	
	protected void disconnectFromSlave() {
		try {
			socket.close();
		}
		catch (IOException e) {}
		socket = null;
	}
	
	protected String getMessage() {
		String message = null;
		try {
			if ( socket != null ) {
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				message = inFromServer.readLine();				
			}
		}
		catch (IOException e) {
		}
		return message;
	}
	
	protected void sendMessage(String message) {
		try {
			if ( socket != null ) {
				DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
				outToServer.writeBytes(message + '\n');		
			}
		}
		catch (IOException e) {
		}
	}
}
