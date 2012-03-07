package prototype.serviceinstance;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Date;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import newNetwork.Connection;
import AbstractAppConfig.AppConfig;
import NewStack.NewStack;
import PubSubModule.IPubSubNode;
import PubSubModule.Notification;
import StateManagement.ContentState;
import StateManagement.ServiceInstanceAppStateManager;
import StateManagement.StateManager;
import prototype.dataserver.IDataServer;
import prototype.datastore.DataStore;
import prototype.userregistrar.ICustodianLogin;

@SuppressWarnings("unused")
public class ServiceInstance implements IUploader{

	DataStore store;
	StateManager stateManager;
	NewStack networkStack;
	IDataServer session;
	private int contentId;
	String cacheId;
	String dataServerInfo;
	

	public ServiceInstance(String Id,DataStore dStore,StateManager manager,NewStack stack,IDataServer stub)
	{
		cacheId = Id;
		store = dStore;
		networkStack = stack;
		session = stub;
		stateManager = manager;
		String dataServerId = AppConfig.getProperty("ServiceInstance.DataServer.IP");
		String dataServerPort = AppConfig.getProperty("ServiceInstance.DataServer.Port");
		String dataServerDTNId = AppConfig.getProperty("ServiceInstance.DataServer.DTNId");
		dataServerInfo = dataServerId+":"+dataServerPort+":"+dataServerDTNId;
		contentId = 0 ;
	}

	public synchronized String generateContentId() throws RemoteException
	{
		contentId++;
		String rootServer = AppConfig.getProperty("ServiceInstance.Name");
		String newId = rootServer+"$"+contentId ;
		System.out.println("Value of ContentId: "+newId);
		return newId;
	}
	
	public static void main(String[] args)  throws IOException{
		//System.out.print("Hello 1");

		try {

			File configFile = new File("config/ServiceInstance.cfg");
			FileInputStream fis;
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);
			fis.close();

			String dirPath = AppConfig.getProperty("ServiceInstance.Directory.Path");
			DataStore store = new DataStore(dirPath);

			File status = store.getFile(AppConfig.getProperty("ServiceInstance.StatusFile"));
			//StateManager stateManager = new StateManager(status,"status");
			StateManager stateManager = new StateManager("status");
			//System.out.print("Hello 1");
			int serverPort = Integer.parseInt(AppConfig.getProperty("ServiceInstance.DataConnection.Port"));
			DataStore usbStore = null;
			if(AppConfig.getProperty("Routing.allowDTN").equals("1"))
			{
				usbStore = new DataStore(AppConfig.getProperty("ServiceInstance.USBPath"));
			}

			//System.out.print("Hello 1111");
			String dataServer = AppConfig.getProperty("ServiceInstance.DataServer.IP");	
			String DataService = AppConfig.getProperty("ServiceInstance.DataServer.Service");
			
			Registry registry;
			//System.out.print("Hello 222");
			registry = LocateRegistry.getRegistry(dataServer);
			//System.out.print("Hello 333");
			IDataServer stub = null;       
			
			
			int maxCons = Integer.parseInt(AppConfig.getProperty("ServiceInstance.NetworkStack.MaximumConnections"));
			//System.out.print("Hello 444");
			List<Integer> portList = new ArrayList<Integer>(maxCons);							
			int port = Integer.parseInt(AppConfig.getProperty("ServiceInstance.NetworkStack.Port"));
			//System.out.print("Hello 555");
			for(int i = 0;i < 20;i++)
			{
				portList.add(port);
				port++;
			}
			//System.out.print("Hello 666");

			String cacheId = AppConfig.getProperty("ServiceInstance.Id");
			//System.out.print("Hello 666.222");
			NewStack stack = null;
			try{
				stack = new NewStack(cacheId,stateManager,store,usbStore,null,serverPort,portList);
			}catch(Exception ex){
				ex.printStackTrace();
			}
			
			//int i=5;
			//int j=10;
			//int k = i+j;
			//System.out.print(k);
			String Id = AppConfig.getProperty("ServiceInstance.Id");

			ServiceInstance obj = new ServiceInstance(Id,store,stateManager,stack,stub);
			//System.out.print("Hello 344");
			IUploader uploaderStub = (IUploader) UnicastRemoteObject.exportObject(obj, 0); 
			Registry registry1 = LocateRegistry.getRegistry();
			//System.out.print("Hello 355");
			boolean found = false;
			//System.out.print("Hello 366");
			System.out.print("Service Name:Service Instance");
			while(!found)
			{
				try
				{
					//System.out.print("Hello 1");
					registry1.bind(AppConfig.getProperty("ServiceInstance.UploaderService"), uploaderStub);
					found = true;
					//System.out.print("Hello 2");
				}
				catch(AlreadyBoundException ex)
				{
					//System.out.print("Hello 3");
					registry1.unbind(AppConfig.getProperty("ServiceInstance.UploaderService"));					
					registry1.bind(AppConfig.getProperty("ServiceInstance.UploaderService"), uploaderStub);
					found = true;
					//System.out.print("Hello 4");
				}
				catch(ConnectException ex)
				{
					//System.out.print("Hello 5");
					String rmiPath = AppConfig.getProperty("ServiceInstance.Directory.rmiregistry");
					Runtime.getRuntime().exec(rmiPath);
					//System.out.print("Hello 6");
				}
			}
			//System.out.print("Hello");
			System.err.println("Service Instance ready");
			

		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
