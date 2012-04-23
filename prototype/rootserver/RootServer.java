package prototype.rootserver;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import StateManagement.Status;
import AbstractAppConfig.AppConfig;
	
public class RootServer implements IRootServer {
		
    private Map<String,List<String>> dataCacheLocMap;
    private Semaphore lookupMutex;
    
    
	public RootServer(Map<String,List<String>> m1,Semaphore mutex) {
		dataCacheLocMap = m1;	
		lookupMutex = mutex;
		List<String> value = new ArrayList<String>();
		String serviceInstanceInfo = AppConfig.getProperty("RootServer.ServiceInstance");
		value.add(serviceInstanceInfo);
		String serviceInstanceInfoKey = AppConfig.getProperty("RootServer.Name")+"$serviceInstance";
		dataCacheLocMap.put(serviceInstanceInfoKey, value);
		/*Status st = Status.getStatus() ;
		List<String> locations = st.findDataLocations(serviceInstanceInfoKey);
		if(locations.size()==0)
			st.insertData("servicelocations", serviceInstanceInfoKey, serviceInstanceInfo );*/
		//RootClient rc = new RootClient() ;
		//rc.start();
		
	}

   /* public boolean register(String dataname,String source) {
    	
    	System.out.println("Registering :"+dataname);
    	synchronized(dataCacheLocMap)
    	{
    	if(dataCacheLocMap.containsKey(dataname))
    	{
    		List<String> val = dataCacheLocMap.get(dataname);
    		if(!val.contains(source))
    		val.add(0,source);
    		dataCacheLocMap.put(dataname, val);
    			
    	}
    	else
    	{
    	List<String> val = new Vector<String>();
    	val.add(0,source);
    	dataCacheLocMap.put(dataname, val);	
    	}	
    	}
    	return true;
    }
    
	*/
	public boolean register(String dataname,String source){
		System.out.println("Registering :"+dataname);
		Status st = Status.getStatus();
		st.insertData("datalocations", dataname, source);
		//st.insertKeyValue("kvdataloc", "fileName", dataname);
		return true ;
	}
    public boolean deregister(String dataname,String source) {
    	
    	System.out.println("Deregistering :"+dataname);
    	synchronized(dataCacheLocMap)
    	{
    	if(dataCacheLocMap.containsKey(dataname))
    	{
    		List<String> val = dataCacheLocMap.get(dataname);
    		val.remove(source);
    		dataCacheLocMap.put(dataname, val);
    		
    	}
    	lookupMutex.release();
    	
    	}
    	return true;
        }
    
    
    
    public boolean deregister(String dataname) {
    	
    	System.out.println("Deregistering :"+dataname);
    	Status st = Status.getStatus();
		st.deRegisterData("datalocations", dataname);
		//st.insertKeyValue("kvdataloc", "fileName", dataname);
		return true ;
    	
    }
    
    public List<String> find(String dataname) {
    	List<String> locations = new ArrayList<String>();
    	Status st = Status.getStatus();
    	locations = st.findDataLocations(dataname);
    	return locations;
    }
    
    public static void main(String args[]) {
	
    	try
    	{
		
    		File configFile = new File("config/RootServer.cfg");
    		FileInputStream fis;
    		fis = new FileInputStream(configFile);
    		new AppConfig();
    		AppConfig.load(fis);
    		fis.close();
		
			Map<String,List<String>> map = new HashMap<String,List<String>>();
			Semaphore mutex = new Semaphore(1,true);
			RootServer obj = new RootServer(map,mutex);
			IRootServer stub = (IRootServer) UnicastRemoteObject.exportObject(obj, 0);
	    
			Registry registry = LocateRegistry.getRegistry();
			boolean found = false;
			System.out.println("Service Name:Root Server");
			while(!found)
			{
				try
				{
					registry.bind(AppConfig.getProperty("RootServer.Service"), stub);
					found = true;
				}
				catch(AlreadyBoundException ex)
				{
					registry.unbind(AppConfig.getProperty("RootServer.Service"));
					registry.bind(AppConfig.getProperty("RootServer.Service"), stub);
					found = true;
				}
				catch(ConnectException ex)
				{
					String rmiPath = AppConfig.getProperty("RootServer.Directory.rmiregistry");
					Runtime.getRuntime().exec(rmiPath);
					//Runtime.getRuntime().exec("C:\\Program Files\\Java\\jdk1.6.0_16\\bin\\rmiregistry.exe");
				}
			}
			System.err.println("Server ready for service : "+AppConfig.getProperty("RootServer.Service"));
			
	    
    	}
    	catch (Exception e)
    	{
    		System.err.println("Server exception: " + e.toString());
    		e.printStackTrace();
    	}
    }
}
