package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;

public class SummonSubcommand implements Subcommand {

    // len:    1      2        3        4       5          6            7   8     9
    // arg:    0      1        2        3       4          5            6   7     8
    // lvlmobs summon <amount> <entity> <level> here
    // lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
    // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world]

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.summon")) {
            instance.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 4) {
            sendMainUsage(sender, label, instance);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-amount");
            messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%amount%", args[1]); // This is after colorize so that args[1] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException ex) {
            List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-entity-type");
            messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%entityType%", args[2]); // This is after colorize so that args[2] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-level");
            messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%level%", args[3]); // This is after colorize so that args[3] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        SummonType summonType = SummonType.HERE;
        if (args.length > 4) {
            switch (args[4].toLowerCase()) {
                case "here":
                    summonType = SummonType.HERE;
                    break;
                case "atplayer":
                    summonType = SummonType.AT_PLAYER;
                    break;
                case "atlocation":
                    summonType = SummonType.AT_LOCATION;
                    break;
                default:
                    List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-summon-type");
                    messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%summonType%", args[4]); // This is after colorize so args[4] is not colorized.
                    messages.forEach(sender::sendMessage);
                    return;
            }
        }

        if (summonType == SummonType.HERE) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                summonMobs(instance, entityType, amount, sender, level, player.getLocation(), summonType, null);
            } else {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-summon-type-console");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.length == 6) {

                boolean offline = false;
                final Player target = Bukkit.getPlayer(args[5]);
                if (target == null) {
                    offline = true;
                } else if (sender instanceof Player) {
                    // Vanished player compatibility.
                    final Player player = (Player) sender;
                    if (!player.canSee(target) && !player.isOp()) {
                        offline = true;
                    }
                }

                if (offline) {
                    List<String> messages = instance.messagesCfg.getStringList("common.player-offline");
                    messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%player%", args[5]); // This is after colorize so that args[5] is not colorized.
                    messages.forEach(sender::sendMessage);
                    return;
                }

                summonMobs(instance, entityType, amount, sender, level, target.getLocation(), summonType, target);
            } else {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.atPlayer.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else { // At Location
            if (args.length == 8) {
                Location location = getRelativeLocation(sender, args[5], args[6], args[7], args[8]);

                if (location != null) {
                    summonMobs(instance, entityType, amount, sender, level, location, summonType, null);
                }
            } else {

            }
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {

        //TODO Cleanup

        // len:    1      2        3        4       5          6            7   8     9
        // arg:    0      1        2        3       4          5            6   9     10
        // lvlmobs summon <amount> <entity> <level> here
        // lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
        // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world]

        boolean isAtLocation = false;
        boolean isAtPlayer = false;
        if (args.length > 4) {
            if (args[4].equalsIgnoreCase("atlocation")) isAtLocation = true;
            if (args[4].equalsIgnoreCase("atplayer")) isAtPlayer = true;
        }

        if (args.length == 3) {
            return Utils.mobs;
        } else if (args.length == 2 || args.length == 4) {
            return Utils.oneToNine;
        } else if (args.length == 5) {
            return Arrays.asList("atLocation", "atPlayer", "here");
        } else if (args.length == 6) {
            if (isAtLocation) {
                return Collections.singletonList("~");
            } else if (isAtPlayer) {
                List<String> usernames = new ArrayList<>();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;

                        if (player.canSee(onlinePlayer) || player.isOp()) {
                            usernames.add(onlinePlayer.getName());
                        }
                    } else {
                        usernames.add(onlinePlayer.getName());
                    }
                }

                return usernames;
            }
        } else if ((args.length == 7 || args.length == 8) && isAtLocation) {
            return Collections.singletonList("~");
        }

        return null;
    }

    private enum SummonType {
        HERE,
        AT_PLAYER,
        AT_LOCATION
    }

    private void sendMainUsage(CommandSender sender, String label, LevelledMobs instance) {
        //TODO is atLocation accessible from console? if so, fix usage for [world] argument?

        List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.usage");
        messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void summonMobs(LevelledMobs instance, EntityType entityType, int amount, CommandSender sender, int level, Location location, SummonType summonType, Player target) {
        if (instance.levelManager.isLevellable(entityType)) {

            if (location == null || location.getWorld() == null) {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.invalid-location");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
                return;
            }

            if (amount < 1) {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.amount-limited.min");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            int maxAmount = 100; //TODO Customisable
            if (amount > maxAmount) {
                amount = maxAmount;

                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.amount-limited.max");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%maxAmount%", maxAmount + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            int minLevel = instance.configUtils.getMinLevel(entityType, location.getWorld());
            if (level < minLevel) {
                level = minLevel;

                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.min");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%minLevel%", minLevel + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            int maxLevel = instance.configUtils.getMaxLevel(entityType, location.getWorld());
            if (level > maxLevel) {
                level = maxLevel;

                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.max");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%maxLevel%", maxLevel + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            if (summonType == SummonType.HERE) {
                location = addVarianceToLocation(location);
            }

            for (int i = 0; i < amount; i++) {
                Entity entity = Objects.requireNonNull(location.getWorld()).spawnEntity(location, entityType);
                instance.levelManager.creatureSpawnListener.processMobSpawn((LivingEntity) entity, CreatureSpawnEvent.SpawnReason.CUSTOM, level);
            }

            String entityName = instance.configUtils.getEntityName(entityType);

            switch (summonType) {
                case HERE:
                    List<String> hereSuccessmessages = instance.messagesCfg.getStringList("command.levelledmobs.summon.here.success");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%prefix%", instance.configUtils.getPrefix());
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%amount%", amount + "");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%level%", level + "");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%entity%", entityName + "");
                    hereSuccessmessages = Utils.colorizeAllInList(hereSuccessmessages);
                    hereSuccessmessages.forEach(sender::sendMessage);
                    break;

                case AT_LOCATION:
                    List<String> atLocationSuccessMessages = instance.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.success");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%prefix%", instance.configUtils.getPrefix());
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%amount%", amount + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%level%", level + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%entity%", entityName + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%x%", location.getBlockX() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%y%", location.getBlockY() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%z%", location.getBlockZ() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%world%", location.getWorld().getName());
                    atLocationSuccessMessages = Utils.colorizeAllInList(atLocationSuccessMessages);
                    atLocationSuccessMessages.forEach(sender::sendMessage);
                    break;

                case AT_PLAYER:
                    List<String> atPlayerSuccessMessages = instance.messagesCfg.getStringList("command.levelledmobs.summon.atPlayer.success");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%prefix%", instance.configUtils.getPrefix());
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%amount%", amount + "");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%level%", level + "");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%entity%", entityName + "");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%player%", target.getDisplayName() + "");
                    atPlayerSuccessMessages = Utils.colorizeAllInList(atPlayerSuccessMessages);
                    atPlayerSuccessMessages.forEach(sender::sendMessage);
                    break;
                default:
                    throw new IllegalStateException("Unexpected SummonType value of " + summonType.toString() + "!");
            }
        } else {
            List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%", instance.configUtils.getEntityName(entityType));
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }
    }

    private Location getRelativeLocation(CommandSender sender, String xStr, String yStr, String zStr, String worldName) {
        double x = 0, y = 0, z = 0;

        boolean xRelative = false, yRelative = false, zRelative = false;

        if (sender instanceof Player || sender instanceof BlockCommandSender) { //Player or Command blocks
            if (xStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    x = ((Player) sender).getLocation().getX();
                } else {
                    x = ((BlockCommandSender) sender).getBlock().getX();
                }

                if (xStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(xStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    x = x + addition;
                }

                xRelative = true;
            }
            if (yStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    y = ((Player) sender).getLocation().getY();
                } else {
                    y = ((BlockCommandSender) sender).getBlock().getY();
                }

                if (yStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(yStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }

                    y = y + addition;
                }

                yRelative = true;
            }
            if (zStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    z = ((Player) sender).getLocation().getZ();
                } else {
                    z = ((BlockCommandSender) sender).getBlock().getZ();
                }

                if (zStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(zStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    z = z + addition;
                }

                zRelative = true;
            }
        }

        if (!xRelative) {
            try {
                x = Double.parseDouble(xStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!yRelative) {
            try {
                y = Double.parseDouble(yStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!zRelative) {
            try {
                z = Double.parseDouble(zStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(world, x, y, z);
    }

    private Location addVarianceToLocation(Location oldLocation) {
        double min = 0.5, max = 2.5;

        for (int i = 0; i < 20; i++) {
            //Creates 3x new Random()s for a different seed each time
            double x = min + (max - min) * new Random().nextDouble();
            double y = min + (max - min) * new Random().nextDouble();
            double z = min + (max - min) * new Random().nextDouble();

            Location newLocation = new Location(oldLocation.getWorld(), x, y, z);
            if (newLocation.getBlock().isPassable() && newLocation.add(0, 1, 0).getBlock().isPassable()) {
                return newLocation;
            }
        }

        return oldLocation;
    }
}
