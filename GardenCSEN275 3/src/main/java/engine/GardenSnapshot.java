package engine;

import model.SensorType;

import java.util.List;

/**
 * GardenSnapshot
 *
 * Purpose:
 * - Provide a compact, read-only summary of the garden.
 * - Useful for the UI teammate or logging teammate.
 *
 * Why not just expose the whole GardenEngine?
 * - A snapshot is safer and simpler.
 * - It prevents accidental modifications to the engine's internal state.
 */
public class GardenSnapshot {

    /** Current simulation day. */
    public final int day;

    /** Number of plants currently alive. */
    public final int plantsAlive;

    /** IDs of all living plants. */
    public final List<String> alivePlantIds;

    /** Names of active pests for the current day. */
    public final List<String> activePests;

    /** Sensors currently present in the garden. */
    public final List<SensorType> sensors;

    /** Number of sprinklers in the garden. */
    public final int sprinklerCount;

    public GardenSnapshot(
            int day,
            int plantsAlive,
            List<String> alivePlantIds,
            List<String> activePests,
            List<SensorType> sensors,
            int sprinklerCount
    ) {
        this.day = day;
        this.plantsAlive = plantsAlive;
        this.alivePlantIds = alivePlantIds;
        this.activePests = activePests;
        this.sensors = sensors;
        this.sprinklerCount = sprinklerCount;
    }
}
