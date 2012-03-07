package prototype.custodian;

import java.rmi.Remote;
import java.rmi.RemoteException;
import prototype.utils.AlreadyRegisteredException;
import prototype.utils.AuthenticationFailedException;
import prototype.utils.NotRegisteredException;

public interface ICustodian extends Remote {
	
	public ICustodianSession authenticate(String userId,String password) throws RemoteException,NotRegisteredException,AuthenticationFailedException;
	public boolean register(String userId,String password) throws RemoteException,AlreadyRegisteredException;
	public boolean register_custodian(String userId) throws RemoteException,NotRegisteredException;
	
	/*
	
	public boolean request_connection(String user) throws RemoteException/*,UserAlreadyConnectedException,UserNotRegisteredException;
	public boolean register(String userId) throws RemoteException;
	public boolean unregister(String userId) throws RemoteException;
	public boolean subscribe(String subject) throws RemoteException;
	public boolean unsubscribe(String subject) throws RemoteException;
	public boolean close_connection() throws RemoteException;
	public Map<String,byte[]> request_data() throws RemoteException;
	//public Map<String,byte[]> request_data() throws RemoteException;
	public boolean find(String dataname,String user) throws RemoteException;
	*/

}
