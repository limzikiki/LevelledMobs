package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.ModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author stumper66
 * @contributors lokka30
 */
public class KillSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.kill")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        boolean useNoDrops = false;
        for (final String arg : args) {
            if ("/nodrops".equalsIgnoreCase(arg)) {
                useNoDrops = true;
                break;
            }
        }

        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "all":
                    if (!sender.hasPermission("levelledmobs.command.kill.all")) {
                        main.configUtils.sendNoPermissionMsg(sender);
                        return;
                    }

                    if (args.length == 2) {
                        if (sender instanceof Player) {
                            final Player player = (Player) sender;
                            parseKillAll(sender, Collections.singletonList(player.getWorld()), main, useNoDrops);
                        } else {
                            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.usage-console");
                            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                            messages = Utils.replaceAllInList(messages, "%label%", label);
                            messages = Utils.colorizeAllInList(messages);
                            messages.forEach(sender::sendMessage);
                        }
                    } else if (args.length == 3 || args.length == 4) {
                        if (args[2].equals("*")) {
                            parseKillAll(sender, Bukkit.getWorlds(), main, useNoDrops);
                            return;
                        }

                        if ("/nodrops".equalsIgnoreCase(args[2])) {
                            parseKillAll(sender, Bukkit.getWorlds(), main, true);
                        }
                        else {
                            World world = Bukkit.getWorld(args[2]);
                            if (world == null) {
                                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.invalid-world");
                                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                                messages = Utils.colorizeAllInList(messages);
                                messages = Utils.replaceAllInList(messages, "%world%", args[2]); //This is after the list is colourised to ensure that an input of a world name '&aGreen' will not be colourised.
                                messages.forEach(sender::sendMessage);
                                return;
                            }
                            parseKillAll(sender, Collections.singletonList(world), main, useNoDrops);
                        }
                    } else {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.usage");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%label%", label);
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                    }

                    break;
                case "near":
                    if (!sender.hasPermission("levelledmobs.command.kill.near")) {
                        main.configUtils.sendNoPermissionMsg(sender);
                        return;
                    }

                    if (args.length == 3) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            int radius;
                            try {
                                radius = Integer.parseInt(args[2]);
                            } catch (NumberFormatException exception) {
                                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius");
                                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                                messages = Utils.colorizeAllInList(messages);
                                messages = Utils.replaceAllInList(messages, "%radius%", args[2]); //After the list is colourised, so %radius% is not coloursied.
                                messages.forEach(sender::sendMessage);
                                return;
                            }

                            int maxRadius = 1000;
                            if (radius > maxRadius) {
                                radius = maxRadius;

                                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius-max");
                                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                                messages = Utils.replaceAllInList(messages, "%maxRadius%", maxRadius + "");
                                messages = Utils.colorizeAllInList(messages);
                                messages.forEach(sender::sendMessage);
                            }

                            int minRadius = 1;
                            if (radius < minRadius) {
                                radius = minRadius;

                                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius-min");
                                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                                messages = Utils.replaceAllInList(messages, "%minRadius%", minRadius + "");
                                messages = Utils.colorizeAllInList(messages);
                                messages.forEach(sender::sendMessage);
                            }

                            int killed = 0;
                            int skipped = 0;
                            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                                if (entity instanceof LivingEntity) {
                                    final LivingEntity livingEntity = (LivingEntity) entity;
                                    if (main.levelInterface.isLevelled(livingEntity)) {
                                        if (skipKillingEntity(main, livingEntity)) {
                                            skipped++;
                                        } else {
                                            if (useNoDrops)
                                                livingEntity.remove();
                                            else
                                                livingEntity.setHealth(0.0);
                                            killed++;
                                        }
                                    }
                                }
                            }

                            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.success");
                            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                            messages = Utils.replaceAllInList(messages, "%killed%", killed + "");
                            messages = Utils.replaceAllInList(messages, "%skipped%", skipped + "");
                            messages = Utils.replaceAllInList(messages, "%radius%", radius + "");
                            messages = Utils.colorizeAllInList(messages);
                            messages.forEach(sender::sendMessage);

                        } else {
                            List<String> messages = main.messagesCfg.getStringList("common.players-only");
                            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                            messages = Utils.colorizeAllInList(messages);
                            messages.forEach(sender::sendMessage);
                        }
                    } else {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.usage");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%label%", label);
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                    }

                    break;
                default:
                    sendUsageMsg(sender, label, main);
            }
        } else {
            sendUsageMsg(sender, label, main);
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args) {

        if (args.length == 2) {
            return Arrays.asList("all", "near");
        }

        if (args.length == 3 || args.length == 4) {
            if (args[1].equalsIgnoreCase("all")) {
                if (sender.hasPermission("levelledmobs.command.kill.all")) {
                    List<String> worlds = new ArrayList<>();

                    worlds.add("/nodrops");
                    if (args.length == 3 ) {
                        for (World world : Bukkit.getWorlds()) {
                            worlds.add("*");
                            if (ModalList.isEnabledInList(main.settingsCfg, "allowed-worlds-list", world.getName())) {
                                worlds.add(world.getName());
                            }
                        }
                    }

                    return worlds;
                }
            } else if (args[1].equalsIgnoreCase("near")) {
                if (sender.hasPermission("levelledmobs.command.kill.near")) {
                    return Utils.oneToNine;
                }
            }
        }

        // Nothing to suggest.
        return null;
    }

    private void sendUsageMsg(final CommandSender sender, final String label, final LevelledMobs instance) {
        List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.kill.usage");
        messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void parseKillAll(final CommandSender sender, final List<World> worlds, final LevelledMobs main, final boolean useNoDrops) {
        int killed = 0;
        int skipped = 0;

        for (World world : worlds) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    if (main.levelInterface.isLevelled(livingEntity)) {
                        if (skipKillingEntity(main, livingEntity)) {
                            skipped++;
                        } else {
                            if (useNoDrops)
                                livingEntity.remove();
                            else
                                livingEntity.setHealth(0.0);
                            killed++;
                        }
                    }
                }
            }
        }

        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.success");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%killed%", killed + "");
        messages = Utils.replaceAllInList(messages, "%skipped%", skipped + "");
        messages = Utils.replaceAllInList(messages, "%worlds%", worlds.size() + "");
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private boolean skipKillingEntity(LevelledMobs main, LivingEntity livingEntity) {

        // Nametagged
        if (livingEntity.getCustomName() != null && main.settingsCfg.getBoolean("kill-skip-conditions.nametagged"))
            return true;

        // Tamed
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && main.settingsCfg.getBoolean("kill-skip-conditions.tamed"))
            return true;

        // Leashed
        if (livingEntity.isLeashed() && main.settingsCfg.getBoolean("kill-skip-conditions.leashed")) return true;

        // Converting zombie villager
        return livingEntity.getType() == EntityType.ZOMBIE_VILLAGER && ((ZombieVillager) livingEntity).isConverting() && main.settingsCfg.getBoolean("kill-skip-conditions.convertingZombieVillager");
    }
}
