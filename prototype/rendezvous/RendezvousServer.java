package prototype.rendezvous;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.registry.Registry;


import java.rmi.registry.LocateRegistry;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import AbstractAppConfig.AppConfig;

import prototype.rootserver.IRootServer;

public class RendezvousServer implements IRendezvous{

	Map<String,List<String>> localContentLookup; 

	public RendezvousServer(Map<String,List<String>> contentLookup) 
	{
		localContentLookup  = contentLookup;
	}

	public List<String> find(String dataname) throws RemoteException,NotBoundException
	{
		if(localContentLookup.containsKey(dataname))
			return localContentLookup.get(dataname);
		else
		{
			String rootServer = null;
			StringTokenizer st = new StringTokenizer(dataname,"$");
			if(st.hasMoreElements())
				rootServer = st.nextToken();		
			System.out.println("Contacting Root Server");
			Registry registry;		
			IRootServer stub = null;
			boolean found = false;
			while(!found)
			{
				try
				{
					registry = LocateRegistry.getRegistry(rootServer);
					stub = (IRootServer) registry.lookup(AppConfig.getProperty("Rendezvous.RootServer.Service"));   //create a parser and make it config driven
					found = true;
				}
				catch(ConnectException ex)
				{
					found = false;
					try {
						String rmiPath = AppConfig.getProperty("Rendezvous.Directory.rmiregistry");
						Runtime.getRuntime().exec(rmiPath);
						//Runtime.getRuntime().exec("C:\\Program Files\\Java\\jdk1.6.0_21\\bin\\rmiregistry.exe");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				catch(RemoteException e)
				{
					registry = LocateRegistry.getRegistry(AppConfig.getProperty("Rendezvous.DefaultRootServer").trim());
					stub = 	(IRootServer) registry.lookup(AppConfig.getProperty("Rendezvous.RootServer.Service"));
					found = true;
				}				
			}
			List<String> l = stub.find(dataname);
			System.out.println("response in rendezvous: "+l);
			synchronized(localContentLookup)
			{
				localContentLookup.put(dataname,l);
			}
			return l;
		}
	}

	public static void main(String args[])
	{	
		System.out.println("Looking for Rendezvous Service");
		try {

			File configFile = new File("config/Rendezvous.cfg");
			FileInputStream fis;
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);
			fis.close();

			Map<String,List<String>> lookup = new HashMap<String,List<String>>();
			RendezvousServer obj = new RendezvousServer(lookup);
			IRendezvous stub = (IRendezvous) UnicastRemoteObject.exportObject(obj, 0);

			Registry registry = LocateRegistry.getRegistry();
			boolean found = false;
			while(!found)
			{
				try
				{
					registry.bind(AppConfig.getProperty("Rendezvous.Service") , stub);
					found = true;
				}
				catch(AlreadyBoundException ex)
				{
					registry.unbind(AppConfig.getProperty("Rendezvous.Service"));
					registry.bind(AppConfig.getProperty("Rendezvous.Service") , stub);
					found = true;
				}
				catch(ConnectException ex)
				{
					Runtime.getRuntime().exec("C:\\Program Files\\Java\\jdk1.7.0_01\\bin\\rmiregistry.exe");

				}
			}

			System.err.println("Services ready");

		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
