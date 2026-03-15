package dev.ayush.agentlens.common.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class CostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private record TokenPrice(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {}

    private static final Map<String, TokenPrice> PRICES = Map.of(
            "gpt-4", new TokenPrice(new BigDecimal("30.00"), new BigDecimal("60.00")),
            "gpt-4o", new TokenPrice(new BigDecimal("2.50"), new BigDecimal("10.00")),
            "gpt-4o-mini", new TokenPrice(new BigDecimal("0.15"), new BigDecimal("0.60")),
            "claude-sonnet-4-20250514", new TokenPrice(new BigDecimal("3.00"), new BigDecimal("15.00")),
            "claude-haiku-4-5-20251001", new TokenPrice(new BigDecimal("0.80"), new BigDecimal("4.00"))
    );

    private static final TokenPrice DEFAULT_PRICE = PRICES.get("gpt-4o");

    public BigDecimal calculateCost(String model, int inputTokens, int outputTokens) {
        TokenPrice price = model != null ? PRICES.getOrDefault(model, DEFAULT_PRICE) : DEFAULT_PRICE;

        BigDecimal inputCost = price.inputPerMillion()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = price.outputPerMillion()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }
}
