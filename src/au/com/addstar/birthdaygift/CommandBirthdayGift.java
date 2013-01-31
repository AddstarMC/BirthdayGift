package au.com.addstar.birthdaygift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandBirthdayGift implements CommandExecutor {
	private BirthdayGift plugin;
	
	public CommandBirthdayGift(BirthdayGift plugin) {
		this.plugin = plugin;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return true;
	}
}
