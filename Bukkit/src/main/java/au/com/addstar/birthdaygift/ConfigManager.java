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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class ConfigManager {

	private BirthdayGift plugin;
	public ConfigManager(BirthdayGift instance) {
		plugin = instance;
	}
	
	public FileConfiguration config() {
		return plugin.getConfig();
	}
	
	public void loadConfig(FileConfiguration config) {
		config.options().copyDefaults(true);
		
		plugin.debugEnabled = config().getBoolean("debug", false);
		plugin.USDateFormat = config().getBoolean("us-date-format", false);
		plugin.giftMessage = config().getString("messages.gift");
		plugin.moneyMessage = config().getString("messages.money");
		plugin.noClaimMessage = config().getString("messages.noclaimpermission");
		
		// Validate reward items
		if (config.contains("item-serialized")) {
			try {
				List<?> objects =  config.getList("item-serialized");
				for (Object object: objects ){
					if(object instanceof ItemStack){
						plugin.rewardItems.add((ItemStack) object);
					}
				}
			} catch (ClassCastException ignored) {}
		}
		if(plugin.rewardItems.size() == 0) {
			List<String> items = config.getStringList("items");
			for (String item : items) {
				int amt = 1;
				int dv = 0;
				item = item.toUpperCase();
				String[] itemparts = item.split(",");
				if (itemparts.length > 0) {
					item = itemparts[0];                    // Item
				}
				if (itemparts.length > 1) {
					amt = Integer.parseInt(itemparts[1]);    // Amount
				}
				
				// Split item ID + data value
				itemparts = item.split(":");
				if (itemparts.length > 0) {
					item = itemparts[0];                    // Item
				}
				if (itemparts.length > 1) {
					dv = Integer.parseInt(itemparts[1]);    // Data value
				}
				Material mat = Material.matchMaterial(item);
				if (mat != null) {
					// Material is valid so add it to the RewardList
					//RewardList.add(item);
					plugin.Debug("Found valid reward: " + item + ":" + dv + " (x " + amt + ")");
					plugin.rewardItems.add(plugin.CreateStack(mat, dv, amt));
				} else {
					// Material not valid
					plugin.Warn("Reward item \"" + item + "\" is not valid!");
				}
			}
		}
		
	}
	
	public void saveConfig(){
		//This updates the reward lists
		config().set("item-serialized",plugin.rewardItems);
		try {
			config().save(new File(plugin.getDataFolder(), "config.yml"));
		}catch (IOException e){
			plugin.getLogger().warning("Unable to save config.");
		}
	}
}
