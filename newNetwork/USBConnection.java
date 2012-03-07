package newNetwork;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;


import NewStack.Packet;


public class USBConnection implements Connection{

	private Connection.Type type;
	private BlockingQueue<Packet> readQueue;    //reads from USB
	private BlockingQueue<Packet> writeQueue;   //writes to USB

	//inQueue - reads from USB , outQueue - writes to USB
	public USBConnection(BlockingQueue<Packet> inQueue,BlockingQueue<Packet> outQueue){

		type = Connection.Type.USB;
		readQueue = inQueue;
		writeQueue = outQueue; 
	}

	public Connection.Type getType()
	{
		return type;
	}	
	public Packet readPacket()
	{
		return readQueue.poll();
	}
	public void writePacket(Packet packet)
	{
		writeQueue.offer(packet);
	}

	public void close() throws IOException
	{

	}

	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getRemoteAddress()
	{
		return null;
	}
	public int getRemotePort()
	{
		return -1;
	}
	
	public InetAddress getLocalAddress()
	{
		return null;
	}
	public int getLocalPort()
	{
		return -1;
	}
}