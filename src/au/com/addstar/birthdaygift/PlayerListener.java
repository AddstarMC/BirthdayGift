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

import java.util.Date;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
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
			plugin.Log("Today is " + player.getName() + "'s birthday!");

			// Set special join message (if set)
			if (plugin.JoinMessage != "") {
				String msg = plugin.JoinMessage.replaceAll("<PLAYER>", player.getName());
				msg = ChatColor.translateAlternateColorCodes('&', msg);
				event.setJoinMessage(msg);
			}

			// Delay the broadcast so the player sees it as the last message on their screen
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (!plugin.ReceivedGiftToday(birthday)) {
						plugin.SetGiftReceived(player.getName(), new Date());
					
						// Broadcast birthday message (if set)
						plugin.Log("Giving birthday gift(s) to " + player.getName());
						if (plugin.JoinMessage != "") {
							String msg = plugin.AnnounceMessage.replaceAll("<PLAYER>", player.getName());
							msg = ChatColor.translateAlternateColorCodes('&', msg);
							server.broadcastMessage(msg);
						}
						
						// Set special join message (if set)
						if (plugin.JoinMessage != "") {
							String msg = plugin.GiftMessage.replaceAll("<PLAYER>", player.getName());
							msg = ChatColor.translateAlternateColorCodes('&', msg);
							player.sendMessage(msg);
						}
	
						// Reward player with money (if applicable)
						if (plugin.Config().isSet("money") && plugin.Config().isInt("money")) {
							plugin.GiveMoney(player.getName(), plugin.Config().getInt("money"));
						}
						
						// Reward player with items (if applicable)
						for (int i = 0; i < plugin.RewardItems.size(); i++) {
							plugin.Debug("RewardItems["+i+"] => " + plugin.RewardItems.get(i).getType().name() + ":" + plugin.RewardItems.get(i).getData().getData());
							plugin.GiveItemStack(player, plugin.RewardItems.get(i));
						}
					}
				}
			}, 20L);
		}
	}
}
