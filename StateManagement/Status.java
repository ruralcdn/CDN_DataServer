package StateManagement ;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.rowset.CachedRowSetImpl;
public class Status{
	String[] st = new String[8];
	Connection con ;
    Statement stat ;
    PreparedStatement prep;
    private static Status status;
	
    private  Status(){
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
		}catch(ClassNotFoundException e){
			e.printStackTrace();
			
		}
						
		try {
			con = DriverManager.getConnection
				("jdbc:mysql://localhost:3306/ruralcdn","root","abc123");
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }	
    public static synchronized Status getStatus(){
    	if(status == null){
    		status = new Status();
    	}
    	return status;
    }
	public void insertData(String table, String contentId, int type, int totseg, int curseg, int off, int prefint, String prefrt, String appid, int sendmetadata, String prefrtport ){
		
		System.out.println("tablename:"+table+"contentid:"+contentId);
		try{
			System.out.println("Inside st.insertdata()");
			prep = con.prepareStatement("insert into "+table+" values(?,?,?,?,?,?,?,?,?,?,?,?)");				
			prep.setString(1,contentId);
			prep.setInt(2,type);
			prep.setInt(3,totseg);
			prep.setInt(4,curseg);
			prep.setInt(5,off);
			prep.setInt(6,prefint);
			prep.setString(7,prefrt);
			prep.setString(8,appid);
			prep.setInt(9,sendmetadata);
			prep.setString(10,prefrtport);
			prep.setString(11,"");
			prep.setString(12,"");
			prep.execute();			
			prep.close();
		}catch(Exception e){
			System.out.println("tablename:"+table+"contentid:"+contentId);
			e.printStackTrace();
		}
	}
	
	public void insertData(String table, String userId, String passOrCustodian)
	{
		
		try{
			System.out.println("Inside st.insertdata()");
			prep = con.prepareStatement("insert into "+table+" values(?,?)");
			System.out.println("table2:"+table);
			prep.setString(1,userId);
			prep.setString(2,passOrCustodian);
			prep.execute();
			prep.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized void  insertKeyValue(String table, String keyAttr, String dataName)
	{
		int temp = 0 ;
		try{
			System.out.println("Inside st.insertKeyValue()");
			stat = con.createStatement();
			stat.executeQuery("select * from "+table);
			System.out.println("table3:"+table);
			ResultSet rs = stat.getResultSet();
			while(rs.next())
				temp = rs.getInt(1) ;
			prep = con.prepareStatement("insert into "+table+" values(?,?,?)");
			System.out.println("table:"+table);
			prep.setInt(1,++temp);
			prep.setString(2, keyAttr);
			prep.setString(3,dataName);
			prep.execute();
			rs.close();
			stat.close();
			prep.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
		
	public void deRegisterData(String table, String dataname){
		try{
			stat = con.createStatement();
			stat.execute("delete from datalocations where data ='"+dataname+"'");
			stat.close();
		}catch(Exception e){
			
		}
	}
	
	public void insertData1(String table, String userId, String passOrCustodian)
	{
		
		try{
			System.out.println("Inside st.insertdata1()");
			prep = con.prepareStatement("insert into "+table+"(userId,custodianId) values(?,?)");
			prep.setString(1,userId);
			prep.setString(2,passOrCustodian);
			prep.execute();			
			
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	public boolean registration(Map<String, String> userInfo){
		boolean userReg = false;
		String ulogname = userInfo.get("ulogname");
		String upwd = userInfo.get("upwd");
		String ufullname = userInfo.get("ufullname");
		String uemail = userInfo.get("uemail");
		String ucontct = userInfo.get("ucontct"); 
		String uaddrs = userInfo.get("uaddrs"); 
		String ucity = userInfo.get("ucity");
		String ustate = userInfo.get("ustate");
		try {
			Statement stmt = con.createStatement();
			stmt.execute("select * from usregs where ulogname = '"+ulogname+"'");
			ResultSet rs = stmt.getResultSet();
			if(rs.next()){
				return userReg ;
			}
			else{
				PreparedStatement stmt1 = con.prepareStatement("insert into usregs(ulogname,upwd,ufullname,uemail,ucontct,uaddrs,ucity,ustate) values(?,?,?,?,?,?,?,?)");
				stmt1.setString(1,ulogname);
				stmt1.setString(2,upwd);
				stmt1.setString(3,ufullname);
				stmt1.setString(4,uemail);
				stmt1.setString(5,ucontct);
				stmt1.setString(6,uaddrs);
				stmt1.setString(7,ucity);
				stmt1.setString(8,ustate);
				stmt1.execute();
				stmt1.close();
				userReg = true ;
			}
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		return userReg ;	
	}
	public boolean execQuery(String s,String contentId, int type){
		boolean flag = false;
		ResultSet resset = null ;
		try{
			stat = con.createStatement();
			stat.execute(s);
			resset = stat.getResultSet();
			while(resset.next()){
				String str = resset.getString("contentid");
				int t = resset.getInt("type");
				if(str.equals(contentId) && t== type)
					{
						flag = true ;
						break ;
					}	
				}
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return flag ;
	}
	public String[] execQuery(String s){
		ResultSet resset = null ;
		try{
			stat = con.createStatement();
			stat.execute(s);
			resset = stat.getResultSet();
			st[0] = "";
			st[2] = "";
			st[3] = "";
			st[4] = "";
			st[5] = "";
			st[6] = "";
			st[7] = "";
			while(resset.next()){
				st[0] = Integer.toString(resset.getInt("curseg")) ;
				st[1] = Integer.toString(resset.getInt("off") );
				st[2] = Integer.toString(resset.getInt("prefint") );
				st[3] = Integer.toString(resset.getInt("totseg")) ;
				st[4] = resset.getString("appid");
				st[5] = Integer.toString(resset.getInt("sendmetadata"));
				st[6] = resset.getString("prefrt");
				st[7] = resset.getString("prefrtport");
				 
			}	
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return st ;
	}	
	
	public String execQueryString(String s, int ch){
		ResultSet resset = null ;
		String requester = null ;
		try
		{
			stat = con.createStatement();
			stat.execute(s);
			resset = stat.getResultSet();
			switch(ch)
			{
				case 1:
					while(resset.next())
					{
						requester = resset.getString("requester");
				 	}	
					resset.close();
					return requester ;
				case 2: 
					if(resset.next())
						//return resset.getString("password");
						return resset.getString("upwd");
					else
						return null ;
				case 3:
					if(resset.next())
						return Boolean.toString(true);
					else
						return Boolean.toString(false);
				
				default:
					break;
			}
			resset.close();
			stat.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return requester ;
	}	
	
	public void updateState(String table, String data, int type, int curSeg){
		
		try
		{
			
			prep= con.prepareStatement("update "+table+" set curseg = ? where contentid = ? " +
					"and type = ?");
			prep.setInt(1,curSeg);
			prep.setString(2,data);
			prep.setInt(3,type);
			prep.execute();
			prep.close();
			

		}
		catch(Exception e){
			e.printStackTrace();
		}

	}
	
	public void updateState(String table, String data, int type){
		
		try
		{
			
			//System.out.println("ContentId and type is: "+data+" "+type);
			//stat = con.createStatement();
			//stat.execute("select * from "+table+" where contentId = '"+data+"' and type='"+type+"'");
			//ResultSet resset = stat.getResultSet();
			//System.out.println("row is "+resset);
			//stat.close();
			prep= con.prepareStatement("delete from "+table+" where contentid = ? " +
					"and type = ?");
			prep.setString(1,data);
			prep.setInt(2,type);
			prep.execute();
			prep.close();
			

		}
		catch(Exception e){
			e.printStackTrace();
		}

	}
	public void updateState(String table,String requester, String data, String fileType)
	{
		System.out.println("table:"+table);
		try{
			
			prep= con.prepareStatement("update "+table+" set requester = ?, fileType = ? where contentid = ? and type = 0");
			prep.setString(1,requester);
			prep.setString(2, fileType);
			prep.setString(3,data);
			prep.execute();
			prep.close();
			

		}catch(Exception e){
			e.printStackTrace();
			System.out.println("table:"+table+"requester:"+requester+"data"+data+"filetype"+fileType);
		}

	}
	public String getContentType(String filename){
		String str = "";
		try{
			Statement stmt = con.createStatement();
			stmt.execute("select fileType from status where contentid ='"+filename+"' and type = 0");
			ResultSet resset = stmt.getResultSet();
			if(resset.next())
				str = resset.getString(1);
			resset.close();
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return str;
	}
	public void updateActiveCustodian(String table,String user, String activeCustodian)
	{
		
		try{
			
			prep= con.prepareStatement("update "+table+" set custodianId = ? where userId = ?");
			prep.setString(1,activeCustodian);
			prep.setString(2,user);
			prep.execute();
			prep.close();
			

		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	public synchronized List<String>  setDownloadRequests(String table, String contentId, int choice){
		List<String> downloadRequest = new ArrayList<String>();
		switch(choice)
		{
			case 0:
				try {
					//System.out.println("Calling status'setDownloadRequest with choice 0");
					prep = con.prepareStatement("insert into "+table+" values(?)");
					prep.setString(1,contentId);
					prep.execute();
					prep.close();
					stat = con.createStatement();
					stat.execute("select * from "+table);
					ResultSet resset=stat.getResultSet();
					while(resset.next()){
						downloadRequest.add(resset.getString("contentId"));
						System.out.println("Adding request to contentId");
					}
					resset.close();
					stat.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return downloadRequest;
				//break ;
			case 1: 
				try{
					//System.out.println("Calling status'setDownloadRequest with choice 1");
					prep = con.prepareStatement("delete from "+table+" where contentId = ?");
					prep.setString(1, contentId);
					prep.execute();
					prep.close();
					stat = con.createStatement();
					stat.execute("select * from "+table);
					ResultSet resset=stat.getResultSet();
					while(resset.next()){
						downloadRequest.add(resset.getString("contentId"));
					}
					resset.close();
					stat.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				return downloadRequest;
			//break ;
		}
		return downloadRequest;
		
	}
	
	public List<String> getDownloadRequests(String table){
		List<String> downloadRequestList = new ArrayList<String>();
		try{
			stat = con.createStatement();
			stat.execute("select * from "+table);
			ResultSet resset = stat.getResultSet();
			while(resset.next()){
				downloadRequestList.add(resset.getString("contentid"));
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return downloadRequestList ;
	}
	 
	public CachedRowSetImpl getKeyNum(String table)
	{
		CachedRowSetImpl cs = null ;
		ResultSet rs ;
		try{
			
			stat = con.createStatement();
			stat.execute("select * from "+table+" where keyField like 'c%'");
			rs  = stat.getResultSet();
			int temp1=0;
			while(rs.next())
				temp1 = rs.getInt(1);
			stat.execute("select * from "+table);
			rs  = stat.getResultSet();
			int temp2=0;
			while(rs.next())
				temp2 = rs.getInt(1);
			if(temp2!=temp1)
			{	
				stat.execute("select * from "+table+" where serial_no > "+temp1);
				rs = stat.getResultSet();
				cs = new CachedRowSetImpl();
				cs.populate(rs);
				Socket clientSocket = new Socket("mycustodian",6789);
				ObjectOutputStream outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
				outToServer.writeObject(cs);
			}
			/*This below query should execute after sending the rows but right 
			 * now for simplicity we put it here*/
			stat.execute("update "+table+" set serial_no = "+temp2+" where keyField like 'c%'");
		}catch(Exception e){
			e.printStackTrace();
		}
		return cs ;
	}
	public List<String> findDataLocations(String dataname){
		List<String> locations = new ArrayList<String>();
		String query = "";
		try{
			System.out.println("In findDataLocations");
			if(dataname.contains("serviceInstance")){
				String serviceInstance = dataname.substring(0,dataname.indexOf("$"));
				System.out.println("Looking for serviceInstance "+serviceInstance);
				query = "select locations from servicelocations where serviceInstance = '"+serviceInstance+"'";
			}	
			else{
				query = "select source from datalocations where data = '"+dataname+"'" ;
			}	
			stat =  con.createStatement();
			stat.execute(query);
			ResultSet resset = stat.getResultSet();
			while(resset.next()){
				locations.add(resset.getString(1));
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			
		}
		return locations ;
		
	}
	
	public Map<String,ContentState> getPendingStatus(String query)
	{
		Map<String, ContentState> map = new HashMap<String, ContentState>();
		try
		{
			stat =  con.createStatement();
			stat.execute(query);
			ResultSet resset = stat.getResultSet();
			while(resset.next())
			{
				String contentId = resset.getString("contentId");
				int off = resset.getInt("off");
				int size = resset.getInt("totseg");
				BitSet bitSet = new BitSet(size);
				int prefInt = resset.getInt("prefint");
				String appId = resset.getString("appid");
				boolean meta = resset.getBoolean("sendmetadata");
				ContentState contentState = new ContentState(contentId,off,bitSet,prefInt,null,size,0,ContentState.Type.tcpDownload,appId,meta);
				map.put(contentId,contentState);
			}
			resset.close();
			stat.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return map ;
	}
	
	public Map<String,String> getActiveCustodian()
	{
		Map<String,String> activeCustodian = new HashMap<String,String>();
		try
		{
			stat = con.createStatement();
			stat.executeQuery("select * from activecustodian");
			ResultSet resset = stat.getResultSet();
			while(resset.next())
			{
				String user = resset.getString("userId");
				String custodian = resset.getString("custodianId");
				activeCustodian.put(user,custodian);
			}
			resset.close();
			stat.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return activeCustodian ;
	}
	
}
	