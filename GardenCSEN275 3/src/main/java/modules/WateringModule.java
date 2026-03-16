package modules;

import engine.GardenEngine;
import engine.SimulationModule;
import model.Plant;
import model.Sprinkler;

import java.util.List;

/**
 * WateringModule
 *
 * Automated irrigation module that monitors plant water levels
 * and activates sprinklers when plants are getting too dry.
 *
 * Behavior:
 * - On rain: turns off sprinklers (nature is watering for us).
 * - On day end: checks each plant's water level and activates
 *   sprinklers if any plant is below the dry threshold.
 * - When sprinklers are on, each living plant gets a small water boost.
 */
public class WateringModule implements SimulationModule {

    /** Plants below this water level trigger the sprinklers. */
    private static final int DRY_THRESHOLD = 30;

    /** How much water each sprinkler cycle adds to a plant. */
    private static final int SPRINKLER_WATER_AMOUNT = 8;

    /** Track whether sprinklers were activated this cycle. */
    private boolean sprinklersActivatedToday = false;

    @Override
    public String name() {
        return "WateringModule";
    }

    /**
     * When it rains, we turn off the sprinklers to avoid overwatering.
     */
    @Override
    public void onRain(GardenEngine engine, int amount) {
        for (Sprinkler s : engine.getSprinklers()) {
            s.setOn(false);
        }
        sprinklersActivatedToday = false;
    }

    /**
     * At end of day, check if any living plant is too dry.
     * If so, turn on sprinklers and give each plant a water boost.
     */
    @Override
    public void onDayEnd(GardenEngine engine) {
        List<Plant> alive = engine.alivePlants();

        boolean needsWater = false;
        for (Plant p : alive) {
            if (p.getWaterLevel() < DRY_THRESHOLD) {
                needsWater = true;
                break;
            }
        }

        if (needsWater) {
            // Turn on all sprinklers.
            for (Sprinkler s : engine.getSprinklers()) {
                s.setOn(true);
            }
            sprinklersActivatedToday = true;

            // Each active sprinkler contributes water to the plants.
            int activeSprinklers = 0;
            for (Sprinkler s : engine.getSprinklers()) {
                if (s.isOn()) {
                    activeSprinklers++;
                }
            }

            int totalWater = activeSprinklers * SPRINKLER_WATER_AMOUNT;
            for (Plant p : alive) {
                p.applyRain(totalWater);
            }
        } else {
            // Plants are fine, turn off sprinklers to conserve.
            for (Sprinkler s : engine.getSprinklers()) {
                s.setOn(false);
            }
            sprinklersActivatedToday = false;
        }
    }

    public boolean wasSprinklersActivatedToday() {
        return sprinklersActivatedToday;
    }
}
