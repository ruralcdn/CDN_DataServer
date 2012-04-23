package prototype.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.registry.Registry;


import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import newNetwork.Connection;

import AbstractAppConfig.AppConfig;
import prototype.dataserver.DataServer;


@SuppressWarnings("unused")
public class CacheServer implements ICacheServer{


	public CacheServer(String Id)
	{

	}

	public void notify(String contentId) throws RemoteException
	{
		System.out.println("received notifcation about content"+contentId);
	}
	public String upload(String myContentName,int segments,String serviceInstance, String username) throws RemoteException{
		
		return null;
	}

	public static void Init(String Id)
	{
		try
		{
			//DataServer.Init(Id);
			CacheServer obj = new CacheServer(Id);
			ICacheServer stub = (ICacheServer) UnicastRemoteObject.exportObject(obj, 0);
			
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(AppConfig.getProperty("CacheServer.Service"), stub);
			System.err.println("Server ready");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String args[])
	{
		String config = new String("config/CacheServer.cfg");
		File configFile = new File(config);
		FileInputStream fis;
		try {
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);			
			fis.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String Id = AppConfig.getProperty("CacheServer.Id");
		//Init(Id);
	}


}
