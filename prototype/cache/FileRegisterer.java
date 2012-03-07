package prototype.cache;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.BlockingQueue;

import prototype.rootserver.IRootServer;
import AbstractAppConfig.AppConfig;

public class FileRegisterer extends Thread{
	
	BlockingQueue<String> files;
	boolean execute;
	
	public FileRegisterer(BlockingQueue<String> fileDownloads)
	{
		files = fileDownloads;
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
			try {
				
				String newFile = files.take();
				Registry registry = LocateRegistry.getRegistry(AppConfig.getProperty("CacheServer.RootServer.IP"));
				IRootServer stub = (IRootServer) registry.lookup(AppConfig.getProperty("CacheServer.RootServer.Service") );
				String location = AppConfig.getProperty("CacheServer.Id")+":"+AppConfig.getProperty("CacheServer.Port")+":"+AppConfig.getProperty("CacheServer.Id");
				stub.register(newFile,location);                              //should be config driven
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}	
	
}