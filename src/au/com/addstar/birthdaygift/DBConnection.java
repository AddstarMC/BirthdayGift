package au.com.addstar.birthdaygift;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
	private BirthdayGift plugin;
	public String DBFilename;
	public Connection Conn;
	public boolean IsConnected = false;
	
	public DBConnection(BirthdayGift instance, String filename) {
		plugin = instance;
		DBFilename = filename;
		OpenDatabase();
	}

	public Connection OpenDatabase() {
		try {
			Conn = DriverManager.getConnection("jdbc:sqlite:plugins/" + plugin.getName() + "/" + DBFilename);
			IsConnected = true;
			ExecuteUpdate("CREATE TABLE IF NOT EXISTS birthdaygift (`player` varchar(250) NOT NULL PRIMARY KEY, `birthdayDate` DATE, `lastGiftDate` DATE)");
			return Conn;
		} catch (SQLException e) {
			plugin.Warn("Unable to open database!");
			e.printStackTrace();
		}
		return null;
	}
	
	public ResultSet ExecuteQuery(String query) {
		Statement st;
		ResultSet res;
		
		if (!IsConnected) { return null; }
		
		try {
			st = Conn.createStatement();
			res = st.executeQuery(query);
			return res;
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return null;
		}
	}
	
	public int ExecuteUpdate(String query) {
		Statement st;
		
		if (!IsConnected) { return 0; }
		
		try {
			st = Conn.createStatement();
			return st.executeUpdate(query);
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return 0;
		}
	}
	
	public boolean CloseDatabase() {
		try {
			Conn.close();
		} catch (SQLException e) {
			plugin.Warn("Close database failed!");
			e.printStackTrace();
		}
		return true;
	}
}
