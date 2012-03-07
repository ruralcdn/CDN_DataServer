/*package NewStack;

import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;

import newNetwork.Connection;
import newNetwork.ControlHeader;

import prototype.datastore.DataStore;


public class RMIServer implements IRMIServer{

	private DataStore store;
	private SegmentationReassembly sar;
	private PolicyModule policyModule;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;

	public RMIServer(DataStore dStore,SegmentationReassembly sr,PolicyModule policy,BlockingQueue<BlockingQueue<Packet>> eQ)
	{
		store = dStore;
		sar = sr;
		policyModule = policy;
		emptyQueue = eQ;
	}

	public int request_data(String requesterId,String data,int offset,Connection.Type type,boolean sendMetaData,int totSeg,int curSeg) throws RemoteException
	{
		if(store.contains(data) && store.contains(data+".marker") && type != Connection.Type.USB)
		{
			BlockingQueue<Packet> packetQueue = emptyQueue.poll();
			if(packetQueue != null)
			{
				String bitMap = null;
				Segmenter segmenter = sar.getSegmenter();
				ControlHeader header = new ControlHeader(requesterId,null,bitMap,offset,requesterId,sendMetaData);
				segmenter.sendSegments(data, data, header, packetQueue,totSeg,curSeg);
				policyModule.setPolicy(requesterId, type);
				return (int) sar.countSegments(data);
			}
			else
				return -1;
		}
		else
			return -1;
	}
}*/

/*package NewStack;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import StateManagement.ContentState;
import StateManagement.StateManager;
import newNetwork.Connection;
import newNetwork.ControlHeader;
import prototype.datastore.DataStore;

public class RMIServer extends Thread {

	private LinkDetector ldetector ;
	private StateManager stateManager;
	@SuppressWarnings("unused")
	private DataStore store;
	private SegmentationReassembly sar;
	private PolicyModule policyModule;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;
	private static boolean execute ;

	public RMIServer(StateManager stateMgr,DataStore dStore,SegmentationReassembly sr,PolicyModule policy,BlockingQueue<BlockingQueue<Packet>> eQ,LinkDetector ld)
	{
		store = dStore;
		sar = sr;
		policyModule = policy;
		emptyQueue = eQ;
		stateManager = stateMgr ;
		ldetector = ld ;
		execute = true;
	}

	@SuppressWarnings("deprecation")
	public void run()
	{
		try
		{
			Thread.sleep(1000);
		} 
		catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while(execute)
		{	
			try
			{
				BlockingQueue<Packet> packetQueue = emptyQueue.take(); 
				List<String> requestedData = stateManager.getTCPDownloadRequests();
				//System.out.println("request list: "+requestedData);
				//System.out.println("Queue size: "+emptyQueue.size()+" request: "+requestedData);
				String request = requestedData.remove(0);
				ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.tcpUpload);
				//if(store.contains(request))
				//{
					if(stateObject.currentSegments!=stateObject.getTotalSegments())
					{
						if(stateObject.currentSegments==0)
							Thread.sleep(1000);
							
					    Segmenter segmenter = sar.getSegmenter();
						boolean addDest = ldetector.addDestination(stateObject.getPreferredRoute().get(0));
						if(addDest)
						{	
							
							String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
							policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
							ControlHeader header = new ControlHeader(finalDestination,null,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
							segmenter.sendSegments(stateObject.getContentId(),stateObject.getUploadId(),header,  packetQueue,stateObject.getTotalSegments(),stateObject.currentSegments);
							requestedData.add(requestedData.size(),request);
							//stateManager.setTCPDownloadRequestList(requestedData);
						}
						else
						{
							requestedData.add(requestedData.size(),request);
							//stateManager.setTCPDownloadRequestList(requestedData);
							emptyQueue.put(packetQueue);
						}
						
					}
					else
					{	
						if(requestedData.size() == 0){
							emptyQueue.put(packetQueue);
							stateManager.setTCPDownloadRequestList(request);
							//stateManager.setTCPDownloadRequestList(requestedData);
							System.out.println("Queue size: "+emptyQueue.size());
							execute = false ;
							suspend();
						}
						else{
							//stateManager.setTCPDownloadRequestList(requestedData);
							stateManager.setTCPDownloadRequestList(request);
							emptyQueue.put(packetQueue);
							System.out.println("Queue size: "+emptyQueue.size());
						}	
					}	
						
				//}
				/*else
				{
					emptyQueue.put(packetQueue);
					requestedData.add(requestedData.size(),request);
					stateManager.setTCPDownloadRequestList(requestedData);
				}*/
/*			}		
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean isNotRunning(){
		return execute;
	}
	
	public void setExecute()
	{
		execute = true ;
	}
}*/

package NewStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import StateManagement.ContentState;
import StateManagement.StateManager;
import newNetwork.Connection;
import newNetwork.ControlHeader;
import prototype.datastore.DataStore;
import NewStack.LinkDetector;

public class RMIServer extends Thread implements IRMIServer{

	private StateManager stateManager;
	private SegmentationReassembly sar;
	private PolicyModule policyModule;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;
	private static boolean execute ;
	Map<String,List<Integer>> pendingContent ;
	private LinkDetector ldetector ;

	public RMIServer(StateManager stateMgr,DataStore dStore,SegmentationReassembly sr,PolicyModule policy,BlockingQueue<BlockingQueue<Packet>> eQ,LinkDetector ld)
	{
		sar = sr;
		policyModule = policy;
		emptyQueue = eQ;
		stateManager = stateMgr ;
		execute = true ;
		ldetector = ld ;
		pendingContent = new HashMap<String, List<Integer>>();
	}

	@SuppressWarnings("deprecation")
	public void run()
	{
		while(execute)
		{	
			try
			{
				BlockingQueue<Packet> packetQueue = emptyQueue.take();
				List<String> requestedData = stateManager.getTCPDownloadRequests();
				String request = requestedData.remove(0);
				List<ContentState> stateObjList = stateManager.getStateObject(request);
				ContentState stateObject = stateObjList.remove(0);
				if(stateObject.currentSegments!=stateObject.getTotalSegments()){
					if(stateObject.currentSegments==0)
						Thread.sleep(1000);
					String	bitMap = null ;
					Segmenter segmenter = sar.getSegmenter();
					boolean addDest = ldetector.addDestination(stateObject.getPreferredRoute().get(0));
					String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
					if(addDest){
						policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
						ControlHeader header = new ControlHeader(finalDestination,null,bitMap,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
						segmenter.sendSegments(stateObject,stateObject.getContentId(),stateObject.getUploadId(),header,  packetQueue,stateObject.getTotalSegments(),stateObject.currentSegments);
						stateObjList.add(stateObjList.size(),stateObject);
						requestedData.add(requestedData.size(),request);
					}
					else{
						stateObjList.add(stateObjList.size(),stateObject);
						emptyQueue.put(packetQueue);
						requestedData.add(requestedData.size(),request);
					}

				}
				else
				{	
					if(stateObjList.size()==0){
						emptyQueue.put(packetQueue);
						if(requestedData.size()==0 ){
							execute = false ;
							stateManager.setTCPDownloadRequestList(request);
							System.out.println("In download finishing Queue: "+emptyQueue.size());
							suspend();
						}
					}
					else{
						requestedData.add(requestedData.size(),request);
						emptyQueue.put(packetQueue);
					}	
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}	
	}
	
	
	public boolean isNotRunning(){
		return execute;
	}
	
	public void setExecute()
	{
		execute = true ;
	}
}