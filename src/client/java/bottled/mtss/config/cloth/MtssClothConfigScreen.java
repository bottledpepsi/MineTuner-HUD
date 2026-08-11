package bottled.mtss.config.cloth;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.sample.HardwareSensorPoller;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;


public final class MtssClothConfigScreen {

    private MtssClothConfigScreen() {
    }

    /** Builds (but does not show) the config screen. */
    public static Screen build(Screen parent) {
        MtssConfig cfg = MtssConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("gui.mtss.cloth.title"));

        // Cloth Config already atomically persists a Java-object config by
        // its own I/O when built through AutoConfig, but this screen is built
        // by hand instead (MtssConfig has its own mtss.json read/write path —
        // MtssConfig.load/save, with its own corrupt-file backup handling and
        // atomic .tmp-then-move write — see MtssConfig for details), so every
        // entry below writes straight into the live `cfg` instance's fields
        // via its save consumer, and this single runnable is what actually
        // persists all of them to disk together.
        builder.setSavingRunnable(() -> {
            cfg.save(); // also re-clamps + re-syncs PanelChrome/ReorderPanel.
            HardwareSensorPoller.reconcileWithConfig(); // picks up enabled/baseUrl/timeout changes live, no restart.
        });

        ConfigEntryBuilder eb = builder.entryBuilder();

        buildGeneralCategory(builder, eb, cfg);
        buildHardwareSensorCategory(builder, eb, cfg);
        buildGuiTuningCategory(builder, eb, cfg);
        buildListsCategory(builder, eb, cfg);

        return builder.build();
    }

    private static void buildGeneralCategory(ConfigBuilder builder, ConfigEntryBuilder eb, MtssConfig cfg) {
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("gui.mtss.cloth.category.general"));

        general.addEntry(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.overlay_enabled"), cfg.overlayEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("gui.mtss.cloth.overlay_enabled.tooltip"))
                .setSaveConsumer(v -> cfg.overlayEnabled = v)
                .build());
    }

    private static void buildHardwareSensorCategory(ConfigBuilder builder, ConfigEntryBuilder eb, MtssConfig cfg) {
        ConfigCategory hw = builder.getOrCreateCategory(Component.translatable("gui.mtss.cloth.category.hardware_sensors"));

        hw.addEntry(eb.startTextDescription(Component.translatable("gui.mtss.cloth.hardware_sensors.description")).build());

        hw.addEntry(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.hardware_sensors_enabled"), cfg.hardwareSensorsEnabled)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("gui.mtss.cloth.hardware_sensors_enabled.tooltip"))
                .setSaveConsumer(v -> cfg.hardwareSensorsEnabled = v)
                .build());

        hw.addEntry(eb.startStrField(Component.translatable("gui.mtss.cloth.hardware_sensor_base_url"), cfg.hardwareSensorBaseUrl)
                .setDefaultValue("http://localhost:8085")
                .setTooltip(Component.translatable("gui.mtss.cloth.hardware_sensor_base_url.tooltip"))
                .setErrorSupplier(v -> isValidHttpUrl(v) ? java.util.Optional.empty()
                        : java.util.Optional.of(Component.translatable("gui.mtss.cloth.error.invalid_url")))
                .setSaveConsumer(v -> cfg.hardwareSensorBaseUrl = v)
                .build());

        hw.addEntry(eb.startIntField(Component.translatable("gui.mtss.cloth.hardware_sensor_poll_interval"), cfg.hardwareSensorPollIntervalMs)
                .setDefaultValue(1500)
                .setMin(100).setMax(60_000)
                .setTooltip(Component.translatable("gui.mtss.cloth.hardware_sensor_poll_interval.tooltip"))
                .setSaveConsumer(v -> cfg.hardwareSensorPollIntervalMs = v)
                .build());

        hw.addEntry(eb.startIntField(Component.translatable("gui.mtss.cloth.hardware_sensor_request_timeout"), cfg.hardwareSensorRequestTimeoutMs)
                .setDefaultValue(300)
                .setMin(50).setMax(10_000)
                .setTooltip(Component.translatable("gui.mtss.cloth.hardware_sensor_request_timeout.tooltip"))
                .setSaveConsumer(v -> cfg.hardwareSensorRequestTimeoutMs = v)
                .build());
    }

    private static boolean isValidHttpUrl(String v) {
        if (v == null || v.isBlank()) return false;
        try {
            java.net.URI uri = java.net.URI.create(v.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void buildGuiTuningCategory(ConfigBuilder builder, ConfigEntryBuilder eb, MtssConfig cfg) {
        ConfigCategory gui = builder.getOrCreateCategory(Component.translatable("gui.mtss.cloth.category.gui_tuning"));

        gui.addEntry(eb.startTextDescription(Component.translatable("gui.mtss.cloth.gui_tuning.description")).build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.reorder_panel_max_visible_rows"), cfg.reorderPanelMaxVisibleRows, 3, 60)
                .setDefaultValue(16)
                .setTooltip(Component.translatable("gui.mtss.cloth.reorder_panel_max_visible_rows.tooltip"))
                .setSaveConsumer(v -> cfg.reorderPanelMaxVisibleRows = v)
                .build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.panel_row_height"), cfg.panelRowHeight, 6, 40)
                .setDefaultValue(13)
                .setTooltip(Component.translatable("gui.mtss.cloth.panel_row_height.tooltip"))
                .setSaveConsumer(v -> cfg.panelRowHeight = v)
                .build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.panel_width"), cfg.panelWidth, 60, 400)
                .setDefaultValue(160)
                .setTooltip(Component.translatable("gui.mtss.cloth.panel_width.tooltip"))
                .setSaveConsumer(v -> cfg.panelWidth = v)
                .build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.wide_panel_width"), cfg.widePanelWidth, 60, 500)
                .setDefaultValue(216)
                .setTooltip(Component.translatable("gui.mtss.cloth.wide_panel_width.tooltip"))
                .setSaveConsumer(v -> cfg.widePanelWidth = v)
                .build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.panel_padding"), cfg.panelPadding, 0, 20)
                .setDefaultValue(4)
                .setTooltip(Component.translatable("gui.mtss.cloth.panel_padding.tooltip"))
                .setSaveConsumer(v -> cfg.panelPadding = v)
                .build());

        gui.addEntry(eb.startIntSlider(Component.translatable("gui.mtss.cloth.drag_snap_threshold"), cfg.dragSnapThresholdPx, 0, 40)
                .setDefaultValue(6)
                .setTooltip(Component.translatable("gui.mtss.cloth.drag_snap_threshold.tooltip"))
                .setSaveConsumer(v -> cfg.dragSnapThresholdPx = v)
                .build());

        gui.addEntry(eb.startFloatField(Component.translatable("gui.mtss.cloth.text_scale_min"), cfg.textScaleMin)
                .setDefaultValue(0.5f)
                .setMin(0.1f).setMax(cfg.textScaleMax)
                .setTooltip(Component.translatable("gui.mtss.cloth.text_scale_min.tooltip"))
                .setSaveConsumer(v -> cfg.textScaleMin = v)
                .build());

        gui.addEntry(eb.startFloatField(Component.translatable("gui.mtss.cloth.text_scale_max"), cfg.textScaleMax)
                .setDefaultValue(2.0f)
                .setMin(cfg.textScaleMin).setMax(10.0f)
                .setTooltip(Component.translatable("gui.mtss.cloth.text_scale_max.tooltip"))
                .setSaveConsumer(v -> cfg.textScaleMax = v)
                .build());
    }

    private static void buildListsCategory(ConfigBuilder builder, ConfigEntryBuilder eb, MtssConfig cfg) {
        ConfigCategory lists = builder.getOrCreateCategory(Component.translatable("gui.mtss.cloth.category.lists"));

        if (cfg.lists.isEmpty()) {
            lists.addEntry(eb.startTextDescription(Component.translatable("gui.mtss.cloth.lists.none")).build());
            return;
        }

        for (MtssConfig.StatListConfig lc : cfg.lists) {
            lists.addEntry(buildListCategory(eb, cfg, lc).build());
        }
    }

    /** One collapsible sub-category per list, named after the list itself so a user
     *  managing several lists can tell them apart at a glance in the category tree. */
    private static SubCategoryBuilder buildListCategory(ConfigEntryBuilder eb, MtssConfig cfg, MtssConfig.StatListConfig lc) {
        SubCategoryBuilder cat = eb.startSubCategory(Component.literal(lc.name != null ? lc.name : ("List " + lc.id)));

        cat.add(eb.startEnumSelector(Component.translatable("gui.mtss.cloth.anchor_corner"), MtssConfig.Corner.class, lc.anchorCorner)
                .setDefaultValue(MtssConfig.Corner.TOP_LEFT)
                .setEnumNameProvider(v -> Component.translatable("gui.mtss.corner." + v.name().toLowerCase()))
                .setSaveConsumer(v -> lc.anchorCorner = v)
                .build());

        cat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.anchor_frac_x"), (float) lc.anchorFracX)
                .setDefaultValue(0.01f)
                .setMin(0f).setMax(1f)
                .setTooltip(Component.translatable("gui.mtss.cloth.anchor_frac_x.tooltip"))
                .setSaveConsumer(v -> lc.anchorFracX = v)
                .build());

        cat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.anchor_frac_y"), (float) lc.anchorFracY)
                .setDefaultValue(0.01f)
                .setMin(0f).setMax(1f)
                .setTooltip(Component.translatable("gui.mtss.cloth.anchor_frac_y.tooltip"))
                .setSaveConsumer(v -> lc.anchorFracY = v)
                .build());

        cat.add(eb.startEnumSelector(Component.translatable("gui.mtss.cloth.snap_x"), MtssConfig.SnapX.class, lc.snapX)
                .setDefaultValue(MtssConfig.SnapX.NONE)
                .setEnumNameProvider(v -> Component.translatable("gui.mtss.snap_x." + v.name().toLowerCase()))
                .setSaveConsumer(v -> lc.snapX = v)
                .build());

        cat.add(eb.startEnumSelector(Component.translatable("gui.mtss.cloth.snap_y"), MtssConfig.SnapY.class, lc.snapY)
                .setDefaultValue(MtssConfig.SnapY.NONE)
                .setEnumNameProvider(v -> Component.translatable("gui.mtss.snap_y." + v.name().toLowerCase()))
                .setSaveConsumer(v -> lc.snapY = v)
                .build());

        cat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.show_background"), lc.showBackground)
                .setDefaultValue(true)
                .setSaveConsumer(v -> lc.showBackground = v)
                .build());

        cat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.text_shadow"), lc.textShadow)
                .setDefaultValue(false)
                .setSaveConsumer(v -> lc.textShadow = v)
                .build());

        cat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.use_custom_color"), lc.useCustomColor)
                .setDefaultValue(false)
                .setSaveConsumer(v -> lc.useCustomColor = v)
                .build());

        cat.add(eb.startColorField(Component.translatable("gui.mtss.cloth.override_color"), lc.overrideColor)
                .setAlphaMode(true)
                .setDefaultValue(0xFFFFFFFF)
                .setTooltip(Component.translatable("gui.mtss.cloth.override_color.tooltip"))
                .setSaveConsumer(v -> lc.overrideColor = v)
                .build());

        cat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.text_scale"), lc.textScale)
                .setDefaultValue(1.0f)
                .setMin(cfg.textScaleMin).setMax(cfg.textScaleMax)
                .setTooltip(Component.translatable("gui.mtss.cloth.text_scale.tooltip"))
                .setSaveConsumer(v -> lc.textScale = v)
                .build());

        cat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.use_template"), lc.useTemplate)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("gui.mtss.cloth.use_template.tooltip"))
                .setSaveConsumer(v -> lc.useTemplate = v)
                .build());

        cat.add(eb.startStrList(Component.translatable("gui.mtss.cloth.template_lines"), new ArrayList<>(lc.templateLines))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("gui.mtss.cloth.template_lines.tooltip"))
                .setSaveConsumer(v -> {
                    lc.templateLines.clear();
                    lc.templateLines.addAll(v);
                })
                .build());

        cat.add(buildStatsSubCategory(eb, lc).build());

        return cat;
    }

    /** Nested one level under each list's category. */
    private static SubCategoryBuilder buildStatsSubCategory(ConfigEntryBuilder eb, MtssConfig.StatListConfig lc) {
        SubCategoryBuilder statsCat = eb.startSubCategory(Component.translatable("gui.mtss.cloth.stats"));

        for (MtssConfig.Stat stat : MtssConfig.Stat.values()) {
            statsCat.add(buildStatSubCategory(eb, lc, stat).build());
        }
        return statsCat;
    }

    private static SubCategoryBuilder buildStatSubCategory(ConfigEntryBuilder eb, MtssConfig.StatListConfig lc, MtssConfig.Stat stat) {
        String statName = I18n.get("stat.mtss." + stat.name().toLowerCase());
        SubCategoryBuilder statCat = eb.startSubCategory(Component.literal(statName));

        boolean enabledNow = lc.isEnabled(stat);
        statCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.stat_enabled"), enabledNow)
                .setDefaultValue(false)
                .setSaveConsumer(v -> lc.setEnabled(stat, v))
                .build());

        StatDefinition def = StatRegistry.get(stat);
        MtssConfig.StatSettings settings = lc.getStatSettings(stat);

        if (def.supportsDecimals()) {
            statCat.add(eb.startIntSlider(Component.translatable("gui.mtss.cloth.stat_decimals"), settings.decimals, 0, 4)
                    .setDefaultValue(def.defaultDecimals())
                    .setSaveConsumer(v -> lc.getStatSettings(stat).decimals = v)
                    .build());
        }

        statCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.stat_show_prefix"), settings.showPrefix)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("gui.mtss.cloth.stat_show_prefix.tooltip"))
                .setSaveConsumer(v -> lc.getStatSettings(stat).showPrefix = v)
                .build());

        if (def.supportsGraph()) {
            statCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.stat_render_as_graph"), settings.renderAsGraph)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> lc.getStatSettings(stat).renderAsGraph = v)
                    .build());
            statCat.add(buildGraphStyleSubCategory(eb, lc, stat).build());
        }

        if (def.supportsThreshold() && lc.getThreshold(stat) != null) {
            statCat.add(buildThresholdSubCategory(eb, lc, stat, def).build());
        }

        return statCat;
    }

    private static SubCategoryBuilder buildGraphStyleSubCategory(ConfigEntryBuilder eb, MtssConfig.StatListConfig lc, MtssConfig.Stat stat) {
        MtssConfig.GraphStyle style = lc.getStatSettings(stat).graphStyle;
        SubCategoryBuilder graphCat = eb.startSubCategory(Component.translatable("gui.mtss.cloth.graph_style"));

        graphCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.graph_show_panel_background"), style.showPanelBackground)
                .setDefaultValue(true)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.showPanelBackground = v)
                .build());

        graphCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.graph_show_gridlines"), style.showGridlines)
                .setDefaultValue(true)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.showGridlines = v)
                .build());

        graphCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.graph_show_peak_markers"), style.showPeakMarkers)
                .setDefaultValue(true)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.showPeakMarkers = v)
                .build());

        graphCat.add(eb.startEnumSelector(Component.translatable("gui.mtss.cloth.graph_value_display"), MtssConfig.GraphValueDisplay.class, style.valueDisplay)
                .setDefaultValue(MtssConfig.GraphValueDisplay.CURRENT)
                .setEnumNameProvider(v -> Component.translatable("gui.mtss.graph_value_display." + v.name().toLowerCase()))
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.valueDisplay = v)
                .build());

        graphCat.add(eb.startIntSlider(Component.translatable("gui.mtss.cloth.graph_smoothing"), style.smoothing, 0, 4)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("gui.mtss.cloth.graph_smoothing.tooltip"))
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.smoothing = v)
                .build());

        graphCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.graph_auto_scale"), style.autoScale)
                .setDefaultValue(true)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.autoScale = v)
                .build());

        graphCat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.graph_fixed_min"), style.fixedMin)
                .setDefaultValue(0f)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.fixedMin = v)
                .build());

        graphCat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.graph_fixed_max"), style.fixedMax)
                .setDefaultValue(100f)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.fixedMax = v)
                .build());

        graphCat.add(eb.startIntSlider(Component.translatable("gui.mtss.cloth.graph_width"), style.width, 20, 400)
                .setDefaultValue(80)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.width = v)
                .build());

        graphCat.add(eb.startIntSlider(Component.translatable("gui.mtss.cloth.graph_height"), style.height, 10, 200)
                .setDefaultValue(28)
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.height = v)
                .build());

        graphCat.add(eb.startEnumSelector(Component.translatable("gui.mtss.cloth.graph_color_mode"), MtssConfig.GraphColorMode.class, style.colorMode)
                .setDefaultValue(MtssConfig.GraphColorMode.CURRENT_THRESHOLD)
                .setEnumNameProvider(v -> Component.translatable("gui.mtss.graph_color_mode." + v.name().toLowerCase()))
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.colorMode = v)
                .build());

        graphCat.add(eb.startColorField(Component.translatable("gui.mtss.cloth.graph_accent_color"), style.accentColor)
                .setAlphaMode(true)
                .setDefaultValue(0xFF55FF55)
                .setTooltip(Component.translatable("gui.mtss.cloth.graph_accent_color.tooltip"))
                .setSaveConsumer(v -> lc.getStatSettings(stat).graphStyle.accentColor = v)
                .build());

        return graphCat;
    }

    private static SubCategoryBuilder buildThresholdSubCategory(ConfigEntryBuilder eb, MtssConfig.StatListConfig lc, MtssConfig.Stat stat, StatDefinition def) {
        MtssConfig.ThresholdSettings threshold = lc.getThreshold(stat);
        SubCategoryBuilder thresholdCat = eb.startSubCategory(Component.translatable("gui.mtss.cloth.threshold"));

        thresholdCat.add(eb.startBooleanToggle(Component.translatable("gui.mtss.cloth.threshold_enabled"), threshold.enabled)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("gui.mtss.cloth.threshold_enabled.tooltip"))
                .setSaveConsumer(v -> lc.getThreshold(stat).enabled = v)
                .build());

        thresholdCat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.threshold_good_min"), threshold.goodMin)
                .setDefaultValue(def.defaultGoodMin())
                .setTooltip(Component.translatable(def.higherIsBetter()
                        ? "gui.mtss.cloth.threshold_good_min.tooltip.higher_better"
                        : "gui.mtss.cloth.threshold_good_min.tooltip.lower_better"))
                .setSaveConsumer(v -> lc.getThreshold(stat).goodMin = v)
                .build());

        thresholdCat.add(eb.startFloatField(Component.translatable("gui.mtss.cloth.threshold_warn_min"), threshold.warnMin)
                .setDefaultValue(def.defaultWarnMin())
                .setTooltip(Component.translatable(def.higherIsBetter()
                        ? "gui.mtss.cloth.threshold_warn_min.tooltip.higher_better"
                        : "gui.mtss.cloth.threshold_warn_min.tooltip.lower_better"))
                .setSaveConsumer(v -> lc.getThreshold(stat).warnMin = v)
                .build());

        return thresholdCat;
    }
}
