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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.monolith.lookup.PlayerDefinition;

public class CommandBirthday implements CommandExecutor {
	private BirthdayGift plugin;
	
	public CommandBirthday(BirthdayGift plugin) {
		this.plugin = plugin;
	}
	
	/*
	 * Handle the /birthday command
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, final String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Sorry, only in game players can use this command");
			return true;
		}
		
		final Player player = (Player) sender;
		final PlayerDefinition def = new PlayerDefinition(player.getUniqueId(), null);
		plugin.getBungee().getBirthday(def, new ResultCallback<BirthdayRecord>()
		{
			@Override
			public void onCompleted( boolean success, BirthdayRecord birthday, Throwable error )
			{
				if (!success) {
					player.sendMessage(ChatColor.RED + "An internal error occured");
					if (error != null) {
						error.printStackTrace();
					}
				} else {

					String currentBirthday = "?";
					if ((birthday != null) && (birthday.birthdayDate != null)) {
						currentBirthday = new SimpleDateFormat("dd MMM yyyy").format(birthday.birthdayDate);
					}

					if (args.length == 0) {
						// Display player's birthday
						if ((birthday == null) || (birthday.birthdayDate == null)) {
							player.sendMessage(ChatColor.RED + "You have not set your birthday yet.");
							if (plugin.USDateFormat) {
								player.sendMessage(ChatColor.YELLOW + "Usage: /birthday MM-DD-YYYY    " + ChatColor.WHITE + "(eg. /birthday 12-31-2001)");
							} else {
								player.sendMessage(ChatColor.YELLOW + "Usage: /birthday DD-MM-YYYY    " + ChatColor.WHITE + "(eg. /birthday 31-12-2001)");
							}
						} else {
							player.sendMessage(ChatColor.YELLOW + "Your birthday is currently set to: " + ChatColor.GREEN + currentBirthday);
						}
					} else {

						if (birthday != null) {
							// Don't allow players to change their birthday once it's set
							player.sendMessage(ChatColor.RED + "Sorry, you cannot change your birthday (" + currentBirthday + ")");
							return;
						}

						if (args.length != 1) {
							// Didn't supply just one argument
							InformInvalidFormat(player);
							return;
						}

						BirthdayParser parser = new BirthdayParser(plugin.USDateFormat, plugin.InputDateFormat);
						if (!parser.ParseBirthday(args[0], true))
						{
							String errorMessage = parser.ErrorMessage;
							if (errorMessage == null || errorMessage.length() == 0)
								InformInvalidFormat(player);
							else
								player.sendMessage(ChatColor.RED + parser.ErrorMessage);

							return;
						}

						Date bdate = parser.ParsedBirthday;


						// Player's birthday is not set
						// Validate the year

						BirthdayRecord rec = new BirthdayRecord(player.getUniqueId());
						rec.birthdayDate = bdate;
						if (plugin.IsPlayerBirthday(rec)) {
							// Don't allow players to set the birthday to today
							player.sendMessage(ChatColor.RED + "Sorry, you cannot set your birthday to today.");
						} else {
							// Set player's birthday
							plugin.getBungee().setBirthday(def, bdate);
							String mydate = new SimpleDateFormat("dd MMM yyyy").format(bdate);
							player.sendMessage(ChatColor.YELLOW + "Your birthday is now set to: " + ChatColor.GREEN + mydate);
						}
					}
				}
			}
		});
		return true;
	}

	private void InformInvalidFormat(Player player)
	{
		player.sendMessage(ChatColor.RED + "Invalid birthday! Please use format: " + plugin.InputDateFormat.toUpperCase());
	}

}
