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
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;
	
	public TFTPSim()
	{
	   try {
	      // Construct a datagram socket and bind it to port 68 on the local host machine.
	      // This socket will be used to receive UDP Datagram packets from clients.
	      receiveSocket = new DatagramSocket(68);
	
	   } catch (SocketException se) {
	      se.printStackTrace();
	      System.exit(1);
	   }
	}
	
	public DatagramPacket FormPacket() throws UnknownHostException
	{
		// Construct a DatagramPacket for receiving packets up
	    // to 516 bytes long (the length of the byte array).
		byte[] data = new byte[516];	
	  
	      
	    receivePacket = new DatagramPacket(data, data.length,InetAddress.getLocalHost(),68);
	
	    System.out.println("TFTPSim: Waiting for packet");
	    // Block until a datagram packet is received from receiveSocket.
	    try {
	       receiveSocket.receive(receivePacket);
	    } catch (IOException e) {
	       e.printStackTrace();
	       System.out.println("Error recieving request");
	       System.exit(1);
	    }
	
	    // Process the received datagram.
	    System.out.println("TFTPSim: Packet received:");
	    System.out.println("From host: " + receivePacket.getAddress());
	    System.out.println("Host port: " + receivePacket.getPort());
	    System.out.println("Length: " + receivePacket.getLength());
	
	      
	     // data = receivePacket.getData();
	      
	    // Now pass it on to the server (to port 69)
	    // Construct a datagram packet that is to be sent to a specified port on a specified host.
	    // The arguments are:
	    //  msg - the message contained in the packet (the byte array)
	    //  the length we care about - k+1
	    //  InetAddress.getLocalHost() - the Internet address of the destination host.
	    //  69 - the destination port number on the destination host.
	    // int length = receivePacket.getLength();
	       
	    // end of loop
	    return receivePacket;
	
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




