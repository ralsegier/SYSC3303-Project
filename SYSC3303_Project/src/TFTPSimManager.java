import java.io.*;
import java.net.*;
import java.util.*;

//define a TFTPSimManager class;
public class TFTPSimManager  implements Runnable
{
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	
	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket clientPacket, serverPacket;
	private DatagramSocket clientSocket, serverSocket;
	private boolean isFrist=true;
	private boolean exitNext;
	

	

	  byte[] data, sending;
	   
	   int clientPort,serverPort;

  public TFTPSimManager( DatagramPacket dp ) throws UnknownHostException {
  	// Get a reference to the data inside the received datagram.
    clientPort = dp.getPort();
    serverPort = 69;
    
    exitNext = false;
	 //  Construct  sendPacket to be sent to the server (to port 69)
  	serverPacket = new DatagramPacket(dp.getData(), dp.getLength(), InetAddress.getLocalHost(), serverPort);
  }

  public void run() {
	  try {
		System.out.println("Recieved Packet from client");
		serverSocket = new DatagramSocket();
		serverSocket.send(serverPacket);
		System.out.println("Forwarded packet to server");
		if(checkForEnd(serverPacket.getData()))return;
		
		byte data[] = new byte[BUFFER_SIZE];
		serverPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),serverPort);
		serverSocket.receive(serverPacket);
		serverPort = serverPacket.getPort();
		System.out.println("Recieved packet from server");
		clientPacket = new DatagramPacket (serverPacket.getData(),serverPacket.getLength(),InetAddress.getLocalHost(),clientPort);
		clientSocket.send(clientPacket);
		System.out.println("Forwarded packet to client");
		if(checkForEnd(clientPacket.getData()))return;
	
		for(;;) {
			if(clientToServer()) return;
			if(serverToClient()) return;
		}
		
	} catch (SocketException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
  }
  
  private boolean checkForEnd(byte data[]) {
	  if(data[0]==0&&data[1]==DATA) {
		  int i;
		  for(i = 4; i < data.length; i++) {
			  if(data[i] == 0) {
				  exitNext = true;
				  return false;
			  }
		  }
	  } else if(data[0]==0 && data[1]==ACK && exitNext) return true;
	  
	  return false;
  }
  
  private boolean clientToServer() {
	 byte data[] = new byte[BUFFER_SIZE];
	 try {
		clientPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),clientPort);
		clientSocket.receive(clientPacket);
		System.out.println("Recieved packet from client");
		serverPacket = new DatagramPacket (clientPacket.getData(),clientPacket.getLength(),InetAddress.getLocalHost(),serverPort);
		serverSocket.send(serverPacket);
		System.out.println("Forwarded packet to server");
		return checkForEnd(clientPacket.getData());
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.exit(1);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.exit(1);
	}
	return false; 	
  }
  
  private boolean serverToClient() {
	  byte data[] = new byte[BUFFER_SIZE];
		 try {
			serverPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),serverPort);
			serverSocket.receive(serverPacket);
			System.out.println("Recieved packet from server");
			clientPacket = new DatagramPacket (serverPacket.getData(),serverPacket.getLength(),InetAddress.getLocalHost(),clientPort);
			clientSocket.send(clientPacket);
			System.out.println("Forwarded packet to client");
			return checkForEnd(serverPacket.getData());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return false; 	
  }
}
