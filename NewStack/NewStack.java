package NewStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import newNetwork.Connection;
import prototype.datastore.DataStore;
import AbstractAppConfig.AppConfig;
import StateManagement.StateManager;

public class NewStack
{
	private String stackId;
	private StateManager stateManager;
	private DataStore store;
	private LinkDetector detector;
	private int segmentSize = Integer.parseInt(AppConfig.getProperty("NetworkStack.SegmentSize"));
	private Scheduler scheduler;
	private PolicyModule policyModule;
	private TCPServer server;
	private SegmentationReassembly sar;
	private static DataUploader uploader;
	private DataDownloader downloader;
	private static RMIServer controlServer;

	Reassembler reassembler;

	public NewStack(String localId,StateManager manager,DataStore dStore,DataStore usbStore,BlockingQueue<String> downloads,int serverPort,List<Integer> connectionPorts)
	{
		stackId = localId;
		stateManager = manager;
		store = dStore;
		policyModule = new PolicyModule();
		scheduler = new Scheduler(policyModule,stateManager,segmentSize);
		sar = new SegmentationReassembly(stateManager,store,scheduler,segmentSize,downloads);
		detector = new LinkDetector(stackId,scheduler,connectionPorts,dStore,usbStore);
		if(serverPort != -1)
		{
			server = new TCPServer(scheduler,serverPort);
			new Thread(server).start();
			controlServer = new RMIServer(stateManager,store,sar,policyModule,scheduler.getDataEmptyQueues(),detector);
		}
		//detector = new LinkDetector(stackId,scheduler,connectionPorts,dStore,usbStore);
		//detector.start();
		if(stateManager.getTCPDownloadRequests().size()>=1)
			controlServer.start();
		uploader = new DataUploader(sar.getSegmenter(),scheduler.getConnectionPool(),detector,stateManager,scheduler.getDataEmptyQueues(),policyModule);
		
	}

	public NewStack(String localId,StateManager manager,DataStore dStore,BlockingQueue<String> downloads)
	{
		stackId = localId;
		stateManager = manager;
		store = dStore;
		policyModule = new PolicyModule();
		scheduler = new Scheduler(policyModule,stateManager,segmentSize);
		sar = new SegmentationReassembly(stateManager,store,scheduler,segmentSize,downloads);
		uploader = new DataUploader(sar.getSegmenter(),scheduler.getConnectionPool(),null,stateManager,scheduler.getDataEmptyQueues(),policyModule);
		uploader.start();
	}

	public void addDestination(String destination)
	{
		try {
			detector.addDestination(destination);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int countSegments(String contentname)
	{
		return (int)sar.countSegments(contentname);
	}

	public void close()
	{
		detector.close();
		scheduler.close();
		server.close();
		sar.close();
		uploader.close();
		downloader.close();
	}

	public void addConnection(String remoteId,Connection con)
	{
		scheduler.addConnection(remoteId, con);
	}
	
	public List<String> getRoute(String destination)
	{
		return RouteFinder.findDTNRoute(destination);
	}
	
	public void setPolicy(String Id,Connection.Type type)
	{
		policyModule.setPolicy(Id, type);
	}
	public String getStackId()
	{
		return stackId;
	}
	public int getServerPort()
	{
		return server.getServerPort(); 
	}
	
	public static RMIServer getRMIServer()
	{
		return controlServer;
	}
	public String getDTNId()
	{
		File configFile = store.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
		Properties Config = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(configFile);
			Config.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return Config.getProperty("DTNId");
	}
	
	public static DataUploader getDataUploader()
	{
		return uploader;
	}
}