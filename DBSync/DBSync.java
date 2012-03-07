package DBSync;

public class DBSync {
	
	SyncServer syncSer ;
	LocalUpdate localUpdate ;
	static boolean sending ;
	public DBSync(int port)
	{
		localUpdate = new LocalUpdate();
		syncSer = new SyncServer(6789);
		syncSer.start();
		localUpdate.start();
	}
}
