package me.lokka30.levelledmobs.events;

import me.lokka30.levelledmobs.LevelInterface;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class MobPreLevelEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * When a mob is levelled, the following enum is used to
     * allow plugins to find the cause of the mob being
     * levelled.
     * <p>
     * NORMAL: Spawned naturally, by a spawn egg, etc.
     * CHANGED_LEVEL: When an existing levelled mob has its level changed.
     */
    public enum LevelCause {
        NORMAL,
        CHANGED_LEVEL
    }

    private final LivingEntity entity;
    private int level;
    private final LevelCause levelCause;
    private final HashSet<LevelInterface.AdditionalLevelInformation> additionalInformation;

    public MobPreLevelEvent(@NotNull LivingEntity entity, int level, @NotNull LevelCause levelCause, @Nullable HashSet<LevelInterface.AdditionalLevelInformation> additionalInformation) {
        this.entity = entity;
        this.level = level;
        this.levelCause = levelCause;
        this.additionalInformation = additionalInformation;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public int getLevel() {
        return level;
    }

    public LevelCause getCause() {
        return levelCause;
    }

    public HashSet<LevelInterface.AdditionalLevelInformation> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
