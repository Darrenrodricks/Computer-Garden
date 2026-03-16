package model;

/**
 * Sprinkler
 *
 * Minimal representation of a sprinkler object in the garden.
 *
 * Why keep this as an object instead of just an integer count?
 * - It gives you room to expand later.
 * - The modules teammate can turn specific sprinklers on/off.
 * - The UI teammate can display individual sprinklers if desired.
 */
public class Sprinkler {

    /** Unique sprinkler ID. */
    private final String id;

    /** Whether this sprinkler is currently active. */
    private boolean on;

    public Sprinkler(String id) {
        this.id = id;
        this.on = false;
    }

    public String getId() {
        return id;
    }

    public boolean isOn() {
        return on;
    }

    /**
     * Turns this sprinkler on or off.
     */
    public void setOn(boolean on) {
        this.on = on;
    }
}
