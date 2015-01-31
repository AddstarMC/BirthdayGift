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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBirthdayGift implements CommandExecutor {
	private BirthdayGift plugin;
	
	public CommandBirthdayGift(BirthdayGift plugin) {
		this.plugin = plugin;
	}
	
	public boolean onCommand(final CommandSender sender, Command cmd, String commandLabel, final String[] args) {
		// Check if player can do this (console always can)
		String action = "";
		
		if (args.length > 0) {
			action = args[0].toUpperCase();
		}
		
		// No parameter given or HELP
		switch(action) {
		case "INFO": {
			/*
			 * Show the birthday record for the given player
			 */
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.info")) { return false; }
			}
			
			if (args.length < 2) {
				// Not enough parameters
				sender.sendMessage(ChatColor.AQUA + "Usage: /bgift info <player>");
			} else {
				// Fetch player info
				final OfflinePlayer player = Bukkit.getPlayer(args[1]);
				plugin.getBungee().getBirthday(player, new ResultCallback<BirthdayRecord>()
				{
					@Override
					public void onCompleted( boolean success, BirthdayRecord rec, Throwable error )
					{
						if (!success) {
							sender.sendMessage(ChatColor.RED + "An internal error occured");
							if (error != null) {
								error.printStackTrace();
							}
							return;
						}
						if (rec == null) {
							sender.sendMessage(ChatColor.RED + "No birthday record found for " + ChatColor.WHITE + player.getName());
						} else {
							String bdate = "";
							String gdate = "Never";
							if (rec.birthdayDate != null) {
								bdate = new SimpleDateFormat("dd MMM yyyy").format(rec.birthdayDate);
							}
							if (rec.lastGiftDate != null) {
								gdate = new SimpleDateFormat("dd MMM yyyy").format(rec.lastGiftDate);
							}
							
							// Calculate age
							int age = plugin.getAge(rec.birthdayDate); 
							
							sender.sendMessage(ChatColor.YELLOW + "Birthday Date: " + ChatColor.WHITE + bdate + " (age " + age + ")");
							sender.sendMessage(ChatColor.YELLOW + "Last Gift Received On: " + ChatColor.WHITE + gdate);
						}
					}
				});
			}
			break;
		}
		case "CLAIM": {
			/*
			 * Claim any waiting birthday gift(s)
			 */
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.claim")) { return false; }
			} else {
				sender.sendMessage("Sorry, only in-game players can use this command");
				return false;
			}
			
			final Player player = (Player)sender;
			// Fetch player info
			plugin.getBungee().getBirthday(player, new ResultCallback<BirthdayRecord>()
			{
				@Override
				public void onCompleted( boolean success, BirthdayRecord rec, Throwable error )
				{
					if (!success) {
						sender.sendMessage(ChatColor.RED + "An internal error occured");
						if (error != null) {
							error.printStackTrace();
						}
						return;
					}

					// Check birthday record
					if (rec == null) {
						// Can't claim if you didn't set your birthdate!
						sender.sendMessage(ChatColor.RED + "Sorry, there are no birthday gifts available for you.");
					} else {
						// Birthday has been set
						if (plugin.IsPlayerBirthday(rec)) {
							plugin.getBungee().claimGift(player, new ResultCallback<Boolean>()
							{
								@Override
								public void onCompleted( boolean success, Boolean claimed, Throwable error )
								{
									if (!success) {
										sender.sendMessage(ChatColor.RED + "An internal error occured");
										if (error != null) {
											error.printStackTrace();
										}
										return;
									}
									
									if (claimed) {
										// It's your birthday and you haven't received a gift yet!! :D
										
										if (!plugin.GiftMessage.isEmpty()) {
											String msg = plugin.GiftMessage.replaceAll("<PLAYER>", sender.getName());
											msg = ChatColor.translateAlternateColorCodes('&', msg);
											sender.sendMessage(msg);
										}

										// Reward player with items (if applicable)
										plugin.Log("Giving birthday gift(s) to " + sender.getName());
										for (int i = 0; i < plugin.RewardItems.size(); i++) {
											plugin.Debug("RewardItems["+i+"] => " + plugin.RewardItems.get(i).getType().name() + ":" + plugin.RewardItems.get(i).getData().getData());
											plugin.GiveItemStack(player, plugin.RewardItems.get(i));
										}

										// Reward player with money (if applicable)
										if (plugin.Config().isSet("money") && plugin.Config().isInt("money")) {
											if (!plugin.MoneyMessage.isEmpty()) {
												String msg = plugin.MoneyMessage.replaceAll("<PLAYER>", player.getDisplayName());
												msg = plugin.MoneyMessage.replaceAll("<MONEY>", plugin.Config().getString("money"));
												msg = ChatColor.translateAlternateColorCodes('&', msg);
												sender.sendMessage(msg);
											}
											plugin.GiveMoney(sender.getName(), plugin.Config().getInt("money"));
										}
									} else {
										sender.sendMessage(ChatColor.RED + "Sorry, you have already claimed your gift today.");
									}
								}
							});
						} else {
							sender.sendMessage(ChatColor.RED + "Sorry, it is not your birthday today.");
						}
					}
				}
			});
			break;
		}
		case "STATS": {
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.stats")) { return false; }
			}

			plugin.getBungee().getStats(new ResultCallback<BirthdayStats>()
			{
				@Override
				public void onCompleted( boolean success, BirthdayStats stats, Throwable error )
				{
					if (!success) {
						sender.sendMessage(ChatColor.RED + "An internal error occured");
						if (error != null) {
							error.printStackTrace();
						}
						return;
					}
		
					sender.sendMessage(ChatColor.GREEN + "BirthdayGift Statistics:");
					sender.sendMessage(ChatColor.YELLOW + "Total birthdays: " + ChatColor.WHITE + stats.TotalBirthdays);
					sender.sendMessage(ChatColor.YELLOW + "Gifts claimed this year: " + ChatColor.WHITE + stats.ClaimedGiftsThisYear);
					sender.sendMessage(ChatColor.YELLOW + "Gifts unclaimed this year: " + ChatColor.WHITE + stats.UnclaimedGiftsThisYear);
					sender.sendMessage(ChatColor.YELLOW + "Birthdays this month: " + ChatColor.WHITE + stats.MonthBirthdays);
				}
			});
			
			break;
		}
		case "SET": {
			/*
			 * Set the player's birthdate (overrides any existing record and CAN be set to today)
			 */
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.set")) { return false; }
			}

			if (args.length < 3) {
				// Not enough parameters
				sender.sendMessage(ChatColor.AQUA + "Usage: /bgift set <player> <" + plugin.InputDateFormat.toUpperCase() + ">");
			} else {
				// Set player's birthday	
				Date bdate;
				final OfflinePlayer player = Bukkit.getPlayer(args[1]);
				String birthdate = args[2].toLowerCase();
				try {
					bdate = new SimpleDateFormat(plugin.InputDateFormat).parse(birthdate);
				} catch (ParseException e) {
					sender.sendMessage(ChatColor.RED + "Invalid birthday! Please use format: " + plugin.InputDateFormat.toUpperCase());
					return true;
				}
				// Set player's birthday
				plugin.getBungee().setBirthday(player, bdate);
				String mydate = new SimpleDateFormat("dd MMM yyyy").format(bdate); 
				sender.sendMessage(ChatColor.WHITE + player.getName() + "'s" + ChatColor.YELLOW + " birthday is now set to: " + ChatColor.GREEN + mydate);
			}
			break;
		}
		case "RESET": {
			/*
			 * Reset the player's "Last gift received date"
			 */
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.reset")) { return false; }
			}

			if (args.length < 2) {
				// Not enough parameters
				sender.sendMessage(ChatColor.AQUA + "Usage: /bgift reset <player>");
			} else {
				// Fetch player info
				final OfflinePlayer player = Bukkit.getPlayer(args[1]);
				plugin.getBungee().getBirthday(player, new ResultCallback<BirthdayRecord>()
				{
					@Override
					public void onCompleted( boolean success, BirthdayRecord rec, Throwable error )
					{
						if (!success) {
							sender.sendMessage(ChatColor.RED + "An internal error occured");
							if (error != null) {
								error.printStackTrace();
							}
							return;
						}
						
						if (rec == null) {
							sender.sendMessage(ChatColor.RED + "No birthday record found for "+ ChatColor.WHITE + player.getName());
						} else {
							plugin.getBungee().resetGiftStatus(player);
							sender.sendMessage(ChatColor.YELLOW + "Last Gift Received date has been reset for " + ChatColor.WHITE + player.getName());
						}
					}
				});
			}
			break;
		}
		case "DELETE": {
			/*
			 * Completely delete the player's birthday record
			 */
			if ((sender instanceof Player)) {
				if (!plugin.RequirePermission((Player) sender, "birthdaygift.delete")) { return false; }
			}

			if (args.length < 2) {
				// Not enough parameters
				sender.sendMessage(ChatColor.AQUA + "Usage: /bgift delete <player>");
			} else {
				// Fetch player info
				
				final OfflinePlayer player = Bukkit.getPlayer(args[1]);
				plugin.getBungee().getBirthday(player, new ResultCallback<BirthdayRecord>()
				{
					@Override
					public void onCompleted( boolean success, BirthdayRecord rec, Throwable error )
					{
						if (!success) {
							sender.sendMessage(ChatColor.RED + "An internal error occured");
							if (error != null) {
								error.printStackTrace();
							}
							return;
						}
						if (rec == null) {
							sender.sendMessage(ChatColor.RED + "No birthday record found for "+ ChatColor.WHITE + player.getName());
						} else {
							plugin.getBungee().deleteBirthday(player);
							sender.sendMessage(ChatColor.YELLOW + "Birthday record for " + ChatColor.WHITE + player.getName() + ChatColor.YELLOW + " has been deleted");
						}
					}
				});
			}
			break;
		}
		default:
			/*
			 * Usage information for the command
			 */
			sender.sendMessage(ChatColor.GREEN + "Available BirthdayGift commands:");
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.info")))
				sender.sendMessage(ChatColor.AQUA + "/bgift info <player> : " + ChatColor.WHITE + "Player's birthday info");
			
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.claim")))
				sender.sendMessage(ChatColor.AQUA + "/bgift claim : " + ChatColor.WHITE + "Claim your birthday gift(s)");
			
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.stats")))
				sender.sendMessage(ChatColor.AQUA + "/bgift stats : " + ChatColor.WHITE + "Birthday stats");
			
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.set")))
				sender.sendMessage(ChatColor.AQUA + "/bgift set <player> <DD-MM-YYYY> : " + ChatColor.WHITE + "Set player's birthday");
			
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.reset")))
				sender.sendMessage(ChatColor.AQUA + "/bgift reset <player> : " + ChatColor.WHITE + "Reset 'gift received' flag");
			
			if (!(sender instanceof Player) || (plugin.HasPermission((Player) sender, "birthdaygift.delete")))
				sender.sendMessage(ChatColor.AQUA + "/bgift delete <player> : " + ChatColor.WHITE + "Delete birthday record");
		}
		return true;
	}
}
