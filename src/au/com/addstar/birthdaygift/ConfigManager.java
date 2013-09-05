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

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

	private BirthdayGift plugin;
	public ConfigManager(BirthdayGift instance) {
		plugin = instance;
	}
	
	public FileConfiguration Config() {
		return plugin.getConfig();
	}
	
	public void LoadConfig(FileConfiguration config) {
		config.options().copyDefaults(true);

		plugin.DebugEnabled = Config().getBoolean("debug", false);
		plugin.USDateFormat = Config().getBoolean("us-date-format", false);
		plugin.JoinMessage = Config().getString("messages.join");
		plugin.AnnounceMessage = Config().getString("messages.announcement");
		plugin.GiftMessage = Config().getString("messages.gift");
		plugin.ClaimMessage = Config().getString("messages.claim");
		plugin.MoneyMessage = Config().getString("messages.money");

		// Validate reward items
		List<?> items = config.getList("items");
		for (int i = 0; i < items.size(); i++) {
			int amt = 1;
			int dv = 0;
			String item = (String) items.get(i);
			item = item.toUpperCase();
			
			// Split item + amount
			String[] itemparts = item.split(",");
			if (itemparts.length > 0) {
				item = itemparts[0];					// Item
			}
			if (itemparts.length > 1) {
				amt = Integer.parseInt(itemparts[1]);	// Amount
			}
			
			// Split item ID + data value
			itemparts = item.split(":");
			if (itemparts.length > 0) {
				item = itemparts[0];					// Item
			}
			if (itemparts.length > 1) {
				dv = Integer.parseInt(itemparts[1]);	// Data value
			}

			Material mat = plugin.GetMaterial(item);
			if (mat != null) {
				// Material is valid so add it to the RewardList
				//RewardList.add(item);
				plugin.Debug("Found valid reward: " + item + ":" + dv + " (x " + amt + ")");
				plugin.RewardItems.add(plugin.CreateStack(mat, dv, amt));
			} else {
				// Material not valid
				plugin.Warn("Reward item \"" + item + "\" is not valid!");
			}
		}
	}
}
