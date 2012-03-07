package prototype.userregistrar;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import StateManagement.Status;
import AbstractAppConfig.AppConfig;
import prototype.utils.AuthenticationFailedException;
import prototype.utils.NotRegisteredException;

public class CustodianLogger implements ICustodianLogin
{	
	Map<String,String> users;
	Map<String,List<String>> userCustodianLookup;
	Map<String,String> activeCustodianLookup;

	public CustodianLogger(Map<String,String> userPasswords,Map<String,List<String>> usercustodianlookup,Map<String,String> activecustodianlookup)
	{

		users = userPasswords;
		userCustodianLookup = usercustodianlookup;
		activeCustodianLookup = activecustodianlookup;
	}

	public String get_active_custodian(String userId) throws RemoteException
	{
		if(activeCustodianLookup.size()==0)
		{
			Status st = Status.getStatus();
			activeCustodianLookup = st.getActiveCustodian();
		}
		return activeCustodianLookup.get(userId);
	}

	/*public IUserRegistrarSession authenticate_user(String userId,String password,String custodianId) throws RemoteException,AuthenticationFailedException,NotRegisteredException
	{
		System.out.println("Inside CustodianLogger.java: authenticate_user");
		System.out.println("CustodianId: " + custodianId);
		Status st = Status.getStatus();
		String passwd = st.execQueryString("select password from userregister where userid = '"+userId+"'" ,2);
		
		if(passwd == null)
		{
			System.out.println("New Registration");
			st.insertData("userregister", userId, password );
			st.insertData("usercustodian", userId, custodianId);
			st.insertData("activecustodian", userId, custodianId);
			List<String> custodianList = new ArrayList<String>();
			custodianList.add(custodianId);
			userCustodianLookup.put(userId,custodianList);
			activeCustodianLookup.put(userId, custodianId);
			IUserRegistrarSession Session = new RegistrarSession(userId,custodianId,userCustodianLookup,activeCustodianLookup);
			IUserRegistrarSession stub = (IUserRegistrarSession) UnicastRemoteObject.exportObject(Session, 0);
			return stub;
		}
		else
		{
			if(passwd.equals(password))
			{
				boolean flag = Boolean.parseBoolean(st.execQueryString("select * from userCustodian where userId ='"+userId+"' and custodianId = '"+custodianId+"'", 3));
				if(flag)
				{	
					if(!activeCustodianLookup.containsKey(userId))
					{
						System.out.println("Value of flag: "+flag);
						activeCustodianLookup.put(userId, custodianId);
						st.updateActiveCustodian("activecustodian", userId, custodianId);
					}	
				}	
				else
				{
					activeCustodianLookup.put(userId, custodianId);
					st.insertData("usercustodian", userId, custodianId);
					st.updateActiveCustodian("activecustodian", userId, custodianId);
				}	
				IUserRegistrarSession Session = new RegistrarSession(userId,custodianId,userCustodianLookup,activeCustodianLookup);
				IUserRegistrarSession stub = (IUserRegistrarSession) UnicastRemoteObject.exportObject(Session, 0);
				return stub ;
				
			}
			else
			  throw new AuthenticationFailedException() ;
		}
		
	}*/

	public IUserRegistrarSession authenticate_user(String userId,String password,String custodianId) throws RemoteException,AuthenticationFailedException,NotRegisteredException
	{
		System.out.println("Inside CustodianLogger.java: authenticate_user");
		System.out.println("CustodianId: " + custodianId);
		Status st = Status.getStatus();
		//String passwd = st.execQueryString("select password from userregister where userid = '"+userId+"'" ,2);
		String passwd = st.execQueryString("select upwd from usregs where ulogname = '"+userId+"'" ,2);
		
		if(passwd == null)
		{
			System.out.println("User does not exist");
			throw new AuthenticationFailedException() ;
		}
		else
		{
			if(passwd.equals(password))
			{
				boolean flag = Boolean.parseBoolean(st.execQueryString("select * from userCustodian where userId ='"+userId+"' and custodianId = '"+custodianId+"'", 3));
				if(flag)
				{	
					if(!activeCustodianLookup.containsKey(userId))
					{
						System.out.println("Value of flag: "+flag);
						activeCustodianLookup.put(userId, custodianId);
						st.updateActiveCustodian("activecustodian", userId, custodianId);
					}	
				}	
				else
				{
					activeCustodianLookup.put(userId, custodianId);
					st.insertData1("usercustodian", userId, custodianId);
					st.updateActiveCustodian("activecustodian", userId, custodianId);
				}	
				IUserRegistrarSession Session = new RegistrarSession(userId,custodianId,userCustodianLookup,activeCustodianLookup);
				IUserRegistrarSession stub = (IUserRegistrarSession) UnicastRemoteObject.exportObject(Session, 0);
				return stub ;
				
			}
			else{
				System.out.println("Password is not correct");
				throw new AuthenticationFailedException() ;
			  
			}
		}
		
	}
	
	public boolean new_registration(Map<String,String> userInfo) throws RemoteException{
		boolean flag = false;
		Status st = Status.getStatus();
		flag = st.registration(userInfo);
		return flag ;
	}
	
	
	public static void main(String args[]) 
	{

		try
		{
			File configFile = new File("config/UserRegistrar.cfg");
			FileInputStream fis;
			fis = new FileInputStream(configFile);
			new AppConfig(); 
			AppConfig.load(fis);
			fis.close();

			Map<String,String> users = new HashMap<String,String>();
			Map<String,List<String>> userCustodianLookup = new HashMap<String,List<String>>();
			Map<String,String> activeCustodianLookup = new HashMap<String,String>();

			CustodianLogger obj = new CustodianLogger(users,userCustodianLookup,activeCustodianLookup);
			
			ICustodianLogin stub = (ICustodianLogin) UnicastRemoteObject.exportObject(obj, 0);

			Registry registry = LocateRegistry.getRegistry();
			System.out.println("Service name: "+AppConfig.getProperty("UserRegistrar.Service"));
			
			boolean found = false;
			while(!found)
			{
				try
				{
					registry.bind(AppConfig.getProperty("UserRegistrar.Service") , stub);
					found = true;
				}
				catch(AlreadyBoundException ex)
				{
					registry.unbind(AppConfig.getProperty("UserRegistrar.Service"));
					registry.bind(AppConfig.getProperty("UserRegistrar.Service") , stub);
					found = true;
				}
				catch(ConnectException ex)
				{
					String rmiPath = AppConfig.getProperty("UserRegistrar.Directory.rmiregistry");
					Runtime.getRuntime().exec(rmiPath);
					//Runtime.getRuntime().exec("C:\\Program Files\\Java\\jdk1.6.0_16\\bin\\rmiregistry.exe");
				}
			}
			System.err.println("User Registrar Server ready");
			
		}
		catch (Exception e) 
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

	}
}





