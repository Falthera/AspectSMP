package com.aspectsmp.visual;

import com.aspectsmp.core.AspectType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.EnumMap;
import java.util.Map;

public class AspectVisualThemes {

    public static class Theme {
        private final AspectType aspect;
        private final Color primaryColor;
        private final Color secondaryColor;
        private final Color accentColor;
        private final Particle auraParticle;
        private final Particle orbitParticle;
        private final Particle trailParticle;
        private final Particle impactParticle;
        private final Particle castParticle;
        private final Particle ultimateParticle;
        private final Particle trailParticleAlt;
        private final Sound castSound;
        private final Sound impactSound;
        private final Sound ultimateSound;
        private final Sound ambientSound;
        private final double auraSpeed;
        private final double orbitSpeed;
        private final double trailSpeed;
        private final int auraParticleCount;
        private final int trailParticleCount;

        public Theme(AspectType aspect, Color primary, Color secondary, Color accent,
                     Particle aura, Particle orbit, Particle trail, Particle impact, Particle cast, Particle ultimate,
                     Particle trailAlt,
                     Sound castSound, Sound impactSound, Sound ultimateSound, Sound ambientSound,
                     double auraSpeed, double orbitSpeed, double trailSpeed,
                     int auraCount, int trailCount) {
            this.aspect = aspect;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            this.accentColor = accent;
            this.auraParticle = aura;
            this.orbitParticle = orbit;
            this.trailParticle = trail;
            this.impactParticle = impact;
            this.castParticle = cast;
            this.ultimateParticle = ultimate;
            this.trailParticleAlt = trailAlt;
            this.castSound = castSound;
            this.impactSound = impactSound;
            this.ultimateSound = ultimateSound;
            this.ambientSound = ambientSound;
            this.auraSpeed = auraSpeed;
            this.orbitSpeed = orbitSpeed;
            this.trailSpeed = trailSpeed;
            this.auraParticleCount = auraCount;
            this.trailParticleCount = trailCount;
        }

        public AspectType getAspect() { return aspect; }
        public Color getPrimaryColor() { return primaryColor; }
        public Color getSecondaryColor() { return secondaryColor; }
        public Color getAccentColor() { return accentColor; }
        public Particle getAuraParticle() { return auraParticle; }
        public Particle getOrbitParticle() { return orbitParticle; }
        public Particle getTrailParticle() { return trailParticle; }
        public Particle getTrailParticleAlt() { return trailParticleAlt; }
        public Particle getImpactParticle() { return impactParticle; }
        public Particle getCastParticle() { return castParticle; }
        public Particle getUltimateParticle() { return ultimateParticle; }
        public Sound getCastSound() { return castSound; }
        public Sound getImpactSound() { return impactSound; }
        public Sound getUltimateSound() { return ultimateSound; }
        public Sound getAmbientSound() { return ambientSound; }
        public double getAuraSpeed() { return auraSpeed; }
        public double getOrbitSpeed() { return orbitSpeed; }
        public double getTrailSpeed() { return trailSpeed; }
        public int getAuraParticleCount() { return auraParticleCount; }
        public int getTrailParticleCount() { return trailParticleCount; }
    }

    private final Map<AspectType, Theme> themes = new EnumMap<>(AspectType.class);

    public AspectVisualThemes() {
        initializeThemes();
    }

    private void initializeThemes() {
        // INFERNO - Fire, lava, ember, heat distortion
        themes.put(AspectType.INFERNO, new Theme(
            AspectType.INFERNO,
            Color.fromRGB(0xFF3300),      // Primary: Intense red-orange
            Color.fromRGB(0xFF8800),      // Secondary: Bright orange
            Color.fromRGB(0xFFFF00),      // Accent: Yellow-white
            Particle.FLAME,               // Aura
            Particle.FLAME,               // Orbit
            Particle.FLAME,               // Trail
            Particle.FLAME,               // Impact
            Particle.FLAME,               // Cast
            Particle.LARGE_SMOKE,         // Ultimate
            Particle.FLAME,               // Trail alt
            Sound.ENTITY_BLAZE_SHOOT,     // Cast
            Sound.ENTITY_GENERIC_EXPLODE, // Impact
            Sound.ENTITY_ENDER_DRAGON_GROWL, // Ultimate
            Sound.BLOCK_FIRE_AMBIENT,     // Ambient
            0.05, 0.02, 0.01,
            8, 6
        ));

        // TIDE - Water, bubbles, flow, frost
        themes.put(AspectType.TIDE, new Theme(
            AspectType.TIDE,
            Color.fromRGB(0x0066FF),      // Primary: Deep blue
            Color.fromRGB(0x00CCFF),      // Secondary: Cyan
            Color.fromRGB(0xFFFFFF),      // Accent: White
            Particle.BUBBLE,              // Aura
            Particle.BUBBLE_POP,          // Orbit
            Particle.SPLASH,              // Trail
            Particle.SPLASH,              // Impact
            Particle.BUBBLE,              // Cast
            Particle.BUBBLE_COLUMN_UP,    // Ultimate
            Particle.BUBBLE_COLUMN_UP,    // Trail alt
            Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, // Cast
            Sound.BLOCK_CONDUIT_ACTIVATE, // Impact
            Sound.ENTITY_ELDER_GUARDIAN_CURSE,   // Ultimate
            Sound.BLOCK_WATER_AMBIENT,    // Ambient
            0.03, 0.015, 0.02,
            6, 5
        ));

        // TEMPEST - Lightning, electricity, storm
        themes.put(AspectType.TEMPEST, new Theme(
            AspectType.TEMPEST,
            Color.fromRGB(0xAA00FF),      // Primary: Electric purple
            Color.fromRGB(0x00FFFF),      // Secondary: Electric cyan
            Color.fromRGB(0xFFFF00),      // Accent: Lightning yellow
            Particle.ELECTRIC_SPARK,      // Aura
            Particle.ELECTRIC_SPARK,      // Orbit
            Particle.CRIT,                // Trail
            Particle.ELECTRIC_SPARK,      // Impact
            Particle.ELECTRIC_SPARK,      // Cast
            Particle.LARGE_SMOKE,         // Ultimate
            Particle.CRIT,                // Trail alt
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER, // Cast
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT,  // Impact
            Sound.ENTITY_ENDER_DRAGON_DEATH,     // Ultimate
            Sound.ENTITY_BREEZE_WIND_BURST,      // Ambient
            0.1, 0.05, 0.03,
            10, 8
        ));

        // RIFT - Void, ender, dimensional, cosmic
        themes.put(AspectType.RIFT, new Theme(
            AspectType.RIFT,
            Color.fromRGB(0x440088),      // Primary: Deep void purple
            Color.fromRGB(0x8800FF),      // Secondary: Bright purple
            Color.fromRGB(0x00FFFF),      // Accent: End cyan
            Particle.PORTAL,              // Aura
            Particle.END_ROD,             // Orbit
            Particle.PORTAL,              // Trail
            Particle.REVERSE_PORTAL,      // Impact
            Particle.PORTAL,              // Cast
            Particle.DRAGON_BREATH,       // Ultimate
            Particle.REVERSE_PORTAL,      // Trail alt
            Sound.ENTITY_ENDERMAN_TELEPORT,    // Cast
            Sound.BLOCK_PORTAL_TRIGGER,       // Impact
            Sound.ENTITY_ENDER_DRAGON_DEATH,  // Ultimate
            Sound.BLOCK_PORTAL_AMBIENT,       // Ambient
            0.02, 0.01, 0.015,
            7, 5
        ));

        // VITALITY - Life, nature, healing, growth
        themes.put(AspectType.VITALITY, new Theme(
            AspectType.VITALITY,
            Color.fromRGB(0x00FF44),      // Primary: Vibrant green
            Color.fromRGB(0x88FF00),      // Secondary: Lime
            Color.fromRGB(0xFFD700),      // Accent: Gold
            Particle.HAPPY_VILLAGER,      // Aura (green sparkles)
            Particle.GLOW,                // Orbit
            Particle.GLOW,                // Trail
            Particle.HEART,               // Impact
            Particle.HAPPY_VILLAGER,      // Cast
            Particle.TOTEM_OF_UNDYING,    // Ultimate
            Particle.HAPPY_VILLAGER,      // Trail alt
            Sound.ENTITY_PLAYER_LEVELUP,  // Cast
            Sound.ENTITY_PLAYER_LEVELUP,  // Impact
            Sound.ITEM_TOTEM_USE,         // Ultimate
            Sound.BLOCK_GRASS_AMBIENT,    // Ambient (use GRASS_RUSTLE)
            0.04, 0.02, 0.01,
            8, 5
        ));

        // WAR - Blood, iron, rage, brutal
        themes.put(AspectType.WAR, new Theme(
            AspectType.WAR,
            Color.fromRGB(0xCC0000),      // Primary: Blood red
            Color.fromRGB(0x884400),      // Secondary: Iron brown
            Color.fromRGB(0xFFAA00),      // Accent: Warning orange
            Particle.ITEM_SLIME,          // Aura (red-tinted)
            Particle.CRIT,                // Orbit
            Particle.ITEM_SLIME,          // Trail
            Particle.DAMAGE_INDICATOR,    // Impact
            Particle.CRIT,                // Cast
            Particle.EXPLOSION,           // Ultimate
            Particle.CRIT,                // Trail alt
            Sound.ENTITY_PLAYER_ATTACK_STRONG, // Cast
            Sound.ENTITY_GENERIC_EXPLODE, // Impact
            Sound.ENTITY_RAVAGER_ROAR,    // Ultimate
            Sound.ENTITY_IRON_GOLEM_HURT, // Ambient
            0.06, 0.03, 0.02,
            6, 4
        ));

        // COSMOS - Stars, gravity, cosmic, celestial
        themes.put(AspectType.COSMOS, new Theme(
            AspectType.COSMOS,
            Color.fromRGB(0x220044),      // Primary: Deep space
            Color.fromRGB(0x8844FF),      // Secondary: Cosmic purple
            Color.fromRGB(0xFFFF88),      // Accent: Starlight
            Particle.END_ROD,             // Aura
            Particle.ENCHANT,             // Orbit
            Particle.END_ROD,             // Trail
            Particle.ENTITY_EFFECT,       // Impact
            Particle.END_ROD,             // Cast
            Particle.LARGE_SMOKE,         // Ultimate
            Particle.END_ROD,             // Trail alt
            Sound.BLOCK_BEACON_POWER_SELECT,   // Cast
            Sound.BLOCK_BEACON_ACTIVATE,       // Impact
            Sound.ENTITY_ENDER_DRAGON_DEATH,   // Ultimate
            Sound.BLOCK_BEACON_AMBIENT,        // Ambient
            0.02, 0.01, 0.01,
            8, 6
        ));

        // FORTUNE - Gold, sparkles, luck, treasure
        themes.put(AspectType.FORTUNE, new Theme(
            AspectType.FORTUNE,
            Color.fromRGB(0xFFD700),      // Primary: Gold
            Color.fromRGB(0xFFAA00),      // Secondary: Amber
            Color.fromRGB(0x00FF88),      // Accent: Lucky green
            Particle.HAPPY_VILLAGER,      // Aura (gold sparkles)
            Particle.ENCHANT,             // Orbit
            Particle.HAPPY_VILLAGER,      // Trail
            Particle.HAPPY_VILLAGER,      // Impact
            Particle.HAPPY_VILLAGER,      // Cast
            Particle.FIREWORK,            // Ultimate
            Particle.HAPPY_VILLAGER,      // Trail alt
            Sound.ENTITY_PLAYER_LEVELUP,  // Cast
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP, // Impact
            Sound.ENTITY_FIREWORK_ROCKET_BLAST, // Ultimate
            Sound.BLOCK_NOTE_BLOCK_BELL,  // Ambient
            0.05, 0.02, 0.02,
            10, 7
        ));

        // CLOUD - Air, wind, sky, freedom
        themes.put(AspectType.CLOUD, new Theme(
            AspectType.CLOUD,
            Color.fromRGB(0xCCEEFF),      // Primary: Pale sky blue
            Color.fromRGB(0xFFFFFF),      // Secondary: White
            Color.fromRGB(0x88CCFF),      // Accent: Light blue
            Particle.CLOUD,               // Aura
            Particle.END_ROD,             // Orbit
            Particle.CLOUD,               // Trail
            Particle.CLOUD,               // Impact
            Particle.CLOUD,               // Cast
            Particle.LARGE_SMOKE,         // Ultimate
            Particle.CLOUD,               // Trail alt
            Sound.ENTITY_BREEZE_WIND_BURST,    // Cast
            Sound.ENTITY_BREEZE_INHALE,        // Impact
            Sound.ENTITY_ENDER_DRAGON_FLAP,    // Ultimate
            Sound.ENTITY_BREEZE_IDLE,          // Ambient
            0.02, 0.01, 0.005,
            8, 6
        ));

        // WINTER - Ice, frost, crystal, frozen
        themes.put(AspectType.WINTER, new Theme(
            AspectType.WINTER,
            Color.fromRGB(0x88DDFF),      // Primary: Ice blue
            Color.fromRGB(0xCCFFFF),      // Secondary: Pale cyan
            Color.fromRGB(0xFFFFFF),      // Accent: Pure white
            Particle.SNOWFLAKE,           // Aura
            Particle.ITEM_SNOWBALL,       // Orbit
            Particle.SNOWFLAKE,           // Trail
            Particle.ITEM_SNOWBALL,       // Impact
            Particle.SNOWFLAKE,           // Cast
            Particle.SNOWFLAKE,           // Ultimate
            Particle.SNOWFLAKE,           // Trail alt
            Sound.BLOCK_GLASS_BREAK,      // Cast
            Sound.BLOCK_GLASS_BREAK,      // Impact
            Sound.ENTITY_ELDER_GUARDIAN_CURSE, // Ultimate
            Sound.BLOCK_SNOW_STEP,        // Ambient
            0.03, 0.015, 0.01,
            10, 7
        ));

        // SHADOW - Darkness, void, stealth, nightmare
        themes.put(AspectType.SHADOW, new Theme(
            AspectType.SHADOW,
            Color.fromRGB(0x110022),      // Primary: Near black purple
            Color.fromRGB(0x440066),      // Secondary: Dark purple
            Color.fromRGB(0xFF0088),      // Accent: Hot pink
            Particle.SOUL,                // Aura
            Particle.SOUL_FIRE_FLAME,     // Orbit
            Particle.SOUL,                // Trail
            Particle.SOUL,                // Impact
            Particle.SOUL,                // Cast
            Particle.SCULK_CHARGE,        // Ultimate
            Particle.SOUL,                // Trail alt
            Sound.ENTITY_WARDEN_HEARTBEAT, // Cast
            Sound.ENTITY_WARDEN_SONIC_BOOM, // Impact
            Sound.ENTITY_WARDEN_DEATH,    // Ultimate
            Sound.BLOCK_SCULK_BLOCK_BREAK, // Ambient
            0.02, 0.01, 0.01,
            6, 4
        ));

        // STORM - Wind, tornado, tempest, chaos
        themes.put(AspectType.STORM, new Theme(
            AspectType.STORM,
            Color.fromRGB(0x6666AA),      // Primary: Storm gray-blue
            Color.fromRGB(0xAAAAFF),      // Secondary: Lightning blue
            Color.fromRGB(0xFFFF44),      // Accent: Lightning yellow
            Particle.CLOUD,               // Aura
            Particle.CRIT,                // Orbit
            Particle.CLOUD,               // Trail
            Particle.CLOUD,               // Impact
            Particle.CLOUD,               // Cast
            Particle.LARGE_SMOKE,         // Ultimate
            Particle.CLOUD,               // Trail alt
            Sound.ENTITY_BREEZE_WIND_BURST,    // Cast
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT, // Impact
            Sound.ENTITY_ENDER_DRAGON_DEATH,    // Ultimate
            Sound.ENTITY_BREEZE_IDLE,          // Ambient
            0.05, 0.03, 0.02,
            8, 6
        ));
    }

    public Theme getTheme(AspectType aspect) {
        return themes.getOrDefault(aspect, themes.get(AspectType.INFERNO));
    }

    public Color getColorForAspect(AspectType aspect) {
        return getTheme(aspect).getPrimaryColor();
    }

    public Particle getParticleForAspect(AspectType aspect, ParticleType type) {
        Theme theme = getTheme(aspect);
        return switch (type) {
            case AURA -> theme.getAuraParticle();
            case ORBIT -> theme.getOrbitParticle();
            case TRAIL -> theme.getTrailParticle();
            case TRAIL_ALT -> theme.getTrailParticleAlt();
            case IMPACT -> theme.getImpactParticle();
            case CAST -> theme.getCastParticle();
            case ULTIMATE -> theme.getUltimateParticle();
        };
    }

    public Sound getSoundForAspect(AspectType aspect, SoundType type) {
        Theme theme = getTheme(aspect);
        return switch (type) {
            case CAST -> theme.getCastSound();
            case IMPACT -> theme.getImpactSound();
            case ULTIMATE -> theme.getUltimateSound();
            case AMBIENT -> theme.getAmbientSound();
        };
    }

    public enum ParticleType {
        AURA, ORBIT, TRAIL, TRAIL_ALT, IMPACT, CAST, ULTIMATE
    }

    public enum SoundType {
        CAST, IMPACT, ULTIMATE, AMBIENT
    }
}