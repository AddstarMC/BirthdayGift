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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.PlayerDefinition;
import org.bukkit.inventory.ItemStack;

/**
 * The type Command birthday gift.
 */
public class CommandBirthdayGift implements CommandExecutor {
    private BirthdayGift plugin;
    
    /**
     * Instantiates a new Command birthday gift.
     *
     * @param plugin the plugin
     */
    public CommandBirthdayGift(BirthdayGift plugin) {
        this.plugin = plugin;
    }
    
    public boolean onCommand(final CommandSender sender, Command cmd, String commandLabel, final String[] args) {
        // Check if player can do this (console always can)
        
        try {
            String action = "";
            
            if (args.length > 0) {
                action = args[0].toUpperCase();
            }
            
            // No parameter given or HELP
            switch (action) {
                case "INFO": {
                    /*
                     * Show the birthday record for the given player
                     */
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.info")) {
                            return false;
                        }
                    }
                    
                    if (args.length < 2) {
                        // Not enough parameters
                        sender.sendMessage(ChatColor.AQUA + "Usage: /bgift info <player>");
                    } else {
                        // Fetch player info
                        Lookup.lookupPlayerName(args[1], (success, player, error) -> {
                            if (!success) {
                                processError(error, sender);
                                return;
                            }
                            plugin.getBungee().getBirthday(player, (success1, rec, error1) -> {
                                if (!success1) {
                                    processError(error1,sender);
                                    return;
                                }
                                if (rec == null) {
                                    sender.sendMessage(ChatColor.RED + "No birthday record found for " + ChatColor.WHITE + player.getName());
                                } else {
                                    String bdate = "";
                                    String gdate = "Never";
                                    if (rec.birthdayDate != null) {
                                        bdate = new SimpleDateFormat("dd MMM yyyy").format(rec.birthdayDate);
                                    }
                                    if (rec.lastGiftDate != null) {
                                        gdate = new SimpleDateFormat("dd MMM yyyy").format(rec.lastGiftDate);
                                    }
                                    
                                    // Calculate age
                                    int age = plugin.getAge(rec.birthdayDate);
                                    
                                    sender.sendMessage(ChatColor.YELLOW + "Birthday Date: " + ChatColor.WHITE + bdate + " (age " + age + ")");
                                    sender.sendMessage(ChatColor.YELLOW + "Last Gift Received On: " + ChatColor.WHITE + gdate);
                                }
                            });
                        });
                    }
                    break;
                }
                case "CLAIM":
                    /*
                     * Claim any waiting birthday gift(s)
                     */
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.claim")) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.noClaimMessage));
                            return true;
                        }
                    } else {
                        sender.sendMessage("Sorry, only in-game players can use this command");
                        return false;
                    }
                    
                    final Player player = (Player) sender;
                    PlayerDefinition def = new PlayerDefinition(player.getUniqueId(), null);
                    
                    // Fetch player info
                    plugin.getBungee().getBirthday(def, (success, rec, error) -> {
                        if (!success) {
                            processError(error,sender);
                        }
                        
                        // Check birthday record
                        if (rec == null) {
                            // Can't claim if you didn't set your birthdate!
                            sender.sendMessage(ChatColor.RED + "Sorry, there are no birthday gifts available for you.");
                        } else {
                            // Birthday has been set
                            if (plugin.IsPlayerBirthday(rec)) {
                                plugin.getBungee().claimGift(player, (success12, claimed, error12) -> {
                                    if (!success12) {
                                        sender.sendMessage(ChatColor.RED + "An internal error occured");
                                        if (error12 != null) {
                                            error12.printStackTrace();
                                        }
                                        return;
                                    }
                                    
                                    if (claimed) {
                                        // It's your birthday and you haven't received a gift yet!! :D
                                        
                                        if (!plugin.giftMessage.isEmpty()) {
                                            String msg = plugin.giftMessage.replaceAll("<PLAYER>", sender.getName());
                                            msg = ChatColor.translateAlternateColorCodes('&', msg);
                                            sender.sendMessage(msg);
                                        }
                                        
                                        // Reward player with items (if applicable)
                                        plugin.Log("Giving birthday gift(s) to " + sender.getName());
                                        for (int i = 0; i < plugin.rewardItems.size(); i++) {
                                            plugin.Debug("rewardItems[" + i + "] => " + plugin.rewardItems.get(i).toString());
                                            plugin.GiveItemStack(player, plugin.rewardItems.get(i));
                                        }
                                        
                                        // Reward player with money (if applicable)
                                        if (plugin.config().isSet("money") && plugin.config().isInt("money")) {
                                            if (!plugin.moneyMessage.isEmpty()) {
                                                String msg = plugin.moneyMessage.replaceAll("<PLAYER>", player.getDisplayName());
                                                msg = msg.replaceAll("<MONEY>",
                                                        plugin.config().getString("money"));
                                                msg = ChatColor.translateAlternateColorCodes('&', msg);
                                                sender.sendMessage(msg);
                                            }
                                            plugin.GiveMoney(player, plugin.config().getInt(
                                                    "money"));
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "Sorry, you have already claimed your gift today.");
                                    }
                                });
                            } else {
                                sender.sendMessage(ChatColor.RED + "Sorry, it is not your birthday today.");
                            }
                        }
                    });
                    break;
                case "STATS":
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.stats")) {
                            return false;
                        }
                    }
                    
                    plugin.getBungee().getStats((success, stats, error) -> {
                        if (!success) {
                            processError(error,sender);
                        }
                        
                        sender.sendMessage(ChatColor.GREEN + "BirthdayGift Statistics:");
                        sender.sendMessage(ChatColor.YELLOW + "Total birthdays: " + ChatColor.WHITE + stats.TotalBirthdays);
                        sender.sendMessage(ChatColor.YELLOW + "Gifts claimed this year: " + ChatColor.WHITE + stats.ClaimedGiftsThisYear);
                        sender.sendMessage(ChatColor.YELLOW + "Gifts unclaimed this year: " + ChatColor.WHITE + stats.UnclaimedGiftsThisYear);
                        sender.sendMessage(ChatColor.YELLOW + "Birthdays this month: " + ChatColor.WHITE + stats.MonthBirthdays);
                    });
                    
                    break;
                case "SET":
                    /*
                     * Set the player's birthdate (overrides any existing record and CAN be set to today)
                     */
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.set")) {
                            return false;
                        }
                    }
                    
                    if (args.length < 3) {
                        // Not enough parameters
                        sender.sendMessage(ChatColor.AQUA + "Usage: /bgift set <player> <" + plugin.inputDateFormat.toUpperCase() + ">");
                    } else {
                        // Set player's birthday
                        Lookup.lookupPlayerName(args[1], (success, player2, error) -> {
                            if (!success) {
                                processError(error, sender);
                                return;
                            }
                            String birthdate = args[2].toLowerCase();
                            
                            BirthdayParser parser = new BirthdayParser(plugin.USDateFormat, plugin.inputDateFormat);
                            
                            if (!parser.parseBirthday(birthdate, false)) {
                                String errorMessage = parser.ErrorMessage;
                                if (errorMessage == null || errorMessage.length() == 0)
                                    sender.sendMessage(ChatColor.RED + "Invalid birthday! Please use format: " + plugin.inputDateFormat.toUpperCase());
                                else
                                    sender.sendMessage(ChatColor.RED + parser.ErrorMessage);
                                
                                return;
                            }
                            
                            Date bdate = parser.ParsedBirthday;
                            
                            // Set player's birthday
                            plugin.getBungee().setBirthday(player2, bdate);
                            String mydate = new SimpleDateFormat("dd MMM yyyy").format(bdate);
                            sender.sendMessage(ChatColor.WHITE + player2.getName() + "'s" + ChatColor.YELLOW + " birthday is now set to: " + ChatColor.GREEN + mydate);
                        });
                    }
                    break;
                
                case "RESET":
                    /*
                     * Reset the player's "Last gift received date"
                     */
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.reset")) {
                            return false;
                        }
                    }
                    
                    if (args.length < 2) {
                        // Not enough parameters
                        sender.sendMessage(ChatColor.AQUA + "Usage: /bgift reset <player>");
                    } else {
                        // Fetch player info
                        Lookup.lookupPlayerName(args[1], (success, player3, error) -> {
                            if (!success) {
                                processError(error, sender);
                                return;
                            }
                            plugin.getBungee().getBirthday(player3, (success13, rec, error13) -> {
                                if (!success13) {
                                    processError(error13,sender);
                                }
                                
                                if (rec == null) {
                                    sender.sendMessage(ChatColor.RED + "No birthday record found " +
                                            "for " + ChatColor.WHITE + player3.getName());
                                } else {
                                    plugin.getBungee().resetGiftStatus(player3);
                                    sender.sendMessage(ChatColor.YELLOW + "Last Gift Received " +
                                            "date has been reset for " + ChatColor.WHITE + player3.getName());
                                }
                            });
                        });
                    }
                    break;
                
                case "DELETE":
                    if ((sender instanceof Player)) {
                        if (!plugin.hasPermission((Player) sender, "birthdaygift.delete")) {
                            return false;
                        }
                    }
                    
                    if (args.length < 2) {
                        // Not enough parameters
                        sender.sendMessage(ChatColor.AQUA + "Usage: /bgift delete <player>");
                    } else {
                        // Fetch player info
                        
                        Lookup.lookupPlayerName(args[1], (success, player4, error) -> {
                            if (!success) {
                                processError(error, sender);
                                return;
                            }
                            plugin.getBungee().getBirthday(player4, (success14, rec, error14) -> {
                                if (!success14) {
                                    if (error14 instanceof TimeoutException) {
                                        sender.sendMessage(ChatColor.RED + "Unable to communicate with Bungee. Query timed out");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "An internal error occured");
                                        if (error14 != null) {
                                            error14.printStackTrace();
                                        }
                                    }
                                }
                                if (rec == null) {
                                    sender.sendMessage(ChatColor.RED + "No birthday record found " +
                                            "for " + ChatColor.WHITE + player4.getName());
                                } else {
                                    plugin.getBungee().deleteBirthday(player4);
                                    sender.sendMessage(ChatColor.YELLOW + "Birthday record for " + ChatColor.WHITE + player4.getName() + ChatColor.YELLOW + " has been deleted");
                                }
                            });
                        });
                    }
                    break;
                
                case "ADDREWARD":
                    if (sender instanceof Player && !plugin.hasPermission((Player) sender,
                            "birthdaygift.reward.add")) {
                        return false;
                    }
                    if(!(sender instanceof  Player)) {
                        sender.sendMessage("Must be run in game with item in hand");
                        return false;
                    }
                    ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                    plugin.rewardItems.add(item);
                    plugin.cfg.saveConfig();
                    sender.sendMessage("[BGIFT] "+ item.getType().name()+ " has been added to " +
                            "the reward list.");
                    listRewards(sender);
                case "LISTREWARD":
                    if (sender instanceof Player && !plugin.hasPermission((Player) sender,
                        "birthdaygift.reward.list")) {
                            return false;
                        }
                    listRewards(sender);
                    break;
                case "DELETEREWARD":
                    if(sender instanceof Player && !plugin.hasPermission((Player) sender,
                            "birthdaygift.reward.delete")) {
                            return false;
                    }
                    if(args.length < 2){
                        sender.sendMessage("USAGE: /bgift DELETEREWARD <index>");
                        return false;
                    }
                    int index = Integer.parseInt(args[1]);
                    if(index < 1)index = 1;
                    ItemStack stack =  plugin.rewardItems.remove(index-1);
                    plugin.cfg.saveConfig();
                    sender.sendMessage("[BGIFT] Removed " +stack.getType().name());
                    listRewards(sender);
                    break;
                default:
                    /*
                     * Usage information for the command
                     */
                    sender.sendMessage(ChatColor.GREEN + "Available BirthdayGift commands:");
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.info")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift info <player> : " + ChatColor.WHITE + "Player's birthday info");
                    
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.claim")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift claim : " + ChatColor.WHITE + "Claim your birthday gift(s)");
                    
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.stats")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift stats : " + ChatColor.WHITE + "Birthday stats");
                    
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.set")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift set <player> <DD-MM-YYYY> : " + ChatColor.WHITE + "Set player's birthday");
                    
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.reset")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift reset <player> : " + ChatColor.WHITE + "Reset 'gift received' flag");
                    
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender, "birthdaygift.delete")))
                        sender.sendMessage(ChatColor.AQUA + "/bgift delete <player> : " + ChatColor.WHITE + "Delete birthday record");
                    if (!(sender instanceof Player) || (plugin.hasPerm((Player) sender,
                            "birthdaygift.reward"))) {
                        sender.sendMessage(ChatColor.AQUA + "/bgift addreward : " + ChatColor.WHITE + "Add custom item reward held in main hand");
                        sender.sendMessage(ChatColor.AQUA + "/bgift listreward : " + ChatColor.WHITE +
                                "List Rewards");
                        sender.sendMessage(ChatColor.AQUA + "/bgift deletereward <index> : " + ChatColor.WHITE +
                                "Delete the <index>  item from the list | index is always 1 or " +
                                "more.");
                    }
            }
        } catch (IllegalStateException e) {
            sender.sendMessage(ChatColor.RED + "Unable to communicate with Bungee as no players are on this server.");
        }
        return true;
    }
    
    private void processError(Throwable error, CommandSender sender) {
        if (error instanceof TimeoutException) {
            sender.sendMessage(ChatColor.RED + "Unable to communicate with Bungee. Query timed out");
        } else if (error != null) {
            sender.sendMessage(ChatColor.RED + "An internal error occured");
            error.printStackTrace();
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown player ?");
        }
    }
    private void listRewards(CommandSender sender){
        sender.sendMessage(ChatColor.GREEN + "----------------------");
        sender.sendMessage(ChatColor.GOLD + "[BGIFT] Current Rewards are:");
        int i = 1;
        for(ItemStack stack : plugin.rewardItems){
            sender.sendMessage("" + ChatColor.GOLD + i + ChatColor.RESET +" : " +stack.getType() +
                    "Ammount:" + stack.getAmount() + " Special info: " + stack.toString());
            i++;
        }
        sender.sendMessage(ChatColor.GREEN +"----------------------");
    }
}
