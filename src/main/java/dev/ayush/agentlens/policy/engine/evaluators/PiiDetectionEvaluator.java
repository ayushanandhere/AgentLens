package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PiiDetectionEvaluator implements PolicyEvaluator {

    private static final Map<String, Pattern> PII_PATTERNS = Map.of(
            "EMAIL", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
            "PHONE", Pattern.compile("(\\+1\\d{10})|(\\(\\d{3}\\)\\s?\\d{3}-\\d{4})|(\\d{3}-\\d{3}-\\d{4})"),
            "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            "CREDIT_CARD", Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b")
    );

    @Override
    public PolicyType getType() {
        return PolicyType.PII_CHECK;
    }

    @Override
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        String promptText = context.getTrace().getPromptText();
        String responseText = context.getTrace().getResponseText();

        if (promptText == null && responseText == null) {
            return PolicyResult.passed();
        }

        Set<String> piiTypesFound = new LinkedHashSet<>();
        Set<String> locations = new LinkedHashSet<>();
        int totalMatches = 0;

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();

            if (promptText != null) {
                int count = countMatches(pattern, promptText);
                if (count > 0) {
                    piiTypesFound.add(piiType);
                    locations.add("prompt");
                    totalMatches += count;
                }
            }
            if (responseText != null) {
                int count = countMatches(pattern, responseText);
                if (count > 0) {
                    piiTypesFound.add(piiType);
                    locations.add("response");
                    totalMatches += count;
                }
            }
        }

        if (!piiTypesFound.isEmpty()) {
            return PolicyResult.violated(Map.of(
                    "pii_types_found", new ArrayList<>(piiTypesFound),
                    "locations", new ArrayList<>(locations),
                    "count", totalMatches
            ));
        }
        return PolicyResult.passed();
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
