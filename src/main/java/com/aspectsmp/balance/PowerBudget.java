package com.aspectsmp.balance;

import com.aspectsmp.core.AspectType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Power Budget Framework for Aspect Balance
 * 
 * DESIGN PHILOSOPHY:
 * - Each ultimate gets 100 Power Points (PP)
 * - Each passive gets 30 Power Points (PP)
 * - Total aspect budget: 130 PP
 * 
 * COST TABLE:
 * - Damage: 1 PP per base damage per 10s uptime (DPS * 10)
 * - Healing: 1 PP per 2 HP/s per 10s uptime
 * - Self-Buff: 5 PP per amplifier per 10s uptime (double if self-only)
 * - Ally-Buff: 3 PP per amplifier per 10s uptime
 * - CC (Stun/Root/Slow): 15 PP per second duration per 10s uptime
 * - Soft CC (Blind/Weakness/Mining Fatigue): 8 PP per second
 * - Debuff (Wither/Poison): 10 PP per second
 * - Utility (Speed/Jump/Resistance): 5 PP per level per 10s
 * - Immunity (Fire/Poison/Wither): 20 PP
 * - CC Immunity: 30 PP
 * - Range: 1 PP per block
 * - Self-Target: 1.5x cost multiplier
 * - Ally-Target: 0.7x cost multiplier
 * - Execute/Execute-threshold: 50 PP + damage cost
 * - Pull/Knockback: 2 PP per block per target
 * - Pull/Knockback on multiple: +1 PP per additional target
 * 
 * UPTIME CALCULATION:
 * - Effect duration / cooldown = uptime ratio
 * - Cost = base_cost * uptime_ratio
 * - For passives: continuous uptime = 1.0
 * 
 * ROLE DEFINITIONS:
 * - TANK: High damage reduction, self-sustain, threat generation
 * - DPS: High damage output, execute potential, mobility
 * - SUPPORT: Healing, ally buffs, utility, CC
 * - UTILITY: Economy, mobility, CC, vision, survival
 * - CONTROL: CC, area denial, displacement, anti-mobility
 * - HYBRID: Balanced mix of two roles
 */

public class PowerBudget {
    
    // Total budget per aspect
    public static final int ULTIMATE_BUDGET = 100;
    public static final int PASSIVE_BUDGET = 30;
    public static final int TOTAL_BUDGET = 130;
    
    // Role assignments for each aspect
    public static final Map<AspectType, Role> ASPECT_ROLES = new EnumMap<>(AspectType.class);
    
    static {
        // TANK aspects
        ASPECT_ROLES.put(AspectType.WAR, Role.TANK);
        ASPECT_ROLES.put(AspectType.VITALITY, Role.SUPPORT);
        ASPECT_ROLES.put(AspectType.TIDE, Role.CONTROL);
        
        // DPS aspects
        ASPECT_ROLES.put(AspectType.INFERNO, Role.DPS);
        ASPECT_ROLES.put(AspectType.TEMPEST, Role.DPS);
        ASPECT_ROLES.put(AspectType.SHADOW, Role.DPS);
        
        // SUPPORT aspects
        ASPECT_ROLES.put(AspectType.VITALITY, Role.SUPPORT);
        ASPECT_ROLES.put(AspectType.FORTUNE, Role.UTILITY);
        
        // CONTROL aspects
        ASPECT_ROLES.put(AspectType.TIDE, Role.CONTROL);
        ASPECT_ROLES.put(AspectType.CLOUD, Role.CONTROL);
        ASPECT_ROLES.put(AspectType.WINTER, Role.CONTROL);
        
        // UTILITY aspects
        ASPECT_ROLES.put(AspectType.FORTUNE, Role.UTILITY);
        ASPECT_ROLES.put(AspectType.COSMOS, Role.UTILITY);
        ASPECT_ROLES.put(AspectType.CLOUD, Role.CONTROL);
        ASPECT_ROLES.put(AspectType.SHADOW, Role.DPS);
        ASPECT_ROLES.put(AspectType.STORM, Role.DPS);
        ASPECT_ROLES.put(AspectType.RIFT, Role.CONTROL);
        ASPECT_ROLES.put(AspectType.WINTER, Role.CONTROL);
    }
    
    public enum Role {
        TANK(1.0, 1.0, 0.5, 1.0),      // High defense, sustain, low damage
        DPS(1.5, 0.8, 0.5, 1.2),       // High damage, low defense, mobility
        SUPPORT(0.8, 1.2, 1.5, 0.8),   // Low damage, high utility/healing
        UTILITY(0.8, 1.0, 1.2, 1.0),   // Balanced utility/economy
        CONTROL(1.0, 1.0, 1.0, 1.5),   // CC, displacement, area denial
        HYBRID(1.1, 1.1, 1.0, 1.1);    // Balanced hybrid
        
        final double damageMult;
        final double defenseMult;
        final double utilityMult;
        final double controlMult;
        
        Role(double dmg, double def, double util, double ctrl) {
            this.damageMult = dmg;
            this.defenseMult = def;
            this.utilityMult = util;
            this.controlMult = ctrl;
        }
    }
    
    // Power cost calculator
    public static class Cost {
        public static double damage(double baseDamage, double uptime) {
            return baseDamage * uptime * 0.1; // 1 PP per dmg per 10s
        }
        
        public static double heal(double hps, double uptime) {
            return hps * uptime * 0.5; // 1 PP per 2 HPS per 10s
        }
        
        public static double selfBuff(int amplifier, double duration, double uptime, boolean selfOnly) {
            double mult = selfOnly ? 1.5 : 1.0;
            return 5 * amplifier * (duration / 200.0) * uptime * mult;
        }
        
        public static double allyBuff(int amplifier, double duration, double uptime) {
            return 3 * amplifier * (duration / 200.0) * uptime;
        }
        
        public static double hardCC(double duration, double uptime) {
            return 15 * duration * uptime; // per second
        }
        
        public static double softCC(double duration, double uptime) {
            return 8 * duration * uptime;
        }
        
        public static double debuff(double duration, double uptime) {
            return 10 * duration * uptime;
        }
        
        public static double utility(int level, double duration, double uptime) {
            return 5 * level * (duration / 200.0) * uptime;
        }
        
        public static double immunity() {
            return 20; // full immunity
        }
        
        public static double ccImmunity() {
            return 30;
        }
        
        public static double range(double blocks) {
            return blocks * 1.0;
        }
        
        public static double execute(double thresholdHP) {
            return 50 + (20 - thresholdHP) * 2; // base 50 + scaling
        }
        
        public static double pull(double blocks, int targets) {
            return 2 * blocks + Math.max(0, targets - 1);
        }
        
        public static double knockback(double blocks, int targets) {
            return 2 * blocks + Math.max(0, targets - 1);
        }
    }
    
    // Validate an aspect's total power cost
    public static ValidationResult validateAspect(AspectType aspect, UltimateDesign ultimate, PassiveDesign passive) {
        double ultimateCost = calculateUltimateCost(ultimate);
        double passiveCost = calculatePassiveCost(passive);
        double totalCost = ultimateCost + passiveCost;
        
        boolean valid = totalCost <= TOTAL_BUDGET * 1.1 && totalCost >= TOTAL_BUDGET * 0.9; // 10% tolerance
        
        return new ValidationResult(aspect, ultimateCost, passiveCost, totalCost, valid);
    }
    
    private static double calculateUltimateCost(UltimateDesign u) {
        double cost = 0;
        double uptime = u.duration / u.cooldown;
        
        if (u.baseDamage > 0) cost += Cost.damage(u.baseDamage, uptime);
        if (u.healPerSecond > 0) cost += Cost.heal(u.healPerSecond, uptime);
        if (u.selfBuffAmplifier > 0) cost += Cost.selfBuff(u.selfBuffAmplifier, u.duration, u.cooldown, true);
        if (u.allyBuffAmplifier > 0) cost += Cost.allyBuff(u.allyBuffAmplifier, u.duration, uptime);
        if (u.hardCCDuration > 0) cost += Cost.hardCC(u.hardCCDuration, uptime);
        if (u.softCCDuration > 0) cost += Cost.softCC(u.softCCDuration, uptime);
        if (u.debuffDuration > 0) cost += Cost.debuff(u.debuffDuration, uptime);
        if (u.utilityLevel > 0) cost += Cost.utility(u.utilityLevel, u.duration, uptime);
        if (u.hasExecute) cost += Cost.execute(u.executeThreshold);
        if (u.pullDistance > 0) cost += Cost.pull(u.pullDistance, u.targets);
        if (u.knockbackDistance > 0) cost += Cost.knockback(u.knockbackDistance, u.targets);
        if (u.hasImmunity) cost += Cost.immunity();
        if (u.hasCCImmunity) cost += Cost.ccImmunity();
        if (u.range > 0) cost += Cost.range(u.range);
        
        // Apply role multiplier
        Role role = ASPECT_ROLES.getOrDefault(u.aspect, Role.HYBRID);
        cost *= role.damageMult;
        
        return cost;
    }
    
    private static double calculatePassiveCost(PassiveDesign p) {
        double cost = 0;
        
        if (p.damageReduction > 0) cost += 5 * p.damageReduction * 10; // 5 PP per 10%
        if (p.damageIncrease > 0) cost += 8 * p.damageIncrease * 10;
        if (p.hasImmunity) cost += Cost.immunity();
        if (p.hasCCImmunity) cost += Cost.ccImmunity();
        if (p.healPerSecond > 0) cost += Cost.heal(p.healPerSecond, 1.0);
        if (p.selfBuffAmplifier > 0) cost += Cost.selfBuff(p.selfBuffAmplifier, 200, 1.0, true);
        if (p.utilityLevel > 0) cost += Cost.utility(p.utilityLevel, 200, 1.0);
        if (p.movementUtility) cost += 10;
        
        Role role = ASPECT_ROLES.getOrDefault(p.aspect, Role.HYBRID);
        cost *= role.defenseMult;
        
        return cost;
    }
    
    public static class UltimateDesign {
        public AspectType aspect;
        public double baseDamage = 0;
        public double healPerSecond = 0;
        public int selfBuffAmplifier = 0;
        public int allyBuffAmplifier = 0;
        public double hardCCDuration = 0;
        public double softCCDuration = 0;
        public double debuffDuration = 0;
        public int utilityLevel = 0;
        public boolean hasExecute = false;
        public double executeThreshold = 10;
        public double pullDistance = 0;
        public double knockbackDistance = 0;
        public int targets = 1;
        public boolean hasImmunity = false;
        public boolean hasCCImmunity = false;
        public double range = 0;
        public double duration = 0;
        public double cooldown = 0;
    }
    
    public static class PassiveDesign {
        public AspectType aspect;
        public double damageReduction = 0;
        public double damageIncrease = 0;
        public boolean hasImmunity = false;
        public boolean hasCCImmunity = false;
        public double healPerSecond = 0;
        public int selfBuffAmplifier = 0;
        public int utilityLevel = 0;
        public boolean movementUtility = false;
    }
    
    public static class ValidationResult {
        public final AspectType aspect;
        public final double ultimateCost;
        public final double passiveCost;
        public final double totalCost;
        public final boolean valid;
        
        public ValidationResult(AspectType aspect, double uc, double pc, double tc, boolean v) {
            this.aspect = aspect;
            this.ultimateCost = uc;
            this.passiveCost = pc;
            this.totalCost = tc;
            this.valid = v;
        }
        
        @Override
        public String toString() {
            return String.format("%s: Ultimate=%.1f, Passive=%.1f, Total=%.1f, Valid=%s", 
                aspect.name(), ultimateCost, passiveCost, totalCost, valid);
        }
    }
}