package prototype.serviceinstance;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IUploader extends Remote
{

	//public String upload(String data,int size,String requester) throws RemoteException;
	//public int processDynamicContent(String objectId,int uploadSize,String contentId,Connection.Type type,String requester,String conId) throws RemoteException;
	public String generateContentId() throws RemoteException ; // Added by Quamar on 24-09-2010
}