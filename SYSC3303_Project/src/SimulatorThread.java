import java.io.*;
import java.net.*;


public class SimulatorThread implements Runnable {
	
	private DatagramSocket socket;
	private int clientPort = 0, serverPort = 0;
	private DatagramPacket firstPacket;
	private InetAddress host;
	
	public SimulatorThread(DatagramPacket firstPacket){
		
		//get the host address
		
		try {
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			System.exit(1);
		}
		
		//create new socket
		//re3
		try{
			socket = new DatagramSocket();
		} catch (SocketException e){
			System.exit(1);
		}
		
		//remember client's port
		//re4
		clientPort = firstPacket.getPort();
		
		//send the received packet to localhost at port 69
		//re5
		DatagramPacket myPacket = new DatagramPacket(firstPacket.getData(), firstPacket.getLength(), host, 69);
		
		try{
			socket.send(myPacket);
		} catch (IOException e){
			System.exit(1);
		}
		
		//remember the first packet, so run() can send it
		this.firstPacket = firstPacket;
	}
	
	public void run(){
		//send the received packet to localhost at port 69
		//re5
		try{
			socket.send(firstPacket);
		} catch (IOException e){
			System.exit(1);
		}
		
		
		//the loop that represent re6 to re10
		while(true){
			//receive packet on socket
			//re6
			DatagramPacket receivedPacket = new DatagramPacket(new byte[ErrorSimulator.MAX_PACKET_BUFFER_SIZE], ErrorSimulator.MAX_PACKET_BUFFER_SIZE);
			try{
				socket.receive(receivedPacket);
			} catch (IOException e){
				System.exit(1);
			}
			
			//determines if server port number needs to be remember
			//if so, remember it
			//re7
			if(serverPort == 0){
				serverPort = receivedPacket.getPort();
			}
			
			//determine where to send the packet and send it to the appropriate place
			//re8, re9, re10
			
			//re9: send to server if port = client
			if(receivedPacket.getPort() == clientPort){
				try{
					DatagramPacket myPacket = new DatagramPacket(firstPacket.getData(), firstPacket.getLength(), host, serverPort);
					socket.send(myPacket);
				} catch (IOException e){
					System.exit(1);
				}
			} 
			//re10: send to client if port = server
			else if (receivedPacket.getPort() == serverPort){
				try{
					DatagramPacket myPacket = new DatagramPacket(firstPacket.getData(), firstPacket.getLength(), host, clientPort);
					socket.send(myPacket);
				} catch (IOException e){
					System.exit(1);
				}
			}
		}
	}
}
