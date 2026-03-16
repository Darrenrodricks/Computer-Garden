import api.GardenSimulationAPI;
import engine.GardenEngine;
import logging.FileLogSink;
import modules.FertilizerModule;
import modules.HeatingModule;
import modules.PestControlModule;
import modules.WateringModule;

/**
 * Simple console runner for local testing.
 *
 * This file is not part of the required API surface,
 * but it is very useful during development because it lets you
 * test the engine without needing the GUI.
 *
 * NOTE: For the grading script, GardenSimulationAPI is used directly.
 * This Main class wires up modules and logging so you can see them
 * working in the console output and log.txt.
 */
public class Main {

    public static void main(String[] args) {
        // Create the engine directly so we can attach modules + logging.
        GardenEngine engine = new GardenEngine("config/garden.json");

        // Set up logging to file.
        FileLogSink logSink = new FileLogSink();
        engine.setLogSink(logSink);

        // Register all 4 modules.
        engine.addModule(new WateringModule());
        engine.addModule(new HeatingModule());
        engine.addModule(new PestControlModule());
        engine.addModule(new FertilizerModule());

        // Initialize the garden.
        engine.initializeGarden();

        System.out.println("=== Garden Initialized ===");
        System.out.println("Alive plants: " + engine.countAlive());
        System.out.println();

        // Simulate several days of varied conditions.
        engine.rain(20);
        engine.temperature(85);
        engine.parasite("aphids");
        engine.rain(10);
        engine.temperature(45);
        engine.parasite("mites");
        engine.rain(30);
        engine.temperature(72);

        // Print final state.
        System.out.println();
        System.out.println("=== Final State ===");
        engine.getState();

        // Close the log file cleanly.
        logSink.close();

        System.out.println();
        System.out.println("Log written to log.txt");
    }
}
