/**
 * 
 */
package au.com.addstar.birthdaygift;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
	private static final Logger logger = Logger.getLogger("BirthdayGift");
	public BirthdayGift plugin;
	public DBConnection dbcon = null;
    public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;
    
	class BirthdayRecord {
		String playerName = "";
		Date birthdayDate = null;
		Date lastGiftDate = null;
	}
		
	@Override
	public void onEnable(){
		// Register necessary events
		PluginDescriptionFile pdfFile = this.getDescription();
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);

		// Read (or initialise) plugin config file
		getConfig().options().copyDefaults(true);

		// Check if vault is loaded (required for economy)
		if (setupEconomy()) {
			Log("Found Vault! Hooking for economy!");
		}
		
		//CheckConfig();
		
		// Open/initialise the database
		dbcon = new DBConnection(this, "birthday.db");
		Log(pdfFile.getName() + " " + pdfFile.getVersion() + " has been enabled");

		// Store any "corrections" to the config in memory
		saveConfig();
	}
	
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		if (dbcon != null) {
			dbcon.CloseDatabase();
		}
		Log(pdfFile.getName() + " has been disabled!");
	}
	
	/*
	 * Handle plugin commands
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("birthday")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry, only in game players can use this command");
			} else {
				Player player = (Player) sender;
				BirthdayRecord birthday = getPlayerRecord(sender.getName());
				if (args.length == 0) {
					// Display player's birthday
					if ((birthday == null) || (birthday.birthdayDate == null)) {
						player.sendMessage(ChatColor.RED + "You have not set your birthday.");
						player.sendMessage(ChatColor.YELLOW + "Usage: /birthday DD-MM-YYYY    " + ChatColor.WHITE + "(eg. /birthday 31-12-2001)");
					} else {
						String mydate = new SimpleDateFormat("dd MMM yyyy").format(birthday.birthdayDate); 
						player.sendMessage(ChatColor.YELLOW + "Your birthday is currently set to: " + ChatColor.GREEN + mydate);
					}
				} else {
					// Set player's birthday	
					Date bdate;
					try {
						bdate = new SimpleDateFormat("dd-MM-yyyy").parse(args[0]);
					} catch (ParseException e) {
						player.sendMessage(ChatColor.RED + "Invalid birthday! Please use format: DD-MM-YYYY");
						return true;
					}
					
					if (birthday == null) {
						// Player's birthday is not set
						BirthdayRecord rec = new BirthdayRecord();
						rec.birthdayDate = bdate;
						if (IsPlayerBirthday(rec)) {
							// Don't allow players to set the birthday to today
							player.sendMessage(ChatColor.RED + "Sorry, you cannot set your birthday to today.");
							return true;
						} else {
							// Set player's birthday
							SetPlayerBirthday(player.getName(), bdate);
							String mydate = new SimpleDateFormat("dd MMM yyyy").format(bdate); 
							player.sendMessage(ChatColor.YELLOW + "Your birthday is now set to: " + ChatColor.GREEN + mydate);
						}
					} else {
						// Don't allow players to change their birthday once it's set
						player.sendMessage(ChatColor.RED + "Sorry, you cannot change your birthday");
					}
				}
			}
		}
		return true;
	}

	/*
	 * 
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
	
	public Material GetMaterial(String name) {
		Material mat = Material.matchMaterial(name);
		if (mat != null) {
			return mat;
		}
		return null;
	}
	
	public boolean GiveMoney(String player, int money) {
		EconomyResponse resp = econ.depositPlayer(player, money);
		if (resp.type == ResponseType.SUCCESS) {
			Log(player + " has been given $" + resp.amount);
			Log("New player balance: " + resp.balance);
			return true;
		} else {
			Warn("Vault payment failed! Error: " + resp.errorMessage);
			return false;
		}
	}
	
	public boolean GiveItem(Player player, Material item, int amount) {
		PlayerInventory inventory = player.getInventory();
		ItemStack itemstack = new ItemStack(item, amount);
		inventory.addItem(itemstack);
		return true;
	}

	public BirthdayRecord getPlayerRecord(String player) {
		String query;
		Statement st;
		ResultSet res;

		if (!dbcon.IsConnected) { return null; }
				
		BirthdayRecord rec = new BirthdayRecord();
		query = "SELECT birthdayDate,lastGiftDate FROM birthdaygift WHERE player='" + player + "'";
		try {
			st = dbcon.Conn.createStatement();
			res = st.executeQuery(query);
			if (!res.next()) {
				return null;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		rec.playerName = player;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			// Get the user's birthday
			if (res.getString("birthdayDate").isEmpty()) {
				Warn("BirthdayDate is empty for player \"" + player + "\"!");
				return null;
			} else {
				rec.birthdayDate = sdf.parse(res.getString("birthdayDate"));
			}

			// Get the last date of the  
			if (!res.getString("lastGiftDate").isEmpty()) {
				rec.lastGiftDate = sdf.parse(res.getString("lastGiftDate"));
			}
		} catch (ParseException | SQLException e) {
			Warn("Date conversation error!");
			e.printStackTrace();
			return null;
		}
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
				Log("Never received a gift");
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
				Log("Already receieved a gift today");
				return true;
			} else {
				Log("Hasn't received a gift today");
				return false;
			}
		}
		return false;
	}

	/*
	 * Set the "lastGiftDate" on the player's record
	 */
	public boolean SetGiftReceived(String player) {
		String datestr = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); 
		String query = "UPDATE birthdaygift SET lastGiftDate='" + datestr + "' WHERE player='" + player + "'";
		dbcon.ExecuteUpdate(query);
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
			query = "INSERT INTO birthdaygift (player, birthdayDate, lastGiftDate) VALUES ('" + player + "', '" + datestr + "', '')";
		} else {
			query = "UPDATE birthdaygift SET birthdayDate='" + datestr + "'";
		}
		dbcon.ExecuteUpdate(query);
		return true;
	}
}