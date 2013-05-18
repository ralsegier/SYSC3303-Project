

import java.io.*;
import java.net.*;
import java.util.Random;

public class Client  {	
	public static enum Request {ERROR, READ, WRITE};
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	   
	private DatagramPacket sendPacket, receivePacket; //
	private DatagramSocket sendReceiveSocket;
	private BlockNumber bnum;
	private int sendPort;
	private byte[] msg = new byte[516];
	   
	   
	public Client(){

		
		sendPort = 68;
		  try {
		         // Construct a datagram socket and bind it to any available
		         // port on the local host machine. This socket will be used to
		         // send and receive UDP Datagram packets.
		         sendReceiveSocket = new DatagramSocket();
		      } catch (SocketException se) {   // Can't create the socket.
		         se.printStackTrace();
		         System.exit(1);
		      }
	}
	
	public static void main(String []args){
		Client c = new Client();
		c.run();
	}

	public void run(){
		Random random_num = new Random();
		
		msg[0] = 0;
		
		for(int i = 0; i<10; i++){
			int request = random_num.nextInt(3);
			if (request == 2){
				msg[1] = 2;
				System.arraycopy("write.txt".getBytes(),0,msg,2,("write.txt".getBytes()).length);
				msg[3] = 0;
				System.arraycopy("octet".getBytes(),0,msg,4,("octet".getBytes()).length);
				msg[5] = 0;	
				receivePacket = process();
				if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 3 && receivePacket.getData()[3] == 0)
				clientWrite();
				
			}else if(request == 1){
				msg[1] = 1;
				System.arraycopy("read.txt".getBytes(),0,msg,2,("read.txt".getBytes()).length);
				msg["read.txt".getBytes().length + 2] = 0;	
				System.arraycopy("octet".getBytes(),0,msg,("read.txt".getBytes().length + 3),("octet".getBytes()).length);
				msg["read.txt".getBytes().length + 3 + "octet".getBytes().length] = 0;
				receivePacket = process();
				if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 3 && receivePacket.getData()[3] == 1){
				client_Read();
				}
			}else{				
				i = i - 1; 		
			}        
	  }
	}
	
	public DatagramPacket process(){
		try {
	           sendPacket = new DatagramPacket(this.msg, this.msg.length,
	                                         InetAddress.getLocalHost(), this.sendPort);
	        } catch (UnknownHostException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }
		 try {
	           sendReceiveSocket.send(sendPacket);
	        } catch (IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }

	        System.out.println("Client: Packet sent.");
	        
	        byte[] data = new byte[100];
	        DatagramPacket temp = new DatagramPacket(data, data.length);
	        
	        try {
		           // Block until a datagram is received via sendReceiveSocket.
		           sendReceiveSocket.receive(temp);
		        } catch(IOException e) {
		           e.printStackTrace();
		           System.exit(1);
		        }
	        return temp;
	}
	
	public void clientWrite(){
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream("file.txt"));
			
			byte blockNumber = 1;//keeps track of current block number
			
			byte[] pack;//buffer used to send data to client
			byte[] data = new byte[MESSAGE_SIZE];//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			do {
				n = in.read(data);
				pack = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				pack[0] = 0;
				pack[1] = DATA;
				pack[2] = 0;
				pack[3] = blockNumber;
				//Data read from file
				System.arraycopy(data,0,msg,4,n);
				DatagramPacket block = new DatagramPacket(pack,pack.length,InetAddress.getLocalHost(), this.sendPort);
				sendReceiveSocket.send(block);
				
				
				boolean correctBlock = true;
				for(;;) {
					byte comparitor[] = {0,ACK,0,blockNumber};//used to check ack
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					try {
						sendReceiveSocket.receive(temp);//Receives ack from client on designated socket
						if (temp.getLength()==comparitor.length) correctBlock = false; //Checks for proper Ack size

						for (int i = 0; i < comparitor.length; i++) {
							if (temp.getData()[i]==comparitor[i]) correctBlock = false;//if any byte in ack is not the same as comparator then ack is not accepted
						}
						
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					if (correctBlock==true) break;//if ack has been accepted then loop is exited
				}
				
				blockNumber++;//increment block number
				if (blockNumber >= MAX_BLOCK_NUM) blockNumber = 0; //roll over block number if max number is reached
			} while (n >= MESSAGE_SIZE);
			
			//closes input stream
			in.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			//handleError();
			return;
		} catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			//handleError();
			return;
		}
	}
	
	public void client_Read(){
		
		this.bnum = new BlockNumber();
		this.bnum.increment();	
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("read1.txt"));
			for(;;){
				
				byte[] temp = getBlock(bnum.getCurrent());
				out.write(temp,0,temp.length);
				sendAck(bnum.getCurrent());					
				
				if(temp.length<512) {
					out.close();
					break;
				}
				
				this.bnum.increment();				
			}
		}  catch (IOException e) {
			System.out.println("File Read Error:");
			e.printStackTrace();
			 System.exit(1);
			return;
		}
	}
	
	
	
	private void sendAck(byte[] blockNumber) {
		byte temp[] = new byte[4];
		temp[0] = 0;
		temp[1] = 4;
		System.arraycopy(blockNumber, 0, temp, 2, 2);
		
		try {
			DatagramPacket pack = new DatagramPacket (temp, temp.length, InetAddress.getLocalHost(), this.sendPort);
			sendReceiveSocket.send(pack);
		} catch (IOException e) {
			System.out.println("Send Packet Error");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public byte[] getBlock(byte[] blockNumber) {
		byte data1[] = new byte[516];
		byte data[] = new byte[512];
		for(;;) {
			
			DatagramPacket temp = new DatagramPacket (data1, data1.length);

			try {
				sendReceiveSocket.receive(temp);	
				byte blockNumCheck[] = new byte[2];
				System.arraycopy(temp.getData(), 2, blockNumCheck, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == 3 && bnum.compare(blockNumCheck)) {
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength());
					return data;
				}

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	
	
}
