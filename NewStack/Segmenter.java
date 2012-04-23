/*package NewStack;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import NewStack.Packet.PacketType;

import newNetwork.ControlHeader;

import prototype.datastore.DataStore;

public class Segmenter extends Thread{

	private DataStore store;
	private int segmentSize;
	BlockingQueue<BlockingQueue<Packet>> fullQueue;
	
	public Segmenter(DataStore st,int size,BlockingQueue<BlockingQueue<Packet>> fQ)
	{
		store = st;
		segmentSize = size;
		fullQueue = fQ;
	}

	public void sendSegments(String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,int downSeg,int curSeg)
	{
		BlockingQueue<Packet> segmentQueue = packetQueue;
		List<String> route = null;
		String bitMap = null;
		String destination = null;
		boolean sendMetaDataFlag = false;

		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
			sendMetaDataFlag = ctrlHeader.getMetaDataFlag();
		}

		boolean sendMeta = false;
		if(bitMap == null)
			sendMeta = true;
		if(sendMetaDataFlag && store.contains(readName+SegmentationReassembly.metadataSuffix) && sendMeta)
		{

			String fileName = readName+SegmentationReassembly.metadataSuffix;

			long fileSize = store.length(fileName);
			int offset = 0;
			int length = (int) fileSize;
			int i = 0;
			while(length > 0 && segmentQueue.remainingCapacity() > 0)
			{
				byte[] segment;
				if(length < segmentSize)
					segment = store.read(fileName, offset,length);
				else
					segment = store.read(fileName, offset,segmentSize);	

				Packet packet = new Packet(route,destination,PacketType.Data,sendName,segment,offset,true);
				if(segmentQueue != null)
					segmentQueue.offer(packet);

				i++;
				offset += segmentSize;
				length -= segmentSize;
			}

		}
		
		for(int i = curSeg;i < downSeg && segmentQueue.remainingCapacity() > 0;i++)
		{
			byte[] segment = store.read(readName, i*segmentSize, segmentSize);
			Packet packet = new Packet(route,destination,PacketType.Data,sendName,
					segment,i*segmentSize,false);
			if(segmentQueue != null)
				segmentQueue.offer(packet);
		}

		fullQueue.add(segmentQueue);
	}
}*/


package NewStack;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import StateManagement.*;
import NewStack.Packet.PacketType;
import newNetwork.ControlHeader;
import prototype.datastore.DataStore;

public class Segmenter extends Thread{

	private DataStore store;
	private int segmentSize;
	BlockingQueue<BlockingQueue<Packet>> fullQueue;
	Status status ;
	Map<String, ContentState> mpUp ;
	BitSet bits ;
	public Segmenter(DataStore st,int size,BlockingQueue<BlockingQueue<Packet>> fQ)
	{
		store = st;
		segmentSize = size;
		fullQueue = fQ;
		status = Status.getStatus();
		mpUp = new HashMap<String, ContentState>();
		bits = new BitSet();
	}

	public void sendSegments(String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,int totSeg, int curSeg)
	{
		
		BlockingQueue<Packet> segmentQueue = packetQueue;
		List<String> route = null;
		String destination = null;
		boolean sendMetaDataFlag = false;
		mpUp = StateManager.getUpMap();
		
		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
			sendMetaDataFlag = ctrlHeader.getMetaDataFlag();
		}

		boolean sendMeta = false;
		sendMeta = true;

		if(sendMetaDataFlag && store.contains(readName+SegmentationReassembly.metadataSuffix) && sendMeta)
		{
			String fileName = readName+SegmentationReassembly.metadataSuffix;
			long fileSize = store.length(fileName);
			int offset = 0;
			int length = (int) fileSize;
			@SuppressWarnings("unused")
			int i = 0;
			while(length > 0 && segmentQueue.remainingCapacity() > 0)
			{
				byte[] segment;
				if(length < segmentSize)
					segment = store.read(fileName, offset,length);
				else
					segment = store.read(fileName, offset,segmentSize);	

				Packet packet = new Packet(route,destination,PacketType.Data,sendName,segment,offset,true);
				if(segmentQueue != null)
					segmentQueue.offer(packet);

				i++;
				offset += segmentSize;
				length -= segmentSize;
			}
		}
		ContentState stateObj = mpUp.get(sendName);
		int j = stateObj.currentSegments;
		for( ;j < totSeg&& segmentQueue.remainingCapacity() > 0;j++)
		{
			byte[] segment = store.read(readName, j*segmentSize, segmentSize);
			Packet packet = new Packet(route,destination,PacketType.Data,sendName,
					segment,j*segmentSize,false);
			if(segmentQueue != null)
					segmentQueue.offer(packet);
		}
		stateObj.currentSegments = j;	
		//System.out.println("");
		mpUp.put(sendName, stateObj);
		if(stateObj.currentSegments==stateObj.getTotalSegments())
		{
			Status st = Status.getStatus();
			st.updateState("status",sendName,1);
		}
		fullQueue.add(segmentQueue);
	}
	
	public void sendSegments(ContentState stateObj,String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,int totSeg,int curSeg)
	{
		BlockingQueue<Packet> segmentQueue = new ArrayBlockingQueue<Packet>(20);
		List<String> route = null;
		String destination = null;
		boolean sendMetaDataFlag = false;
		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
			sendMetaDataFlag = ctrlHeader.getMetaDataFlag();
		}

		boolean sendMeta = false;
		sendMeta = true;


		if(sendMetaDataFlag && store.contains(readName+SegmentationReassembly.metadataSuffix) && sendMeta)
		{

			String fileName = readName+SegmentationReassembly.metadataSuffix;

			long fileSize = store.length(fileName);
			int offset = 0;
			int length = (int) fileSize;
			@SuppressWarnings("unused")
			int i = 0;
			while(length > 0 && segmentQueue.remainingCapacity() > 0)
			{
				byte[] segment;
				if(length < segmentSize)
					segment = store.read(fileName, offset,length);
				else
					segment = store.read(fileName, offset,segmentSize);	

				Packet packet = new Packet(route,destination,PacketType.Data,sendName,segment,offset,true);
				if(segmentQueue != null)
					segmentQueue.offer(packet);

				i++;
				offset += segmentSize;
				length -= segmentSize;
			}

		}
		int j = stateObj.currentSegments;
		for( ;j < (totSeg)&& segmentQueue.remainingCapacity() > 0;j++)
		{
			byte[] segment = store.read(readName, j*segmentSize, segmentSize);
			Packet packet = new Packet(route,destination,PacketType.Data,sendName,
					segment,j*segmentSize,false);
			if(segmentQueue != null)
					segmentQueue.offer(packet);
		}
		stateObj.currentSegments = j;	
		if(stateObj.currentSegments==stateObj.getTotalSegments())
		{
			System.out.println("Calling Segmenter send segments");
			Status st = Status.getStatus();
			st.updateState("status", sendName, 1);
		}
		fullQueue.add(segmentQueue);
		//System.out.println("Hi am here: "+fullQueue.size());
	}
}