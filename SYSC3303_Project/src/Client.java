

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Client  {	
	public static enum Request {ERROR, READ, WRITE};
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	public static final String FILE_DIR = "ClientFiles/";

	private String file;
	private String mode;
	private DatagramPacket sendPacket, receivePacket; //
	private DatagramSocket sendReceiveSocket;
	private BlockNumber bnum;
	private int sendPort;
	private int wellKnownPort;
	private byte msg[];
	private int counter;
	
	   
	   
	public Client(){
		counter = 0;
		mode = "octet";
		wellKnownPort = 69;
		
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
		msg = new byte[BUFFER_SIZE];
		int iterator = 2;
		//for(;;){
		System.out.println("1) Read");
		System.out.println("2) Write");
		System.out.println("Select a mode:");
		Scanner scanner = new Scanner (System.in);
		int request = scanner.nextInt();
		
		msg[0] = 0;
		
		//for(int i = 0; i<10; i++){
			//int request = random_num.nextInt(2);
			this.sendPort = 0;
			if (request == 2){
				file = new String("write.txt");
				msg[1] = 2;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(mode.getBytes(),0,msg,iterator,mode.getBytes().length);
				iterator+=mode.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1);
				clientWrite();
				
			}else if(request == 1){
				file = new String("read.txt");
				msg[1] = 1;
				System.arraycopy(file.getBytes(),0,msg,iterator,file.getBytes().length);
				iterator+=file.getBytes().length;
				msg[iterator] = 0;
				iterator ++;
				System.arraycopy(mode.getBytes(),0,msg,iterator,mode.getBytes().length);
				iterator+=mode.getBytes().length;
				msg[iterator] = 0;	
				sendData(iterator+1);
				clientRead();		
			} else {
				//break;
			}
		//}
	}
	
	public void sendData(int size){
		 try {
			 sendPacket = new DatagramPacket(this.msg, size,InetAddress.getLocalHost(), this.wellKnownPort);
			 sendReceiveSocket.send(sendPacket);
	        } catch (IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }

	        System.out.println("Client: Packet sent.");
	}
	
	public void clientWrite(){
		try {
			//Opens an input stream
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(FILE_DIR+file));
			
			bnum = new BlockNumber();
			
			byte[] pack;//buffer used to send data to client
			byte[] data;//buffer used to hold data read from file
			int n;
			
			//Reads data from file and makes sure data is still read
			do {
				data = new byte[MESSAGE_SIZE];
				for(;;) {
					byte ack[] = new byte[BUFFER_SIZE];//Ack data buffer
					DatagramPacket temp = new DatagramPacket (ack, ack.length);//makes new packet to receive ack from client
					try {
						System.out.println("Waiting for Ack " + counter);
						counter++;
						sendReceiveSocket.receive(temp);//Receives ack from client on designated socket
						if (this.sendPort == 0) this.sendPort = temp.getPort();
						System.out.println("Recieved Ack");
						byte bn[] = new byte[2];
						System.arraycopy(ack, 2, bn, 0, 2);
						if(ack[0] == 0 && ack[1] == ACK && bnum.compare(bn)) {
							System.out.println("Ack good");
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Ack Reception Error");
						System.exit(1);
					}
					
				}
				
				
				
				bnum.increment();
				
				n = in.read(data);
				pack = new byte[BUFFER_SIZE];//new empty buffer created
				//first four bits are set to TFTP Requirements
				pack[0] = 0;
				pack[1] = DATA;
				System.arraycopy(bnum.getCurrent(), 0, pack, 2, 2);
				//Data read from file
				System.arraycopy(data,0,msg,4,n);
				System.out.println("Sending data to port: " + this.sendPort);
				DatagramPacket block = new DatagramPacket(pack,pack.length,InetAddress.getLocalHost(), this.sendPort);
				sendReceiveSocket.send(block);
				System.out.println("Sent data block");
				
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
	
	public void clientRead(){
		
		this.bnum = new BlockNumber();
		this.bnum.increment();	
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(FILE_DIR+file));
			for(;;){
				int length;
				byte[] temp = getBlock(bnum.getCurrent());
				for(length = 4; length < temp.length; length++) {
					if (temp[length] == 0) break;
				}
				out.write(temp,0,temp.length);
				System.out.println("Sending ack");
				sendAck(bnum.getCurrent());					
				
				if(length+1<=BUFFER_SIZE) {
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
		byte msg[] = new byte[516];
		byte data[] = new byte[512];
		System.out.println("getting data block");
		for(;;) {
			
			DatagramPacket temp = new DatagramPacket (msg, msg.length);

			try {
				System.out.println("Waiting for packet on ip: "+sendReceiveSocket.getInetAddress());
				System.out.println("Waiting for packet on port: "+sendReceiveSocket.getLocalPort());
				sendReceiveSocket.receive(temp);
				System.out.println("Block recieved");
				if(this.sendPort==0) sendPort = temp.getPort();
				byte blockNumCheck[] = new byte[2];
				System.arraycopy(temp.getData(), 2, blockNumCheck, 0, 2);
				if (temp.getData()[0] == 0 && temp.getData()[1] == DATA && bnum.compare(blockNumCheck)) {
					System.out.println("Data is good");
					System.arraycopy(temp.getData(), 4,data, 0, temp.getLength()-4);
					return data;
				}
				for(int i = 0; i < 4; i ++) {
					System.out.println("Byte "+i+": "+temp.getData()[i]);
				}
				System.out.println("Expecting: 0"+DATA+bnum.getCurrent()[0]+bnum.getCurrent()[1]);
				System.out.println((temp.getData()[0] == 0)+" "+(temp.getData()[1] == DATA)+" "+(bnum.compare(blockNumCheck)));
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error in getBlock");
				System.exit(1);
			}
		}
	}
	
	
	
}
/*
byte[] data = new byte[BUFFER_SIZE];
DatagramPacket temp = new DatagramPacket(data, data.length);

try {
       // Block until a datagram is received via sendReceiveSocket.
    sendReceiveSocket.receive(temp);
    System.out.println("Packet Recieved");
    } catch(IOException e) {
       e.printStackTrace();
       System.out.println("Error Recieving");
       System.exit(1);
    }
return temp;*/
