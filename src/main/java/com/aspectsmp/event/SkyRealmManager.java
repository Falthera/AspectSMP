package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class SkyRealmManager {

    private final AspectSMP plugin;
    private World ninthSkyWorld;

    public SkyRealmManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void ensureNinthSkyLoaded() {
        if (ninthSkyWorld != null && ninthSkyWorld.isChunkLoaded(0, 0)) return;
        ninthSkyWorld = Bukkit.getWorld("ninth_sky");
        if (ninthSkyWorld == null) {
            WorldCreator creator = new WorldCreator("ninth_sky");
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            ninthSkyWorld = Bukkit.createWorld(creator);
        }
    }

    public World getNinthSky() {
        return ninthSkyWorld;
    }
}
