package com.aquarium.service.water;

import com.aquarium.common.enums.AlertLevel;
import com.aquarium.common.enums.WaterMetric;
import com.aquarium.common.dto.SensorData;
import com.aquarium.common.dto.WaterQualityAlert;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Component
@ConfigurationProperties(prefix = "water-quality.thresholds")
public class WaterQualityThresholds {

    private MetricThreshold temperature = new MetricThreshold();
    private MetricThreshold ph = new MetricThreshold();
    private MetricThreshold chlorine = new MetricThreshold();
    private MetricThreshold dissolvedOxygen = new MetricThreshold();

    @Data
    public static class MetricThreshold {
        private double min;
        private double max;
        private double warningMin;
        private double warningMax;
    }

    public List<WaterQualityAlert> evaluate(SensorData data, String tankId) {
        List<WaterQualityAlert> alerts = new ArrayList<>();

        checkMetric(alerts, tankId, WaterMetric.TEMPERATURE, data.getTemperature(),
                temperature.getMin(), temperature.getMax(),
                temperature.getWarningMin(), temperature.getWarningMax());

        checkMetric(alerts, tankId, WaterMetric.PH, data.getPh(),
                ph.getMin(), ph.getMax(),
                ph.getWarningMin(), ph.getWarningMax());

        checkMetric(alerts, tankId, WaterMetric.CHLORINE, data.getChlorine(),
                chlorine.getMin(), chlorine.getMax(),
                chlorine.getWarningMin(), chlorine.getWarningMax());

        checkMetric(alerts, tankId, WaterMetric.DISSOLVED_OXYGEN, data.getDissolvedOxygen(),
                dissolvedOxygen.getMin(), dissolvedOxygen.getMax(),
                dissolvedOxygen.getWarningMin(), dissolvedOxygen.getWarningMax());

        return alerts;
    }

    private void checkMetric(List<WaterQualityAlert> alerts, String tankId,
                             WaterMetric metric, double value,
                             double min, double max,
                             double warningMin, double warningMax) {
        if (value < min || value > max) {
            alerts.add(buildAlert(tankId, metric, value, min, max,
                    AlertLevel.CRITICAL,
                    String.format("%s严重异常: 当前值 %.2f%s, 安全范围 [%.2f, %.2f]",
                            metric.getName(), value, metric.getUnit(), min, max)));
        } else if (value < warningMin || value > warningMax) {
            alerts.add(buildAlert(tankId, metric, value, warningMin, warningMax,
                    AlertLevel.WARNING,
                    String.format("%s偏离警告: 当前值 %.2f%s, 警告范围 [%.2f, %.2f]",
                            metric.getName(), value, metric.getUnit(), warningMin, warningMax)));
        }
    }

    private WaterQualityAlert buildAlert(String tankId, WaterMetric metric,
                                          double actualValue, double thresholdMin, double thresholdMax,
                                          AlertLevel level, String message) {
        return WaterQualityAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .tankId(tankId)
                .metric(metric)
                .actualValue(actualValue)
                .thresholdMin(thresholdMin)
                .thresholdMax(thresholdMax)
                .level(level)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
