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

public class CommandBirthday implements CommandExecutor {
	private BirthdayGift plugin;
	
	public CommandBirthday(BirthdayGift plugin) {
		this.plugin = plugin;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("birthday")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry, only in game players can use this command");
			} else {
				Player player = (Player) sender;
				BirthdayRecord birthday = plugin.getPlayerRecord(sender.getName());
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
						if (plugin.IsPlayerBirthday(rec)) {
							// Don't allow players to set the birthday to today
							player.sendMessage(ChatColor.RED + "Sorry, you cannot set your birthday to today.");
							return true;
						} else {
							// Set player's birthday
							plugin.SetPlayerBirthday(player.getName(), bdate);
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
}
