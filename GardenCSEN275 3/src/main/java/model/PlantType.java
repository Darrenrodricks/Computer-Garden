package model;

import java.util.List;

/**
 * PlantType
 *
 * This represents the SHARED definition of a kind of plant.
 *
 * Example:
 * - Rose
 * - Tomato
 * - Basil
 *
 * Why separate PlantType from Plant?
 * - Many plant instances can share the same type data.
 * - This avoids repeating water requirement and parasite lists in every object.
 */
public class PlantType {

    /** Human-readable plant type name, like "Rose". */
    private final String name;

    /** How much water this plant type ideally needs. */
    private final int waterRequirement;

    /** List of pest names this plant type is vulnerable to. */
    private final List<String> parasiteVulnerabilities;

    /** Minimum safe temperature for this plant type. */
    private final int minTempF;

    /** Maximum safe temperature for this plant type. */
    private final int maxTempF;

    public PlantType(
            String name,
            int waterRequirement,
            List<String> parasites,
            int minTempF,
            int maxTempF
    ) {
        this.name = name;
        this.waterRequirement = waterRequirement;
        this.parasiteVulnerabilities = parasites;
        this.minTempF = minTempF;
        this.maxTempF = maxTempF;
    }

    public String getName() {
        return name;
    }

    public int getWaterRequirement() {
        return waterRequirement;
    }

    public List<String> getParasiteVulnerabilities() {
        return parasiteVulnerabilities;
    }

    public int getMinTempF() {
        return minTempF;
    }

    public int getMaxTempF() {
        return maxTempF;
    }
}
