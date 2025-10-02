package dev.lmv.lmvac.api.implement.api.lmvEvents.events.flag;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class FlagEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final FlagType flagType;
    private int flagsCount;
    public int count = -1;
    private final String checkType;
    private final int violationLevel;

    private boolean cancelled;

    private Vector flagVelocity;

    public FlagEvent(@NotNull Player player, FlagType type, int flagsCount, @NotNull String checkType, int violationLevel) {
        this.player = player;
        this.flagType = type;
        this.checkType = checkType;
        this.flagsCount = flagsCount;
        this.violationLevel = violationLevel;
        Vector v = player.getVelocity();
        if (!player.isOnGround()) {
            this.flagVelocity = new Vector(Math.max(-0.1, Math.min(0.1, v.getX())), -0.1, Math.max(-0.1, Math.min(0.1, v.getZ())));
        } else {
            this.flagVelocity = new Vector(0,0,0);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public FlagType getFlagType() {
        return flagType;
    }

    @NotNull
    public int getFlagsCount() {
        return flagsCount;
    }

    @NotNull
    public int getCurrentCount() {
        return count;
    }

    @NotNull
    public String getCheckType() {
        return checkType;
    }

    public int getViolationLevel() {
        return violationLevel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    public Vector getFlagVelocity() {
        return flagVelocity;
    }

    public void setFlagVelocity(Vector flagVelocity) {
        this.flagVelocity = flagVelocity;
    }
}