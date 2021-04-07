package me.lokka30.levelledmobs.customdrops;

/**
 * @author stumper66
 */
public class CustomDropsDefaults {
    public boolean noMultiplier;
    public boolean noSpawner;
    public boolean equipped;
    public boolean override;
    public int amount;
    public int priority;
    public int minLevel;
    public int maxLevel;
    public int customModelData;
    public int maxDropGroup;
    public double chance;
    public double equippedSpawnChance;
    public String groupId;
    public String damage;

    public CustomDropsDefaults() {
        // these are the defaults of the defaults
        this.chance = 0.2;
        this.amount = 1;
        this.minLevel = -1;
        this.maxLevel = -1;
        this.customModelData = -1;
        this.priority = 0;
        this.equippedSpawnChance = 1.0;
        this.maxDropGroup = 0;
        this.noMultiplier = false;
        this.noSpawner = false;
        this.equipped = false;
        this.override = false;
    }

    public void setDefaultsFromDropItem(CustomDropItem drop) {
        this.chance = drop.dropChance;
        this.amount = drop.getAmount();
        this.minLevel = drop.minLevel;
        this.maxLevel = drop.maxLevel;
        this.customModelData = drop.customModelDataId;
        this.noMultiplier = drop.noMultiplier;
        this.noSpawner = drop.noSpawner;
        this.equipped = drop.isEquipped;
        this.priority = drop.priority;
        this.equippedSpawnChance = drop.equippedSpawnChance;
        this.maxDropGroup = drop.maxDropGroup;
    }
}
