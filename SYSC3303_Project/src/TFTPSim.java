// TFTPSim.java
// This class is the beginnings of an error simulator for TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  
// One socket (68) is used to receive from the client, and another to send/receive


import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim {

	// UDP datagram packets and sockets used to send / receive
	private DatagramSocket socket;
	
	public TFTPSim()
	{
	   try {
	      // Construct a datagram socket and bind it to port 68 on the local host machine.
	      // This socket will be used to receive UDP Datagram packets from clients.
	      socket = new DatagramSocket(68);
	   } catch (SocketException se) {
	      se.printStackTrace();
	      System.exit(1);
	   }
	}
	
	public DatagramPacket FormPacket() throws UnknownHostException
	{
		byte data[] = new byte[516];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			System.out.println("Waiting for packet from client on port "+socket.getPort());
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Recieved Packet");
		return packet;
	
	}
	
	public static void main( String args[] ) throws UnknownHostException
	{
		TFTPSim s = new TFTPSim();
		for(;;){
			Thread connect = new Thread ( new TFTPSimManager(s.FormPacket()));
	        connect.start();
		}
	}
}




