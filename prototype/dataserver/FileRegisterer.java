package prototype.dataserver;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import PubSubModule.IPubSubNode;
import PubSubModule.Notification;
import StateManagement.ServiceInstanceAppStateManager;
import StateManagement.Status;
import prototype.rootserver.IRootServer;
import prototype.userregistrar.ICustodianLogin;
import AbstractAppConfig.AppConfig;


public class FileRegisterer extends Thread{
	
	BlockingQueue<String> files;
	boolean execute;
	ServiceInstanceAppStateManager AppManager;
	List<String> upSync ;
	String cacheId ;
	public FileRegisterer(BlockingQueue<String> fileDownloads,ServiceInstanceAppStateManager appManager, String id)
	{
		files = fileDownloads;
		AppManager = appManager;
		execute = true;
		//upSync = DataServer.UploadSync;
		cacheId = id ;
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
				
				String newFile = files.take();
				System.out.println("Requesting root server:"+AppConfig.getProperty("DataServer.RootServer.Service"));
				Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("DataServer.RootServer.IP"));
				IRootServer stub = (IRootServer) registry.lookup(AppConfig.getProperty("DataServer.RootServer.Service") );
				String location = AppConfig.getProperty("DataServer.Id")+":"+AppConfig.getProperty("DataServer.Port")+":"+AppConfig.getProperty("DataServer.Id");
				stub.register(newFile,location);                              
				/*if(upSync.contains(newFile))
				{
					System.out.println("executing if block in FileRegisterer");
					//int count = Integer.parseInt(newFile.substring(newFile.indexOf('$')+1,newFile.lastIndexOf('.')));
					@SuppressWarnings("unused")
					String str ;
					str = "insert into dirtable values('"+cacheId+"','"+newFile+"')";
					//insmp.put(str);
				}
				*/
				try
				{
					System.out.println("Upload to dataserver complete");
					String fileName = newFile.substring(0, newFile.lastIndexOf('.'));
					System.out.println("File name without extension is: "+fileName);
					//String requester = AppManager.getUploadRequester(newFile);
					String requester = AppManager.getUploadRequester(fileName);
					System.out.println(fileName+" requester name is: "+requester);
					System.out.println("Calling updateState");
					Status st = Status.getStatus();
					st.updateState("status",newFile, 0);
					
					Registry serverRegistry;
					String userRegistrarIP = AppConfig.getProperty("UserRegistrar.IP");
					serverRegistry = LocateRegistry.getRegistry(userRegistrarIP);

					ICustodianLogin  userRegistrarStub = (ICustodianLogin) serverRegistry.lookup(AppConfig.getProperty("UserRegistrar.Service") );    //should be config driven
					String custodianId = userRegistrarStub.get_active_custodian(requester).split(":")[0];

					System.out.println("Active custodian is: "+custodianId);
					Registry custodianRegistry = LocateRegistry.getRegistry(custodianId);
					IPubSubNode pubsubStub = (IPubSubNode) custodianRegistry.lookup(AppConfig.getProperty("PubSub.Service") );
					Notification notification = new Notification(Notification.Type.UploadAck,requester+":"+newFile);
					pubsubStub.notify(notification);
					

				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}	
	
}