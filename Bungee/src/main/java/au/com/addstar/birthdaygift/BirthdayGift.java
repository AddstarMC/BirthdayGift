package au.com.addstar.birthdaygift;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.md_5.bungee.api.plugin.Plugin;

public class BirthdayGift extends Plugin {
	public BirthdayGift plugin;
	public Database dbcon = null;
	public boolean DebugEnabled = false;
	public String InputDateFormat;

	public static class BirthdayRecord {
		public String playerName = "";
		public Date birthdayDate = null;
		public Date lastGiftDate = null;
		public Date lastAnnouncedDate = null;
	}

	public static class BirthdayStats {
		public int TotalBirthdays = 0;
		public int MonthBirthdays = 0;
		public int ClaimedGiftsThisYear = 0;
		public int UnclaimedGiftsThisYear = 0;
		public Date NextBirthdayDate = null;
		public String NextBirthdayPlayer = "";
	}

	@Override
	public void onEnable() {
		// Register necessary events
		getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
		getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

		loadMessages();

		// Open/initialise the database
		dbcon = new Database(this, "birthday.db");
		if (!dbcon.IsConnected) {
			getLogger().severe("Failed to connect to database.");
			return;
		}
	}

	@Override
	public void onDisable() {
		if ((dbcon != null) && (dbcon.IsConnected)) {
			dbcon.CloseDatabase();
		}
	}

	private void loadMessages() {
		File file = new File(getDataFolder(), "messages.properties");
		try {
			if (file.exists()) {
				Messages.load(file);
			} else {
				Messages.load(getResourceAsStream("/messages.properties"));
			}
		} catch (IOException e) {
			getLogger().warning(
					"An error occured while attempting to load messages (used file? "
							+ file.exists() + "):");
			e.printStackTrace();
		}
	}

	public void Debug(String data) {
		if (DebugEnabled) {
			getLogger().info("DEBUG: " + data);
		}
	}

	public BirthdayRecord getPlayerRecord(String player) {
		String query;
		ResultSet res;

		if (!dbcon.IsConnected) {
			return null;
		}

		if ((player == null) || (player == "")) {
			Debug("getPlayerRecord() was called with empty player!");
			return null;
		}

		BirthdayRecord rec = new BirthdayRecord();
		query = "SELECT birthdayDate,lastGiftDate,lastAnnouncedDate FROM birthdaygift WHERE player=?";
		res = dbcon.PreparedQuery(query, new String[] { player.toLowerCase() });
		if (res == null) {
			// Query failed! This should not happen, just return because the
			// error is already logged by PreparedQuery()
			return null;
		}

		// No results
		try {
			if (!res.next()) {
				Debug("No birthday set for " + player);
				return null;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		rec.playerName = player;

		// Get last
		try {
			String data;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			// Get the player's birthday
			data = res.getString("birthdayDate");
			if ((data != null) && (!data.isEmpty())) {
				rec.birthdayDate = sdf.parse(data);
			} else {
				getLogger().warning(
						"BirthdayDate is empty for player \"" + player + "\"!");
				return null;
			}

			// Get the last gift received date
			data = res.getString("lastGiftDate");
			if ((data != null) && (!data.isEmpty())) {
				rec.lastGiftDate = sdf.parse(data);
			}

			// Get the last announced date
			data = res.getString("lastAnnouncedDate");
			if ((data != null) && (!data.isEmpty())) {
				rec.lastAnnouncedDate = sdf.parse(data);
			}
		} catch (ParseException | SQLException e) {
			getLogger().warning("Date conversation error!");
			e.printStackTrace();
			return null;
		}
		Debug("Retrieved DB record for " + player + ": " + rec.birthdayDate);
		return rec;
	}

	/*
	 * Check if it is the player's birthday today
	 */
	public boolean IsPlayerBirthday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			// Get current date without year/time
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			String today = sdf.format(new Date());
			String bdate = sdf.format(birthday.birthdayDate);

			// Check if today is the player's birthday (ignoring year)
			if (bdate.equals(today)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/*
	 * Check if the player has already received a gift today
	 */
	public boolean ReceivedGiftToday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			if (birthday.lastGiftDate == null) {
				// Player has never received birthday gifts
				Debug("Never received a gift");
				return false;
			}

			// Get current date without time (annoying, right?)
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date today;
			try {
				today = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				// This should never happen!
				getLogger().warning("Unable to parse current date!");
				e.printStackTrace();
				return false;
			}

			// Check if player has received a gift today
			if (birthday.lastGiftDate.equals(today)) {
				Debug("Already receieved a gift today");
				return true;
			} else {
				Debug("Hasn't received a gift today");
				return false;
			}
		}
		return false;
	}

	/*
	 * Set the "lastGiftDate" on the player's record
	 */
	public boolean SetGiftReceived(String player, Date newdate) {
		String datestr = "";
		if (newdate != null) {
			datestr = new SimpleDateFormat("yyyy-MM-dd").format(newdate);
		}
		String query = "UPDATE birthdaygift SET lastGiftDate=? WHERE player=?";
		dbcon.PreparedUpdate(query,
				new String[] { datestr, player.toLowerCase() });
		return true;
	}

	/*
	 * Check if the player's birthday has already been announced (broadcast)
	 */
	public boolean AnnouncedToday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			if (birthday.lastAnnouncedDate == null) {
				// Player has never had a birthday announced
				Debug("Never had a birthday announcement");
				return false;
			}

			// Get current date without time (annoying, right?)
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date today;
			try {
				today = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				// This should never happen!
				getLogger().warning("Unable to parse current date!");
				e.printStackTrace();
				return false;
			}

			// Check if player has received a gift today
			if (birthday.lastAnnouncedDate.equals(today)) {
				Debug("Already announced birthday today");
				return true;
			} else {
				Debug("Birthday has not been announced today");
				return false;
			}
		}
		return false;
	}

	/*
	 * Set the "lastAnnouncedDate" on the player's record
	 */
	public boolean SetAnnounced(String player, Date newdate) {
		String datestr = "";
		if (newdate != null) {
			datestr = new SimpleDateFormat("yyyy-MM-dd").format(newdate);
		}
		String query = "UPDATE birthdaygift SET lastAnnouncedDate=? WHERE player=?";
		dbcon.PreparedUpdate(query,
				new String[] { datestr, player.toLowerCase() });
		return true;
	}

	/*
	 * Set the player's birthday (updates existing record if already exists)
	 */
	public boolean SetPlayerBirthday(String player, Date bdate) {
		String datestr;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		datestr = sdf.format(bdate);
		BirthdayRecord birthday = getPlayerRecord(player);

		String query;
		if (birthday == null) {
			query = "INSERT INTO birthdaygift (birthdayDate, player, lastGiftDate, lastAnnouncedDate) VALUES (?, ?, '', '')";
		} else {
			query = "UPDATE birthdaygift SET birthdayDate=? WHERE player=?";
		}
		dbcon.PreparedUpdate(query,
				new String[] { datestr, player.toLowerCase() });
		return true;
	}

	/*
	 * Delete the birthday record of the given user
	 */
	public boolean DeletePlayerBirthday(String player) {
		String query = "DELETE FROM birthdaygift WHERE player=?";
		dbcon.PreparedUpdate(query, new String[] { player.toLowerCase() });
		return true;
	}

	public BirthdayStats getBirthdayStats() {
		if (!dbcon.IsConnected) {
			return null;
		}
		
		try {
			BirthdayStats stats = new BirthdayStats();
			ResultSet res;
	
			String year = new SimpleDateFormat("YYYY").format(new Date());
			String month = new SimpleDateFormat("MM").format(new Date());
	
			// Total birthday records
			res = dbcon.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift");
			if ((res != null) && (res.next())) {
				stats.TotalBirthdays = res.getInt(1);
			}
	
			// Total birthdays this month
			res = dbcon
					.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%m', BirthdayDate) = '"
							+ month + "'");
			if ((res != null) && (res.next())) {
				stats.MonthBirthdays = res.getInt(1);
			}
	
			// Total claimed gifts this year
			res = dbcon
					.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%Y', lastGiftDate) = '"
							+ year + "'");
			if ((res != null) && (res.next())) {
				stats.ClaimedGiftsThisYear = res.getInt(1);
			}
	
			// Total unclaimed gifts this year
			res = dbcon
					.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%Y', lastAnnouncedDate) = '"
							+ year
							+ "' AND strftime('%Y', lastGiftDate) IS NOT '"
							+ year + "'");
			if ((res != null) && (res.next())) {
				stats.UnclaimedGiftsThisYear = res.getInt(1);
			}
			
			return stats;
		} catch (SQLException e) {
			getLogger().warning("Failed to get birthday stats:");
			e.printStackTrace();
			
			return null;
		}
	}
}
