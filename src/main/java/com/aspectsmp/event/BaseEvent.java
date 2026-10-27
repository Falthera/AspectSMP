package com.aspectsmp.event;

import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseEvent {
    protected final com.aspectsmp.AspectSMP plugin;
    protected final AtomicReference<EventPhase> phase = new AtomicReference<>(EventPhase.SKY_AWAKENS);
    protected final AtomicBoolean active = new AtomicBoolean(false);
    protected final Set<UUID> participants = ConcurrentHashMap.newKeySet();
    protected final List<BukkitTask> activeTasks = new java.util.ArrayList<>();

    public BaseEvent(com.aspectsmp.AspectSMP plugin) {
        this.plugin = plugin;
    }

    public EventPhase getPhase() { return phase.get(); }
    public void setPhase(EventPhase phase) { this.phase.set(phase); }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean active) { this.active.set(active); }
    
    public Set<UUID> getParticipants() {
        return Set.copyOf(participants);
    }
    
    public void addParticipant(UUID uuid) {
        participants.add(uuid);
    }
    
    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
    }
    
    public List<BukkitTask> getActiveTasks() {
        return List.copyOf(activeTasks);
    }
    
    protected void trackTask(BukkitTask task) {
        if (task != null) {
            activeTasks.add(task);
        }
    }
    
    protected void stopAllTasks() {
        for (BukkitTask task : activeTasks) {
            if (task != null) task.cancel();
        }
        activeTasks.clear();
    }

    public abstract void startEvent();
    public abstract void stopEvent();
    
    public abstract void saveState();
    public abstract void loadState();
}