package com.moviri.plugins.ws;

import com.moviri.plugins.config.KeyValuePair;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class LogLine {

    public enum Status {
        INFO("info"),
        WARN("warn"),
        ERROR("error");

        private final String value;

        Status(final String value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return this.value;
        }
    }

    private final String content;
    private final String job;
    private final String buildId;
    private final Status status;
    private final Map<String, String> dimensions;

    public LogLine(String content, String job, String buildId, Status status) {
        this.content = content;
        this.job = job;
        this.buildId = buildId;
        this.status = status;
        this.dimensions = new HashMap<>();
    }

    public LogLine(String content, String job, String buildId, Status status, Map<String, String> dimensions) {
        this.content = content;
        this.job = job;
        this.buildId = buildId;
        this.status = status;
        this.dimensions = dimensions;
    }

    public LogLine(String content, String job, String buildId) {
        this(content, job, buildId, Status.INFO);
    }

    public void addDimensions(List<KeyValuePair> customDimensions) {
        for (KeyValuePair pair : customDimensions) {
            this.dimensions.put(pair.getKey(), pair.getValue());
        }
    }

    public Map<String, String> toMap() {
        Map<String, String> result = new HashMap<>(Map.ofEntries(
                Map.entry("content", this.content),
                Map.entry("jenkins.job", this.job),
                Map.entry("jenkins.build_id", this.buildId),
                Map.entry("status", this.status.toString())
        ));
        result.putAll(this.dimensions);
        return result;
    }

}
