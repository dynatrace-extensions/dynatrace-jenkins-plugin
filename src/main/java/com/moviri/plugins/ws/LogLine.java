package com.moviri.plugins.ws;

import lombok.Getter;

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

    public LogLine(String content, String job, String buildId, Status status) {
        this.content = content;
        this.job = job;
        this.buildId = buildId;
        this.status = status;
    }

    public LogLine(String content, String job, String buildId) {
        this(content, job, buildId, Status.INFO);
    }

    public Map<String, String> toMap() {
        return Map.ofEntries(
                Map.entry("content", this.content),
                Map.entry("jenkins.job", this.job),
                Map.entry("jenkins.build_id", this.buildId),
                Map.entry("status", this.status.toString())
        );
    }

}
