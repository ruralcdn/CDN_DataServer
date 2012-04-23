/*package NewStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import AbstractAppConfig.AppConfig;
import NewStack.Packet;
import StateManagement.StateManager;

import prototype.datastore.DataStore;

import newNetwork.USBConnection;
import newNetwork.Connection;
import newNetwork.TCPConnection;

public class LinkDetector extends Thread{

	private String connectionId;
	private static List<String> destinationConnectionIds;
	private List<String> localIPs;
	private Scheduler scheduler;
	private boolean execute;
	private static int dataConnectionId;
	private DataStore store;
	private DataStore DTNStore;
	private List<Integer> connectionPorts;	
	
	public LinkDetector(String Id,Scheduler sched,List<Integer> portList,DataStore dStore,DataStore usbStore)
	{
		connectionId = Id;
		scheduler = sched;
		localIPs = new ArrayList<String>();
		execute = true;
		dataConnectionId = 0;
		connectionPorts = portList;
		store = dStore;
		DTNStore = usbStore;
		destinationConnectionIds = new ArrayList<String>();

	}

	public void addDestination(String destination)
	{
		// Populate the list of local network interfaces
		try{
			InetAddress host = InetAddress.getLocalHost();
			/*******************Changes made  in 09oct ***************************/
/*					InetAddress[] IPs = InetAddress.getAllByName(host.getHostName());
			for(int j = 0 ; j < IPs.length ; j++){
				String ip = IPs[j].getHostAddress();
				if(!localIPs.contains(ip))
				{
					System.out.println("IP:" + ip);
					localIPs.add(ip);
				}				
			}
		}
		catch (Exception ioe){ System.out.println(ioe);
		ioe.printStackTrace();
		}
		
		//Destination should be of format DestinationID:Port
		String[] connectionInfo = destination.split(":");
		InetAddress add;
		try {
			add = InetAddress.getByName(connectionInfo[0]);
			int port = Integer.parseInt(connectionInfo[1]);
			InetAddress local = InetAddress.getByName("localhost");
			if(add.equals(local))
			{
				//create Links for local connection
				Map<String,List<Connection>> cp = scheduler.getConnectionPool();
				List<Connection> connections = cp.get(connectionInfo[0]);

				if(connections != null)
				{
					Connection[] temp = new Connection[1];
					Connection[] conArray = connections.toArray(temp);

					for(int i = 0;i < conArray.length;i++)
					{
						Connection con = conArray[i];
						if(con.getLocalAddress().equals(local))
							return;
					}
				}

				if(!connectionPorts.isEmpty())
				{
					dataConnectionId++;
					Connection con;
					try {

						con = new TCPConnection(dataConnectionId,add,port,local,connectionPorts.get(0));
						connectionPorts.remove(0);
						System.out.println("New Connection created thru method addDes in Link Detct");
						Packet packet = new Packet(connectionId);
						con.writePacket(packet);
						System.out.println("NEW Connection Established in link detector thru method addDes in Link Detct");
						scheduler.addConnection(connectionInfo[0],con);
						destinationConnectionIds.add(destination);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
			else if(!destinationConnectionIds.contains(destination))
			{
					
						for(int i = 0;i < localIPs.size();i++)
						//while(it.hasNext())
						{
							//String IP = it.next();
							//it.remove();
							//local = InetAddress.getByName(it.next()\);
							//it.remove();
							local = InetAddress.getByName(localIPs.get(i));
							if(!connectionPorts.isEmpty())
							{
								dataConnectionId++;
								Connection con;
								try {

									con = new TCPConnection(dataConnectionId,add,port,local,connectionPorts.get(0));
									connectionPorts.remove(0);
									System.out.println("New Connection created thru method addDes in Link Detct elseif");
									Packet packet = new Packet(connectionId); // authentication packet 
									con.writePacket(packet);
									System.out.println("NEW Connection Established in link detectorthru method addDes in Link Detct elseif");
									scheduler.addConnection(connectionInfo[0],con);


								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}

						}

						destinationConnectionIds.add(destination);
				}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public void close()
	{
		execute = false;
	}

	public void run()
	{

		boolean DTNlink = false;

		while(execute)
		{

			//************************ ADD USB detection*******************************
			if(DTNStore != null)
			{
				File DTNStatusFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.StatusFile"));
				//StateManager usbStateManager = new StateManager(DTNStatusFile,null);
				StateManager usbStateManager = new StateManager(null);
				if(DTNStatusFile.exists() && !DTNlink)
				{
					System.out.println("Found DTN link :)");		
					File configFile = store.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties Config = new Properties();
					FileInputStream fis;
					try {
						fis = new FileInputStream(configFile);
						Config.load(fis);
						fis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					File DTNconfigFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties DTNConfig = new Properties();
					FileInputStream DTNfis;
					try {
						DTNfis = new FileInputStream(DTNconfigFile);
						DTNConfig.load(DTNfis);
						DTNfis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					String connectionId = Config.getProperty("DTNId");
					String DTNconnectionId = DTNConfig.getProperty("DTNId");

					//create a stack for end system 
					NewStack stack = new NewStack(DTNconnectionId,usbStateManager,DTNStore,null);

					//create Queue to communicate
					//readQueue reads from USb and writeQueue writes to USB
					BlockingQueue<Packet> readQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					BlockingQueue<Packet> writeQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					//create a USb connection
					USBConnection usbCon = new USBConnection(readQueue,writeQueue);
					//create a USB Connection
					USBConnection usbRouterCon = new USBConnection(writeQueue,readQueue);
					scheduler.addConnection(DTNconnectionId, usbCon);
					stack.addConnection(connectionId, usbRouterCon);
					DTNlink = true;
				}
				else if(DTNlink && !DTNStatusFile.exists())
				{
					DTNlink = false;
				}
			}
			
			try{
				InetAddress host = InetAddress.getLocalHost();
				/*******************Changes made  in 09oct ***************************/
/*				InetAddress[] IPs = InetAddress.getAllByName(host.getHostName());
				for(int j = 0 ; j < IPs.length ; j++){
					String ip = IPs[j].getHostAddress();
					if(!localIPs.contains(ip))
					{
						System.out.println("IP:" + ip);
						InetAddress localIP = IPs[j];
						for(int i = 0;i < destinationConnectionIds.size();i++){
							dataConnectionId++;
							if(!connectionPorts.isEmpty())
							{
								String destination = destinationConnectionIds.get(i);
								String[] connectionInfo = destination.split(":");
								InetAddress IP = InetAddress.getByName(connectionInfo[0]);
								int port = Integer.parseInt(connectionInfo[1]);
								Connection con = new TCPConnection(dataConnectionId,IP,port,localIP,connectionPorts.get(0));
								connectionPorts.remove(0);
								System.out.println("New Connection created in run method");
								Packet packet = new Packet(connectionId);
								con.writePacket(packet);
								System.out.println("NEW Connection Established in link detector in run method");
								scheduler.addConnection(connectionInfo[0],con);
							}
							else
								System.out.println("local port unavailable");
						}

						localIPs.add(ip);

					}
					
				}
			}
			catch (Exception ioe){ System.out.println(ioe);
			ioe.printStackTrace();
			}
		}

	}
	
	public static void setDestinationIds(List<String> destinationIds)
	{
		destinationConnectionIds = destinationIds ;
		System.out.println("destinationConnectionIds in Link Detector: " + destinationConnectionIds);
		
	}
	
	public static List<String> getDestinationIds()
	{
		return destinationConnectionIds ;
		
	}



}

*/

package NewStack;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import NewStack.Packet;
import StateManagement.ContentState;
import StateManagement.StateManager;
import prototype.datastore.DataStore;
import newNetwork.Connection;
import newNetwork.TCPConnection;

public class LinkDetector extends Thread{

	private String connectionId;
	private static List<String> destinationConnectionIds;
	private List<String> localIPs;
	private Scheduler scheduler;
	@SuppressWarnings("unused")
	private boolean execute;
	private static boolean flag = true ;
	private static int dataConnectionId;
	@SuppressWarnings("unused")
	private DataStore store;
	@SuppressWarnings("unused")
	private DataStore DTNStore;
	private List<Integer> connectionPorts;	
	Map<String, ContentState> mpUp ;
	
	public LinkDetector(String Id,Scheduler sched,List<Integer> portList,DataStore dStore,DataStore usbStore)
	{
		connectionId = Id;
		scheduler = sched;
		localIPs = new ArrayList<String>();
		execute = true;
		dataConnectionId = 0;
		connectionPorts = portList;
		store = dStore;
		DTNStore = usbStore;
		destinationConnectionIds = new ArrayList<String>();
		mpUp = new HashMap<String, ContentState>();
	}

	public boolean addDestination(String destination)
	{
		
		try 
		{
			
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for(NetworkInterface netInf : Collections.list(nets))
			{
				if(!netInf.isPointToPoint() && !netInf.isLoopback())
				{
					Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
					for(InetAddress inet : Collections.list(inetAdd))
					{
						if(!localIPs.contains(inet.getHostAddress()))
						{
							localIPs.add(inet.getHostAddress());
						}
					}
				}
			}
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		boolean addDest = true ;
		String[] connectionInfo = destination.split(":");
		InetAddress local;
		try
		{
			InetAddress add = InetAddress.getByName(connectionInfo[0]);
			int port = Integer.parseInt(connectionInfo[1]);
			if(!destinationConnectionIds.contains(destination))
			{
				
				for(int i = 0;i < localIPs.size();i++)
				{
					System.out.println("Adding the Destinations in Scheduler");
					local = InetAddress.getByName(localIPs.get(i));
					if(!connectionPorts.isEmpty())
					{
						dataConnectionId++;
						Connection con;
						try 
						{
							con = new TCPConnection(dataConnectionId,add,port,local,connectionPorts.get(0));
							connectionPorts.remove(0);
							System.out.println("New Connection created thru method addDes in Link Detct elseif");
							Packet packet = new Packet(connectionId); // authentication packet 
							con.writePacket(packet);
							flag = false ;
							System.out.println("NEW Connection Established in link detectorthru method addDes in Link Detct elseif");
							scheduler.addConnection(connectionInfo[0],con);
							//if(!destinationConnectionIds.contains(destination))
							destinationConnectionIds.add(destination);
							

						}
						catch (ConnectException e)
						{
							System.out.println("Serever is terminated or Network unreachable");
							addDest = false ;
							mpUp = StateManager.getUpMap();
							Set<String> key = mpUp.keySet();
							Iterator<String> it = key.iterator();
							while(it.hasNext())
							{
								String contentId = it.next();
								ContentState contentState = mpUp.get(contentId);
								if(contentState.getPreferredRoute().contains(destination))
								{	
									contentState.currentSegments = 0 ;
									mpUp.put(contentId,contentState);
									//destinationConnectionIds.remove(destination);
								}	
									
							}
							Thread.sleep(1000);	
				 		}
						catch (IOException e)
						{
							System.out.println("Link Failure occured");
							addDest = false ;
							if(!flag)
							{
								mpUp = StateManager.getUpMap();
								Set<String> key = mpUp.keySet();
								Iterator<String> it = key.iterator();
								while(it.hasNext())
								{
									String contentId = it.next();
									ContentState contentState = mpUp.get(contentId);
									/**
									 * Here below the value 50 should be configuration derived
									 * Right now we have 5 Queues and each queue contains 10 packets
									 * So that total loss at max is of 50 packets in case of Link failure 
									 */
									if(contentState.currentSegments > 50)
										contentState.currentSegments -= 50 ;
									else
										contentState.currentSegments = 0 ;
									mpUp.put(contentId,contentState);
										//destinationConnectionIds.remove(destination);
												
								}
								flag = true ;
							}
							Thread.sleep(1000);
						}
					}	
				}	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return addDest ;
	}


	public void close()
	{
		execute = false;
	}

	/*public void run()
	{

		boolean DTNlink = false;

		while(execute)
		{

			//************************ ADD USB detection*******************************
			if(DTNStore != null)
			{
				File DTNStatusFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.StatusFile"));
				//StateManager usbStateManager = new StateManager(DTNStatusFile,null);
				StateManager usbStateManager = new StateManager(null);
				if(DTNStatusFile.exists() && !DTNlink)
				{
					System.out.println("Found DTN link :)");		
					File configFile = store.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties Config = new Properties();
					FileInputStream fis;
					try {
						fis = new FileInputStream(configFile);
						Config.load(fis);
						fis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					File DTNconfigFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties DTNConfig = new Properties();
					FileInputStream DTNfis;
					try {
						DTNfis = new FileInputStream(DTNconfigFile);
						DTNConfig.load(DTNfis);
						DTNfis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					String connectionId = Config.getProperty("DTNId");
					String DTNconnectionId = DTNConfig.getProperty("DTNId");

					//create a stack for end system 
					NewStack stack = new NewStack(DTNconnectionId,usbStateManager,DTNStore,null);

					//create Queue to communicate
					//readQueue reads from USb and writeQueue writes to USB
					BlockingQueue<Packet> readQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					BlockingQueue<Packet> writeQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					//create a USb connection
					USBConnection usbCon = new USBConnection(readQueue,writeQueue);
					//create a USB Connection
					USBConnection usbRouterCon = new USBConnection(writeQueue,readQueue);
					scheduler.addConnection(DTNconnectionId, usbCon);
					stack.addConnection(connectionId, usbRouterCon);
					DTNlink = true;
				}
				else if(DTNlink && !DTNStatusFile.exists())
				{
					DTNlink = false;
				}
			}
			
			try{
				InetAddress host = InetAddress.getLocalHost();
				/*******************Changes made  in 09oct ***************************/
/*				InetAddress[] IPs = InetAddress.getAllByName(host.getHostName());
				for(int j = 0 ; j < IPs.length ; j++){
					String ip = IPs[j].getHostAddress();
					if(!localIPs.contains(ip))
					{
						System.out.println("IP:" + ip);
						InetAddress localIP = IPs[j];
						for(int i = 0;i < destinationConnectionIds.size();i++){
							dataConnectionId++;
							if(!connectionPorts.isEmpty())
							{
								String destination = destinationConnectionIds.get(i);
								String[] connectionInfo = destination.split(":");
								InetAddress IP = InetAddress.getByName(connectionInfo[0]);
								int port = Integer.parseInt(connectionInfo[1]);
								Connection con = new TCPConnection(dataConnectionId,IP,port,localIP,connectionPorts.get(0));
								connectionPorts.remove(0);
								System.out.println("New Connection created in run method");
								Packet packet = new Packet(connectionId);
								con.writePacket(packet);
								System.out.println("NEW Connection Established in link detector in run method");
								scheduler.addConnection(connectionInfo[0],con);
							}
							else
								System.out.println("local port unavailable");
						}

						localIPs.add(ip);

					}
					
				}
			}
			catch (Exception ioe){ System.out.println(ioe);
			ioe.printStackTrace();
			}
		}

	}*/

	public static void setDestinationIds(List<String> destinationIds)
	{
		destinationConnectionIds = destinationIds ;
		System.out.println("destinationConnectionIds in Link Detector: " + destinationConnectionIds);
		
	}
	
	public static List<String> getDestinationIds()
	{
		return destinationConnectionIds ;
		
	}

	
}