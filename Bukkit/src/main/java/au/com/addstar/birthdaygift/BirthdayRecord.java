package au.com.addstar.birthdaygift;

import java.util.Date;
import java.util.UUID;

public class BirthdayRecord {
	public final UUID playerId;

	public Date birthdayDate = null;
	public Date lastGiftDate = null;
	public Date lastAnnouncedDate = null;
	
	public BirthdayRecord(UUID id) {
		playerId = id;
	}
}