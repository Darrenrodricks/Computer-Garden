package model;

/**
 * SensorType
 *
 * Minimal representation of the sensors present in the garden.
 *
 * Later, if your team wants more realism, this can become:
 * - an abstract Sensor class
 * - with MoistureSensor, TemperatureSensor, PestSensor subclasses
 *
 * For now, enums are enough to show that the system contains sensors.
 */
public enum SensorType {
    MOISTURE,
    TEMPERATURE,
    PEST
}
