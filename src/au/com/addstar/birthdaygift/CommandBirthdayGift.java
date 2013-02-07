package au.com.addstar.birthdaygift;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.birthdaygift.BirthdayGift.BirthdayRecord;

public class CommandBirthdayGift implements CommandExecutor {
	private BirthdayGift plugin;
	
	public CommandBirthdayGift(BirthdayGift plugin) {
		this.plugin = plugin;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("bgift")) {
			// Check if player can do this (console always can)
			if (sender instanceof Player) {
				//Player player = (Player) sender;
				if ((!sender.isOp()) && (!sender.hasPermission("birthdaygift.admin"))) {
					sender.sendMessage(ChatColor.RED + "Sorry, you do not have permission for this command.");
					return false;
				}
			}

			String action = "";
			if (args.length > 0) {
				action = args[0].toUpperCase();
			}
			
			// No parameter given or HELP
			switch(action) {
			case "INFO":
				/*
				 * Show the birthday record for the given player
				 */
				if (args.length < 2) {
					// Not enough parameters
					sender.sendMessage(ChatColor.AQUA + "Usage: /bgift info <player>");
				} else {
					// Fetch player info
					String player = args[1].toLowerCase();
					BirthdayRecord rec = plugin.getPlayerRecord(player);
					if (rec == null) {
						sender.sendMessage(ChatColor.RED + "No birthday record found for " + ChatColor.WHITE + player);
					} else {
						String bdate = "";
						String gdate = "Never";
						if (rec.birthdayDate != null) {
							bdate = new SimpleDateFormat("dd MMM yyyy").format(rec.birthdayDate);
						}
						if (rec.lastGiftDate != null) {
							gdate = new SimpleDateFormat("dd MMM yyyy").format(rec.lastGiftDate);
						}
						
						sender.sendMessage(ChatColor.YELLOW + "Birthday Date: " + ChatColor.WHITE + bdate);
						sender.sendMessage(ChatColor.YELLOW + "Last Gift Received On: " + ChatColor.WHITE + gdate);
					}
				}
				break;
			case "STATS":
				sender.sendMessage(ChatColor.RED + "Feature not implemented yet.");
				break;
			case "SET":
				/*
				 * Set the player's birthdate (overrides any existing record and CAN be set to today)
				 */
				if (args.length < 3) {
					// Not enough parameters
					sender.sendMessage(ChatColor.AQUA + "Usage: /bgift set <player> <DD-MM-YYYY>");
				} else {
					// Set player's birthday	
					Date bdate;
					String player = args[1].toLowerCase();
					String birthdate = args[2].toLowerCase();
					try {
						bdate = new SimpleDateFormat("dd-MM-yyyy").parse(birthdate);
					} catch (ParseException e) {
						sender.sendMessage(ChatColor.RED + "Invalid birthday! Please use format: DD-MM-YYYY");
						return true;
					}
					BirthdayRecord rec = new BirthdayRecord();
					rec.birthdayDate = bdate;
					
					// Set player's birthday
					plugin.SetPlayerBirthday(player, bdate);
					String mydate = new SimpleDateFormat("dd MMM yyyy").format(bdate); 
					sender.sendMessage(ChatColor.WHITE + player + "'s" + ChatColor.YELLOW + " birthday is now set to: " + ChatColor.GREEN + mydate);
				}
				break;
			case "RESET":
				/*
				 * Reset the player's "Last gift received date"
				 */
				if (args.length < 2) {
					// Not enough parameters
					sender.sendMessage(ChatColor.AQUA + "Usage: /bgift reset <player>");
				} else {
					// Fetch player info
					String player = args[1].toLowerCase();
					BirthdayRecord rec = plugin.getPlayerRecord(player);
					if (rec == null) {
						sender.sendMessage(ChatColor.RED + "No birthday record found for "+ ChatColor.WHITE + player);
					} else {
						plugin.SetGiftReceived(player, null);
						sender.sendMessage(ChatColor.YELLOW + "Last Gift Received date has been reset for " + ChatColor.WHITE + player);
					}
				}
				break;
			case "DELETE":
				/*
				 * Completely delete the player's birthday record
				 */
				if (args.length < 2) {
					// Not enough parameters
					sender.sendMessage(ChatColor.AQUA + "Usage: /bgift delete <player>");
				} else {
					// Fetch player info
					String player = args[1].toLowerCase();
					BirthdayRecord rec = plugin.getPlayerRecord(player);
					if (rec == null) {
						sender.sendMessage(ChatColor.RED + "No birthday record found for "+ ChatColor.WHITE + player);
					} else {
						plugin.DeletePlayerBirthday(player);
						sender.sendMessage(ChatColor.YELLOW + "Birthday record for " + ChatColor.WHITE + player + ChatColor.YELLOW + " has been deleted");
					}
				}
				break;
			case "REGIFT":
				sender.sendMessage(ChatColor.RED + "Feature not implemented yet.");
				break;
			case "REPAY":
				sender.sendMessage(ChatColor.RED + "Feature not implemented yet.");
				break;
			default:
				/*
				 * Usage information for the command
				 */
				sender.sendMessage(ChatColor.GREEN + "Available BirthdayGift commands:");
				sender.sendMessage(ChatColor.AQUA + "/bgift info <player> : " + ChatColor.WHITE + "Player's birthday info");
				sender.sendMessage(ChatColor.AQUA + "/bgift stats : " + ChatColor.WHITE + "Birthday stats");
				sender.sendMessage(ChatColor.AQUA + "/bgift set <player> <DD-MM-YYYY> : " + ChatColor.WHITE + "Set player's birthday");
				sender.sendMessage(ChatColor.AQUA + "/bgift reset <player> : " + ChatColor.WHITE + "Reset 'gift received' flag");
				sender.sendMessage(ChatColor.AQUA + "/bgift delete <player> : " + ChatColor.WHITE + "Delete birthday record");
				sender.sendMessage(ChatColor.AQUA + "/bgift regift <player> : " + ChatColor.WHITE + "Re-issue birthday gift(s) only");
				sender.sendMessage(ChatColor.AQUA + "/bgift repay <player> : " + ChatColor.WHITE + "Re-issue birthday money only");
			}
		}
		return true;
	}
}
