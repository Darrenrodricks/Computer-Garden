package engine;

import model.Plant;
import model.PlantType;
import model.SensorType;
import model.Sprinkler;

import java.util.*;

/**
 * GardenEngine
 *
 * This class is the heart of the simulation.
 *
 * Responsibilities:
 * 1. Store all current garden state.
 * 2. Know what day the simulation is on.
 * 3. Apply external events like rain, temperature, and parasites.
 * 4. Update plant state over time.
 * 5. Let external modules react to events.
 * 6. Expose clean summaries for the API and UI/logging teammates.
 *
 * Important design choice:
 * - This engine does NOT depend on JavaFX.
 * - That keeps the API test standalone, exactly as required.
 */
public class GardenEngine {

    /**
     * Relative path to the JSON config that defines initial plant counts.
     */
    private final String configPath;

    /**
     * Current simulated day.
     *
     * Day starts at 0 when the garden is initialized.
     * Each API event (rain/temperature/parasite) advances the simulation by one day.
     */
    private int day = 0;

    /**
     * Main list of plants currently alive or potentially removable.
     *
     * Dead plants are removed at the end of each day to keep getPlants()
     * aligned with the API requirement.
     */
    private final List<Plant> plants = new ArrayList<>();

    /**
     * Master definitions of the plant varieties supported by the garden.
     *
     * Example:
     * - Rose
     * - Tomato
     * - Basil
     *
     * Each PlantType stores shared properties like:
     * - water requirement
     * - parasite vulnerabilities
     * - safe temperature range
     */
    private final Map<String, PlantType> plantTypes = new HashMap<>();

    /**
     * These objects exist to reflect the "big garden" idea from the requirements:
     * sensors, sprinklers, etc.
     *
     * For now, sensors are represented as enum values.
     * Sprinklers are real objects with IDs and on/off state.
     */
    private final List<SensorType> sensors = new ArrayList<>();
    private final List<Sprinkler> sprinklers = new ArrayList<>();

    /**
     * Tracks pests active during the current day only.
     * It gets cleared at the end of the day.
     */
    private final Set<String> activePestsToday = new HashSet<>();

    /**
     * Hooks for teammate-written modules like:
     * - WateringModule
     * - HeatingModule
     * - PestControlModule
     *
     * The engine does not need to know the concrete class names.
     * It only needs them to implement SimulationModule.
     */
    private final List<SimulationModule> modules = new ArrayList<>();

    /**
     * Logging hook.
     *
     * Right now it defaults to a no-op implementation so the engine can run
     * even before the logging teammate wires in a real log file writer.
     */
    private LogSink log = LogSink.noop();

    /**
     * Event values that apply only for the current simulated day.
     * These get reset after the day ends.
     */
    private Integer todaysRain = null;
    private Integer todaysTemp = null;

    /**
     * Constructor:
     * - remembers the config path
     * - registers plant types
     * - registers garden hardware like sensors/sprinklers
     */
    public GardenEngine(String configPath) {
        this.configPath = configPath;
        registerDefaultPlantTypes();
        registerDefaultHardware();
    }

    /**
     * Allows the logging teammate to inject a real logger.
     */
    public void setLogSink(LogSink sink) {
        this.log = (sink == null) ? LogSink.noop() : sink;
    }

    /**
     * Allows teammates to add modules to the simulation.
     */
    public void addModule(SimulationModule module) {
        if (module != null) {
            modules.add(module);
        }
    }

    public int getDay() {
        return day;
    }

    /**
     * Builds the garden from scratch.
     *
     * Steps:
     * 1. Clear old simulation data.
     * 2. Load configured plants.
     * 3. Ensure every plant variety is represented.
     * 4. Ensure at least 10 plants are alive at startup.
     * 5. Emit an initialization log event.
     * 6. Notify modules that initialization finished.
     */
    public void initializeGarden() {
        // Reset the simulation clock and world state.
        day = 0;
        plants.clear();
        activePestsToday.clear();
        resetDailyEnvironment();

        // Load plant seeds from config (or fallback defaults if config fails).
        List<ConfigLoader.PlantSeed> seeds = ConfigLoader.loadSeeds(configPath);

        // The spec says all varieties should be alive in the garden at the start.
        int idCounter = 1;
        for (PlantType type : plantTypes.values()) {
            plants.add(new Plant(type.getName() + "#" + (idCounter++), type));
        }

        // Add the quantities requested in the config file.
        for (ConfigLoader.PlantSeed seed : seeds) {
            PlantType type = plantTypes.get(seed.name);
            if (type == null) {
                // Ignore unknown plant names rather than crashing.
                continue;
            }

            for (int i = 0; i < seed.amount; i++) {
                plants.add(new Plant(type.getName() + "#" + (idCounter++), type));
            }
        }

        // The spec says at least 10 plants should be alive after initialization.
        while (countAlive() < 10) {
            PlantType fallback = plantTypes.values().iterator().next();
            plants.add(new Plant(fallback.getName() + "#" + (idCounter++), fallback));
        }

        // Record an initialization event.
        log.event(
                day,
                "INITIALIZE",
                "SUCCESS",
                countAlive(),
                "Seeded=" + countAlive() + ", Types=" + plantTypes.keySet()
        );

        // Give modules a chance to initialize their own internal state.
        for (SimulationModule m : modules) {
            safeRun(() -> m.onInitialize(this), "MODULE_INIT", m.name());
        }
    }

    /**
     * Returns the data structure required by the API specification.
     *
     * Before building the result, we remove dead plants to ensure the output only
     * reflects currently living plants.
     */
    public Map<String, Object> getPlants() {
        plants.removeIf(p -> !p.isAlive());

        List<String> names = new ArrayList<>();
        List<Integer> waterReq = new ArrayList<>();
        List<List<String>> parasites = new ArrayList<>();

        for (Plant p : plants) {
            names.add(p.getType().getName());
            waterReq.add(p.getType().getWaterRequirement());
            parasites.add(new ArrayList<>(p.getType().getParasiteVulnerabilities()));
        }

        Map<String, Object> out = new HashMap<>();
        out.put("plants", names);
        out.put("waterRequirement", waterReq);
        out.put("parasites", parasites);

        return out;
    }

    /**
     * Simulates one rainy day.
     *
     * Flow:
     * 1. Store rainfall as today's condition.
     * 2. Apply rain to every living plant.
     * 3. Let modules respond.
     * 4. End the day and advance time.
     */
    public void rain(int amount) {
        todaysRain = amount;

        log.event(day, "RAIN", String.valueOf(amount), countAlive(), "Applying rain");

        for (Plant p : plants) {
            if (p.isAlive()) {
                p.applyRain(amount);
            }
        }

        for (SimulationModule m : modules) {
            safeRun(() -> m.onRain(this, amount), "MODULE_RAIN", m.name());
        }

        endOfDay("RAIN", String.valueOf(amount));
    }

    /**
     * Simulates one temperature day.
     *
     * Plants outside their safe temperature range lose health.
     * Plants inside their safe range can recover a little.
     */
    public void temperature(int tempF) {
        todaysTemp = tempF;

        log.event(day, "TEMPERATURE", tempF + "F", countAlive(), "Applying temperature");

        for (Plant p : plants) {
            if (p.isAlive()) {
                p.applyTemperature(tempF);
            }
        }

        for (SimulationModule m : modules) {
            safeRun(() -> m.onTemperature(this, tempF), "MODULE_TEMPERATURE", m.name());
        }

        endOfDay("TEMPERATURE", tempF + "F");
    }

    /**
     * Simulates one parasite infestation day.
     *
     * Only plants vulnerable to the given parasite take direct damage.
     */
    public void parasite(String parasiteName) {
        if (parasiteName == null) {
            parasiteName = "unknown";
        }

        activePestsToday.add(parasiteName);

        log.event(day, "PARASITE", parasiteName, countAlive(), "Infestation triggered");

        for (Plant p : plants) {
            if (p.isAlive()) {
                p.applyParasite(parasiteName);
            }
        }

        for (SimulationModule m : modules) {
            String pest = parasiteName;
            safeRun(() -> m.onParasite(this, pest), "MODULE_PARASITE", m.name());
        }

        endOfDay("PARASITE", parasiteName);
    }

    /**
     * Produces a readable summary of the current state.
     *
     * This is a good place to:
     * - log alive plants
     * - show counts
     * - show current garden infrastructure
     */
    public void getState() {
        int alive = countAlive();

        log.event(
                day,
                "STATE",
                "SNAPSHOT",
                alive,
                "AliveIDs=" + alivePlantIds() + " | Sensors=" + sensors + " | Sprinklers=" + sprinklers.size()
        );

        // Console output is useful during development and harmless for testing.
        System.out.println("DAY " + day + ", STATE, SNAPSHOT, PLANTS_ALIVE=" + alive);
        System.out.println("Alive: " + alivePlantIds());
    }

    /**
     * Gives UI/logging teammates a compact read-only summary object.
     */
    public GardenSnapshot snapshot() {
        return new GardenSnapshot(
                day,
                countAlive(),
                alivePlantIds(),
                new ArrayList<>(activePestsToday),
                new ArrayList<>(sensors),
                sprinklers.size()
        );
    }

    /**
     * Handles the common "day ends now" logic.
     *
     * Why centralize this?
     * Because rain/temp/parasite all need the same end-of-day behavior.
     */
    private void endOfDay(String event, String value) {
        // Every living plant gets a daily metabolism tick.
        for (Plant p : plants) {
            if (p.isAlive()) {
                p.dailyTick();
            }
        }

        // Remove dead plants so future API results stay correct.
        int before = plants.size();
        plants.removeIf(p -> !p.isAlive());
        int after = plants.size();
        int deaths = Math.max(0, before - after);

        log.event(day, "DAY_END", event + ":" + value, after, "DeathsToday=" + deaths);

        // Advance the simulation clock.
        day++;

        // Reset conditions that should not persist automatically into the next day.
        resetDailyEnvironment();
        activePestsToday.clear();

        // Allow modules to do any cleanup or post-processing.
        for (SimulationModule m : modules) {
            safeRun(() -> m.onDayEnd(this), "MODULE_DAY_END", m.name());
        }
    }

    /**
     * Clears one-day-only environment values.
     */
    private void resetDailyEnvironment() {
        todaysRain = null;
        todaysTemp = null;
    }

    /**
     * Counts currently alive plants.
     */
    public int countAlive() {
        int alive = 0;
        for (Plant p : plants) {
            if (p.isAlive()) {
                alive++;
            }
        }
        return alive;
    }

    /**
     * Returns a new list containing only living plants.
     * We return a new list instead of the original list to reduce accidental external modification.
     */
    public List<Plant> alivePlants() {
        List<Plant> out = new ArrayList<>();
        for (Plant p : plants) {
            if (p.isAlive()) {
                out.add(p);
            }
        }
        return out;
    }

    public Integer getTodaysRain() {
        return todaysRain;
    }

    public Integer getTodaysTemp() {
        return todaysTemp;
    }

    public List<SensorType> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    public List<Sprinkler> getSprinklers() {
        return Collections.unmodifiableList(sprinklers);
    }

    /**
     * Helper used in logs and snapshots.
     */
    private List<String> alivePlantIds() {
        List<String> ids = new ArrayList<>();
        for (Plant p : plants) {
            if (p.isAlive()) {
                ids.add(p.getId());
            }
        }
        return ids;
    }

    /**
     * Registers the built-in plant varieties supported by the simulation.
     *
     * PlantType parameters:
     * - name
     * - water requirement
     * - parasite vulnerabilities
     * - minimum safe temperature
     * - maximum safe temperature
     */
    private void registerDefaultPlantTypes() {
        plantTypes.put("Rose", new PlantType("Rose", 10, List.of("aphids", "mites"), 55, 95));
        plantTypes.put("Tomato", new PlantType("Tomato", 15, List.of("aphids", "whiteflies"), 60, 100));
        plantTypes.put("Basil", new PlantType("Basil", 8, List.of("mites"), 60, 95));
    }

    /**
     * Registers example hardware for the garden.
     *
     * This satisfies the requirement that the garden contain things such as:
     * - sensors
     * - sprinklers
     * - multiple objects beyond just plants
     */
    private void registerDefaultHardware() {
        sensors.add(SensorType.MOISTURE);
        sensors.add(SensorType.TEMPERATURE);
        sensors.add(SensorType.PEST);

        sprinklers.add(new Sprinkler("Sprinkler#1"));
        sprinklers.add(new Sprinkler("Sprinkler#2"));
        sprinklers.add(new Sprinkler("Sprinkler#3"));
    }

    /**
     * Defensive wrapper around teammate code.
     *
     * This is important because the requirements strongly emphasize survivability.
     * If a module throws an exception, we log it and continue instead of crashing.
     */
    private void safeRun(Runnable r, String event, String moduleName) {
        try {
            r.run();
        } catch (Exception e) {
            log.event(day, "ERROR", event, countAlive(), moduleName + ": " + e.getMessage());
        }
    }
}
