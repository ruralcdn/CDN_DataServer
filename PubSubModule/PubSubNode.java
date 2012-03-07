package PubSubModule;

import java.rmi.RemoteException;
import java.util.List;

import StateManagement.CustodianAppStateManager;


public class PubSubNode implements IPubSubNode{
	
	CustodianAppStateManager appStateManager;
	List<PubSubNode> nodes;
	
	public PubSubNode(CustodianAppStateManager manager,List<PubSubNode> nodeList)
	{	
		appStateManager = manager;
		nodes = nodeList;
	}
	
	public void notify(Notification notification) throws RemoteException
	{
		if(nodes == null && appStateManager != null)
		{
			if(notification.getNotificationType() == Notification.Type.UploadAck)
			{
				try
				{
					String[] notificationContent = notification.getContent().split(":");
					List<String> uploadList = appStateManager.getUploadNotification(notificationContent[0]);
					uploadList.add(notificationContent[1]);
					appStateManager.setUploadNotification(notificationContent[0], uploadList);

					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
			
	}
	
}