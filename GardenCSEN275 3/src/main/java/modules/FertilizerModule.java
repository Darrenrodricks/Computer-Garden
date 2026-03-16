package modules;

import engine.GardenEngine;
import engine.SimulationModule;
import model.Plant;

import java.util.List;

/**
 * FertilizerModule
 *
 * Automated fertilization module that periodically provides
 * nutrient boosts to living plants.
 *
 * Behavior:
 * - Every few days, applies a small health boost to all living plants.
 * - Does not fully heal plants (spec compliance).
 * - Simulates slow-release fertilizer in the soil.
 * - Tracks total fertilizer applications for logging.
 */
public class FertilizerModule implements SimulationModule {

    /** Apply fertilizer every this many days. */
    private static final int FERTILIZE_INTERVAL = 3;

    /** Small health recovery per fertilizer application. */
    private static final int FERTILIZER_BOOST = 2;

    /** Count of total fertilizer applications. */
    private int totalApplications = 0;

    @Override
    public String name() {
        return "FertilizerModule";
    }

    /**
     * At end of each day, check if it's time to fertilize.
     * Fertilizer helps all plants recover a tiny bit of health.
     */
    @Override
    public void onDayEnd(GardenEngine engine) {
        int currentDay = engine.getDay();

        if (currentDay > 0 && currentDay % FERTILIZE_INTERVAL == 0) {
            List<Plant> alive = engine.alivePlants();

            for (Plant p : alive) {
                // Small health recovery via a gentle water+nutrient mix.
                p.applyRain(FERTILIZER_BOOST);
            }

            totalApplications++;
        }
    }

    public int getTotalApplications() {
        return totalApplications;
    }
}
