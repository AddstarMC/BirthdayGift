package au.com.addstar.birthdaygift;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.Iterables;

import au.com.addstar.birthdaygift.BirthdayGift.BirthdayRecord;
import au.com.addstar.birthdaygift.BirthdayGift.BirthdayStats;

public class Bungee implements PluginMessageListener
{
	public static final String CHANNEL = "BGift";
	
	private BirthdayGift plugin;
	
	private List<WaitingHandler<BirthdayRecord, String>> mGetBirthdayWaiters;
	private List<WaitingHandler<BirthdayStats, Void>> mGetStatsWaiters;
	private List<WaitingHandler<Boolean, String>> mClaimWaiters;
	
	public Bungee(BirthdayGift plugin) {
		this.plugin = plugin;
		Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
	}
	
	@Override
	public void onPluginMessageReceived( String channel, Player player, byte[] message )
	{
		if (!channel.equals(CHANNEL)) {
			return;
		}
		
		ByteArrayInputStream stream = new ByteArrayInputStream(message);
		DataInputStream in = new DataInputStream(stream);
		
		String subType = null;
		try {
			subType = in.readUTF();
			
			switch(subType) {
			case "^Get":
				onGetBirthday(in);
				break;
			case "^Claim":
				onClaimGifts(in);
				break;
			case "^Stats":
				onGetStats(in);
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
	
	private void onGetBirthday(DataInputStream in) throws IOException {
		BirthdayRecord record = null;
		String playerName = in.readUTF();
		
		if (in.readBoolean()) {
			record = new BirthdayRecord();
			record.playerName = playerName;
			record.birthdayDate = new Date(in.readLong());
			long claimDate = in.readLong();
			if (claimDate != 0) {
				record.lastGiftDate = new Date(claimDate);
			}
		}
		
		// Find who asked for this and removed expired entries
		Iterator<WaitingHandler<BirthdayRecord, String>> it = mGetBirthdayWaiters.iterator();
		while(it.hasNext()) {
			WaitingHandler<BirthdayRecord, String> handler = it.next();
			
			if (handler.isExpired()) {
				handler.callback.onCompleted(false, null, new TimeoutException());
				it.remove();
			} else if (handler.value.equals(playerName)) {
				handler.callback.onCompleted(true, record, null);
				it.remove();
			}
		}
	}
	
	private void onClaimGifts(DataInputStream in) throws IOException {
		String playerName = in.readUTF();
		boolean success = in.readBoolean();
		
		// Find who asked for this and removed expired entries
		Iterator<WaitingHandler<Boolean, String>> it = mClaimWaiters.iterator();
		while(it.hasNext()) {
			WaitingHandler<Boolean, String> handler = it.next();
			
			if (handler.isExpired()) {
				handler.callback.onCompleted(false, null, new TimeoutException());
				it.remove();
			} else if (handler.value.equals(playerName)) {
				handler.callback.onCompleted(true, success, null);
				it.remove();
			}
		}
	}
	
	private void onGetStats(DataInputStream in) throws IOException {
		BirthdayStats stats = new BirthdayStats();
		stats.TotalBirthdays = in.readInt();
		stats.MonthBirthdays = in.readInt();
		stats.ClaimedGiftsThisYear = in.readInt();
		stats.UnclaimedGiftsThisYear = in.readInt();
		stats.NextBirthdayDate = new Date(in.readLong());
		stats.NextBirthdayPlayer = in.readUTF();
		
		// Find who asked for this and removed expired entries
		Iterator<WaitingHandler<BirthdayStats, Void>> it = mGetStatsWaiters.iterator();
		while(it.hasNext()) {
			WaitingHandler<BirthdayStats, Void> handler = it.next();
			
			if (handler.isExpired()) {
				handler.callback.onCompleted(false, null, new TimeoutException());
				it.remove();
			} else {
				handler.callback.onCompleted(true, stats, null);
				it.remove();
			}
		}
	}
	
	public void setBirthday(OfflinePlayer player, Date date) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("Set");
		out.writeUTF(player.getName());
		out.writeLong(date.getTime());
		
		send(out);
	}
	
	public void getBirthday(OfflinePlayer player, ResultCallback<BirthdayRecord> callback) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("Get");
		out.writeUTF(player.getName());
		
		send(out);
		
		mGetBirthdayWaiters.add(new WaitingHandler<BirthdayRecord, String>(callback, player.getName()));
	}
	
	public void getStats(ResultCallback<BirthdayStats> callback) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("Stats");
		
		send(out);
		
		mGetStatsWaiters.add(new WaitingHandler<BirthdayStats, Void>(callback, null));
	}
	
	public void claimGift(Player player, ResultCallback<Boolean> callback) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("Claim");
		out.writeUTF(player.getName());
		
		send(out);
		
		mClaimWaiters.add(new WaitingHandler<Boolean, String>(callback, player.getName()));
	}
	
	public void resetGiftStatus(OfflinePlayer player) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("ResetClaim");
		out.writeUTF(player.getName());
		
		send(out);
	}
	
	public void deleteBirthday(OfflinePlayer player) {
		ByteOutput out = new ByteOutput();
		out.writeUTF("Del");
		out.writeUTF(player.getName());
		
		send(out);
	}
	
	private void send(ByteOutput out) {
		Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
		if (player == null) {
			throw new IllegalStateException("Unable to send message, no players are online on this server");
		}
		
		player.sendPluginMessage(plugin, CHANNEL, out.toBytes());
	}
	
	private static class WaitingHandler<T,V> {
		public final long since;
		public ResultCallback<T> callback;
		public final V value;
		
		public WaitingHandler(ResultCallback<T> callback, V value) {
			since = System.currentTimeMillis();
			this.callback = callback;
			this.value = value;
		}
		
		public boolean isExpired() {
			return System.currentTimeMillis() - since > 5000;
		}
	}
}
