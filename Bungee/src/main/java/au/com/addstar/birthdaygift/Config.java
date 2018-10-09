package au.com.addstar.birthdaygift;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

public class Config {

	private BirthdayGift plugin;
	private Properties config;
	
	public Config(BirthdayGift instance) {
		plugin = instance;
	}
	
	boolean load() {
		File configFile = new File(plugin.getDataFolder(), "config.properties");
		// Copy the default config over
		if (!configFile.exists()) {
			try {
				ByteSource src = new ByteSource() {
					@Override
					public InputStream openStream() {
						return plugin.getResourceAsStream("config.properties");
					}
				};
				
				src.copyTo(Files.asByteSink(configFile));
			} catch (IOException e) {
				plugin.getLogger().severe("Failed to copy default config to the plugin folder. Birthday Gift will be unable to run");
				e.printStackTrace();
				return false;
			}
		}
		
		config = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream(configFile);
			config.load(in);
			plugin.DebugEnabled = getBoolean("debug", false);
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to load config:");
			e.printStackTrace();
			return false;
		} finally {
			Closeables.closeQuietly(in);
		}
		
		return true;
	}
	
	 boolean getBoolean(String key, boolean def) {
		if (config.containsKey(key)) {
			return Boolean.parseBoolean(config.getProperty(key));
		} else {
			return def;
		}
	}
	
	int getInt(String key, int def) {
		if (config.containsKey(key)) {
			try {
				return Integer.parseInt(config.getProperty(key));
			} catch (NumberFormatException e) {
				return def;
			}
		} else {
			return def;
		}
	}
	
	String getString(String key, String def) {
		return config.getProperty(key, def);
	}
}
