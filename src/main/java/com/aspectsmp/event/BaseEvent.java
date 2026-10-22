package com.aspectsmp.event;

public abstract class BaseEvent {
    protected final com.aspectsmp.AspectSMP plugin;
    protected EventPhase phase;
    protected boolean active = false;

    public BaseEvent(com.aspectsmp.AspectSMP plugin) {
        this.plugin = plugin;
    }

    public EventPhase getPhase() { return phase; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public abstract void startEvent();
    public abstract void stopEvent();
}