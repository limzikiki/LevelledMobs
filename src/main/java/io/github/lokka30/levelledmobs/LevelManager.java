package io.github.lokka30.levelledmobs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import io.github.lokka30.levelledmobs.utils.Addition;
import io.github.lokka30.levelledmobs.utils.ModalList;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LevelManager {

    private final LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");
        isSpawnerKey = new NamespacedKey(instance, "isSpawner");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.
    public final NamespacedKey isSpawnerKey; //This is stored on levelled mobs to tell plugins that a mob was created from a spawner

    public final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("GHAST", "MAGMA_CUBE", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    public final static int maxCreeperBlastRadius = 100;
    //public final Pattern slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        // Don't level these
        if (entityType == EntityType.PLAYER || entityType == EntityType.UNKNOWN || entityType == EntityType.ARMOR_STAND)
            return false;

        // Check if the entity is blacklisted. If not, continue.
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", entityType.toString()))
            return false;

        // Check if the entity is overriden. If so, force it to be levelled.
        if (instance.settingsCfg.getStringList("overriden-entities").contains(entityType.toString())) return true;

        // These entities don't implement Monster or Boss and thus must be forced to return true
        if (forcedTypes.contains(entityType.toString())) {
            return true;
        }

        // Grab the Entity class, which is used to check for certain assignments
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) return false;

        return Monster.class.isAssignableFrom(entityClass)
                || Boss.class.isAssignableFrom(entityClass)
                || instance.settingsCfg.getBoolean("level-passive");
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity livingEntity) {

        // Ignore these entity types and metadatas
        if (livingEntity.getType() == EntityType.PLAYER
                || livingEntity.getType() == EntityType.UNKNOWN
                || livingEntity.getType() == EntityType.ARMOR_STAND

                // Citizens plugin compatibility
                || livingEntity.hasMetadata("NPC")

                // Shopkeepers plugin compatibility
                || livingEntity.hasMetadata("shopkeeper")

                // EliteMobs plugin compatibility
                || livingEntity.hasMetadata("Elitemob") && !instance.settingsCfg.getBoolean("allow-elite-mobs")
                || livingEntity.hasMetadata("Supermob") && !instance.settingsCfg.getBoolean("allow-super-mobs")

                //InfernalMobs plugin compatibility)
                || livingEntity.hasMetadata("infernalMetadata") && !instance.settingsCfg.getBoolean("allow-infernal-mobs")) {
            return false;
        }

        // Check 'no level conditions'
        if (livingEntity.getCustomName() != null && instance.settingsCfg.getBoolean("no-level-conditions.nametagged")) {
            return false;
        }
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && instance.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            return false;
        }

        // Check WorldGuard flag.
        if (instance.hasWorldGuardInstalled && !instance.worldGuardManager.regionAllowsLevelling(livingEntity))
            return false;

        // Check for overrides
        if (instance.settingsCfg.getStringList("overriden-entities").contains(livingEntity.getType().toString()))
            return true;

        //Check allowed entities for normal entity types
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", livingEntity.getType().toString()))
            return false;

        // Specific allowed entities check for BABY_ZOMBIE
        if (livingEntity instanceof Zombie && Utils.isZombieBaby((Zombie) livingEntity)) {
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", "BABY_ZOMBIE"))
                return false;
        }

        return isLevellable(livingEntity.getType());
    }

    public void updateNametagWithDelay(final LivingEntity livingEntity, final String nametag, final List<Player> players, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, nametag, players);
            }
        }.runTaskLater(instance, delay);
    }

    public void updateNametagWithDelay(final LivingEntity livingEntity, final List<Player> players, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, getNametag(livingEntity), players);
            }
        }.runTaskLater(instance, delay);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void getLevelledItemDrops(final LivingEntity livingEntity, final List<ItemStack> currentDrops) {

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "1: Method called. " + currentDrops.size() + " drops will be analysed.");

        // Must be a levelled mob
        if (!livingEntity.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING))
            return;

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "2: LivingEntity is a levelled mob.");

        // Get their level
        final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER));
        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "3: Entity level is " + level + ".");

        final boolean doNotMultiplyDrops = instance.noDropMultiplierEntities.contains(livingEntity.getName());

        if (!doNotMultiplyDrops) {
            // Get currentDrops added per level value
            final int addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                    .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "4: Item drop addition is +" + addition + ".");

            // Modify current drops
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "5: Scanning " + currentDrops.size() + " items...");
            for (ItemStack currentDrop : currentDrops) {
                Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "6: Scanning drop " + currentDrop.getType().toString() + " with current amount " + currentDrop.getAmount() + "...");

                if (instance.mobDataManager.isLevelledDropManaged(livingEntity.getType(), currentDrop.getType())) {
                    currentDrop.setAmount(currentDrop.getAmount() + (currentDrop.getAmount() * addition));
                    Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was managed. New amount: " + currentDrop.getAmount() + ".");
                } else {
                    Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was unmanaged.");
                }
            }
        }

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs")){
            getCustomItemDrops(livingEntity, level, currentDrops, true);
        }
    }

    public void getCustomItemDrops(final LivingEntity livingEntity, final int level, final List<ItemStack> drops, final boolean isLevellable){

        final int preCount = drops.size();
        final List<CustomDropsUniversalGroups> applicableGroups = getApllicableGroupsForMob(livingEntity, isLevellable);
        final boolean isSpawner = livingEntity.getPersistentDataContainer().has(isSpawnerKey, PersistentDataType.STRING);

        for (final CustomDropsUniversalGroups group : applicableGroups){
            if (!instance.customDropsitems_groups.containsKey(group)) continue;

            getCustomItemDrops2(livingEntity, level, instance.customDropsitems_groups.get(group), drops, isSpawner);
        }

        if (instance.customDropsitems.containsKey(livingEntity.getType())){
            getCustomItemDrops2(livingEntity, level, instance.customDropsitems.get(livingEntity.getType()), drops, isSpawner);
        }

        final int postCount = drops.size();

        if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            ArrayList<String> applicableGroupsNames = new ArrayList<>();
            applicableGroups.forEach(applicableGroup -> applicableGroupsNames.add(applicableGroup.toString()));

            Utils.logger.info("&7Custom drops for " + livingEntity.getName());
            Utils.logger.info("&8- &7Groups: &b" + String.join("&7, &b", applicableGroupsNames) + "&7.");
            Utils.logger.info(String.format("&8 --- &7Precount: &b%s&7, postcount: &b%s&7.", preCount, postCount));
        }
    }

    private void getCustomItemDrops2(final LivingEntity livingEntity, final int level, final List<CustomItemDrop> customDrops, final List<ItemStack> newDrops, final boolean isSpawner){

        for (final CustomItemDrop drop : customDrops){
            boolean doDrop = true;
            if (drop.maxLevel > -1 && level > drop.maxLevel) doDrop = false;
            if (drop.minLevel > -1 && level < drop.minLevel) doDrop = false;
            if (drop.noSpawner && isSpawner)  doDrop = false;
            if (!doDrop){
                if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format("&8- &7Mob: &b%s&7, level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                            livingEntity.getName(), level, isSpawner, drop.getMaterial().name(), drop.minLevel, drop.maxLevel, drop.noSpawner));
                }
                continue;
            }

            int newDropAmount = drop.amount;
            if (drop.getHasAmountRange()){
                final int change = ThreadLocalRandom.current().nextInt(0, drop.getamountRangeMax() - drop.getamountRangeMin() + 1);
                newDropAmount = drop.getamountRangeMin() + change;
            }

            boolean didNotMakeChance = false;
            double chanceRole = 0.0;

            if (drop.dropChance < 1.0){
                if (!drop.noMultiplier) {
                    final int addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                            .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
                    newDropAmount = newDropAmount + (newDropAmount * addition);
                }

                chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
                if (1.0 - chanceRole >= drop.dropChance) didNotMakeChance = true;
            }

            if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                Utils.logger.info(String.format(
                        "&8 - &7Mob: &b%s&7, item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &b%s&7.",
                        livingEntity.getName(), drop.getMaterial().name(), drop.getAmountAsString(), newDropAmount, drop.dropChance, chanceRole, !didNotMakeChance)
                );
            }
            if (didNotMakeChance) continue;

            // if we made it this far then the item will be dropped

            ItemStack newItem = drop.getItemStack();
            newItem.setAmount(newDropAmount);

            newDrops.add(newItem);
        }
    }

    @Nonnull
    private List<CustomDropsUniversalGroups> getApllicableGroupsForMob(final LivingEntity le, final boolean isLevelled){
        List<CustomDropsUniversalGroups> groups = new ArrayList<>();
        groups.add(CustomDropsUniversalGroups.ALL_MOBS);

        if (isLevelled) groups.add(CustomDropsUniversalGroups.ALL_LEVELLABLE_MOBS);
        final EntityType eType = le.getType();

        if (le instanceof Monster || le instanceof Boss || instance.groups_HostileMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_HOSTILE_MOBS);
        }

        if (le instanceof WaterMob || instance.groups_AquaticMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (le.getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            groups.add(CustomDropsUniversalGroups.ALL_OVERWORLD_MOBS);
        }
        else if (le.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            groups.add(CustomDropsUniversalGroups.ALL_NETHER_MOBS);
        }

        if (le instanceof Flying || eType.equals(EntityType.PARROT) || eType.equals(EntityType.BAT)){
            groups.add(CustomDropsUniversalGroups.ALL_FLYING_MOBS);
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(le instanceof Flying) && !(le instanceof WaterMob) && !(le instanceof Boss) && !(eType.equals(EntityType.BAT))){
            groups.add(CustomDropsUniversalGroups.ALL_GROUND_MOBS);
        }

        if (le instanceof WaterMob || instance.groups_AquaticMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (le instanceof Animals || le instanceof WaterMob || instance.groups_PassiveMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_PASSIVE_MOBS);
        }

        return groups;
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int getLevelledExpDrops(final LivingEntity ent, final int xp) {
        if (ent.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING)) {
            final int level = Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

            return (int) Math.round(xp + (xp * instance.mobDataManager.getAdditionsForLevel(ent, Addition.CUSTOM_XP_DROP, level)));
        } else {
            return xp;
        }
    }

    // When the persistent data container levelled key has been set on the entity already (i.e. when they are damaged)
    public String getNametag(final LivingEntity livingEntity) {
        return getNametag(livingEntity, Objects.requireNonNull(
                livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER)));
    }

    // When the persistent data container levelled key has not been set on the entity yet (i.e. for use in CreatureSpawnListener)
    public String getNametag(final LivingEntity livingEntity, final int level) {
        final String entityName = instance.configUtils.getEntityName(livingEntity.getType());
        final String displayName = livingEntity.getCustomName() == null ? entityName : livingEntity.getCustomName();

        // If show label for default levelled mobs is disabled and the mob is the min level, then don't modify their tag.
        if (!instance.settingsCfg.getBoolean("show-label-for-default-levelled-mobs") && level == instance.settingsCfg.getInt("fine-tuning.min-level")) {
            return livingEntity.getCustomName(); // Can be null, that is meant to be the case.
        }

        final AttributeInstance maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final String health = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";
        final String healthRounded = maxHealth == null ? "?" : (int)Utils.round(maxHealth.getBaseValue()) + "";

        String nametag = instance.settingsCfg.getString("creature-nametag");

        if (nametag == null || nametag.isEmpty()) return null;

        nametag = nametag.replace("%level%", level + "");
        nametag = nametag.replace("%displayname%", displayName);
        nametag = nametag.replace("%typename%", entityName);
        nametag = nametag.replace("%health%", Utils.round(livingEntity.getHealth()) + "");
        nametag = nametag.replace("%health_rounded%", (int)Utils.round(livingEntity.getHealth()) + "");
        nametag = nametag.replace("%max_health%", health);
        nametag = nametag.replace("%max_health_rounded%", healthRounded);
        nametag = nametag.replace("%heart_symbol%", "❤");
        nametag = MicroUtils.colorize(nametag);

        return nametag;
    }

    /*
     * Credit
     * - Thread: https://www.spigotmc.org/threads/changing-an-entitys-nametag-with-packets.482855/
     *
     * - Users:
     *   - @CoolBoy (https://www.spigotmc.org/members/CoolBoy.102500/)
     *   - @Esophose (https://www.spigotmc.org/members/esophose.34168/)
     *   - @7smile7 (https://www.spigotmc.org/members/7smile7.43809/)
     */
    public void updateNametag(final LivingEntity entity, final String nametag, final List<Player> players) {
        if (!instance.hasProtocolLibInstalled) return;

        for (Player player : players) {
            final WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
            final WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
            final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);
            final Optional<Object> optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());
            dataWatcher.setObject(watcherObject, optional);
            dataWatcher.setObject(3, entity.isCustomNameVisible() || instance.settingsCfg.getBoolean("creature-nametag-always-visible"));

            final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
            packet.getIntegers().write(0, entity.getEntityId());

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (InvocationTargetException ex) {
                Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7! Stack trace:");
                ex.printStackTrace();
            }
        }
    }

    private BukkitTask nametagAutoUpdateTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final long period = 6; // run every ? seconds.

        nametagAutoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    final Location location = player.getLocation();

                    for (final Entity entity : player.getWorld().getEntities()) {

                        // Mob must be a livingentity that is ...living.
                        if (!(entity instanceof LivingEntity)) continue;
                        final LivingEntity livingEntity = (LivingEntity) entity;

                        // Mob must be levelled
                        if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                            continue;

                        //if within distance, update nametag.
                        if (livingEntity.getLocation().distanceSquared(location) <= maxDistance) {
                            instance.levelManager.updateNametag(livingEntity, instance.levelManager.getNametag(livingEntity), Collections.singletonList(player));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(instance, 0, 20 * period);
    }

    public void stopNametagAutoUpdateTask() {
        if (!instance.hasProtocolLibInstalled) return;

        Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");

        if (nametagAutoUpdateTask != null)
            nametagAutoUpdateTask.cancel();
    }
}
