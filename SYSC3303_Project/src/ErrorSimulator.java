import java.net.*;
import java.io.*;


public class ErrorSimulator {

	final int PORT = 68;
	private DatagramSocket socket;
	private DatagramPacket packet;
	public static final int MAX_PACKET_BUFFER_SIZE = 516;
	
	//create a socket bound to port 68
	public ErrorSimulator(){
		try{
			socket = new DatagramSocket(PORT);
		} catch (SocketException e){
			System.exit(1);
		}
	}
	
	//listen on port 68 for packets
	//put the information on to the variables to prepare to pass to new thread
	public DatagramPacket listen(){
		packet = null;
		packet = new DatagramPacket(new byte[MAX_PACKET_BUFFER_SIZE], MAX_PACKET_BUFFER_SIZE);
		
		try{
			socket.receive(packet);
		} catch (IOException e){
			System.exit(1);
		}
		
		return packet;
	}
	
	
	public static void main(String[] args) {
		//create the error simulator
		ErrorSimulator errorSimulator = new ErrorSimulator();
		
		//loop for server
		for(int i = 1; ; i++){
			//listen for requests on port 68 (re1)		
			//once received, start new thread (re2)
			Thread newConnection = new Thread(new SimulatorThread(errorSimulator.listen()), "SimThread " + i);
			newConnection.start();
		}
		
	}

}
