package prototype.custodian;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import AbstractAppConfig.AppConfig;
import NewStack.NewStack;
import PubSubModule.IPubSubNode;
import PubSubModule.PubSubNode;
import StateManagement.CustodianAppStateManager;
import StateManagement.StateManager;
import prototype.datastore.DataStore;
import prototype.userregistrar.IUserRegistrarSession;
import prototype.userregistrar.ICustodianLogin;
import prototype.utils.AlreadyRegisteredException;
import prototype.utils.AuthenticationFailedException;
import prototype.utils.NotRegisteredException;


public class Logger implements ICustodian{

	IUserRegistrarSession sessionStub;  						//local
	//String Id;                 									//global
	ICustodianLogin stub;                    						//global
	//BlockingQueue<Map<String,List<String>>> dataRequests;  	//global
	DataStore store;											//global
	NewStack networkStack;									//global
	StateManager stateManager;							     //global
	CustodianAppStateManager appStateManager;


	public Logger(ICustodianLogin loginStub,StateManager sManager,CustodianAppStateManager caManager/*BlockingQueue<Map<String,List<String>>> datarequests*/,
			DataStore st,NewStack stack)
	{
		stub = loginStub;
		//config = configuration;
		stateManager = sManager;
		appStateManager = caManager;
		//dataRequests = datarequests;  //global
		store = st;
		networkStack = stack;

	}
	public ICustodianSession authenticate(String username,String password) throws RemoteException,NotRegisteredException,AuthenticationFailedException
	{
		System.out.println("Inside Logger.java:authenticate");
		synchronized(stub)
		{
			sessionStub = stub.authenticate_user(username, password,networkStack.getStackId()+":"+networkStack.getServerPort()+":"+networkStack.getDTNId());
		}
		ICustodianSession session;
		if(sessionStub != null)
		{
			session = new CustodianSession(username,sessionStub,stateManager,appStateManager,store,networkStack);
			ICustodianSession custodianSessionStub = (ICustodianSession) UnicastRemoteObject.exportObject(session, 0);
			System.out.println("User authenticated,beginning session");
			return custodianSessionStub;
		}
		else
			return null;
	}
	public boolean register(String userId,String password) throws RemoteException,AlreadyRegisteredException
	{
		System.out.println("Inside Logger.java:register");
		boolean flag = false;
		synchronized(stub)
		{
			//flag = stub.register_user(userId, password);
			System.out.println("UserRegistered: "+flag);
		}
		return flag;
	}

	public boolean register_custodian(String userId) throws RemoteException,NotRegisteredException
	{
		boolean flag = false;
		synchronized(stub)
		{
			//flag = stub.register_user_custodian(userId,networkStack.getStackId()+":"+networkStack.getServerPort()+":"+networkStack.getDTNId());
			System.out.println("User registered withcustodian: "+flag);
		}
		return flag;
	}

	public static void main(String args[]) {

		try {
			File configFile = new File("config/Custodian.cfg");
			FileInputStream fis;
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);
			fis.close();

			DataStore store = new DataStore(AppConfig.getProperty("Custodian.Directory.Path"));
			File statusFile = store.getFile(AppConfig.getProperty("Custodian.StatusFile"));
			//StateManager stateManager = new StateManager(statusFile,null);
			StateManager stateManager = new StateManager(null);
			CustodianAppStateManager appStateManager = new CustodianAppStateManager(statusFile);

			//pubsub Module
			PubSubNode node = new PubSubNode(appStateManager,null);
			IPubSubNode pubsubStub = (IPubSubNode) UnicastRemoteObject.exportObject(node, 0);
			Registry pubsubRegistry = LocateRegistry.getRegistry();
			pubsubRegistry.bind(AppConfig.getProperty("Custodian.PubSub.Service") , pubsubStub);
			
		

			int port = Integer.parseInt(AppConfig.getProperty("Custodian.Port"));
			
			String custodianId = AppConfig.getProperty("Custodian.Id");
			
			DataStore usbStore = null;
			if(AppConfig.getProperty("Routing.allowDTN").equals("1"))
			{
				usbStore = new DataStore(AppConfig.getProperty("Custodian.USBPath"));
			}
			
			int clientPort = Integer.parseInt(AppConfig.getProperty("Custodian.NetworkStack.Port"));
			int tempPort = clientPort;
			List<Integer> cacheConnectionPorts = new ArrayList<Integer>(Integer.parseInt(AppConfig.getProperty("Custodian.ConnectionPool.Size")));     //config driven
			for(int i = 0; i < 20;i++)
			{
				cacheConnectionPorts.add(tempPort);
				tempPort++;
			}
			
			NewStack stack = new NewStack(custodianId,stateManager,store,usbStore,null,port,cacheConnectionPorts);

			String RegistrarServer = AppConfig.getProperty("Custodian.UserRegistrar.IP");    //config driven
			Registry registrarRegistry = LocateRegistry.getRegistry(RegistrarServer);
			String userRegistrarService = AppConfig.getProperty("Custodian.UserRegistrar.Service");
			ICustodianLogin registrarStub = (ICustodianLogin) registrarRegistry.lookup(userRegistrarService);
			

			Logger obj = new Logger(registrarStub,stateManager,appStateManager,store,stack);
			ICustodian custodianStub = (ICustodian) UnicastRemoteObject.exportObject(obj, 0);

			
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(AppConfig.getProperty("Custodian.Service") , custodianStub);


			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
	}


}