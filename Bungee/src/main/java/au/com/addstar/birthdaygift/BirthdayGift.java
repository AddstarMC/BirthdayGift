package au.com.addstar.birthdaygift;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.md_5.bungee.api.plugin.Plugin;

public class BirthdayGift extends Plugin {
	public BirthdayGift plugin;
	public Database dbcon = null;
	public boolean DebugEnabled = false;
	private Config config;

	@Override
	public void onEnable() {
		// Register necessary events
		getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
		getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

		loadMessages();
		
		if (!getDataFolder().exists()||(!getDataFolder().mkdirs()))getLogger().warning("Could not" +
				" create Data folder");
		
		config = new Config(this);
		if (!config.load()) {
			return;
		}

		// Open/initialise the database
		dbcon = new Database(this);
		if (!dbcon.openDatabase(config)) {
			getLogger().severe("Failed to connect to database.");
			dbcon = null;
		}
	}

	@Override
	public void onDisable() {
		if (dbcon != null) {
			dbcon.close();
		}
	}

	private void loadMessages() {
		File file = new File(getDataFolder(), "messages.properties");
		try {
			if (file.exists()) {
				Messages.load(file);
			} else {
				Messages.load(getResourceAsStream("bgift_messages.properties"));
			}
		} catch (IOException e) {
			getLogger().warning(
					"An error occured while attempting to load messages (used file? "
							+ file.exists() + "):");
			e.printStackTrace();
		}
	}

	public void Debug(String data) {
		if (DebugEnabled) {
			getLogger().info("DEBUG: " + data);
		}
	}

	/*
	 * Check if it is the player's birthday today
	 */
	public boolean IsPlayerBirthday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			// Get current date without year/time
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			String today = sdf.format(new Date());
			String bdate = sdf.format(birthday.birthdayDate);

			// Check if today is the player's birthday (ignoring year)
			return bdate.equals(today);
		}
		return false;
	}

	/*
	 * Check if the player has already received a gift today
	 */
	public boolean ReceivedGiftToday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			if (birthday.lastGiftDate == null) {
				// Player has never received birthday gifts
				Debug("Never received a gift");
				return false;
			}

			// Get current date without time (annoying, right?)
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date today;
			try {
				today = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				// This should never happen!
				getLogger().warning("Unable to parse current date!");
				e.printStackTrace();
				return false;
			}

			// Check if player has received a gift today
			if (birthday.lastGiftDate.equals(today)) {
				Debug("Already receieved a gift today");
				return true;
			} else {
				Debug("Hasn't received a gift today");
				return false;
			}
		}
		return false;
	}

	/*
	 * Check if the player's birthday has already been announced (broadcast)
	 */
	public boolean AnnouncedToday(BirthdayRecord birthday) {
		// Is there a birthday record for this player?
		if (birthday != null) {
			if (birthday.lastAnnouncedDate == null) {
				// Player has never had a birthday announced
				Debug("Never had a birthday announcement");
				return false;
			}

			// Get current date without time (annoying, right?)
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date today;
			try {
				today = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				// This should never happen!
				getLogger().warning("Unable to parse current date!");
				e.printStackTrace();
				return false;
			}

			// Check if player has received a gift today
			if (birthday.lastAnnouncedDate.equals(today)) {
				Debug("Already announced birthday today");
				return true;
			} else {
				Debug("Birthday has not been announced today");
				return false;
			}
		}
		return false;
	}
}
