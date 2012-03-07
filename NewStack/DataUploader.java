package NewStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import newNetwork.Connection;
import newNetwork.ControlHeader;
import StateManagement.ContentState;
import StateManagement.StateManager;

public class DataUploader extends Thread{

	private StateManager stateManager;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;
	private Map<String,List<Connection>> connectionPool;
	private static boolean execute;
	private boolean pickTCPData;
	private Segmenter segmenter;
	LinkDetector ldetector;
	PolicyModule policyModule;

	public DataUploader(Segmenter seg,Map<String,List<Connection>> cp,LinkDetector detector,StateManager manager,BlockingQueue<BlockingQueue<Packet>> eQ,PolicyModule policy)
	{
		segmenter = seg;
		connectionPool = cp;
		stateManager = manager;
		emptyQueue = eQ;
		execute = true;
		ldetector = detector;
		policyModule = policy;
		pickTCPData = true;
	}

	public void close()
	{
		execute = false;
	}

	@SuppressWarnings("deprecation")
	public void run()
	{
		while(execute)
		{
			try {

				BlockingQueue<Packet> packetQueue = emptyQueue.take(); 
				Set<String> destinationSet = connectionPool.keySet();  
				if(pickTCPData)
				{
					pickTCPData = false;
					List<String> tcpUploadData = stateManager.getTCPUploadRequests();
					int size = tcpUploadData.size();
					int count = 0;
					if(size == 0) 
					{
						pickTCPData = false;
						emptyQueue.put(packetQueue); 
						continue;
					}
					count++;  
					String request = tcpUploadData.remove(0);  
					ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.tcpUpload);
					System.out.println("Request: "+request+" Route: "+stateObject.getPreferredRoute());
					String destination = stateObject.getPreferredRoute().get(0);
					String[] conInfo = destination.split(":"); 
					ldetector.addDestination(destination);
					while(!destinationSet.contains(conInfo[0]) && count <= size)
					{
						tcpUploadData.add(tcpUploadData.size(),request);
						stateManager.setTCPUPloadRequestList(tcpUploadData);
						tcpUploadData = stateManager.getTCPUploadRequests();
						count++;
						request = tcpUploadData.remove(0);
						stateObject = stateManager.getStateObject(request, ContentState.Type.tcpUpload);
						destination = stateObject.getPreferredRoute().get(0);
						conInfo = destination.split(":"); 
						ldetector.addDestination(destination);
						System.out.println("Value of Count in DataUploader::run()= "+count) ;
					}

					if(count > size)
						emptyQueue.put(packetQueue);
					else
					{	
						boolean flag = false ;
						ContentState downloadStateObject = stateManager.getStateObject(stateObject.getContentId(),ContentState.Type.tcpDownload);
						ControlHeader header = null;
						if(downloadStateObject == null)
						{
							flag = true ;
						}
						else{
							int downCurSeg = downloadStateObject.getCurrentSegments();
							if(downCurSeg == downloadStateObject.getTotalSegments())
								flag = true ;
						}
						if(flag)
						{
							if(stateObject.currentSegments != stateObject.getTotalSegments())
							{
								String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
								policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
								header = new ControlHeader(stateObject.getAppId(),null,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
								segmenter.sendSegments(stateObject.getContentId(),stateObject.getUploadId(),header,packetQueue,stateObject.getTotalSegments(), stateObject.currentSegments);
								tcpUploadData.add(tcpUploadData.size(),request);
								stateManager.setTCPUPloadRequestList(tcpUploadData);
							}
							else
							{
								if(tcpUploadData.size() == 0 )
								{
									execute = false ;
									stateManager.setTCPUPloadRequestList(tcpUploadData);
									System.out.println("Execute = " + execute);	
									suspend();
																	
								}
								else
									stateManager.setTCPUPloadRequestList(tcpUploadData);
							}
						}
						else
							emptyQueue.put(packetQueue);
					}
				}	

				else
				{
					boolean intersect = false;
					pickTCPData = true;
					List<String> dtnData = stateManager.getDTNData();
					int count = 0;
					int size = dtnData.size();
					if(dtnData.size() == 0)
					{
						pickTCPData = true;
						emptyQueue.put(packetQueue);
						continue;
					}
					String request = dtnData.remove(0);
					count++;
					ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.dtn);
					List<String>  route = stateObject.getPreferredRoute();
					for(int i = 0;i < route.size();i++)
					{
						String nextHop = route.get(i).split(":")[0];
						if(destinationSet.contains(nextHop))
						{
							List<Connection> cons = connectionPool.get(nextHop);
							for(int j = 0;j < cons.size();j++)
							{
								if(cons.get(j).getType() == Connection.Type.USB)
									intersect = true;

							}

						}
					}
					while(!intersect && count <= size)
					{
						dtnData.add(dtnData.size(),request);
						stateManager.setDTNRequestList(dtnData);
						dtnData = stateManager.getDTNData();
						count++;
						request = dtnData.remove(0);
						stateObject = stateManager.getStateObject(request, ContentState.Type.dtn);
						route = stateObject.getPreferredRoute();
						for(int i = 0;i < route.size();i++)
						{
							String nextHop = route.get(i).split(":")[0];
							if(destinationSet.contains(nextHop))
							{
								List<Connection> cons = connectionPool.get(nextHop);
								for(int j = 0;i < cons.size();i++)
								{
									if(cons.get(j).getType() == Connection.Type.USB)
										intersect = true;

								}
							}
						}
					}

					if(count > size)
						emptyQueue.put(packetQueue);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
	public boolean isNotRunning()
	{
		return execute;
	}
	
	public void setExecute()
	{
		execute = true;
	}


}