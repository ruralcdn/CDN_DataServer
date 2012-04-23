package NewStack;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import NewStack.Packet;
import StateManagement.ContentState;
import StateManagement.StateManager;
import StateManagement.Status;
import prototype.datastore.DataStore;

public class Reassembler extends Thread{

	private BlockingQueue<Packet> packetQueue;
	private DataStore store;
	private boolean execute;
	private int segmentSize;		
	private BlockingQueue<String> fileDownloads;
	StateManager stateManager;
	Map<String, ContentState> mpContent ;
	ContentState conProp ;
	public Reassembler(BlockingQueue<Packet> queue,DataStore st,StateManager manager,int segmentsize,BlockingQueue<String> downloads)
	{
		packetQueue = queue;
		store = st;
		execute = true;
		stateManager = manager;
		segmentSize = segmentsize;
		fileDownloads = downloads;
		mpContent = new HashMap<String, ContentState>();
		try {
			System.setOut(new PrintStream(new FileOutputStream("output.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void close()
	{
		execute = false;	
	}
	
	public void run()
	{
		
		while(execute)
		{
			try {
				Packet packet = packetQueue.take();				
				mpContent = StateManager.getDownMap();
				if(mpContent == null)
					continue;
				if(packet.isMetaData()){
					System.out.println("Received a metaData Packet");
					String data = packet.getName();
					long offset = packet.getSequenceNumber();
					byte[] segment = packet.getData();
					store.write(data+SegmentationReassembly.metadataSuffix,offset,segment);
				}
				else
				{
					String data = packet.getName();
					ContentState conProp ;
					BitSet bs ;
					try{
						conProp = mpContent.get(data);
						bs= conProp.bitMap;
					}catch(Exception e){
						continue ;
					}
					long offset = packet.getSequenceNumber();
					//System.out.println("Segemnts received : "+(int)offset/segmentSize);
					byte[] segment = packet.getData();
					store.write(data, offset, segment);
				
					if(stateManager != null)
					{
						try	{
							int currentsegments = conProp.currentSegments; 
							if(currentsegments == conProp.getTotalSegments())
							{
								System.out.println("Current Segments = "+ currentsegments);								
							}
							else
							{
								if(bs.get((int) (offset/segmentSize))==false)
								{
									currentsegments++;
									bs.set((int) (offset/segmentSize));
								}
								conProp.currentSegments = currentsegments;
								conProp.bitMap = bs ;
								mpContent.put(data,conProp);
								if(currentsegments == conProp.getTotalSegments())	
								{
									
									System.out.println("In Reassembler.java Received the Complete File! :D :D :D :D :D :D :D :D :D :D :D :D ");
									Status st = Status.getStatus();
									String str = "";
									str = st.getContentType(data);
									if(str.length()!=0)
										store.rename(data,str);
									if(fileDownloads != null){
										if(str.length()!=0)
											fileDownloads.put(data+"."+str);
										else
											fileDownloads.put(data);
									}	
									st.updateState("status",data, 0);
									
								}
							}
						}catch(Exception e){
							System.out.println("Exception in Reassembler");
							//e.printStackTrace();	  
						}
					}
				}
			}catch (InterruptedException e){
				e.printStackTrace();			
			}
		}
	}

}