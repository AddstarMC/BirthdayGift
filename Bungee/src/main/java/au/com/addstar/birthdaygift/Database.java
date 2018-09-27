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
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

public class Database {
	private BirthdayGift plugin;
	private Connection con;
	
	private PreparedStatement updateGiftDate;
	private PreparedStatement updateAnnounceDate;
	private PreparedStatement getBirthday;
	private PreparedStatement insertBirthday;
	private PreparedStatement deleteBirthday;
	
	public Database(BirthdayGift instance) {
		plugin = instance;
	}

	public boolean openDatabase(Config config) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", config.getString("host", "localhost"), config.getInt("port", 3306), config.getString("database", "birthdaygift")), config.getString("user", "username"), config.getString("password", "password"));
			
			createTable();
			createStatements();
			return true;
		} catch (SQLException e) {
			plugin.getLogger().severe("Unable to open database!");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			plugin.getLogger().severe("Unable to find a suitable MySQL driver!");
			e.printStackTrace();
		}
		return false;
	}
	
	private void createTable() throws SQLException {
		Statement statement = con.createStatement();
		try {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS birthdaygift ("
						+ "`id` CHAR(36) PRIMARY KEY,"
						+ "`birthday` DATE, `lastGift` DATE,"
						+ "`lastAnnounced` DATE)");
		} finally {
			statement.close();
		}
	}
	
	private void createStatements() throws SQLException {
		getBirthday = con.prepareStatement("SELECT birthday,lastGift,lastAnnounced FROM birthdaygift WHERE id=?");
		updateGiftDate = con.prepareStatement("UPDATE birthdaygift SET lastGift=? WHERE id=?");
		updateAnnounceDate = con.prepareStatement("UPDATE birthdaygift SET lastAnnounced=? WHERE id=?");
		insertBirthday = con.prepareStatement("REPLACE birthdaygift (id, birthday) VALUES (?, ?)");
		deleteBirthday = con.prepareStatement("DELETE FROM birthdaygift WHERE id=?");
	}
	
	private void setParameters(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; ++i) {
        	if (args[i] == null) {
        		statement.setNull(i+1, Types.DATE);
        	} else if (args[i] instanceof Number) {
                statement.setObject(i+1, args[i]);
            } else if (args[i] instanceof String) {
                statement.setString(i+1, (String)args[i]);
            } else if (args[i] instanceof Date) {
            	statement.setDate(i+1, new java.sql.Date(((Date)args[i]).getTime()));
            } else {
                statement.setString(i+1, String.valueOf(args[i]));
            }
        }
    }
	
	private int executeUpdate(PreparedStatement statement, Object... args) {
        try {
            setParameters(statement, args);
            
            return statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing sql update", e);
            return 0;
        }
    }
    
    private ResultSet executeQuery(PreparedStatement statement, Object... args) throws SQLException {
        setParameters(statement, args);
	
		return statement.executeQuery();
    }
    
    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Unable to close ResultSet", e);
            }
        }
    }
    
    private void closeStatement(Statement st) {
    	if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Unable to close Statement", e);
            }
        }
    }
	
	public BirthdayRecord getBirthday(UUID player) {
		ResultSet rs = null;
		try {
			rs = executeQuery(getBirthday, player.toString());
			
			if (rs.next()) {
				BirthdayRecord record = new BirthdayRecord(player);
				record.birthdayDate = rs.getDate("birthday");
				record.lastGiftDate = rs.getDate("lastGift");
				record.lastAnnouncedDate = rs.getDate("lastAnnounced");
				
				plugin.Debug("Retrieved DB record for " + player + ": " + record.birthdayDate);
				return record;
			} else {
				plugin.Debug("No birthday set for " + player);
				return null;
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Error executing sql query", e);
			return null;
		} finally {
			closeResultSet(rs);
		}
	}
	
	public void setBirthday(UUID playerId, Date birthday) {
		executeUpdate(insertBirthday, playerId, birthday);
	}
	
	public void deleteBirthday(UUID player) {
		executeUpdate(deleteBirthday, player);
	}
	
	public void setAnnounceDate(UUID player, Date date) {
		executeUpdate(updateAnnounceDate, date, player);
	}
	
	public void setGiftDate(UUID player, Date date) {
		executeUpdate(updateGiftDate, date, player);
	}
	
	public BirthdayStats getStats() {
		Statement statement = null;
		ResultSet rs = null;
		try {
			BirthdayStats stats = new BirthdayStats();
			
			String year = new SimpleDateFormat("YYYY").format(new Date());
			String month = new SimpleDateFormat("MM").format(new Date());
			
			statement = con.createStatement();
			
			// Total birthday records
			rs = statement.executeQuery("SELECT COUNT(*) FROM birthdaygift");
			if (rs.next()) {
				stats.TotalBirthdays = rs.getInt(1);
			}
			rs.close();
			
			// Total birthdays this month
			rs = statement.executeQuery("SELECT COUNT(*) FROM birthdaygift WHERE DATE_FORMAT(birthday, '%m') = '" + month + "'");
			if (rs.next()) {
				stats.MonthBirthdays = rs.getInt(1);
			}
			rs.close();
			
			// Total claimed gifts this year
			rs = statement.executeQuery("SELECT COUNT(*) FROM birthdaygift WHERE DATE_FORMAT(lastGift, '%Y') = '" + year + "'");
			if (rs.next()) {
				stats.ClaimedGiftsThisYear = rs.getInt(1);
			}
			rs.close();
			
			// Total unclaimed gifts this year
			rs = statement.executeQuery("SELECT COUNT(*) FROM birthdaygift WHERE DATE_FORMAT(lastAnnounced, '%Y') = '" + year + "' AND DATE_FORMAT(lastGift, '%Y') != '" + year + "'");
			if (rs.next()) {
				stats.UnclaimedGiftsThisYear = rs.getInt(1);
			}
			rs.close();
			
			return stats;
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Error executing sql query", e);
			return null;
		} finally {
			closeResultSet(rs);
			closeStatement(statement);
		}
	}

	public boolean close() {
		try {
			con.close();
		} catch (SQLException e) {
			plugin.getLogger().warning("Close database failed!");
			e.printStackTrace();
		}
		return true;
	}
}

