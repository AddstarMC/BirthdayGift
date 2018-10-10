package au.com.addstar.birthdaygift;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Date;

/**
 * Created for the AddstarMC Project. Created by Narimm on 10/10/2018.
 */
public class BirthdayEvent extends Event {
    
    private HandlerList handlers = new HandlerList();
    private final Date birthdate;
    private final Player player;
    
    public BirthdayEvent(Date birthdate, Player player) {
        this.birthdate = birthdate;
        this.player = player;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    /**
     * Retrieves the players birthday
     * @return date
     */
    public Date getBirthdate() {
        return birthdate;
    }
    
    
    /**
     * Retrieve the player
      * @return player
     */
    public Player getPlayer() {
        return player;
    }
}
