package com.aquarium.service.water;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WaterChemistryCalculator {

    private static final double EPSILON = 1e-9;
    private static final double MIN_CHLORINE = 0.0;
    private static final double MAX_CHLORINE = 10.0;
    private static final double MIN_PH = 0.0;
    private static final double MAX_PH = 14.0;

    private WaterChemistryCalculator() {}

    /**
     * 计算次氯酸（HOCl）解离比例，即有效氯占总氯的比例。
     * 公式: α = 1 / (1 + 10^(pH - pKa))
     * 其中 pKa 在 25℃ 时约为 7.54
     *
     * 【除零修复】当 pH → -∞ 时，10^(pH - pKa) → 0，分母 → 1，不会除零。
     * 但当 pH → +∞ 时，10^(pH - pKa) → ∞，分母 → ∞，α → 0，也不会除零。
     * 真正风险: pH 值非法（NaN / Infinity）会导致计算异常。
     */
    public static double calculateHoclRatio(double ph, double temperature) {
        if (!isValidPh(ph)) {
            log.warn("Invalid pH value: {}, returning default ratio 0.5", ph);
            return 0.5;
        }

        double pKa = 7.54 - 0.012 * (temperature - 25.0);

        double exponent = ph - pKa;
        if (exponent > 20) {
            return 0.0;
        }
        if (exponent < -20) {
            return 1.0;
        }

        double tenPower = Math.pow(10, exponent);
        double denominator = 1.0 + tenPower;

        if (denominator < EPSILON) {
            log.warn("Denominator too small in HOCl ratio calculation: pH={}, temp={}", ph, temperature);
            return 0.0;
        }

        return 1.0 / denominator;
    }

    /**
     * 根据总氯和 pH 计算游离氯浓度。
     * 游离氯 = 总氯 × HOCl 解离比例
     */
    public static double calculateFreeChlorine(double totalChlorine, double ph, double temperature) {
        if (!isValidChlorine(totalChlorine)) {
            log.warn("Invalid total chlorine value: {}", totalChlorine);
            return 0.0;
        }

        double ratio = calculateHoclRatio(ph, temperature);
        return totalChlorine * ratio;
    }

    /**
     * 根据游离氯反推总氯含量（用于排氯量计算）。
     * 总氯 = 游离氯 / HOCl 解离比例
     *
     * 【除零修复】当 HOCl 比例接近 0 时（高 pH），直接返回原始值避免除零。
     */
    public static double calculateTotalFromFreeChlorine(double freeChlorine, double ph, double temperature) {
        if (!isValidChlorine(freeChlorine)) {
            log.warn("Invalid free chlorine value: {}", freeChlorine);
            return 0.0;
        }

        double ratio = calculateHoclRatio(ph, temperature);
        if (ratio < EPSILON) {
            log.warn("HOCl ratio too small ({}), cannot invert. Returning free chlorine as-is.", ratio);
            return freeChlorine;
        }

        return freeChlorine / ratio;
    }

    /**
     * 计算需要的排氯时间（秒）。
     * 根据当前氯气浓度、目标浓度、换水速率估算。
     *
     * 【除零修复】当换水速率为 0 时，返回默认值 60 秒。
     */
    public static int estimateChlorineDrainSeconds(
            double currentChlorine,
            double targetChlorine,
            double tankVolume,
            double drainRate) {

        if (!isValidChlorine(currentChlorine) || !isValidChlorine(targetChlorine)) {
            log.warn("Invalid chlorine values for drain estimation: current={}, target={}",
                    currentChlorine, targetChlorine);
            return 60;
        }

        if (currentChlorine <= targetChlorine) {
            return 0;
        }

        if (drainRate < EPSILON || tankVolume < EPSILON) {
            log.warn("Drain rate or tank volume too small: rate={}, volume={}", drainRate, tankVolume);
            return 60;
        }

        double ratio = (currentChlorine - targetChlorine) / currentChlorine;
        double turnovers = -Math.log(1.0 - Math.min(ratio, 0.99));
        double seconds = (tankVolume * turnovers) / drainRate;

        return (int) Math.min(Math.max(seconds, 10), 600);
    }

    public static boolean isValidPh(double ph) {
        return !Double.isNaN(ph) && !Double.isInfinite(ph) && ph >= MIN_PH && ph <= MAX_PH;
    }

    public static boolean isValidChlorine(double chlorine) {
        return !Double.isNaN(chlorine) && !Double.isInfinite(chlorine)
                && chlorine >= MIN_CHLORINE && chlorine <= MAX_CHLORINE;
    }

    public static boolean isValidTemperature(double temperature) {
        return !Double.isNaN(temperature) && !Double.isInfinite(temperature)
                && temperature >= -10 && temperature <= 50;
    }
}
