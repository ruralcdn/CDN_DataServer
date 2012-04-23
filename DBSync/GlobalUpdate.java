/*package DBSync;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.BlockingQueue;
import java.io.*;

public class GlobalUpdate extends Thread {
	BlockingQueue<Socket> clsocks ;
	Socket sock;
	Connection con ;
	Statement stmt ;
	ResultSet rs ;
		
	public GlobalUpdate(BlockingQueue<Socket> clientSocks){
		try{
 			Class.forName("com.mysql.jdbc.Driver");
 			con = DriverManager.getConnection
				("jdbc:mysql://localhost:3306/syncdb","root","abc123");
 			clsocks = clientSocks ;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
		
	public void run()
	{
		while(true){
			try {
				sock =clsocks.take();
				String client = sock.getInetAddress().getHostName();
				System.out.println("Client Host Name is: "+client);
				ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
				ObjectOutputStream ous = new ObjectOutputStream(sock.getOutputStream());
				if(client.equals("localhost"))
				{
					String query = (String) ois.readObject();
					stmt = con.createStatement();
	 				stmt.execute(query);
	 				File outFile = new File("cache_db.log");
	 				BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true));
	 				writer.write(query);
	 				writer.newLine();
	 				writer.close();
	 				
	 			}
				else
				{
					System.out.println("Logs received in syncServer from: "+client);
					File logFile ;
					logFile = logFileRead(ois);
					stmt.execute("select updated_till from synctable where entity ='"+client+"'");
					rs = stmt.getResultSet();
					int cp = 0 ;
					if(rs.next())
						cp = rs.getInt(1);
					rs.close();
					logFileSent(ous, cp);
					ois.close();
					ous.close();
					System.out.println("Logs sent from syncServer to: "+client);
					executeLogStatement(logFile);
					updateLogFile(logFile,"cache_db.log", client);
					if(logFile.exists())
						logFile.delete();
				}	
				stmt.execute("select * from synctable where entity !='"+client+"'");
				rs = stmt.getResultSet();
				while(rs.next())
				{
					int current = rs.getInt(2);
 					String serverName = rs.getString(1);
 					System.out.println("In SyncServer entity: "+serverName+" with updated_till value: "+current);
 					InetAddress server = InetAddress.getByName(serverName);
 	 				InetAddress host = InetAddress.getByName("myrootserver");
 	 				Socket clientSocket = new Socket(server, 6789,host,0);
 	 				OutputStream ous1 = clientSocket.getOutputStream();
 	 				InputStream  ois1 = clientSocket.getInputStream();
 	 				logFileSent(ous1,current);
 	 				File logFile ;
					logFile = logFileRead(ois1);
					executeLogStatement(logFile);
					updateLogFile(logFile,"cache_db.log", client);
					if(logFile.exists())
						logFile.delete();
					ois1.close();
					ous1.close();
					
				}
				
				 
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public File logFileRead(InputStream ois) throws Exception{
		File logFile = new File("cache_down.log");
		int bytesRead ;
		int current = 0 ;
		byte[] data = new byte[10240];
		FileOutputStream fos = new FileOutputStream(logFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		bytesRead = ois.read(data,0,data.length);
		current = bytesRead ;
		do{
			bytesRead = ois.read(data,current, data.length-current);
			if(bytesRead>-1)
				current += bytesRead ;
		}while(bytesRead>-1);
		bos.write(data,0,current);
		bos.close();
		return logFile ;
	}
	
	public void logFileSent(OutputStream ous, int cp)throws Exception{
		File toBeRead = new File("cache_db.log");
		File toBeSent = new File("cache_up.log");
		BufferedReader reader = new BufferedReader(new FileReader(toBeRead)); 
		BufferedWriter writer = new BufferedWriter(new FileWriter(toBeSent));
		String str;
		int count = 0 ;
		while((str = reader.readLine())!=null && str.length()!=0){
			if(++count<=cp)
				continue ;
			else{
				writer.write(str);
				writer.newLine();
			}
		}
		reader.close();
		writer.close();
		byte[] data = new byte[(int) toBeSent.length()];
		FileInputStream fis = new FileInputStream(toBeSent);
		BufferedInputStream bis = new BufferedInputStream(fis);
		bis.read(data,0,data.length);
		ous.write(data,0,data.length);
		ous.flush();
		if(toBeSent.exists())
			toBeSent.delete();
	}
	
	public void executeLogStatement(File logFile) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(logFile)); 
		String str ;
		Statement stmt = con.createStatement();
		while((str=reader.readLine())!=null && str.length()!=0){
			stmt.execute(str);
		}
	}
	
	public void updateLogFile(File temp, String logFile, String Client){
		try{
			File cache_log = new File(logFile);
			BufferedReader reader = new BufferedReader(new FileReader(temp)); 
			BufferedWriter writer = new BufferedWriter(new FileWriter(cache_log,true));
			String str;
			while((str = reader.readLine())!=null){
				writer.write(str);
				writer.newLine();
				
			}
			reader.close();
			writer.close();
			BufferedReader reader1 = new BufferedReader(new FileReader(cache_log));
			int count = 0 ;
			while(reader1.readLine()!= null)
				++count ;
			Statement stmt = con.createStatement();
			stmt.execute("update synctable set updated_till ="+count+" where entity ='"+Client+"'");
			System.out.println("Logs are updated with checkpoint: "+count+" for the client: "+Client);
			stmt.close();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

}*/

package DBSync;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.BlockingQueue;
import java.io.*;

public class GlobalUpdate extends Thread {
	BlockingQueue<Socket> clsocks ;
	Socket sock;
	Connection con ;
	Statement stmt ;
	ResultSet rs ;
		
	public GlobalUpdate(BlockingQueue<Socket> clientSocks){
		try{
 			Class.forName("com.mysql.jdbc.Driver");
 			con = DriverManager.getConnection
				("jdbc:mysql://localhost:3306/syncdb","root","abc123");
 			clsocks = clientSocks ;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
		
	public void run()
	{
		while(true){
			try {
				//sock =clsocks.poll();
				sock = clsocks.take();
				//if(sock != null){
				String client = sock.getInetAddress().getHostName();
				System.out.println("Client Host Name is: "+client);
				if(client.equals("localhost"))
				{
					ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
					String query = (String) ois.readObject();
					stmt = con.createStatement();
					stmt.execute(query);
					File outFile = new File("cache_db.log");
					BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true));
					writer.write(query);
					writer.newLine();
					writer.close();

				}
				else
				{
					System.out.println("Logs received in syncServer from: "+client);
					OutputStream ous = sock.getOutputStream();
					InputStream  ois = sock.getInputStream();
					//logFile = logFileRead(ois);
					logFileRead(ois);
					stmt.execute("select updated_till from synctable where entity ='"+client+"'");
					rs = stmt.getResultSet();
					int cp = 0 ;
					if(rs.next())
						cp = rs.getInt(1);
					rs.close();
					logFileSent(ous, cp);
					ois.close();
					ous.close();
					System.out.println("Logs sent from syncServer to: "+client);
					//executeLogStatement(logFile);
					executeLogStatement();
					//updateLogFile(logFile,"cache_db.log", client);
					updateLogFile("download.log","cache_db.log", client);
					File logFile = new File("download.log");
					if(logFile.exists())
						logFile.delete();
				}	
				stmt.execute("select * from synctable where entity !='"+client+"'");
				rs = stmt.getResultSet();
				while(rs.next())
				{
					int current = rs.getInt(2);
					String serverName = rs.getString(1);
					System.out.println("In SyncServer entity: "+serverName+" with updated_till value: "+current);
					InetAddress server = InetAddress.getByName(serverName);
					InetAddress host = InetAddress.getByName("myrootserver");
					Socket clientSocket = new Socket(server, 6789,host,0);
					OutputStream ous1 = clientSocket.getOutputStream();
					InputStream  ois1 = clientSocket.getInputStream();
					logFileSent(ous1,current);
					logFileRead(ois1);
					//executeLogStatement(logFile);
					executeLogStatement();
					updateLogFile("download.log","cache_db.log", serverName);
					File logFile = new File("download.log");
					if(logFile.exists())
						logFile.delete();
					ois1.close();
					ous1.close();

				}
				
				 
			//}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void logFileRead(InputStream ois) throws Exception{
		File logFile = new File("download.log");
		int bytesRead ;
		int current = 0 ;
		byte[] data = new byte[10240];
		FileOutputStream fos = new FileOutputStream(logFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		bytesRead = ois.read(data,0,data.length);
		current = bytesRead ;
		/*do{
			bytesRead = ois.read(data,current, data.length-current);
			if(bytesRead>-1)
				current += bytesRead ;
		}while(bytesRead>-1);
		*/
		bos.write(data,0,current);
		bos.close();
		fos.close();
		//return logFile ;
	}
	
	public void logFileSent(OutputStream ous, int cp)throws Exception{
		System.out.println("In LogFile Sent");
		File toBeRead = new File("cache_db.log");
		File toBeSent = new File("upload.log");
		BufferedReader reader = new BufferedReader(new FileReader(toBeRead)); 
		BufferedWriter writer = new BufferedWriter(new FileWriter(toBeSent));
		String str;
		int count = 0 ;
		boolean blank = true ;
		//while((str = reader.readLine())!=null && str.length()!=0){
		while((str = reader.readLine())!=null){
			++count ;
			if(count<=cp){
				//System.out.println("Comparing the updated line");
				continue ;
			}	
			else{
				//System.out.println("writing logs in file");
				writer.write(str);
				writer.newLine();
				blank = false ;
			}
		}
		if(blank)
			writer.newLine();
		reader.close();
		writer.close();
		byte[] data = new byte[(int) toBeSent.length()];
		FileInputStream fis = new FileInputStream(toBeSent);
		BufferedInputStream bis = new BufferedInputStream(fis);
		bis.read(data,0,data.length);
		ous.write(data,0,data.length);
		ous.flush();
		bis.close();
		fis.close();
		System.out.println("LogFile has been Sent");
		if(toBeSent.exists()){
			boolean flag = toBeSent.delete();
			System.out.println("Delete the temp file: "+flag);
		}	
	}
	
	//public void executeLogStatement(File logFile) throws Exception{
	public void executeLogStatement() throws Exception{
		File logFile = new File("download.log");
		BufferedReader reader = new BufferedReader(new FileReader(logFile)); 
		String str ;
		Statement stmt = con.createStatement();
		while((str=reader.readLine())!=null && str.length()>1){
			stmt.execute(str);
		}
		reader.close();
	}
	
	//public void updateLogFile(File temp, String logFile, String Client){
	public void updateLogFile(String downloadFile, String logFile, String Client){
		try{
			File temp = new File(downloadFile);
			File cache_log = new File(logFile);
			BufferedReader reader = new BufferedReader(new FileReader(temp)); 
			BufferedWriter writer = new BufferedWriter(new FileWriter(cache_log,true));
			String str;
			while((str = reader.readLine())!=null && str.length()!=0){
				writer.write(str);
				writer.newLine();
			}
			reader.close();
			writer.close();
			BufferedReader reader1 = new BufferedReader(new FileReader(cache_log));
			int count = 0 ;
			while(reader1.readLine()!= null)
				++count ;
			reader1.close();
			Statement stmt = con.createStatement();
			stmt.execute("update synctable set updated_till ="+count+" where entity ='"+Client+"'");
			System.out.println("Logs are updated with checkpoint: "+count+" for the client: "+Client);
			stmt.close();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

}

