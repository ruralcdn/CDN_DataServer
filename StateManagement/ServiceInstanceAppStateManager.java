package StateManagement;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class ServiceInstanceAppStateManager
{

	File status;
    FileInputStream fis ;
	public static final String uploadRequesterSuffix = new String(".UploadRequester");
	private static Map<String, String> requesterMap; 
	
	//public ServiceInstanceAppStateManager(File state)
	public ServiceInstanceAppStateManager()
	{
		//status = state;
		requesterMap = new HashMap<String, String>();
	}

	public synchronized String getUploadRequester(String contentId)
	{
		Status st = Status.getStatus();
		String uploaderName = null ;
		if(!requesterMap.containsKey(contentId))
			uploaderName = st.execQueryString("select requester from status where contentid = '"+contentId+"' and type = '0'",1);
		if(uploaderName == null)
		    return requesterMap.get(contentId);
		else
		{
			requesterMap.put(contentId, uploaderName);
			return uploaderName;
		}
		
	}
	

	public synchronized void setRequesterDetail(String uploadId,String requesterId, String fileType)
	{
		Status st = Status.getStatus();
		System.out.println("Calling status from setRequester");
		st.updateState("status",requesterId, uploadId, fileType);			
		requesterMap.put(uploadId,requesterId);
	}
	

}