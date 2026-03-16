package modules;

import engine.GardenEngine;
import engine.SimulationModule;
import model.Plant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PestControlModule
 *
 * Automated pest management module that detects parasite events
 * and deploys countermeasures to protect the garden.
 *
 * Behavior:
 * - When a parasite event occurs, the module records the pest type.
 * - It provides partial mitigation: a small health recovery for
 *   affected plants (does NOT heal back to full, per spec).
 * - Tracks all pests encountered for logging/reporting.
 * - On day end, clears active pest tracking.
 *
 * The module simulates deploying pesticides or biological controls
 * that slow the damage but don't eliminate it entirely.
 */
public class PestControlModule implements SimulationModule {

    /** Small health recovery to partially offset parasite damage. */
    private static final int PEST_MITIGATION = 4;

    /** Pests currently being treated. */
    private final Set<String> activeTreatments = new HashSet<>();

    /** Historical record of all pests ever encountered. */
    private final Set<String> pestHistory = new HashSet<>();

    /** Whether pest control was deployed today. */
    private boolean deployedToday = false;

    @Override
    public String name() {
        return "PestControlModule";
    }

    /**
     * Respond to a parasite infestation.
     *
     * Strategy:
     * - Record the pest.
     * - For each living plant that is vulnerable to this pest,
     *   apply a small health recovery (simulating pesticide deployment).
     * - The plant already took damage from Plant.applyParasite(),
     *   so this just softens it a bit.
     */
    @Override
    public void onParasite(GardenEngine engine, String parasite) {
        activeTreatments.add(parasite);
        pestHistory.add(parasite);
        deployedToday = true;

        List<Plant> alive = engine.alivePlants();

        for (Plant p : alive) {
            // Check if this plant is vulnerable to the pest.
            if (p.getType().getParasiteVulnerabilities().contains(parasite)) {
                // Partial mitigation: give a small water+recovery boost.
                // This simulates the pest control system helping the plant cope.
                p.applyRain(PEST_MITIGATION);
            }
        }
    }

    /**
     * At end of day, clear active treatments.
     * The pest has been dealt with for today.
     */
    @Override
    public void onDayEnd(GardenEngine engine) {
        activeTreatments.clear();
        deployedToday = false;
    }

    public Set<String> getActiveTreatments() {
        return new HashSet<>(activeTreatments);
    }

    public Set<String> getPestHistory() {
        return new HashSet<>(pestHistory);
    }

    public boolean wasDeployedToday() {
        return deployedToday;
    }
}
