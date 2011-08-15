package worker;

public class ConstantUsers extends Thread {
	boolean running = true;
	ClientEmulator client;
	
	public ConstantUsers(ClientEmulator client) {
		this.client = client;
		start();
	}
	
	public void startRunning () {
		running = true;
	}
	
	public void stopRunning () {
		running = false;
	}
	
	public boolean isRunning () {
		return running;
	}
	
	public void run() {
		while(true) {
			System.out.println("sessions.size()="+client.sessions.size() + " nbOfUsers="+client.nbOfUsers);
			while ( client.sessions.size() != client.nbOfUsers ) {
				for (int index=0; index < client.nbOfUsers; index++) {
					if (!client.sessions.elementAt(index).isAlive())
						client.sessions.elementAt(index).start();
				}
			}
			try{
				Thread.sleep(5000);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
}
