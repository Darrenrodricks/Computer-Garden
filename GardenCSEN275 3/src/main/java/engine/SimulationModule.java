package engine;

/**
 * SimulationModule
 *
 * This is the interface your modules teammate should implement.
 *
 * Why an interface?
 * - The engine should not depend on specific classes like WateringModule directly.
 * - That keeps the system more modular and easier to expand.
 *
 * Typical teammate implementations:
 * - WateringModule
 * - HeatingModule
 * - PestControlModule
 */
public interface SimulationModule {

    /**
     * Default helper for logging/debugging.
     * Concrete classes can override this if they want a custom module name.
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Called after the garden finishes initializing.
     */
    default void onInitialize(GardenEngine engine) {}

    /**
     * Called when the simulation receives rain for the day.
     */
    default void onRain(GardenEngine engine, int amount) {}

    /**
     * Called when the simulation receives a temperature event.
     */
    default void onTemperature(GardenEngine engine, int tempF) {}

    /**
     * Called when the simulation receives a parasite event.
     */
    default void onParasite(GardenEngine engine, String parasite) {}

    /**
     * Called after the engine finishes its end-of-day processing.
     */
    default void onDayEnd(GardenEngine engine) {}
}
