package modules;

import engine.GardenEngine;
import engine.SimulationModule;
import model.Plant;

import java.util.List;

/**
 * HeatingModule
 *
 * Automated climate control module that detects extreme temperatures
 * and mitigates damage to plants.
 *
 * Behavior:
 * - When temperature is too cold: activates heaters, giving plants
 *   a small health recovery to partially offset cold damage.
 * - When temperature is too hot: activates cooling/shade systems,
 *   giving plants a small health recovery to partially offset heat damage.
 * - Does NOT fully heal plants (per the API spec, modules should not
 *   restore plants to full health instantly).
 *
 * The module uses a simple threshold system:
 * - Below 50F = cold stress zone, heaters activate
 * - Above 100F = heat stress zone, cooling activates
 * - Between 50-100F = comfortable, no action needed
 */
public class HeatingModule implements SimulationModule {

    private static final int COLD_THRESHOLD = 50;
    private static final int HOT_THRESHOLD = 100;

    /** Partial mitigation: recover a few health points (not full heal). */
    private static final int MITIGATION_RECOVERY = 3;

    private boolean heatersOn = false;
    private boolean coolingOn = false;

    @Override
    public String name() {
        return "HeatingModule";
    }

    /**
     * Respond to temperature events.
     *
     * If temperature is extreme, provide partial protection.
     * The plant still takes damage from Plant.applyTemperature(),
     * but this module softens the blow by recovering a few HP.
     */
    @Override
    public void onTemperature(GardenEngine engine, int tempF) {
        List<Plant> alive = engine.alivePlants();

        if (tempF < COLD_THRESHOLD) {
            heatersOn = true;
            coolingOn = false;

            // Partial mitigation: recover a little health for each plant.
            for (Plant p : alive) {
                // We use applyTemperature with a "safe" temperature to
                // simulate the heater bringing conditions closer to normal.
                // Instead, we directly give a small health bump via rain
                // (a small amount of warm water helps in cold).
                p.applyRain(2);
            }

        } else if (tempF > HOT_THRESHOLD) {
            heatersOn = false;
            coolingOn = true;

            // Partial mitigation: shade/misting helps a bit.
            for (Plant p : alive) {
                p.applyRain(3);
            }

        } else {
            // Comfortable range, turn everything off.
            heatersOn = false;
            coolingOn = false;
        }
    }

    /**
     * At end of day, reset climate systems.
     */
    @Override
    public void onDayEnd(GardenEngine engine) {
        heatersOn = false;
        coolingOn = false;
    }

    public boolean isHeatersOn() {
        return heatersOn;
    }

    public boolean isCoolingOn() {
        return coolingOn;
    }
}
