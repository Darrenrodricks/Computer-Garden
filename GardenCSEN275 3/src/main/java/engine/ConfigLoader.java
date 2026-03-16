package engine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * ConfigLoader
 *
 * Purpose:
 * - Read the garden seed configuration from JSON.
 * - Convert it into a simple list of (name, amount) pairs.
 *
 * Why keep this separate?
 * - GardenEngine should focus on simulation logic, not file parsing.
 * - Keeping file loading isolated makes the engine easier to test.
 *
 * Note:
 * - This parser is intentionally simple.
 * - Later, your team can replace it with Gson or Jackson if desired.
 */
public class ConfigLoader {

    /**
     * Small immutable record-like class representing one entry from the config file.
     *
     * Example:
     * { "name": "Rose", "amount": 5 }
     */
    public static class PlantSeed {
        public final String name;
        public final int amount;

        public PlantSeed(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    /**
     * Attempts to load plant seed entries from a relative config path.
     *
     * Strategy:
     * 1. Try several likely relative locations.
     * 2. If none work, return a safe fallback list.
     *
     * This prevents the simulation from failing completely just because the config file
     * is missing or placed in a slightly different development location.
     */
    public static List<PlantSeed> loadSeeds(String configPath) {
        List<String> candidates = List.of(
                configPath,
                "src/main/resources/" + configPath,
                "src/main/resources/config/garden.json",
                "config/garden.json"
        );

        for (String p : candidates) {
            List<PlantSeed> parsed = tryParse(p);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        // Safe fallback if file is missing or malformed.
        return List.of(
                new PlantSeed("Rose", 5),
                new PlantSeed("Tomato", 5)
        );
    }

    /**
     * Tries to parse one candidate file.
     *
     * Returns:
     * - parsed list if successful
     * - empty list if anything fails
     *
     * Returning an empty list instead of throwing is intentional:
     * it lets loadSeeds() try the next candidate path.
     */
    private static List<PlantSeed> tryParse(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return List.of();
            }

            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);

            List<PlantSeed> seeds = new ArrayList<>();

            /**
             * Very lightweight parsing approach:
             * - split on "{"
             * - inspect chunks containing both "name" and "amount"
             *
             * This is not a full JSON parser, but it is enough for the simple config format.
             */
            String[] blocks = json.split("\\{");
            for (String b : blocks) {
                if (!b.contains("\"name\"") || !b.contains("\"amount\"")) {
                    continue;
                }

                String name = extractString(b, "\"name\"");
                Integer amount = extractInt(b, "\"amount\"");

                if (name != null && amount != null && amount > 0) {
                    seeds.add(new PlantSeed(name, amount));
                }
            }

            return seeds;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Extracts a quoted string value associated with a given key from a chunk.
     *
     * Example:
     * chunk contains -> "name": "Rose"
     * key -> "\"name\""
     * result -> "Rose"
     */
    private static String extractString(String chunk, String key) {
        int i = chunk.indexOf(key);
        if (i < 0) {
            return null;
        }

        int q1 = chunk.indexOf("\"", i + key.length());
        int q2 = (q1 < 0) ? -1 : chunk.indexOf("\"", q1 + 1);

        if (q1 < 0 || q2 < 0) {
            return null;
        }

        return chunk.substring(q1 + 1, q2).trim();
    }

    /**
     * Extracts an integer value associated with a given key from a chunk.
     *
     * Example:
     * chunk contains -> "amount": 5
     * result -> 5
     */
    private static Integer extractInt(String chunk, String key) {
        int i = chunk.indexOf(key);
        if (i < 0) {
            return null;
        }

        int colon = chunk.indexOf(":", i);
        if (colon < 0) {
            return null;
        }

        String after = chunk.substring(colon + 1).replaceAll("[^0-9]", " ").trim();
        if (after.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(after.split("\\s+")[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
