# Computerized Garden Simulation - User Manual

## Overview

This is an automated computerized gardening system that simulates a large garden
with multiple plant varieties, environmental sensors, sprinklers, and automated
modules for watering, climate control, pest management, and fertilization.

The system can run in two modes:
1. **GUI Mode** (JavaFX) - Interactive visual interface for managing the garden
2. **API Mode** (Console) - Headless mode used by the grading script

---

## How to Run

### GUI Mode (JavaFX)
```bash
# From the project root, compile and run:
javac -d out --module-path <javafx-lib-path> --add-modules javafx.controls \
    src/main/java/**/*.java
java --module-path <javafx-lib-path> --add-modules javafx.controls \
    -cp out ui.GardenApp
```

### API / Console Mode
```bash
javac -d out src/main/java/**/*.java
java -cp out Main
```

### Grading Script Mode
The grading script creates a `GardenSimulationAPI` object directly.
All modules and logging are wired in automatically by the constructor.
No additional setup is needed.

---

## GUI User Guide

### Layout
- **Top Bar**: Shows the current simulation day, number of alive plants, and status
- **Left Panel (Garden View)**: Visual display of all living plants as colored circles
  - Green = Healthy (health > 70)
  - Yellow = Stressed (health 40-70)
  - Red = Critical (health < 40)
  - Hover over any plant to see full details (ID, type, health, water, vulnerabilities)
- **Center Panel (Controls)**: Manual simulation controls
- **Right Panel (Module Status)**: Real-time status of all automated modules
- **Bottom Panel (Event Log)**: Live view of all events written to log.txt

### Manual Controls
1. **Rain**: Drag the slider to set rain amount (0-50), then click "Trigger Rain"
2. **Temperature**: Drag the slider to set temperature (40-120F), click "Set Temperature"
3. **Parasite**: Choose a parasite from the dropdown, click "Trigger Parasite"
4. **Get State**: Click to log a snapshot of the current garden state

### Auto-Simulation
Click "Start Auto-Sim" to run the simulation automatically:
- Random events (rain, temperature, parasites) are triggered every 3 seconds
- The simulation runs for 24 simulated days
- Click "Stop Auto-Sim" to pause at any time

### Reset
Click "Reset Garden" to reinitialize the garden from scratch.

---

## Garden Contents

### Plant Varieties
| Plant  | Water Requirement | Vulnerable To         | Safe Temp Range |
|--------|------------------|-----------------------|-----------------|
| Rose   | 10               | aphids, mites         | 55-95F          |
| Tomato | 15               | aphids, whiteflies    | 60-100F         |
| Basil  | 8                | mites                 | 60-95F          |

### Hardware
- **Sensors**: Moisture sensor, Temperature sensor, Pest detection sensor
- **Sprinklers**: 3 automated sprinklers (individually controllable by the WateringModule)

---

## Modules (4 Total)

### 1. WateringModule
- Monitors plant water levels via the moisture sensor
- Activates sprinklers when any plant's water level drops below 30
- Each active sprinkler provides 8 units of water per plant
- Automatically turns off sprinklers during rain events to prevent overwatering

### 2. HeatingModule
- Monitors temperature via the temperature sensor
- Activates heaters when temperature drops below 50F
- Activates cooling/shade systems when temperature exceeds 100F
- Provides partial mitigation (does not fully heal plants)
- All climate systems reset at end of day

### 3. PestControlModule
- Detects parasite infestations via the pest sensor
- Deploys targeted countermeasures for affected plant varieties
- Provides partial health recovery for vulnerable plants (4 HP mitigation)
- Tracks pest history for reporting
- Does NOT heal plants back to full health (per API specification)

### 4. FertilizerModule
- Applies slow-release fertilizer every 3 days
- Provides a small health boost (2 HP) to all living plants
- Tracks total fertilizer applications

---

## Logging System

### Log File
All events are recorded in `log.txt` in the working directory.

### Log Format
```
TIMESTAMP, DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE, DETAILS
```

### Event Types
| Event          | Description                                |
|----------------|--------------------------------------------|
| INITIALIZE     | Garden initialized with plants             |
| RAIN           | Rainfall event with amount                 |
| TEMPERATURE    | Temperature set for the day                |
| PARASITE       | Pest infestation triggered                 |
| DAY_END        | End-of-day summary with death count        |
| STATE          | Snapshot of current garden status           |
| ERROR          | Module error (caught, garden continues)    |
| MODULE_INIT    | Module initialization event                |
| MODULE_RAIN    | Module response to rain                    |
| MODULE_TEMPERATURE | Module response to temperature         |
| MODULE_PARASITE| Module response to parasite                |
| MODULE_DAY_END | Module end-of-day processing               |

### Example Log Entries
```
2025-03-10 14:30:00, 0, INITIALIZE, SUCCESS, 15, Seeded=15, Types=[Rose, Tomato, Basil]
2025-03-10 14:30:01, 0, RAIN, 20, 15, Applying rain
2025-03-10 14:30:01, 0, DAY_END, RAIN:20, 15, DeathsToday=0
2025-03-10 14:30:02, 1, TEMPERATURE, 85F, 15, Applying temperature
2025-03-10 14:30:02, 1, DAY_END, TEMPERATURE:85F, 14, DeathsToday=1
```

---

## Survivability Design

The system is designed to run for 24+ hours without crashing:

1. **Exception Handling**: All module callbacks are wrapped in try/catch.
   If a module throws an exception, the error is logged and the simulation continues.

2. **Fallback Config**: If the config file is missing or malformed, the system
   uses default plants (Rose and Tomato) instead of crashing.

3. **Automated Care**: The WateringModule, HeatingModule, PestControlModule,
   and FertilizerModule work together to keep plants alive under varying conditions.

4. **No Hardcoding**: Plant health is affected realistically by environmental events.
   Plants can and will die if conditions are harsh enough. The modules mitigate
   damage but do not prevent it entirely.

---

## Project Structure

```
src/main/java/
  api/
    GardenSimulationAPI.java    -- Public API (grading script entry point)
  engine/
    GardenEngine.java           -- Core simulation engine
    ConfigLoader.java           -- JSON config parser
    GardenSnapshot.java         -- Read-only state snapshot
    SimulationModule.java       -- Module interface
    LogSink.java                -- Logging interface
  model/
    Plant.java                  -- Individual plant instance
    PlantType.java              -- Shared plant type definition
    SensorType.java             -- Sensor enum
    Sprinkler.java              -- Sprinkler object
  modules/
    WateringModule.java         -- Automated irrigation
    HeatingModule.java          -- Climate control
    PestControlModule.java      -- Pest management
    FertilizerModule.java       -- Fertilization system
  logging/
    FileLogSink.java            -- File-based log writer
  ui/
    GardenApp.java              -- JavaFX GUI application
  Main.java                     -- Console test runner

src/main/resources/
  config/
    garden.json                 -- Plant configuration file
```

---

## Configuration

Edit `src/main/resources/config/garden.json` to change the initial garden setup:

```json
{
  "plants": [
    { "name": "Rose", "amount": 4 },
    { "name": "Tomato", "amount": 4 },
    { "name": "Basil", "amount": 4 }
  ]
}
```

Supported plant names: Rose, Tomato, Basil
(These are the built-in plant types registered in GardenEngine.)

---

## API Reference

See the separate Gardening System API document for the full API specification.

| Method               | Description                          |
|----------------------|--------------------------------------|
| initializeGarden()   | Seeds the garden from config         |
| getPlants()          | Returns map of alive plant data      |
| rain(int)            | Simulates rainfall                   |
| temperature(int)     | Sets daily temperature (40-120F)     |
| parasite(String)     | Triggers pest infestation            |
| getState()           | Logs current garden state            |
