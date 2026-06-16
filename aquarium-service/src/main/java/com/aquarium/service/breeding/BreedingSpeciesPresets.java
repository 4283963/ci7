package com.aquarium.service.breeding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "breeding")
public class BreedingSpeciesPresets {

    private Map<String, SpeciesPreset> presets = new HashMap<>();

    public BreedingSpeciesPresets() {
        presets.put("guppy", SpeciesPreset.builder()
                .name("孔雀鱼")
                .targetTemp(26.5)
                .targetPh(7.2)
                .waterFlowLevel("low")
                .filterPumpEnabled(false)
                .aerationLevel("medium")
                .pregnancyDays(28)
                .motherRemovalHoursAfterBirth(24)
                .frySafeDays(30)
                .minTemp(24.0)
                .maxTemp(28.0)
                .build());

        presets.put("betta", SpeciesPreset.builder()
                .name("斗鱼")
                .targetTemp(27.0)
                .targetPh(6.8)
                .waterFlowLevel("low")
                .filterPumpEnabled(false)
                .aerationLevel("low")
                .pregnancyDays(3)
                .motherRemovalHoursAfterBirth(0)
                .frySafeDays(45)
                .minTemp(24.0)
                .maxTemp(30.0)
                .build());

        presets.put("molly", SpeciesPreset.builder()
                .name("玛丽鱼")
                .targetTemp(27.5)
                .targetPh(7.8)
                .waterFlowLevel("medium")
                .filterPumpEnabled(false)
                .aerationLevel("high")
                .pregnancyDays(35)
                .motherRemovalHoursAfterBirth(12)
                .frySafeDays(25)
                .minTemp(25.0)
                .maxTemp(30.0)
                .build());

        presets.put("platy", SpeciesPreset.builder()
                .name("月光鱼")
                .targetTemp(25.5)
                .targetPh(7.5)
                .waterFlowLevel("low")
                .filterPumpEnabled(false)
                .aerationLevel("medium")
                .pregnancyDays(30)
                .motherRemovalHoursAfterBirth(24)
                .frySafeDays(28)
                .minTemp(22.0)
                .maxTemp(28.0)
                .build());
    }

    public SpeciesPreset getPreset(String species) {
        return presets.getOrDefault(species, presets.get("guppy"));
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SpeciesPreset {
        private String name;
        private double targetTemp;
        private double targetPh;
        private String waterFlowLevel;
        private boolean filterPumpEnabled;
        private String aerationLevel;
        private int pregnancyDays;
        private int motherRemovalHoursAfterBirth;
        private int frySafeDays;
        private double minTemp;
        private double maxTemp;
    }
}
