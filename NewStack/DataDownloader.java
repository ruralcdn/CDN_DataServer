package NewStack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

import newNetwork.Connection;
import StateManagement.ContentState;
import StateManagement.StateManager;


public class DataDownloader extends Thread{
	
	StateManager stateManager;
	boolean execute;
	String localId;
	LinkDetector ldetector;
	
	public DataDownloader(String Id,StateManager manager,LinkDetector detector)
	{
		localId = Id;
		stateManager = manager;
		ldetector = detector;
		execute = true;
	}
	
	public void close()
	{
		execute = false;
	}
	
	public void run()
	{
		while(execute)
		{
			try
			{
				List<String> requestedData = stateManager.getTCPDownloadRequests();

				Iterator<String> it = requestedData.iterator();
				while(it.hasNext())
				{
					String data = it.next();
					it.remove();
					ContentState stateObject = stateManager.getStateObject(data, ContentState.Type.tcpDownload);
					List<String> caches = stateObject.getPreferredRoute();
					if(!caches.isEmpty())
					{
					//String bitMap = stateObject.getBitMap();
						@SuppressWarnings("unused")
						int offset = stateObject.getOffset();
						@SuppressWarnings("unused")
						Connection.Type type = Connection.Type.values()[stateObject.getPreferredInterface()];
						int totalSegments = stateObject.getTotalSegments();
						//int currentSegments = stateObject.getCurrentSegments();
						@SuppressWarnings("unused")
						boolean sendMetaDataFlag = stateObject.getMetaDataFlag(); 
						int currentSegments = stateObject.getCurrentSegments();//0;
						/*for(int i = 0;i < bitMap.length();i++)
						{
							if(bitMap.charAt(i) == '1')
								currentSegments++;
						}
						*/
						if(currentSegments != totalSegments)
						{
							//cache location is of type IP:port:DTNId
							String cache = caches.get(0);
							String[] cacheInformation = cache.split(":");
							String Id = cacheInformation[0];
							int port = Integer.parseInt(cacheInformation[1]);
							//create control channel with the cache location
							Registry registry = LocateRegistry.getRegistry(Id);
							System.out.println("In DataDownloader.java IP is : "+Id);
							IRMIServer stub = (IRMIServer) registry.lookup(new String(Id+"controlserver") );    //should be config driven
							System.out.println("Requesting Data thru DataDownloader.java");
							ldetector.addDestination(Id+":"+port);
							//stub.request_data(localId,data,offset,type,sendMetaDataFlag,totalSegments,currentSegments);
							UnicastRemoteObject.unexportObject(stub,true);
						}
						else
						{
							System.out.println("File already downloaded");
						}
					}	
				}
				
				/*try
				{
					Thread.sleep(100);
				}catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				*/
			}catch(Exception e)
			{
				e.printStackTrace();
			}

		}
	}

}
