package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;

public class EntityDeathListener implements Listener {

    private final LevelledMobs instance;

    public EntityDeathListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // These entities will be forced not to have levelled drops
    final HashSet<String> bypassDrops = new HashSet<>(Arrays.asList("ARMOR_STAND", "ITEM_FRAME", "DROPPED_ITEM", "PAINTING"));

    @EventHandler(ignoreCancelled = true)
    public void onDeath(final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType())) {
            return;
        }

        final LivingEntity livingEntity = event.getEntity();

        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {

            // Set levelled item drops
            event.getDrops().clear();
            event.getDrops().addAll(instance.levelManager.getLevelledItemDrops(livingEntity, event.getDrops()));

            // Set levelled exp drops
            event.setDroppedExp(instance.levelManager.getLevelledExpDrops(livingEntity, event.getDroppedExp()));
        }
    }
}
