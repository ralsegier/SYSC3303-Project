import java.io.*;
import java.net.*;


public class ServerThread implements Runnable{
	public static enum Request {ERROR, READ, WRITE};
	public static final String FILE_DIR = "ServerFiles\\";
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	
	private DatagramPacket request;
	private DatagramSocket socket;
	private InetAddress ip;
	private int port;
	private String file;
	private String mode;
	private Request requestType;
	private int ackCount;
	
	/**
	 * Constructor for ServerThread
	 * @param request - The initial DatagramPacket request sent from the client
	 */
	public ServerThread(DatagramPacket request) {
		this.request = request;
		this.ackCount = 0;
		try {
			this.ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method parses request to determine request type and handles request accordingly
	 */
	public void processRequest() {
		System.out.println("New client request:");
		
		
		
		parseRequest();
		
		System.out.println("With file: "+file);
		System.out.println("Encoded in: "+mode);
		System.out.print("Type: ");
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if (requestType==Request.READ) {
			System.out.println("read");
			//handle read request
			handleRead();
		} else if (requestType==Request.WRITE) {
			System.out.println("write");
			//submit write request
			handleWrite();
		} else {
			//submit invalid request
			System.out.println(requestType);
			handleError();
		}
	}
	
	/**
	 * Parses request DatagramPacket and populates instance variables with data
	 */
	private void parseRequest() {
		int length  = this.request.getLength(); //temporarily stores length of request data
		byte data[] = this.request.getData(); //copies data from request
		//this.ip = this.request.getAddress(); //stores ip address in instance variable
		this.port = this.request.getPort(); //stores port number in instance variable
		File here;
		

		
		if (data[0]!=0) requestType = Request.ERROR; //Makes sure that request data starts with a 0
		else if (data[1]==1) requestType = Request.READ;//Checks if request is a read request
		else if (data[1]==2) requestType = Request.WRITE;//Checks if request is a write request
		else requestType = Request.ERROR;//If not a read or write, sets request type to invalid
		
		if (requestType!=Request.ERROR) {
			//find filename
			int fileCount;//keeps track of position in data array while getting file name
			//finds length of file name (number of bytes between request type and next 0 or end of array)
			for(fileCount = 2; fileCount < length; fileCount++) {
				if (data[fileCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;//if there is no zero before the end of the array request is set to Invalid
			else {
				here = new File(FILE_DIR + new String(data,2,fileCount-2));//Otherwise, filename is converted into a string and stored in instance variable
				file = FILE_DIR + new String(data,2,fileCount-2);
				System.out.println("File is : file");
			}
			
			//find mode
			int modeCount;//keeps track of position in data array while getting encoding mode
			//finds length of encoding mode (number of bytes between request type and next 0 or end of array)
			for(modeCount = fileCount+1; modeCount < length; modeCount++) {
				if (data[modeCount] == 0) break;
			}
			if (fileCount==length) requestType=Request.ERROR;//if there is no zero before the end of the array request is set to Invalid
			mode = new String(data,fileCount+1,modeCount-fileCount-1);//Otherwise, filename is converted into a string and stored in instance variable
			
			if(modeCount!=length-1) requestType=Request.ERROR;//Checks that there is no data after final zero
		}
	}
		

	/**
	 * handles a read request.  Continually loops, reading in data from selected file,
	 * packing this data into a TFTP Packet,
	 * sending the TFTP Packet to the client,
	 * waiting for a corresponding acknowledgement from client,
	 * and repeating until the entire file is sent
	 */
	private void handleRead() {
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			
			BlockNumber bn = new BlockNumber();
			bn.increment();
			
			byte[] msg;//buffer used to send data to client
			byte[] data = new byte[MESSAGE_SIZE];//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			do {
				n = in.read(data);
				msg = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				msg[0] = 0;
				msg[1] = DATA;
				System.arraycopy(bn.getCurrent(),0,msg,2,2);
				//Data read from file
				System.arraycopy(data,0,msg,4,n);
				DatagramPacket send = new DatagramPacket(msg,msg.length,ip,port);
				try {
					System.out.println("Sending to ip: " + ip);
					System.out.println("Sending to port: " + port);
					//sends packet via default port
					socket.send(send);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Sent chunk");
				
				
				boolean correctBlock;
				for(;;) {
					correctBlock = true;
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					try {
						socket.receive(temp);//Receives ack from client on designated socket
						if (temp.getLength()!=4) correctBlock = false; //Checks for proper Ack size

						byte block[] = new byte[2];
						System.arraycopy(temp.getData(), 2, block, 0, 2);
						
						if(temp.getData()[0]!=0 || temp.getData()[1]!=ACK || !bn.compare(block)) correctBlock = false;
						
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					if (correctBlock==true) break;//if ack has been accepted then loop is exited
				}
				
				bn.increment();
			} while (n >= MESSAGE_SIZE);
			
			//closes input stream
			in.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
	
	/**
	 * sends an ack to the client, confirming having received the latest block
	 * @param blockNumber - current block number
	 */
	private void sendAck(byte blockNumber[]) {
		byte msg[] = {0,ACK,0,0};
		System.arraycopy(blockNumber,0,msg,2,2);
		DatagramPacket temp = new DatagramPacket (msg, msg.length,ip,port);
		try {
			System.out.println("Sending ack to port "+port);
			socket.send(temp);
			System.out.println("Ack "+this.ackCount+" sent");
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * waits for TFTP Packet from client until appropriate data block is recieved
	 * @param blockNumber - expected block number
	 * @return returns byte array of data to be written in write request
	 */
	private byte[] getBlock(BlockNumber blockNumber) {
		byte incomingMsg[];// = new byte[BUFFER_SIZE];
		byte data[] = new byte[BUFFER_SIZE];
		for(;;) {
			incomingMsg = new byte[BUFFER_SIZE];
			DatagramPacket temp = new DatagramPacket (incomingMsg, incomingMsg.length);
			
			try {
				System.out.println("Waiting for data");
				socket.receive(temp);
				System.out.println("Data received");
				byte bn[] = new byte[2];
				System.arraycopy(temp.getData(), 2, bn, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && blockNumber.compare(bn)) {
					System.out.println("Data good");
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength()-4);
					return data;
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	/**
	 * Uses getBlock() and sendAck() methods to get data and send the appropriate ack
	 * Writes data blocks to designated file
	 */
	private void handleWrite() {
		BlockNumber bn = new BlockNumber();
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			for (;;) {
				sendAck(bn.getCurrent());
				bn.increment();
				ackCount++;
				byte[] temp = getBlock(bn);
				int length;
				for(length = 4; length < temp.length; length++) {
					if (temp[length] == 0) break;
				}
				out.write(temp, 0, temp.length);
				if(length<MESSAGE_SIZE) {
					System.out.println("Closing file");
					out.close();
					sendAck(bn.getCurrent());
					break;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			handleError();
			return;
		}
	}
	
	/**
	 * Will handle any errors that occur during a client request
	 * Not implemented properly for this increment
	 */
	private void handleError() {
		//TODO: Implement real error method
		/*NOTE: This is a test method filler simply
		 * replying with the appropriate request
		 * as per SYSC 3303 assignment 1
		 */
		byte data[] = {0, 5};
		//sendData(data);
	}

	@Override
	public void run() {
		processRequest();
	}
	

}
