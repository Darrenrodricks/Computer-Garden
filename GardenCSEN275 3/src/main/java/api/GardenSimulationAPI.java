package api;

import engine.GardenEngine;
import logging.FileLogSink;
import modules.FertilizerModule;
import modules.HeatingModule;
import modules.PestControlModule;
import modules.WateringModule;

import java.util.Map;

/**
 * GardenSimulationAPI
 *
 * This is the class the grading script is expected to call directly.
 *
 * Why this class matters:
 * - The API PDF requires a public class that exposes a fixed set of methods.
 * - The GUI is NOT used for this part of grading.
 * - Because of that, this class must be simple, stable, and independent from JavaFX.
 *
 * Design idea:
 * - This class is intentionally thin.
 * - It does not contain the main simulation logic.
 * - Instead, it delegates all real work to GardenEngine.
 *
 * That separation helps because:
 * - the API stays small and easy to test,
 * - the engine can grow without changing the required API surface,
 * - teammates can work on modules/logging/UI without touching this file much.
 *
 * Modules and logging are automatically wired in on construction
 * so the grading script gets full functionality (watering, heating,
 * pest control, fertilizer, and file-based logging to log.txt).
 */
public class GardenSimulationAPI {

    /**
     * The engine contains the real garden state and simulation rules.
     * This API object simply forwards requests into the engine.
     */
    private final GardenEngine engine;

    /** File logger for writing events to log.txt. */
    private final FileLogSink logSink;

    /**
     * Default constructor used by the grader or by your own tests.
     *
     * We pass a RELATIVE config path here.
     * The project specification says paths should not be absolute.
     *
     * Expected location:
     * src/main/resources/config/garden.json
     *
     * This constructor also registers all modules and logging
     * so the simulation is fully functional out of the box.
     */
    public GardenSimulationAPI() {
        this.engine = new GardenEngine("config/garden.json");
        this.logSink = new FileLogSink();
        wireModulesAndLogging();
    }

    /**
     * Optional convenience constructor.
     *
     * This is helpful during development if you want to test
     * with a different config file, but the grader likely will not use it.
     */
    public GardenSimulationAPI(String configPath) {
        this.engine = new GardenEngine(configPath);
        this.logSink = new FileLogSink();
        wireModulesAndLogging();
    }

    /**
     * Registers all modules and the file logger on the engine.
     * Called once from the constructor.
     */
    private void wireModulesAndLogging() {
        engine.setLogSink(logSink);
        engine.addModule(new WateringModule());
        engine.addModule(new HeatingModule());
        engine.addModule(new PestControlModule());
        engine.addModule(new FertilizerModule());
    }

    /**
     * Required by the API spec.
     *
     * What it does:
     * - resets the garden
     * - loads plant definitions from the config
     * - ensures enough plants exist at the start
     * - ensures all plant varieties are represented
     */
    public void initializeGarden() {
        engine.initializeGarden();
    }

    /**
     * Required by the API spec.
     *
     * Expected return structure:
     * - "plants" -> List<String>
     * - "waterRequirement" -> List<Integer>
     * - "parasites" -> List<List<String>>
     *
     * Dead plants should not remain in the result.
     */
    public Map<String, Object> getPlants() {
        return engine.getPlants();
    }

    /**
     * Required by the API spec.
     *
     * Simulates a rainy day.
     * The engine handles:
     * - applying rain to plants
     * - giving modules a chance to react
     * - moving the simulation to the next day
     */
    public void rain(int amount) {
        engine.rain(amount);
    }

    /**
     * Required by the API spec.
     *
     * Simulates one day's temperature.
     * Valid testing range is expected to be 40..120 F.
     */
    public void temperature(int tempF) {
        engine.temperature(tempF);
    }

    /**
     * Required by the API spec.
     *
     * Simulates a parasite or pest event for the day.
     * Example values: "aphids", "mites", etc.
     */
    public void parasite(String parasiteName) {
        engine.parasite(parasiteName);
    }

    /**
     * Required by the API spec.
     *
     * This should record and/or print a clear current snapshot of the garden.
     * The engine handles the actual summary.
     */
    public void getState() {
        engine.getState();
    }
}
