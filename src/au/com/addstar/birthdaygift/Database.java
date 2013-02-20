package au.com.addstar.birthdaygift;
/*
* BirthdayGift
* Copyright (C) 2013 add5tar <copyright at addstar dot com dot au>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
	private BirthdayGift plugin;
	public String DBFilename;
	public Connection Conn;
	public boolean IsConnected = false;
	
	public Database(BirthdayGift instance, String filename) {
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
		
		if (!IsConnected) { return null; }
		
		try {
			st = Conn.createStatement();
			return st.executeQuery(query);
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return null;
		}
	}
	
	public ResultSet PreparedQuery(String query, String[] params) {
		PreparedStatement ps;
		
		if (!IsConnected) { return null; }
		
		try {
			ps = Conn.prepareStatement(query);
			// Construct PreparedStatement by adding all supplied params to the query
			plugin.Debug("SQL Query: " + query);
			for (int x=0; x < params.length; x++) {
				plugin.Debug("Param " + (x+1) + ": "+ params[x]);
				ps.setString(x+1, params[x]);
			}
			return ps.executeQuery();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return null;
		}
	}
	
	public int ExecuteUpdate(String query) {
		Statement st;
		
		if (!IsConnected) { return -1; }
		
		try {
			st = Conn.createStatement();
			return st.executeUpdate(query);
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return -1;
		}
	}
	
	public int PreparedUpdate(String query, String[] params) {
		PreparedStatement ps;
		
		if (!IsConnected) { return -1; }
		
		try {
			ps = Conn.prepareStatement(query);
			// Construct PreparedStatement by adding all supplied params to the query
			plugin.Debug("SQL Update: " + query);
			for (int x=0; x < params.length; x++) {
				plugin.Debug("Param " + (x+1) + ": "+ params[x]);
				ps.setString(x+1, params[x]);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return -1;
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

	/*
	 * Clean up user provided strings to protect against SQL injection
	 */
	public String SQLEncode(String origtext) {
		String newtext = origtext;
        newtext = newtext.replaceAll("\\\\", "\\\\\\\\");
        newtext = newtext.replaceAll("\\n","\\\\n");
        newtext = newtext.replaceAll("\\r", "\\\\r");
        newtext = newtext.replaceAll("\\t", "\\\\t");
        newtext = newtext.replaceAll("\\0", "\\\\0");
        newtext = newtext.replaceAll("'", "\\\\'");
        newtext = newtext.replaceAll("\\\"", "\\\\\"");
        return newtext;
	}
}
