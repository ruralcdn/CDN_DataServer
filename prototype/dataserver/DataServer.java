package prototype.dataserver;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import newNetwork.Connection;
import AbstractAppConfig.AppConfig;
import DBSync.DBSync;
import NewStack.NewStack;
import StateManagement.ContentProperties;
import StateManagement.ContentState;
import StateManagement.ServiceInstanceAppStateManager;
import StateManagement.StateManager;
import prototype.datastore.DataStore;
import prototype.dataserver.IDataServer;
import prototype.rootserver.IRootServer;

public class DataServer implements IDataServer {

	DataStore store;
	NewStack networkStack;
	StateManager stateManager;
	String cacheId;
	BlockingQueue<String> fileDownloads;
	ServiceInstanceAppStateManager AppManager;
	static Map<String,ContentProperties> contentPropMap ;
	//static List<String> UploadSync ;
	DBSync dbSync ;
	public DataServer(String Id) throws Exception 
	{

		int port = Integer.parseInt(AppConfig.getProperty("DataServer.Port"));
		String path = AppConfig.getProperty("DataServer.Directory.Path");         
		store = new DataStore(path);
		contentPropMap = new HashMap<String,ContentProperties>();
		DataStore usbStore = null;
		if(AppConfig.getProperty("Routing.allowDTN").equals("1"))
		{
			usbStore = new DataStore(AppConfig.getProperty("DataServer.USBPath"));
		}

		cacheId = Id;  
		stateManager = new StateManager("status");
		AppManager = new ServiceInstanceAppStateManager();
				
		int maxDownloads = Integer.parseInt(AppConfig.getProperty("DataServer.MaximumDownloads"));
		BlockingQueue<String> fileDownloads = new ArrayBlockingQueue<String>(maxDownloads);   
		FileRegisterer registerer = new FileRegisterer(fileDownloads,AppManager, cacheId);
		registerer.start();
		int portStart = Integer.parseInt(AppConfig.getProperty("DataServer.NetworkStack.Port"));
		int portListSize = Integer.parseInt(AppConfig.getProperty("DataServer.ConnectionPool.size"));
		List<Integer> portList = new ArrayList<Integer>();
		for (int i = 0 ; i < portListSize ; i++)
		{
			portList.add(portStart);
			portStart++ ;
		}
		networkStack = new NewStack(cacheId,stateManager,store,usbStore,fileDownloads,port,portList);
		
	}

	public void upload(String data,int size,String requester, String fileType) throws RemoteException
	{
		System.out.println("Inside DataServer.java: upload");
		ContentState stateObject = new ContentState(data,0,new BitSet(size),-1,null,size,0,ContentState.Type.tcpDownload,cacheId,true);
		//String fullName = data+"."+fileType;
		//UploadSync.add(fullName);
		stateManager.setStateObject(stateObject);
		System.out.println("Calling DataServer.java: AppManager.setRequesterDetail");
		AppManager.setRequesterDetail(data, requester, fileType);
	}


	public int TCPRead(int AppId,String dataname,Connection.Type type,String conId) throws RemoteException
	{
		
		if(store.contains(dataname))
		{
			System.out.println("Inside TCP Read method");
			int size = networkStack.countSegments(dataname);
			List<String> destinations = new ArrayList<String>();
			destinations.add(conId);
			ContentState downLoadStateObject = new ContentState(dataname,dataname,0,new BitSet(size), Connection.Type.DSL.ordinal(),
				destinations,size,0,ContentState.Type.tcpUpload,cacheId,true);
			
			stateManager.setTCPDownloadState(downLoadStateObject);
			return size;
			
		}
		else
			return -1;
	}

	public boolean DTNRead(int AppId,String dataname,String dataRequester,String conId) throws RemoteException
	{
	
		if(store.contains(dataname))
		{
			return true;
		}
		else
			return false;		
	}

	public boolean delete(String contentId) throws RemoteException
	{
		boolean flag = false ;
		if(store.contains(contentId))
			store.delete(contentId);
		
		try {
			
			System.out.println("Requesting root server:"+AppConfig.getProperty("DataServer.RootServer.Service"));
			Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("DataServer.RootServer.IP"));
			IRootServer stub = (IRootServer) registry.lookup(AppConfig.getProperty("DataServer.RootServer.Service") );
			stub.deregister(contentId);                              
			//String str ;
			//str = "delete from content";
			//insmp.put(str);
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return flag ;
	}
	
	public void subscribe() throws RemoteException{
		/*try {
			
			insmp.put("subscribe for user");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	public void unsubscribe() throws RemoteException{
		/*try {
			insmp.put("unsubscribe for user");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	public static Map<String, ContentProperties> getMap(){
		return contentPropMap ;
	}
	public void notify(String data) throws RemoteException
	{

	}

	public static void main(String args[]) { 

		try 
		{
			String config = new String("config/DataServer.cfg");
			File configFile = new File(config);
			FileInputStream fis;
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);			
			fis.close();
			String Id = AppConfig.getProperty("DataServer.Id");
			DataServer obj = new DataServer(Id);
			IDataServer stub = (IDataServer) UnicastRemoteObject.exportObject(obj, 0);

			Registry registry = LocateRegistry.getRegistry();
			boolean found = false;
			while(!found)
			{
				try
				{
					registry.bind(AppConfig.getProperty("DataServer.Service") , stub);
					found = true;
				}
				catch(AlreadyBoundException ex)
				{
					registry.unbind(AppConfig.getProperty("DataServer.Service"));
					registry.bind(AppConfig.getProperty("DataServer.Service") , stub);
					found = true;
				}
				catch(ConnectException ex)
				{
					String rmiPath = AppConfig.getProperty("DataServer.Directory.rmiregistry");
					Runtime.getRuntime().exec(rmiPath);
				}
			}
			System.err.println("Service Ready");
			
		
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}
