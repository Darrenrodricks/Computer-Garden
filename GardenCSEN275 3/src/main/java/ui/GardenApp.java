package ui;

import api.GardenSimulationAPI;
import engine.GardenEngine;
import engine.GardenSnapshot;
import logging.FileLogSink;
import model.Plant;
import model.Sprinkler;
import modules.FertilizerModule;
import modules.HeatingModule;
import modules.PestControlModule;
import modules.WateringModule;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class GardenApp extends Application {

    // ── Color Palette ──────────────────────────────────────────────
    private static final String BG_CREAM       = "#FAF7F2";
    private static final String CARD_WHITE     = "#FFFFFF";
    private static final String BORDER_LIGHT   = "#E8E2D9";
    private static final String TEXT_DARK      = "#2C2416";
    private static final String TEXT_MID       = "#6B5D4D";
    private static final String TEXT_LIGHT     = "#9C8E7C";
    private static final String ACCENT_GREEN   = "#4A7C59";
    private static final String ACCENT_WARM    = "#C4813D";
    private static final String ACCENT_RED     = "#B85450";
    private static final String ACCENT_BLUE    = "#5B8FA8";
    private static final String HEADER_BG      = "#3B5A3E";
    private static final String HEADER_BG_DARK = "#2D4630";
    private static final String LOG_BG         = "#1E2420";
    private static final String LOG_TEXT       = "#A8C4A0";
    private static final String HEALTHY_BG     = "#E8F5E3";
    private static final String STRESSED_BG    = "#FFF8E7";
    private static final String CRITICAL_BG    = "#FDEEEE";
    private static final String HEALTHY_BORDER = "#9ECB94";
    private static final String STRESSED_BORDER = "#E8D08C";
    private static final String CRITICAL_BORDER = "#E0A5A3";
    private static final String MODULE_CARD_BG = "#F5F2EC";

    // ── Plant Image URLs (Wikimedia) ───
    private static final Map<String, String> PLANT_IMAGES = new HashMap<>();
    static {
        // High-quality, free-to-use plant images
        PLANT_IMAGES.put("Rose",   "https://images.unsplash.com/photo-1490750967868-88aa4f44baee?w=200&h=200&fit=crop&crop=center");
        PLANT_IMAGES.put("Tomato", "https://images.unsplash.com/photo-1592841200221-a6898f307baa?w=200&h=200&fit=crop&crop=center");
        PLANT_IMAGES.put("Basil",  "https://images.unsplash.com/photo-1618375569909-3c8616cf7733?w=200&h=200&fit=crop&crop=center");
    }

    // ── Cached images ─────────────────────────────────────────────
    private final Map<String, Image> imageCache = new HashMap<>();

    // ── Core simulation objects ───────────────────────────────────
    private GardenEngine engine;
    private FileLogSink logSink;
    private WateringModule wateringModule;
    private HeatingModule heatingModule;
    private PestControlModule pestControlModule;
    private FertilizerModule fertilizerModule;

    // ── UI components ─────────────────────────
    private Label dayLabel;
    private Label aliveLabel;
    private Label statusLabel;
    private FlowPane gardenPane;
    private TextArea logArea;
    private Label sprinklerStatusLabel;
    private Label heaterStatusLabel;
    private Label coolingStatusLabel;
    private Label pestControlStatusLabel;
    private Label fertilizerStatusLabel;
    private Label sprinklerDot;
    private Label heaterDot;
    private Label coolingDot;
    private Label pestDot;

    // ── Auto-simulation timer ────────────────────────────────────
    private Timer autoSimTimer;
    private boolean autoSimRunning = false;

    @Override
    public void start(Stage primaryStage) {
        // Initialize the engine
        engine = new GardenEngine("config/garden.json");
        logSink = new FileLogSink();
        engine.setLogSink(logSink);

        wateringModule = new WateringModule();
        heatingModule = new HeatingModule();
        pestControlModule = new PestControlModule();
        fertilizerModule = new FertilizerModule();
        engine.addModule(wateringModule);
        engine.addModule(heatingModule);
        engine.addModule(pestControlModule);
        engine.addModule(fertilizerModule);
        engine.initializeGarden();

        // Preload images in background
        preloadImages();

        // ── Root Layout ──────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_CREAM + ";");

        root.setTop(createHeader());

        // Main content: 3-column layout
        HBox mainContent = new HBox(16);
        mainContent.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        VBox gardenColumn = createGardenView();
        HBox.setHgrow(gardenColumn, Priority.ALWAYS);

        VBox controlColumn = createControlPanel();
        controlColumn.setMinWidth(300);
        controlColumn.setMaxWidth(320);

        VBox moduleColumn = createModulePanel();
        moduleColumn.setMinWidth(230);
        moduleColumn.setMaxWidth(250);

        mainContent.getChildren().addAll(gardenColumn, controlColumn, moduleColumn);
        root.setCenter(mainContent);
        root.setBottom(createLogViewer());

        Scene scene = new Scene(root, 1200, 820);

        // Load Google Fonts via CSS
        scene.getStylesheets().add("data:text/css," +
                java.net.URLEncoder.encode(getGlobalCSS(), java.nio.charset.StandardCharsets.UTF_8)
        );

        primaryStage.setTitle("Garden Simulation");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();

        refreshAll();
    }

    /**
     * Global CSS for consistent styling across all components.
     */
    private String getGlobalCSS() {
        return
                ".scroll-pane { -fx-background: transparent; -fx-background-color: transparent; }" +
                        ".scroll-pane .viewport { -fx-background-color: transparent; }" +
                        ".scroll-pane .scroll-bar { -fx-background-color: transparent; }" +
                        ".scroll-pane .scroll-bar .thumb { -fx-background-color: #C4B9A8; -fx-background-radius: 4; }" +
                        ".scroll-pane .scroll-bar .track { -fx-background-color: transparent; }" +
                        ".scroll-pane .scroll-bar .increment-button, .scroll-pane .scroll-bar .decrement-button { -fx-opacity: 0; }" +
                        ".slider .track { -fx-background-color: " + BORDER_LIGHT + "; -fx-background-radius: 4; -fx-pref-height: 6; }" +
                        ".slider .thumb { -fx-background-color: " + ACCENT_GREEN + "; -fx-background-radius: 10; -fx-pref-width: 18; -fx-pref-height: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1); }" +
                        ".combo-box { -fx-background-color: " + CARD_WHITE + "; -fx-border-color: " + BORDER_LIGHT + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 10; -fx-font-size: 13; }" +
                        ".combo-box .list-cell { -fx-font-size: 13; -fx-text-fill: " + TEXT_DARK + "; }" +
                        ".text-area { -fx-font-family: 'Menlo', 'Consolas', monospace; -fx-font-size: 11.5; }" +
                        ".text-area .content { -fx-background-color: " + LOG_BG + "; }" +
                        ".separator .line { -fx-border-color: " + BORDER_LIGHT + "; -fx-border-width: 0.5 0 0 0; }";
    }

    /**
     * Preloads plant images in background threads.
     */
    private void preloadImages() {
        for (Map.Entry<String, String> entry : PLANT_IMAGES.entrySet()) {
            try {
                Image img = new Image(entry.getValue(), 120, 120, true, true, true);
                imageCache.put(entry.getKey(), img);
            } catch (Exception e) {
                // Image loading failed; will use fallback
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════

    private VBox createHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " + HEADER_BG + ", " + HEADER_BG_DARK + ");"
        );

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Leaf icon
        SVGPath leaf = new SVGPath();
        leaf.setContent("M12 2C6.5 2 2 6.5 2 12c0 4 2.5 7.5 6 9 0-4 1.5-7 4-9.5C14.5 9 17 7.5 21 7c-1-3-4.5-5-9-5z");
        leaf.setFill(Color.web("#8BC48A"));
        leaf.setScaleX(1.1);
        leaf.setScaleY(1.1);

        Text title = new Text("Garden Simulation");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stats badges
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_RIGHT);

        dayLabel = createStatBadge("DAY", "0", "#8BC48A");
        aliveLabel = createStatBadge("ALIVE", "0", "#F0D68C");
        statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font("SansSerif", 13));
        statusLabel.setTextFill(Color.web("#B8C9B4"));
        statusLabel.setStyle("-fx-padding: 4 12; -fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 12;");

        statsRow.getChildren().addAll(dayLabel, aliveLabel, statusLabel);
        titleRow.getChildren().addAll(leaf, title, spacer, statsRow);
        header.getChildren().add(titleRow);

        return header;
    }

    /**
     * Creates a compact stat badge for the header.
     */
    private Label createStatBadge(String labelText, String value, String color) {
        Label badge = new Label(labelText + "  " + value);
        badge.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        badge.setTextFill(Color.web(color));
        badge.setStyle(
                "-fx-padding: 4 14; " +
                        "-fx-background-color: rgba(255,255,255,0.1); " +
                        "-fx-background-radius: 14; " +
                        "-fx-border-color: rgba(255,255,255,0.12); " +
                        "-fx-border-radius: 14;"
        );
        return badge;
    }

    // ══════════════════════════════════════════════════════════════
    //  GARDEN VIEW (Left Column)
    // ══════════════════════════════════════════════════════════════

    private VBox createGardenView() {
        VBox gardenBox = new VBox(12);
        gardenBox.setPadding(new Insets(0));

        // Section header
        HBox sectionHeader = new HBox(8);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        Label gardenTitle = new Label("Your Garden");
        gardenTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        gardenTitle.setTextFill(Color.web(TEXT_DARK));

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Label legend = new Label("● Healthy   ● Stressed   ● Critical");
        legend.setFont(Font.font("SansSerif", 11));
        legend.setTextFill(Color.web(TEXT_LIGHT));

        sectionHeader.getChildren().addAll(gardenTitle, titleSpacer, legend);

        // Garden grid container
        gardenPane = new FlowPane(12, 12);
        gardenPane.setPadding(new Insets(16));
        gardenPane.setPrefWrapLength(500);
        gardenPane.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(gardenPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_CREAM + "; -fx-background-color: " + BG_CREAM + ";");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        gardenBox.getChildren().addAll(sectionHeader, scrollPane);
        return gardenBox;
    }

    // ══════════════════════════════════════════════════════════════
    //  CONTROL PANEL (Center Column)
    // ══════════════════════════════════════════════════════════════

    private VBox createControlPanel() {
        VBox controls = new VBox(14);
        controls.setPadding(new Insets(0));

        Label controlTitle = new Label("Controls");
        controlTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        controlTitle.setTextFill(Color.web(TEXT_DARK));

        // ─── Rain Card ───
        VBox rainCard = createCard();
        Label rainIcon = new Label("\u2602");  // Umbrella
        rainIcon.setFont(Font.font("SansSerif", 20));
        Label rainTitle = new Label("  Rainfall");
        rainTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        rainTitle.setTextFill(Color.web(TEXT_DARK));
        HBox rainHeader = new HBox(4);
        rainHeader.setAlignment(Pos.CENTER_LEFT);
        rainHeader.getChildren().addAll(rainIcon, rainTitle);

        Slider rainSlider = new Slider(0, 50, 15);
        rainSlider.setShowTickLabels(true);
        rainSlider.setShowTickMarks(false);
        rainSlider.setMajorTickUnit(10);
        rainSlider.setMinorTickCount(0);
        Label rainValue = new Label("15 mm");
        rainValue.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        rainValue.setTextFill(Color.web(ACCENT_BLUE));
        rainSlider.valueProperty().addListener((obs, o, v) ->
                rainValue.setText(v.intValue() + " mm"));

        Button rainBtn = createActionButton("Trigger Rain", ACCENT_BLUE);
        rainBtn.setOnAction(e -> {
            engine.rain((int) rainSlider.getValue());
            statusLabel.setText("Rain  (" + (int) rainSlider.getValue() + " mm)");
            refreshAll();
        });

        HBox rainValueRow = new HBox();
        rainValueRow.setAlignment(Pos.CENTER_RIGHT);
        rainValueRow.getChildren().add(rainValue);

        rainCard.getChildren().addAll(rainHeader, rainSlider, rainValueRow, rainBtn);

        // ─── Temperature Card ───
        VBox tempCard = createCard();
        Label tempIcon = new Label("\u2600");  // Sun
        tempIcon.setFont(Font.font("SansSerif", 20));
        Label tempTitle = new Label("  Temperature");
        tempTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        tempTitle.setTextFill(Color.web(TEXT_DARK));
        HBox tempHeader = new HBox(4);
        tempHeader.setAlignment(Pos.CENTER_LEFT);
        tempHeader.getChildren().addAll(tempIcon, tempTitle);

        Slider tempSlider = new Slider(40, 120, 72);
        tempSlider.setShowTickLabels(true);
        tempSlider.setShowTickMarks(false);
        tempSlider.setMajorTickUnit(20);
        tempSlider.setMinorTickCount(0);
        Label tempValue = new Label("72\u00B0F");
        tempValue.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        tempValue.setTextFill(Color.web(ACCENT_WARM));
        tempSlider.valueProperty().addListener((obs, o, v) ->
                tempValue.setText(v.intValue() + "\u00B0F"));

        Button tempBtn = createActionButton("Set Temperature", ACCENT_WARM);
        tempBtn.setOnAction(e -> {
            engine.temperature((int) tempSlider.getValue());
            statusLabel.setText("Temp set  " + (int) tempSlider.getValue() + "\u00B0F");
            refreshAll();
        });

        HBox tempValueRow = new HBox();
        tempValueRow.setAlignment(Pos.CENTER_RIGHT);
        tempValueRow.getChildren().add(tempValue);

        tempCard.getChildren().addAll(tempHeader, tempSlider, tempValueRow, tempBtn);

        // ─── Parasite Card ───
        VBox parasiteCard = createCard();
        Label bugIcon = new Label("\uD83D\uDC1B");  // Bug emoji
        bugIcon.setFont(Font.font("SansSerif", 18));
        Label parasiteTitle = new Label("  Pest Attack");
        parasiteTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        parasiteTitle.setTextFill(Color.web(TEXT_DARK));
        HBox parasiteHeader = new HBox(4);
        parasiteHeader.setAlignment(Pos.CENTER_LEFT);
        parasiteHeader.getChildren().addAll(bugIcon, parasiteTitle);

        ComboBox<String> parasiteCombo = new ComboBox<>();
        parasiteCombo.getItems().addAll("aphids", "mites", "whiteflies", "beetles", "slugs");
        parasiteCombo.setValue("aphids");
        parasiteCombo.setMaxWidth(Double.MAX_VALUE);

        Button parasiteBtn = createActionButton("Deploy Pest", ACCENT_RED);
        parasiteBtn.setOnAction(e -> {
            engine.parasite(parasiteCombo.getValue());
            statusLabel.setText("Pest attack  (" + parasiteCombo.getValue() + ")");
            refreshAll();
        });

        parasiteCard.getChildren().addAll(parasiteHeader, parasiteCombo, parasiteBtn);

        // ─── Actions Row ───
        HBox actionsRow = new HBox(8);

        Button stateBtn = new Button("Log Snapshot");
        stateBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(stateBtn, Priority.ALWAYS);
        stateBtn.setStyle(
                "-fx-background-color: " + CARD_WHITE + "; " +
                        "-fx-border-color: " + BORDER_LIGHT + "; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-text-fill: " + TEXT_MID + "; " +
                        "-fx-font-size: 12; -fx-padding: 8 14; -fx-cursor: hand;"
        );
        stateBtn.setOnAction(e -> {
            engine.getState();
            statusLabel.setText("State logged");
            refreshAll();
        });

        Button resetBtn = new Button("Reset");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);
        resetBtn.setStyle(
                "-fx-background-color: " + CARD_WHITE + "; " +
                        "-fx-border-color: " + BORDER_LIGHT + "; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-text-fill: " + TEXT_MID + "; " +
                        "-fx-font-size: 12; -fx-padding: 8 14; -fx-cursor: hand;"
        );
        resetBtn.setOnAction(e -> {
            stopAutoSim();
            engine.initializeGarden();
            statusLabel.setText("Garden reset");
            refreshAll();
        });

        actionsRow.getChildren().addAll(stateBtn, resetBtn);

        // ─── Auto-Sim Card ───
        VBox autoCard = createCard();
        autoCard.setStyle(autoCard.getStyle() + "-fx-border-color: " + ACCENT_GREEN + "; -fx-border-width: 1.5;");

        Label autoTitle = new Label("Auto-Simulation");
        autoTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        autoTitle.setTextFill(Color.web(ACCENT_GREEN));

        Label autoDesc = new Label("Runs random weather and pest events every 3 seconds for 24 simulated days.");
        autoDesc.setFont(Font.font("SansSerif", 11));
        autoDesc.setTextFill(Color.web(TEXT_LIGHT));
        autoDesc.setWrapText(true);

        Button autoBtn = createActionButton("Start Auto-Sim", ACCENT_GREEN);
        autoBtn.setOnAction(e -> {
            if (!autoSimRunning) {
                startAutoSim();
                autoBtn.setText("Stop Auto-Sim");
                autoBtn.setStyle(createButtonStyle(ACCENT_RED));
            } else {
                stopAutoSim();
                autoBtn.setText("Start Auto-Sim");
                autoBtn.setStyle(createButtonStyle(ACCENT_GREEN));
            }
        });

        autoCard.getChildren().addAll(autoTitle, autoDesc, autoBtn);

        controls.getChildren().addAll(
                controlTitle,
                rainCard, tempCard, parasiteCard,
                actionsRow,
                autoCard
        );

        ScrollPane controlScroll = new ScrollPane(controls);
        controlScroll.setFitToWidth(true);
        controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setStyle("-fx-background: " + BG_CREAM + "; -fx-background-color: transparent;");

        VBox wrapper = new VBox(controlScroll);
        wrapper.setMinWidth(300);
        wrapper.setMaxWidth(320);
        VBox.setVgrow(controlScroll, Priority.ALWAYS);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════
    //  MODULE STATUS (Right Column)
    // ══════════════════════════════════════════════════════════════

    private VBox createModulePanel() {
        VBox moduleBox = new VBox(12);
        moduleBox.setPadding(new Insets(0));

        Label moduleTitle = new Label("System Status");
        moduleTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        moduleTitle.setTextFill(Color.web(TEXT_DARK));

        // ─── Watering Module ───
        VBox waterCard = createModuleCard();
        HBox waterHeader = createModuleHeader("\uD83D\uDCA7", "Watering System");
        sprinklerDot = createStatusDot(false);
        sprinklerStatusLabel = new Label("Sprinklers OFF");
        sprinklerStatusLabel.setFont(Font.font("SansSerif", 12));
        sprinklerStatusLabel.setTextFill(Color.web(TEXT_LIGHT));
        HBox sprinklerRow = new HBox(8);
        sprinklerRow.setAlignment(Pos.CENTER_LEFT);
        sprinklerRow.getChildren().addAll(sprinklerDot, sprinklerStatusLabel);
        waterCard.getChildren().addAll(waterHeader, sprinklerRow);

        // ─── Climate Module ───
        VBox climateCard = createModuleCard();
        HBox climateHeader = createModuleHeader("\uD83C\uDF21", "Climate Control");

        heaterDot = createStatusDot(false);
        heaterStatusLabel = new Label("Heaters OFF");
        heaterStatusLabel.setFont(Font.font("SansSerif", 12));
        heaterStatusLabel.setTextFill(Color.web(TEXT_LIGHT));
        HBox heaterRow = new HBox(8);
        heaterRow.setAlignment(Pos.CENTER_LEFT);
        heaterRow.getChildren().addAll(heaterDot, heaterStatusLabel);

        coolingDot = createStatusDot(false);
        coolingStatusLabel = new Label("Cooling OFF");
        coolingStatusLabel.setFont(Font.font("SansSerif", 12));
        coolingStatusLabel.setTextFill(Color.web(TEXT_LIGHT));
        HBox coolingRow = new HBox(8);
        coolingRow.setAlignment(Pos.CENTER_LEFT);
        coolingRow.getChildren().addAll(coolingDot, coolingStatusLabel);

        climateCard.getChildren().addAll(climateHeader, heaterRow, coolingRow);

        // ─── Pest Control Module ───
        VBox pestCard = createModuleCard();
        HBox pestHeader = createModuleHeader("\uD83D\uDEE1", "Pest Control");
        pestDot = createStatusDot(false);
        pestControlStatusLabel = new Label("Idle");
        pestControlStatusLabel.setFont(Font.font("SansSerif", 12));
        pestControlStatusLabel.setTextFill(Color.web(TEXT_LIGHT));
        HBox pestRow = new HBox(8);
        pestRow.setAlignment(Pos.CENTER_LEFT);
        pestRow.getChildren().addAll(pestDot, pestControlStatusLabel);
        pestCard.getChildren().addAll(pestHeader, pestRow);

        // ─── Fertilizer Module ───
        VBox fertCard = createModuleCard();
        HBox fertHeader = createModuleHeader("\uD83C\uDF31", "Fertilizer");
        fertilizerStatusLabel = new Label("0 applications");
        fertilizerStatusLabel.setFont(Font.font("SansSerif", 12));
        fertilizerStatusLabel.setTextFill(Color.web(TEXT_LIGHT));
        fertCard.getChildren().addAll(fertHeader, fertilizerStatusLabel);

        // ─── Hardware Info ───
        VBox hwCard = createModuleCard();
        Label hwTitle = new Label("Infrastructure");
        hwTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        hwTitle.setTextFill(Color.web(TEXT_DARK));

        Label sensorsInfo = new Label("Sensors: Moisture, Temperature, Pest");
        sensorsInfo.setFont(Font.font("SansSerif", 11));
        sensorsInfo.setTextFill(Color.web(TEXT_LIGHT));
        sensorsInfo.setWrapText(true);

        Label sprinklersInfo = new Label("Sprinklers: 3 installed");
        sprinklersInfo.setFont(Font.font("SansSerif", 11));
        sprinklersInfo.setTextFill(Color.web(TEXT_LIGHT));

        hwCard.getChildren().addAll(hwTitle, sensorsInfo, sprinklersInfo);

        moduleBox.getChildren().addAll(
                moduleTitle,
                waterCard, climateCard, pestCard, fertCard,
                hwCard
        );

        ScrollPane moduleScroll = new ScrollPane(moduleBox);
        moduleScroll.setFitToWidth(true);
        moduleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        moduleScroll.setStyle("-fx-background: " + BG_CREAM + "; -fx-background-color: transparent;");

        VBox wrapper = new VBox(moduleScroll);
        VBox.setVgrow(moduleScroll, Priority.ALWAYS);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════
    //  LOG VIEWER
    // ══════════════════════════════════════════════════════════════

    private VBox createLogViewer() {
        VBox logBox = new VBox(6);
        logBox.setPadding(new Insets(0, 20, 16, 20));

        HBox logHeader = new HBox(8);
        logHeader.setAlignment(Pos.CENTER_LEFT);

        Label logTitle = new Label("Event Log");
        logTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        logTitle.setTextFill(Color.web(TEXT_DARK));

        Label logFile = new Label("log.txt");
        logFile.setFont(Font.font("SansSerif", 11));
        logFile.setTextFill(Color.web(TEXT_LIGHT));
        logFile.setStyle("-fx-padding: 2 8; -fx-background-color: " + MODULE_CARD_BG + "; -fx-background-radius: 8;");

        logHeader.getChildren().addAll(logTitle, logFile);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        logArea.setFont(Font.font("Menlo", 11));
        logArea.setStyle(
                "-fx-control-inner-background: " + LOG_BG + "; " +
                        "-fx-text-fill: " + LOG_TEXT + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-color: #2D3530; " +
                        "-fx-border-width: 1;"
        );

        logBox.getChildren().addAll(logHeader, logArea);
        return logBox;
    }

    // ══════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates a card container.
     */
    private VBox createCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: " + CARD_WHITE + "; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: " + BORDER_LIGHT + "; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);"
        );
        return card;
    }

    /**
     * Creates a module status card with background.
     */
    private VBox createModuleCard() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(
                "-fx-background-color: " + MODULE_CARD_BG + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: " + BORDER_LIGHT + "; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 0.5;"
        );
        return card;
    }

    /**
     * Creates a module header with icon and title.
     */
    private HBox createModuleHeader(String icon, String titleText) {
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("SansSerif", 15));
        Label title = new Label(titleText);
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        title.setTextFill(Color.web(TEXT_DARK));
        header.getChildren().addAll(iconLabel, title);
        return header;
    }

    /**
     * Creates a small status indicator.
     */
    private Label createStatusDot(boolean active) {
        Label dot = new Label("●");
        dot.setFont(Font.font("SansSerif", 10));
        dot.setTextFill(active ? Color.web(ACCENT_GREEN) : Color.web("#CCCCCC"));
        return dot;
    }

    /**
     * Creates an action button.
     */
    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(createButtonStyle(color));
        btn.setOnMouseEntered(e -> btn.setStyle(createButtonHoverStyle(color)));
        btn.setOnMouseExited(e -> btn.setStyle(createButtonStyle(color)));
        return btn;
    }

    private String createButtonStyle(String color) {
        return "-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12.5; " +
                "-fx-padding: 10 18; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);";
    }

    private String createButtonHoverStyle(String color) {
        return "-fx-background-color: derive(" + color + ", -12%); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12.5; " +
                "-fx-padding: 10 18; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);";
    }

    // ══════════════════════════════════════════════════════════════
    //  PLANT CARD RENDERING
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates a plant card with image, health bar, and details.
     */
    private VBox createPlantNode(Plant p) {
        int health = p.getHealth();
        int water = p.getWaterLevel();
        String typeName = p.getType().getName();

        // Determine health state
        String bgColor, borderColor, healthColor, healthLabel;
        if (health > 70) {
            bgColor = HEALTHY_BG;
            borderColor = HEALTHY_BORDER;
            healthColor = ACCENT_GREEN;
            healthLabel = "Healthy";
        } else if (health > 40) {
            bgColor = STRESSED_BG;
            borderColor = STRESSED_BORDER;
            healthColor = ACCENT_WARM;
            healthLabel = "Stressed";
        } else {
            bgColor = CRITICAL_BG;
            borderColor = CRITICAL_BORDER;
            healthColor = ACCENT_RED;
            healthLabel = "Critical";
        }

        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(10, 10, 10, 10));
        card.setPrefWidth(130);
        card.setMinWidth(130);
        card.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        // ─── Plant Image ───
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(80, 80);
        imageContainer.setMaxSize(80, 80);
        imageContainer.setMinSize(80, 80);

        Rectangle clip = new Rectangle(80, 80);
        clip.setArcWidth(16);
        clip.setArcHeight(16);

        Image img = imageCache.get(typeName);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(80);
            iv.setFitHeight(80);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            iv.setClip(clip);

            // Apply desaturation for stressed/critical plants
            if (health <= 40) {
                javafx.scene.effect.ColorAdjust adjust = new javafx.scene.effect.ColorAdjust();
                adjust.setSaturation(-0.6);
                adjust.setBrightness(-0.1);
                iv.setEffect(adjust);
            } else if (health <= 70) {
                javafx.scene.effect.ColorAdjust adjust = new javafx.scene.effect.ColorAdjust();
                adjust.setSaturation(-0.3);
                iv.setEffect(adjust);
            }

            imageContainer.getChildren().add(iv);
        } else {
            // Fallback: colored circle with first letter
            Circle fallback = new Circle(30, Color.web(healthColor, 0.2));
            fallback.setStroke(Color.web(healthColor, 0.4));
            fallback.setStrokeWidth(2);
            Text initial = new Text(typeName.substring(0, 1));
            initial.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
            initial.setFill(Color.web(healthColor));
            imageContainer.getChildren().addAll(fallback, initial);
        }

        // ─── Plant Name ───
        Label nameLabel = new Label(typeName);
        nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web(TEXT_DARK));

        // ─── Health Bar ───
        StackPane healthBarBg = new StackPane();
        healthBarBg.setPrefHeight(6);
        healthBarBg.setMaxHeight(6);
        healthBarBg.setPrefWidth(100);
        healthBarBg.setStyle(
                "-fx-background-color: rgba(0,0,0,0.08); " +
                        "-fx-background-radius: 3;"
        );

        Region healthBarFill = new Region();
        healthBarFill.setPrefHeight(6);
        healthBarFill.setMaxHeight(6);
        double healthPercent = health / 100.0;
        healthBarFill.setMaxWidth(100 * healthPercent);
        healthBarFill.setPrefWidth(100 * healthPercent);
        healthBarFill.setStyle(
                "-fx-background-color: " + healthColor + "; " +
                        "-fx-background-radius: 3;"
        );
        StackPane.setAlignment(healthBarFill, Pos.CENTER_LEFT);

        StackPane healthBar = new StackPane(healthBarBg, healthBarFill);
        healthBar.setMaxWidth(100);
        healthBar.setPrefWidth(100);

        // ─── Stats Row ───
        HBox statsRow = new HBox(6);
        statsRow.setAlignment(Pos.CENTER);

        Label healthStat = new Label("\u2665 " + health);
        healthStat.setFont(Font.font("SansSerif", 10));
        healthStat.setTextFill(Color.web(healthColor));

        Label waterStat = new Label("\uD83D\uDCA7 " + water);
        waterStat.setFont(Font.font("SansSerif", 10));
        waterStat.setTextFill(Color.web(ACCENT_BLUE));

        statsRow.getChildren().addAll(healthStat, waterStat);

        // ─── Tooltip ───
        Tooltip tip = new Tooltip(
                "ID: " + p.getId()
                        + "\nType: " + typeName
                        + "\nHealth: " + health + "/100"
                        + "\nWater: " + water + "/100"
                        + "\nStatus: " + healthLabel
                        + "\nVulnerable to: " + p.getType().getParasiteVulnerabilities()
                        + "\nTemp range: " + p.getType().getMinTempF() + "-" + p.getType().getMaxTempF() + "\u00B0F"
        );
        tip.setStyle("-fx-font-size: 11; -fx-background-radius: 6;");
        Tooltip.install(card, tip);

        card.getChildren().addAll(imageContainer, nameLabel, healthBar, statsRow);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    //  REFRESH / STATE UPDATE
    // ══════════════════════════════════════════════════════════════

    private void refreshAll() {
        GardenSnapshot snap = engine.snapshot();

        // Update header stats
        dayLabel.setText("DAY  " + snap.day);
        aliveLabel.setText("ALIVE  " + snap.plantsAlive);

        // Update garden
        gardenPane.getChildren().clear();
        List<Plant> alive = engine.alivePlants();
        for (Plant p : alive) {
            gardenPane.getChildren().add(createPlantNode(p));
        }

        // If no plants alive
        if (alive.isEmpty()) {
            Label emptyLabel = new Label("All plants have died. Reset the garden to start over.");
            emptyLabel.setFont(Font.font("SansSerif", 14));
            emptyLabel.setTextFill(Color.web(TEXT_LIGHT));
            emptyLabel.setWrapText(true);
            emptyLabel.setPadding(new Insets(40));
            gardenPane.getChildren().add(emptyLabel);
        }

        // Update module status
        updateModuleStatus();

        // Update log
        List<String> entries = logSink.getRecentEntries(50);
        StringBuilder sb = new StringBuilder();
        for (String entry : entries) {
            sb.append(entry).append("\n");
        }
        logArea.setText(sb.toString());
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private void updateModuleStatus() {
        // Sprinklers
        boolean anySprinklerOn = false;
        for (Sprinkler s : engine.getSprinklers()) {
            if (s.isOn()) { anySprinklerOn = true; break; }
        }
        sprinklerStatusLabel.setText("Sprinklers " + (anySprinklerOn ? "ACTIVE" : "OFF"));
        sprinklerStatusLabel.setTextFill(anySprinklerOn ? Color.web(ACCENT_BLUE) : Color.web(TEXT_LIGHT));
        sprinklerDot.setTextFill(anySprinklerOn ? Color.web(ACCENT_BLUE) : Color.web("#CCCCCC"));

        // Heaters
        boolean heatOn = heatingModule.isHeatersOn();
        heaterStatusLabel.setText("Heaters " + (heatOn ? "ACTIVE" : "OFF"));
        heaterStatusLabel.setTextFill(heatOn ? Color.web(ACCENT_WARM) : Color.web(TEXT_LIGHT));
        heaterDot.setTextFill(heatOn ? Color.web(ACCENT_WARM) : Color.web("#CCCCCC"));

        boolean coolOn = heatingModule.isCoolingOn();
        coolingStatusLabel.setText("Cooling " + (coolOn ? "ACTIVE" : "OFF"));
        coolingStatusLabel.setTextFill(coolOn ? Color.web(ACCENT_BLUE) : Color.web(TEXT_LIGHT));
        coolingDot.setTextFill(coolOn ? Color.web(ACCENT_BLUE) : Color.web("#CCCCCC"));

        // Pest control
        boolean pestActive = pestControlModule.wasDeployedToday();
        pestControlStatusLabel.setText(pestActive ? "DEPLOYED" : "Idle");
        pestControlStatusLabel.setTextFill(pestActive ? Color.web(ACCENT_RED) : Color.web(TEXT_LIGHT));
        pestDot.setTextFill(pestActive ? Color.web(ACCENT_RED) : Color.web("#CCCCCC"));

        // Fertilizer
        fertilizerStatusLabel.setText(fertilizerModule.getTotalApplications() + " applications");
    }

    // ══════════════════════════════════════════════════════════════
    //  AUTO-SIMULATION
    // ══════════════════════════════════════════════════════════════

    private void startAutoSim() {
        autoSimRunning = true;
        autoSimTimer = new Timer(true);
        autoSimTimer.scheduleAtFixedRate(new TimerTask() {
            private int simDay = 0;

            @Override
            public void run() {
                if (simDay >= 24 || engine.countAlive() == 0) {
                    Platform.runLater(() -> {
                        engine.getState();
                        statusLabel.setText("Auto-sim complete");
                        refreshAll();
                        stopAutoSim();
                    });
                    cancel();
                    return;
                }

                double rand = Math.random();
                Platform.runLater(() -> {
                    try {
                        if (rand < 0.35) {
                            int rainAmount = 5 + (int) (Math.random() * 30);
                            engine.rain(rainAmount);
                            statusLabel.setText("[Auto] Rain " + rainAmount + " mm");
                        } else if (rand < 0.70) {
                            int temp = 40 + (int) (Math.random() * 80);
                            engine.temperature(temp);
                            statusLabel.setText("[Auto] Temp " + temp + "\u00B0F");
                        } else {
                            String[] pests = {"aphids", "mites", "whiteflies", "beetles"};
                            String pest = pests[(int) (Math.random() * pests.length)];
                            engine.parasite(pest);
                            statusLabel.setText("[Auto] Pest " + pest);
                        }
                        refreshAll();
                    } catch (Exception e) {
                        statusLabel.setText("Error: " + e.getMessage());
                    }
                });
                simDay++;
            }
        }, 0, 3000);
    }

    private void stopAutoSim() {
        autoSimRunning = false;
        if (autoSimTimer != null) {
            autoSimTimer.cancel();
            autoSimTimer = null;
        }
    }

    private void shutdown() {
        stopAutoSim();
        if (logSink != null) {
            logSink.close();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}