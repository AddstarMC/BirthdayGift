package au.com.addstar.birthdaygift;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PluginMessageListener implements Listener {
	public static final String CHANNEL = "bungee:gift";

	private BirthdayGift plugin;

	public PluginMessageListener(BirthdayGift plugin) {
		this.plugin = plugin;
		ProxyServer.getInstance().registerChannel(CHANNEL);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPluginMessage(PluginMessageEvent event) {
		if (!event.getTag().equals(CHANNEL)	|| !(event.getSender() instanceof Server)) {
			return;
		}

		ServerInfo sender = ((Server) event.getSender()).getInfo();

		ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
		DataInputStream in = new DataInputStream(stream);
		String subType = null;

		try {
			subType = in.readUTF();

			switch (subType) {
			case "Set":
				setBirthday(UUID.fromString(in.readUTF()), in.readLong());
				break;
			case "Get":
				getBirthday(UUID.fromString(in.readUTF()), sender);
				break;
			case "Claim":
				claimGift(UUID.fromString(in.readUTF()), sender);
				break;
			case "ResetClaim":
				resetClaim(UUID.fromString(in.readUTF()));
				break;
			case "Stats":
				getStats(sender);
				break;
			case "Del":
				deleteBirthday(UUID.fromString(in.readUTF()));
				break;
			}
		} catch (IOException e) {
			if (subType != null) {
				plugin.getLogger().warning("Read error while decoding " + subType + " packet:");
			} else {
				plugin.getLogger().warning("Read error while decoding unknown packet:");
			}
			e.printStackTrace();
		} catch (Throwable e) {
			if (subType != null) {
				plugin.getLogger().warning("Error while decoding " + subType + " packet:");
			} else {
				plugin.getLogger().warning("Error while decoding unknown packet:");
			}
			e.printStackTrace();
		}
	}

	private void setBirthday(UUID id, long date) {
		plugin.dbcon.setBirthday(id, new Date(date));
	}

	private void getBirthday(UUID id, ServerInfo sender) {
		BirthdayRecord record = plugin.dbcon.getBirthday(id);
		ByteOutput out = new ByteOutput();
		out.writeUTF("^Get");
		out.writeUTF(id.toString()); // Name so we can identify the response
		
		if (record == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
			out.writeLong(record.birthdayDate.getTime());
			if (record.lastGiftDate != null) {
				out.writeLong(record.lastGiftDate.getTime());
			} else {
				out.writeLong(0);
			}
		}
		
		sender.sendData(CHANNEL, out.toBytes());
	}
	
	private void deleteBirthday(UUID id) {
		plugin.dbcon.deleteBirthday(id);
	}
	
	private void claimGift(UUID id, ServerInfo sender) {
		BirthdayRecord record = plugin.dbcon.getBirthday(id);
		ByteOutput out = new ByteOutput();
		out.writeUTF("^Claim");
		out.writeUTF(id.toString()); // Name so we can identify the response
		
		if (record == null) {
			out.writeBoolean(false);
		} else if (plugin.ReceivedGiftToday(record)) {
			out.writeBoolean(false);
		} else {
			plugin.dbcon.setGiftDate(id, new Date());
			out.writeBoolean(true);
		}
		
		sender.sendData(CHANNEL, out.toBytes());
	}
	
	private void resetClaim(UUID id) {
		plugin.dbcon.setGiftDate(id, null);
	}
	
	private void getStats(ServerInfo sender) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("^Stats");
		
		BirthdayStats stats = plugin.dbcon.getStats();
		if (stats == null) {
			return;
		}
		
		out.writeInt(stats.TotalBirthdays);
		out.writeInt(stats.MonthBirthdays);
		out.writeInt(stats.ClaimedGiftsThisYear);
		out.writeInt(stats.UnclaimedGiftsThisYear);
		
		sender.sendData(CHANNEL, out.toBytes());
	}
}
