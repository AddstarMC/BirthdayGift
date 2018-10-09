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
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.event.BCPlayerJoinEvent;

public class PlayerListener implements Listener {

	private BirthdayGift plugin;

	public PlayerListener(BirthdayGift instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(BCPlayerJoinEvent event) {
		final ProxiedPlayer player = event.getPlayer();

		// Do not do broadcasts/join messages if another plugin silenced the
		// join message (eg. VanishNoPacket)
		if (event.getJoinMessage() == null) {
			return;
		}

		final BirthdayRecord birthday = plugin.dbcon.getBirthday(player.getUniqueId());
		if (plugin.IsPlayerBirthday(birthday)) {
			plugin.getLogger().info("Today is " + player.getName() + "'s birthday!");

			// Set special join message
			String msg = Messages.Join.replaceAll("<PLAYER>",player.getDisplayName());
			msg = ChatColor.translateAlternateColorCodes('&', msg);
			event.setJoinMessage(msg);

			// Delay the broadcast so the player sees it as the last message on
			// their screen
			ProxyServer.getInstance().getScheduler()
					.schedule(plugin, () -> {
						// Broadcast birthday message (if set, and hasn't
						// already happened today)
						if (!plugin.ReceivedGiftToday(birthday)) {
							// Broadcast the announcement
							if (BungeeChat.instance.getSyncManager().getPropertyBoolean(player, "VNP:online", true) && !Messages.Announce.isEmpty()) {
								if (!plugin.AnnouncedToday(birthday)) {
									plugin.dbcon.setAnnounceDate(player.getUniqueId(), new Date());
									String msg1 = Messages.Announce.replaceAll("<PLAYER>", player.getDisplayName());
									msg1 = ChatColor.translateAlternateColorCodes('&', msg1);
									ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(msg1));
								}
							}

							// Remind player about how to claim
							String msg1 = Messages.Claim.replaceAll("<PLAYER>", player.getDisplayName());
							msg1 = ChatColor.translateAlternateColorCodes('&', msg1);
							player.sendMessage(TextComponent.fromLegacyText(msg1));
						}
					}, 1, TimeUnit.SECONDS);
		}
	}
}
