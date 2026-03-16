package model;

/**
 * Plant
 *
 * This represents one specific plant instance in the garden.
 *
 * Important distinction:
 * - PlantType = shared blueprint
 * - Plant = one individual plant with its own current health and water
 */
public class Plant {

    /** Unique ID so logs and UI can refer to a specific plant instance. */
    private final String id;

    /** The shared type definition this plant belongs to. */
    private final PlantType type;

    /** Current water level from 0 to 100. */
    private int waterLevel;

    /** Current health from 0 to 100. */
    private int health;

    /** Whether the plant is still alive. */
    private boolean alive;

    public Plant(String id, PlantType type) {
        this.id = id;
        this.type = type;

        // Starting values are intentionally reasonable, not perfect extremes.
        this.waterLevel = 60;
        this.health = 100;
        this.alive = true;
    }

    public String getId() {
        return id;
    }

    public PlantType getType() {
        return type;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public int getHealth() {
        return health;
    }

    /**
     * Applies one rainy day's effect to this plant.
     *
     * Simplified rule:
     * - rain increases water level
     * - too much water can slightly hurt health
     */
    public void applyRain(int amount) {
        waterLevel = clamp(waterLevel + amount, 0, 100);

        // Small penalty for overwatering.
        if (waterLevel > 95) {
            health = clamp(health - 2, 0, 100);
        }

        checkDeath();
    }

    /**
     * Applies one day's temperature effect.
     *
     * Rule:
     * - outside safe range => lose health
     * - inside safe range => recover a little
     */
    public void applyTemperature(int tempF) {
        if (tempF < type.getMinTempF() || tempF > type.getMaxTempF()) {
            health = clamp(health - 10, 0, 100);
        } else {
            health = clamp(health + 1, 0, 100);
        }

        checkDeath();
    }

    /**
     * Applies one parasite attack.
     *
     * Only hurts the plant if its type is vulnerable to that parasite.
     */
    public void applyParasite(String parasiteName) {
        if (type.getParasiteVulnerabilities().contains(parasiteName)) {
            health = clamp(health - 12, 0, 100);
            checkDeath();
        }
    }

    /**
     * Simulates the plant's normal day-to-day life.
     *
     * Rule:
     * - each day the plant consumes water
     * - if too dry, it loses health
     * - if conditions are decent, it recovers a little
     */
    public void dailyTick() {
        int use = Math.max(1, type.getWaterRequirement() / 2);
        waterLevel = clamp(waterLevel - use, 0, 100);

        if (waterLevel < 15) {
            health = clamp(health - 8, 0, 100);
        } else if (waterLevel >= 30 && waterLevel <= 80) {
            health = clamp(health + 1, 0, 100);
        }

        checkDeath();
    }

    /**
     * Marks the plant dead when health reaches zero.
     */
    private void checkDeath() {
        if (health <= 0) {
            alive = false;
        }
    }

    /**
     * Utility helper that keeps numbers inside a fixed range.
     */
    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
