package NewStack;

import java.util.concurrent.BlockingQueue;

import StateManagement.StateManager;

import prototype.datastore.DataStore;

public class SegmentationReassembly{

	private Scheduler scheduler;
	private DataStore store;
	private int segmentSize;		//config driven
	Reassembler reassembler;
	Segmenter segmenter;
	public static final String metadataSuffix = new String(".metadata");
	
	public SegmentationReassembly(StateManager manager,DataStore st,Scheduler sched,int segmentsize,BlockingQueue<String> downloads)
	{
		store = st;
		scheduler = sched;
		segmentSize = segmentsize;
		reassembler = new Reassembler(scheduler.getDataInQueue(),store,manager,segmentSize,downloads);
		reassembler.start();
		segmenter = new Segmenter(store,segmentSize,scheduler.getDataFullQueues());
	}

	public long countSegments(String dataname)
	{
		long smallchunk = (store.length(dataname)%segmentSize);
		if(smallchunk == 0)
			return (store.length(dataname)/segmentSize);
		else
			return (((store.length(dataname) - smallchunk)/segmentSize) + 1);
	}

	public void close()
	{
		reassembler.close();
	}
	public Segmenter getSegmenter()
	{
		return segmenter;
	}

}