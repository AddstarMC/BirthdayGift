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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

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
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	public boolean VaultEnabled = false;
	public boolean DebugEnabled = false;
	public String GiftMessage = "";
	public String MoneyMessage = "";
	public boolean USDateFormat = false;
	public String InputDateFormat;
	private static final Logger logger = Logger.getLogger("BirthdayGift");
	public ConfigManager cfg = new ConfigManager(this);
	public List<ItemStack> RewardItems = new ArrayList<ItemStack>();
	public PluginDescriptionFile pdfFile = null;
	public PluginManager pm = null;
	
	private Bungee bungee;
	
	@Override
	public void onEnable(){
		// Register necessary events
		pdfFile = this.getDescription();
		pm = this.getServer().getPluginManager();

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

		if (USDateFormat) {
			InputDateFormat = "MM-dd-yyyy";
		} else {
			InputDateFormat = "dd-MM-yyyy";
		}
		
		getCommand("birthday").setExecutor(new CommandBirthday(this));
		getCommand("birthdaygift").setExecutor(new CommandBirthdayGift(this));
		getCommand("birthdaygift").setAliases(Arrays.asList("bgift"));
		
		bungee = new Bungee(this);
	}
	
	@Override
	public void onDisable(){
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
		
	public int getAge(Date dateOfBirth) {
	    Calendar now = Calendar.getInstance();
	    Calendar dob = Calendar.getInstance();

	    dob.setTime(dateOfBirth);

	    int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

	    if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) 
	    {
	        age--;
	    }

	    return age;
	}
	
	public Bungee getBungee() {
		return bungee;
	}
}