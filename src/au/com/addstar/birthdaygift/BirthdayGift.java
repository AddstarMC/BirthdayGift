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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author add5tar
 *
 */
public final class BirthdayGift extends JavaPlugin {
	public BirthdayGift plugin;
	public Database dbcon = null;
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	public boolean VaultEnabled = false;
	public boolean DebugEnabled = false;
	public String JoinMessage = "";
	public String AnnounceMessage = "";
	public String GiftMessage = "";
	public String ClaimMessage = "";
	public String MoneyMessage = "";
	private static final Logger logger = Logger.getLogger("BirthdayGift");
	public ConfigManager cfg = new ConfigManager(this);
	public List<ItemStack> RewardItems = new ArrayList<ItemStack>();
	public PluginDescriptionFile pdfFile = null;
	public PluginManager pm = null;
	
	static class BirthdayRecord {
		String playerName = "";
		Date birthdayDate = null;
		Date lastGiftDate = null;
		Date lastAnnouncedDate = null;
	}
	
	static class BirthdayStats {
		int TotalBirthdays = 0;
		int MonthBirthdays = 0;
		int ClaimedGiftsThisYear = 0;
		int UnclaimedGiftsThisYear = 0;
		Date NextBirthdayDate = null;
		String NextBirthdayPlayer = "";
	}
		
	@Override
	public void onEnable(){
		// Register necessary events
		pdfFile = this.getDescription();
		pm = this.getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);

		// Check if vault is loaded (required for economy)
		VaultEnabled = setupEconomy();
		if (VaultEnabled) {
			Log("Found Vault! Hooking for economy!");
		} else {
			Log("Vault was not detected! Economy rewards are not available.");
		}
		
		// Read (or initialise) plugin config file
		cfg.LoadConfig(getConfig());

		// Save the default config (if one doesn't exist)
		saveDefaultConfig();

		getCommand("birthday").setExecutor(new CommandBirthday(this));
		getCommand("birthdaygift").setExecutor(new CommandBirthdayGift(this));
		getCommand("birthdaygift").setAliases(Arrays.asList("bgift"));

		// Open/initialise the database
		dbcon = new Database(this, "birthday.db");
		if (dbcon.IsConnected) {
			Log(pdfFile.getName() + " " + pdfFile.getVersion() + " has been enabled");
		} else {
			Log(pdfFile.getName() + " " + pdfFile.getVersion() + " could not be enabled!");
			this.setEnabled(false);
			return;
		}
	}
	
	@Override
	public void onDisable(){
		if ((dbcon != null) && (dbcon.IsConnected)) {
			dbcon.CloseDatabase();
		}

		// cancel all tasks we created
        getServer().getScheduler().cancelTasks(this);
		
		Log(pdfFile.getName() + " has been disabled!");
	}
	
	/*
	 * Detect/configure Vault
	 */
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

	public void Log(String data) {
		logger.info("[BirthdayGift] " + data);
	}

	public void Warn(String data) {
		logger.warning("[BirthdayGift] " + data);
	}
	
	public void Debug(String data) {
		if (DebugEnabled) {
			logger.info("[BirthdayGift] DEBUG: " + data);
		}
	}

	public FileConfiguration Config() {
		return getConfig();
	}
	
	public Material GetMaterial(String name) {
		Material mat = Material.matchMaterial(name);
		if (mat != null) {
			return mat;
		}
		return null;
	}
	
	public boolean GiveMoney(String player, int money) {
		if (VaultEnabled) {
			EconomyResponse resp = econ.depositPlayer(player, money);
			if (resp.type == ResponseType.SUCCESS) {
				Log(player + " has been given $" + resp.amount + " (new balance $" + resp.balance + ")");
				return true;
			} else {
				Warn("Vault payment failed! Error: " + resp.errorMessage);
			}
		}
		return false;
	}
	
	public boolean GiveItemStack(Player player, ItemStack itemstack) {
		PlayerInventory inventory = player.getInventory();
		HashMap<Integer, ItemStack> result = inventory.addItem(itemstack);
		//TODO: Check "result" to ensure all items were given
		if (result == null) {
			return false;
		}
		return true;
	}

	public ItemStack CreateStack(Material item, int datavalue, int amount) {
		ItemStack itemstack = new ItemStack(item, amount, (short)datavalue);
		return itemstack;
	}

	public BirthdayRecord getPlayerRecord(String player) {
		String query;
		ResultSet res;

		if (!dbcon.IsConnected) { return null; }
		
		if ((player == null) || (player == "")) {
			Debug("getPlayerRecord() was called with empty player!");
			return null;
		}
		
		BirthdayRecord rec = new BirthdayRecord();
		query = "SELECT birthdayDate,lastGiftDate,lastAnnouncedDate FROM birthdaygift WHERE player=?";
		res = dbcon.PreparedQuery(query, new String[]{player.toLowerCase()});
		if (res == null) {
			// Query failed! This should not happen, just return because the error is already logged by PreparedQuery()
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
				Warn("BirthdayDate is empty for player \"" + player + "\"!");
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
			Warn("Date conversation error!");
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
	 *  Check if the player has already received a gift today
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
				Warn("Unable to parse current date!");
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
		dbcon.PreparedUpdate(query, new String[]{datestr, player.toLowerCase()});
		return true;
	}

	/*
	 *  Check if the player's birthday has already been announced (broadcast)
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
				Warn("Unable to parse current date!");
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
		dbcon.PreparedUpdate(query, new String[]{datestr, player.toLowerCase()});
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
		dbcon.PreparedUpdate(query, new String[]{datestr, player.toLowerCase()});
		return true;
	}
	
	/*
	 * Delete the birthday record of the given user
	 */
	public boolean DeletePlayerBirthday(String player) {
		String query = "DELETE FROM birthdaygift WHERE player=?";
		dbcon.PreparedUpdate(query, new String[]{player.toLowerCase()});
		return true;
	}
	
	/*
	 * Check if the player has the specified permission
	 */
	public boolean HasPermission(Player player, String perm) {
		if (player instanceof Player) {
			// Real player
			if (player.hasPermission(perm)) {
				return true;
			}
		} else {
			// Console has permissions for everything
			return true;
		}
		return false;
	}
	
	/*
	 * Check required permission and send error response to player if not allowed
	 */
	public boolean RequirePermission(Player player, String perm) {
		if (!HasPermission(player, perm)) {
			if (player instanceof Player) {
				player.sendMessage(ChatColor.RED + "Sorry, you do not have permission for this command.");
				return false;
			}
		}
		return true;
	}
	
	public BirthdayStats getBirthdayStats() throws SQLException {
		if (!dbcon.IsConnected) { return null; }

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
		res = dbcon.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%m', BirthdayDate) = '" + month + "'");
		if ((res != null) && (res.next())) {
			stats.MonthBirthdays = res.getInt(1);
		}
		
		// Total claimed gifts this year
		res = dbcon.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%Y', lastGiftDate) = '" + year + "'");
		if ((res != null) && (res.next())) {
			stats.ClaimedGiftsThisYear = res.getInt(1);
		}
		
		// Total unclaimed gifts this year
		res = dbcon.ExecuteQuery("SELECT COUNT(*) FROM birthdaygift WHERE strftime('%Y', lastAnnouncedDate) = '" + year + "' AND strftime('%Y', lastGiftDate) IS NOT '" + year + "'");
		if ((res != null) && (res.next())) {
			stats.UnclaimedGiftsThisYear = res.getInt(1);
		}
		
		return stats;
	}
}