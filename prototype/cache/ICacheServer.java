package prototype.cache;

import java.rmi.Remote;
import java.rmi.RemoteException;

import newNetwork.Connection;

@SuppressWarnings("unused")
public interface ICacheServer extends Remote{
	
	public void notify(String data) throws RemoteException; 
	public String upload(String myContentName,int segments,String serviceInstance, String username) throws RemoteException ;
}