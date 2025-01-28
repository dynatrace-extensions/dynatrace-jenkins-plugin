package com.moviri.plugins.ws;

import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class MintMetric {

    public enum MetricType {
        GAUGE("gauge"),
        COUNT("count"),
        DELTA("count,delta");

        private final String value;

        MetricType(final String value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return this.value;
        }
    }

    private final String key;
    private final double value;
    private final Map<String, String> dimensions;
    private final MetricType metricType;

    public MintMetric(String key, double value, Map<String, String> dimensions, MetricType metricType) {
        this.key = key;
        this.value = value;
        this.dimensions = dimensions;
        this.metricType = metricType;
    }

    public MintMetric(String key, double value, Map<String, String> dimensions) {
        this(key, value, dimensions, MetricType.GAUGE);
    }

    public MintMetric(String key, double value) {
        this(key, value, new HashMap<>(), MetricType.GAUGE);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder dimensionStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : this.dimensions.entrySet()) {
            dimensionStringBuilder.append(entry.getKey().toLowerCase()).append("=").append(entry.getValue().toLowerCase()).append(",");
        }
        String dimensionString = dimensionStringBuilder.toString();
        if (!dimensionString.isEmpty()) {
            dimensionString = dimensionString.substring(0, dimensionString.length() - 1);
        }

        stringBuilder.append(this.key);
        if (!dimensionString.isEmpty()) {
            stringBuilder.append(",");
            stringBuilder.append(dimensionString);
        }
        stringBuilder.append(" ");
        stringBuilder.append(this.metricType.toString());
        stringBuilder.append(",");
        stringBuilder.append(this.value);
        stringBuilder.append(" ");
        long now = Instant.now().toEpochMilli();
        stringBuilder.append(now);
        return stringBuilder.toString();
    }
}
