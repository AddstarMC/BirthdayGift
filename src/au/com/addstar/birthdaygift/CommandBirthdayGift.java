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
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.info")) { return false; }
				
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
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.stats")) { return false; }

				sender.sendMessage(ChatColor.RED + "Feature not implemented yet.");
				break;
			case "SET":
				/*
				 * Set the player's birthdate (overrides any existing record and CAN be set to today)
				 */
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.set")) { return false; }

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
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.reset")) { return false; }

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
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.delete")) { return false; }

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
			default:
				/*
				 * Usage information for the command
				 */
				sender.sendMessage(ChatColor.GREEN + "Available BirthdayGift commands:");
				if (plugin.HasPermission((Player) sender, "birthdaygift.info"))
					sender.sendMessage(ChatColor.AQUA + "/bgift info <player> : " + ChatColor.WHITE + "Player's birthday info");
				if (plugin.HasPermission((Player) sender, "birthdaygift.stats"))
					sender.sendMessage(ChatColor.AQUA + "/bgift stats : " + ChatColor.WHITE + "Birthday stats");
				if (plugin.HasPermission((Player) sender, "birthdaygift.set"))
					sender.sendMessage(ChatColor.AQUA + "/bgift set <player> <DD-MM-YYYY> : " + ChatColor.WHITE + "Set player's birthday");
				if (plugin.HasPermission((Player) sender, "birthdaygift.reset"))
					sender.sendMessage(ChatColor.AQUA + "/bgift reset <player> : " + ChatColor.WHITE + "Reset 'gift received' flag");
				if (plugin.HasPermission((Player) sender, "birthdaygift.delete"))
					sender.sendMessage(ChatColor.AQUA + "/bgift delete <player> : " + ChatColor.WHITE + "Delete birthday record");
			}
		}
		return true;
	}
}
