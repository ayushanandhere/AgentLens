package dev.ayush.agentlens.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostCalculatorTest {

    private final CostCalculator calculator = new CostCalculator();

    @Test
    void calculatesKnownModelPricing() {
        BigDecimal cost = calculator.calculateCost("gpt-4o", 1_000, 500);

        assertThat(cost).isEqualByComparingTo("0.007500");
    }

    @Test
    void fallsBackToDefaultPricingForUnknownModel() {
        BigDecimal cost = calculator.calculateCost("unknown-model", 1_000, 500);

        assertThat(cost).isEqualByComparingTo("0.007500");
    }
}
