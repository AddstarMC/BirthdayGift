package au.com.addstar.birthdaygift;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;

import au.com.addstar.birthdaygift.BirthdayGift.*;

public class PlayerListener implements Listener {
	
	private BirthdayGift plugin;
	public PlayerListener(BirthdayGift instance) {
		plugin = instance;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final Server server = player.getServer();
		
		final BirthdayRecord birthday = plugin.getPlayerRecord(player.getName());
		if (plugin.IsPlayerBirthday(birthday)) {
			event.setJoinMessage(ChatColor.YELLOW + player.getName() + " joined the game.. and it's their " + ChatColor.UNDERLINE + ChatColor.RED + "birthday" + ChatColor.RESET + ChatColor.YELLOW + " today!");
			plugin.Log("It's " + player.getName() + "'s birthday today!");
			
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (!plugin.ReceivedGiftToday(birthday)) {
						plugin.SetGiftReceived(player.getName());
					
						plugin.Log("Giving birthday gift(s) to " + player.getName());
						server.broadcastMessage(ChatColor.AQUA + "-=-=-=- Please wish a " + ChatColor.BLUE + "Happy Birthday" + ChatColor.AQUA + " to " + player.getName() + " -=-=-=-");
						player.sendMessage(ChatColor.LIGHT_PURPLE + "We hope you like your birthday gifts (and money)!!");
						
						plugin.GiveMoney(player.getName(), 1000);
						plugin.GiveItem(player, Material.CAKE, 1);
						plugin.GiveItem(player, Material.COOKIE, 5);
						plugin.GiveItem(player, Material.DIAMOND, 5);
						plugin.GiveItem(player, Material.EMERALD, 5);
						plugin.GiveItem(player, Material.BEACON, 1);
					}
				}
			}, 20L);
		}
	}
}
