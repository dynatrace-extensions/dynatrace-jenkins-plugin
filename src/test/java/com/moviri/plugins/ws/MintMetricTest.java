package com.moviri.plugins.ws;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MintMetricTest {

    @Test
    void testGaugeMetricType() {
        String type = "GAUGE";
        MintMetric.MetricType metricType = MintMetric.MetricType.valueOf(type);
        assertEquals(MintMetric.MetricType.GAUGE, metricType);
        assertEquals("gauge", metricType.toString());
    }

    @Test
    void testCountMetricType() {
        String type = "COUNT";
        MintMetric.MetricType metricType = MintMetric.MetricType.valueOf(type);
        assertEquals(MintMetric.MetricType.COUNT, metricType);
        assertEquals("count", metricType.toString());
    }

    @Test
    void testDeltaMetricType() {
        String type = "DELTA";
        MintMetric.MetricType metricType = MintMetric.MetricType.valueOf(type);
        assertEquals(MintMetric.MetricType.DELTA, metricType);
        assertEquals("count,delta", metricType.toString());
    }

    @Test
    void testFullConstructor() {
        MintMetric mintMetric = new MintMetric(
                "sampleKey",
                123.456,
                Map.of("keyDim", "valueDim"),
                MintMetric.MetricType.DELTA);
        assertEquals("sampleKey", mintMetric.getKey());
        assertEquals(123.456, mintMetric.getValue());
        assertEquals(Map.of("keyDim", "valueDim"), mintMetric.getDimensions());
        assertEquals(MintMetric.MetricType.DELTA, mintMetric.getMetricType());
    }

    @Test
    void testBasicConstructor() {
        MintMetric mintMetric = new MintMetric("sampleKey", 123.456);
        assertEquals("sampleKey", mintMetric.getKey());
        assertEquals(123.456, mintMetric.getValue());
        assertEquals(Map.of(), mintMetric.getDimensions());
        assertEquals(MintMetric.MetricType.GAUGE, mintMetric.getMetricType());
    }

    @Test
    void testToString() {
        MintMetric mintMetric = new MintMetric(
                "sampleKey",
                123.456,
                Map.of("keyDim", "valueDim"),
                MintMetric.MetricType.DELTA);
        assertTrue(mintMetric.toString().startsWith(
                (
                        "sampleKey,keydim=valuedim count,delta,123.456 "
                                + Instant.now().toEpochMilli()
                ).substring(0, mintMetric.toString().length() - 4)));
    }
}