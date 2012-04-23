package prototype.custodian;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import newNetwork.Connection;

import AbstractAppConfig.AppConfig;
import NewStack.NewStack;
import PubSubModule.Notification;
import StateManagement.ContentState;
import StateManagement.CustodianAppStateManager;
import StateManagement.StateManager;
import prototype.rendezvous.IRendezvous;
import prototype.serviceinstance.IUploader;
import prototype.userregistrar.IUserRegistrarSession;
import prototype.utils.Utils;
import prototype.cache.ICacheServer;
import prototype.dataserver.IDataServer;
import prototype.datastore.DataStore;


@SuppressWarnings("unused")
public class CustodianSession implements ICustodianSession{

	String userId;   												//passed in constructor
	//String custodianId;											//passed in constructor
	IUserRegistrarSession sessionStub;							//passed in constructor
	DataStore store;											//global
	NewStack networkStack;
	StateManager stateManager;
	CustodianAppStateManager appStateManager;

	//a user to pending publication notification may be required for pubsub implementation

	public CustodianSession(String u,IUserRegistrarSession stub,StateManager sManager,CustodianAppStateManager caManager
			,DataStore st,NewStack stack)
	{
		userId = u;
		sessionStub = stub;
		store = st;
		stateManager = sManager;
		appStateManager = caManager;
		networkStack = stack;
		System.out.println("Session established userId: "+userId);
	}

	public boolean subscribe(String subject) throws RemoteException
	{
		return true;
	}

	public boolean unsubscribe(String subject) throws RemoteException
	{
		return true;
	}

	public List<Notification> poll_notification() throws RemoteException
	{
		List<Notification> notificationList = new ArrayList<Notification>();
		try{

			List<String> uploadList = appStateManager.getUploadNotification(userId);

			Iterator<String> it = uploadList.iterator();

			while(it.hasNext())
			{
				Notification notification = new Notification(Notification.Type.UploadAck,it.next());
				it.remove();
				notificationList.add(notification);
			}

			appStateManager.setUploadNotification(userId, new ArrayList<String>());

			return notificationList;
		}catch(Exception e)
		{
			e.printStackTrace();
			return notificationList;
		}
	}

	public boolean close_connection() throws RemoteException
	{

		System.out.println("Closing Connection");
		sessionStub.close_connection();
		networkStack.close();
		return UnicastRemoteObject.unexportObject(this, true);

	}

	//destination should be of format: youtube.com
	public List<String> processDynamicContent(String uploadContentId,int uploadSize,String replyContentId,Connection.Type uploadType,Connection.Type downloadType,String dest) throws RemoteException
	{
		List<String> reply = null;
		try
		{
			System.out.println("New Process Request for Dynamic Content");
			//should hav information in hosts file for destination

			Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("Custodian.Rendezvous.IP"));    
			String rendezvousService = AppConfig.getProperty("Custodian.Rendezvous.Service");
			IRendezvous stub = (IRendezvous) registry.lookup(rendezvousService);   //config
			List<String> l = stub.find(dest+"$serviceInstance");
			String serviceInstanceInfo = l.get(0);
			System.out.println("response from rendezvous:    "+l);	

			String[] connectionInfo = serviceInstanceInfo.split(":");
			String cacheAddress = connectionInfo[0];

			//create control channel with the cache location
			Registry serverRegistry = LocateRegistry.getRegistry(cacheAddress);
			IUploader serverStub = (IUploader) serverRegistry.lookup(AppConfig.getProperty("Custodian.ServiceInstance.UploaderService") );    //should be config driven
			int size =0 ;
			//int size = serverStub.processDynamicContent(uploadContentId,uploadSize, replyContentId,downloadType,userId,networkStack.getStackId()+":"+networkStack.getServerPort());
			if(uploadType != Connection.Type.USB)
			{
				char[] bits = new char[uploadSize];
				for(int i = 0;i < uploadSize;i++)
					bits[i] = '0';
				String bitMap = new String(bits);
				List<String> destinations = new ArrayList<String>();
				destinations.add(serviceInstanceInfo);

				//downloadState has totalsegments set to -1 as we do not want to track the state of 
				//download at custodian
				ContentState downloadStateObject = new ContentState(uploadContentId,0,null,
						uploadType.ordinal(),destinations,-1,0,ContentState.Type.tcpDownload,networkStack.getStackId(),true);
				ContentState uploadStateObject = new ContentState(uploadContentId,uploadContentId,0,null,
						uploadType.ordinal(),destinations,size,0,ContentState.Type.tcpUpload,networkStack.getStackId(),true);
				stateManager.setStateObject(downloadStateObject);
			//	stateManager.setTCPUploadState(uploadStateObject);
				String bitMap1;
				char[] bits1 = new char[size];
				
				for(int i = 0;i < size;i++)
				{
					bits1[i] = '0';
				}
				bitMap1 = new String(bits1);     
				ContentState replyStateObject = new ContentState(replyContentId,0,null,-1,null,size,0,ContentState.Type.tcpDownload,networkStack.getStackId(),true);
				stateManager.setStateObject(replyStateObject);
				
			}

			reply = new ArrayList<String>();
			reply.add(Integer.toString(size));
			reply.add(serviceInstanceInfo);
			return reply;

		}catch(Exception e)
		{
			e.printStackTrace();
			return reply;
		}
	}

	public String DTNUpload(String data,int size) throws RemoteException
	{
		String destination = null; 

		try {
			destination = AppConfig.getProperty("Custodian.UploadCache");
			System.out.println("New Data To be Uploaded");
			//inform service instance about new upload
			//should hav a hosts file to get info on the cache
			String[] connectionInfo = destination.split(":");
			String IP = connectionInfo[0];

			//create control channel with the cache location
			Registry serverRegistry = LocateRegistry.getRegistry(IP);
			IDataServer serverStub;
			
			serverStub = (IDataServer) serverRegistry.lookup(AppConfig.getProperty("Custodian.DataServer.Service") );
			serverStub.upload(data, size,userId,null);

		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return destination;
		}    //should be config driven
		return destination;
	}

	//dest should be of the form: youtube.com
	public List<String> DTNUpload(String data,int size,String dest) throws RemoteException
	{
		List<String> reply = null;
		String contentName = null;
		String serviceInstanceInfo = null;
		try {
			System.out.println("New Data To be Uploaded");
			//inform service instance about new upload

			Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("Custodian.Rendezvous.IP"));    
			String rendezvousService = AppConfig.getProperty("Custodian.Rendezvous.Service");
			IRendezvous stub = (IRendezvous) registry.lookup(rendezvousService);   //config
			List<String> l = stub.find(dest+"$serviceInstance");
			serviceInstanceInfo = l.get(0);
			System.out.println("response from rendezvous:    "+l);	


			String[] connectionInfo = serviceInstanceInfo.split(":");
			String IP = connectionInfo[0];

			//create control channel with the cache location
			Registry serverRegistry = LocateRegistry.getRegistry(IP);
			IUploader serverStub;

			serverStub = (IUploader) serverRegistry.lookup(AppConfig.getProperty("Custodian.ServiceInstance.UploaderService") );
			//contentName = serverStub.upload(data, size,userId);
			reply = new ArrayList<String>();
			reply.add(contentName);
			reply.add(serviceInstanceInfo);
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return reply;
		}    //should be config driven
		return reply;

	}

	public void upload(String data,int size) throws RemoteException
	{
		System.out.println("Inside CustodianSession.java: upload1111");
		String destination = null;
		try
		{
			destination = AppConfig.getProperty("Custodian.UploadCache");
			System.out.println("Destination = " + destination);
			System.out.println("New Data To be Uploaded");
			//inform service instance about new upload
			//should have a hosts file storing cache information
			String[] connectionInfo = destination.split(":");
			String IP = connectionInfo[0];
			System.out.println("IP = " + IP);
			//create control channel with the cache location
			Registry serverRegistry = LocateRegistry.getRegistry(IP);
			IDataServer serverStub = (IDataServer) serverRegistry.lookup(AppConfig.getProperty("Custodian.DataServer.Service") );
			System.out.println("Calling upload inside CustodianSession.java");
			serverStub.upload(data, size,userId,null);

			char[] bits = new char[size];
			for(int i = 0;i < size;i++)
				bits[i] = '0';
			List<String> destinations = new ArrayList<String>();
			destinations.add(destination);
			ContentState downloadStateObject = new ContentState(data,0,null,
					-1,null,-1,0,ContentState.Type.tcpDownload,networkStack.getStackId(),true);
			ContentState uploadStateObject = new ContentState(data,data,0,null,
					Connection.Type.DSL.ordinal(),destinations,-1,0,ContentState.Type.tcpUpload,networkStack.getStackId(),true);
			stateManager.setStateObject(downloadStateObject);
			stateManager.setTCPUploadState(uploadStateObject);
			
			


		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	//dest should be of format youtube.com
	public String upload(String userContentName,int size,String dest) throws RemoteException
	{
		System.out.println("Inside CustodianSession.java: upload");
		System.out.println("destination: " + dest);
		String contentName = null;
		try
		{
			System.out.println("New Data To be Uploaded");
			//inform service instance about new upload
			//dest info should be there in hosts file

			Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("Custodian.Rendezvous.IP"));    
			String rendezvousService = AppConfig.getProperty("Custodian.Rendezvous.Service");
			IRendezvous stub = (IRendezvous) registry.lookup(rendezvousService);   //config
			List<String> l = stub.find(dest+"$serviceInstance");
			String serviceInstanceInfo = l.get(0);
			System.out.println("response from rendezvous:    "+l);	

			File f = new File("config/output.cfg");
			FileOutputStream fop = new FileOutputStream(f);;				
			if(f.exists())
			{
		          String str=l.toString();
		          fop.write(str.getBytes());
		          fop.flush();
		          fop.close();
			}
			
			
			String[] connectionInfo = serviceInstanceInfo.split(":");
			String IP = connectionInfo[0];

			System.out.println("IP= "+IP);
			
			//create control channel with the cache location
			Registry serverRegistry = LocateRegistry.getRegistry(IP);
			IUploader serverStub = (IUploader) serverRegistry.lookup(AppConfig.getProperty("Custodian.ServiceInstance.UploaderService") );    //should be config driven

			System.out.println("Calling serverStub.upload");
			//contentName = serverStub.upload(userContentName, size,userId);



			char[] bits = new char[size];
			for(int i = 0;i < size;i++)
				bits[i] = '0';
			List<String> destinations = new ArrayList<String>();
			destinations.add(serviceInstanceInfo);
			ContentState downloadStateObject = new ContentState(userContentName,0,null,
					-1,null,-1,0,ContentState.Type.tcpDownload,networkStack.getStackId(), true);
			ContentState uploadStateObject = new ContentState(userContentName,userContentName,0,null,
					Connection.Type.DSL.ordinal(),destinations,-1,0,ContentState.Type.tcpUpload,networkStack.getStackId(),true);
			stateManager.setStateObject(downloadStateObject);
			stateManager.setTCPUploadState(uploadStateObject);


		}catch(Exception e)
		{
			e.printStackTrace();
			return contentName;
		}
		return contentName;
		//inform service instance about new upload
		//update state with the list of the data that should be uploaded to the service Instance
	}


	/*
	 * find method is called by the user when he wants to fetch some content object
	 * the method returns length of the file if 
	 * the file is already downloaded with the custodian(non-Javadoc)
	 * returns 1,if a request has been enqueued for the file after knowing the location at which it is present
	 * return -1,if the file has not been found in the network
	 * @see prototype.custodian.ICustodianSession#find(java.lang.String, network.Connection.Type)
	 */

	private void notifyNeighborCaches(String contentName)
	{
		try {
			//information should be in hosts file
			String neighborList = AppConfig.getProperty("Custodian.RelativeCaches");
			List<String> caches = Utils.parse(neighborList);
			Iterator<String> it = caches.iterator();
			while(it.hasNext())
			{
				String cacheInfo = it.next();
				it.remove();
				String[] connectionInfo = cacheInfo.split(":");
				String IP = connectionInfo[0];

				//create control channel with the cache location
				Registry serverRegistry;

				serverRegistry = LocateRegistry.getRegistry(IP);

				ICacheServer serverStub = (ICacheServer) serverRegistry.lookup(AppConfig.getProperty("Custodian.CacheService") );    //should be config driven
				serverStub.notify(contentName);

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public long find(int AppId,String dataname,Connection.Type type) throws RemoteException
	{		
		notifyNeighborCaches(dataname);
		System.out.println("Find request received by user on "+dataname);
		if(store.contains(dataname) && store.contains(dataname+".marker"))
		{
			if(type == Connection.Type.USB)
			{
				//create DTN state but we will handle DTN later
				return 1;//stack.sendDTNSegments(AppId,dataname,userId);
			}
			else
			{
				//stack.setPolicy(AppId,type);
				return networkStack.countSegments(dataname);
			}
		}
		else if(stateManager.containsTCPDownloadRequest(dataname))
		{
			return 1;
		}
		else
		{
			try
			{
				System.out.println("Requesting rendezvous");
				Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("Custodian.Rendezvous.IP"));    
				String rendezvousService = AppConfig.getProperty("Custodian.Rendezvous.Service");
				IRendezvous stub = (IRendezvous) registry.lookup(rendezvousService);   //config
				List<String> l = stub.find(dataname);
				System.out.println("response from rendezvous:    "+l);					
				
				
				File f = new File("config/output.cfg");
				FileOutputStream fop = new FileOutputStream(f);;				
				if(f.exists())
				{
			          String str=l.toString();
			          fop.write(str.getBytes());
			          fop.flush();
			          fop.close();
				}
							
				
				//List<String> l = new ArrayList<String>();
				//l.add("localhost:5678:10");
				
				int size = 1;
				if(l != null)
				{
					//cache location is of type IP:port:DTNId
					String cache = l.get(0);
					String[] cacheInformation = cache.split(":");
					String IP = cacheInformation[0];
					//create control channel with the cache location
					Registry cacheRegistry = LocateRegistry.getRegistry(IP);
					IDataServer cacheStub = (IDataServer) cacheRegistry.lookup(AppConfig.getProperty("Custodian.DataServer.Service") );    //should be config driven
					//cacheStub.createServerStack(custodianId);
					//cacheCManager.getDataConnection(IP,port,stateManager);
					if(type != Connection.Type.USB)
					{
						
						size = cacheStub.TCPRead(1,dataname,type,networkStack.getStackId());
						String bitMap = new String("");
						if(size > 0)
						{
							char[] bits = new char[size];
							for(int i =0;i < size;i++)
							{
								bits[i] = '0';
							}
							bitMap = new String(bits);
						}
						
						ContentState downloadStateObject = new ContentState(dataname,0,null,
								Connection.Type.DSL.ordinal(),l,size,0,ContentState.Type.tcpDownload,networkStack.getStackId(),true);
						stateManager.setTCPDownloadState(downloadStateObject);
						return size;
					}
					else
					{
						if(cacheStub.DTNRead(1,dataname,userId,networkStack.getStackId()))
							return 1;
						else
							return -1;
					}

				}
				else
					return -1;

			}catch(Exception e)
			{
				System.out.println("Exception in contacting rendezvous"+e.toString());
				e.printStackTrace();
				return -1;
			}	
		}
	}
}