package com.example.sales_test_1.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class YandexCloudLoggingAppender extends AppenderBase<ILoggingEvent> {

    private static final String API_URL = "https://logging.api.cloud.yandex.net/logging/v1/write";

    private String apiKey;
    private String logGroupId;
    private String appName = "vault";

    @Override
    protected void append(ILoggingEvent event) {
        if (apiKey == null || apiKey.isBlank() || logGroupId == null || logGroupId.isBlank()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Api-Key " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            String body = buildBody(event);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status >= 400) {
                addWarn("Yandex Cloud Logging returned HTTP " + status);
            }
            conn.disconnect();
        } catch (Exception e) {
            addError("Failed to send log to Yandex Cloud Logging", e);
        }
    }

    private String buildBody(ILoggingEvent event) {
        String ts      = Instant.ofEpochMilli(event.getTimeStamp()).toString();
        String level   = toYandexLevel(event.getLevel().toString());
        String message = escape(event.getFormattedMessage());
        String logger  = escape(event.getLoggerName());
        String thread  = escape(event.getThreadName());

        StringBuilder payload = new StringBuilder("{");
        payload.append("\"app\":\"").append(appName).append("\",");
        payload.append("\"logger\":\"").append(logger).append("\",");
        payload.append("\"thread\":\"").append(thread).append("\"");
        if (event.getThrowableProxy() != null) {
            payload.append(",\"exception\":\"")
                   .append(escape(event.getThrowableProxy().getClassName()
                           + ": " + event.getThrowableProxy().getMessage()))
                   .append("\"");
        }
        payload.append("}");

        return "{"
            + "\"destination\":{\"log_group_id\":\"" + logGroupId + "\"},"
            + "\"entries\":[{"
            +   "\"timestamp\":\"" + ts + "\","
            +   "\"level\":\"" + level + "\","
            +   "\"message\":\"" + message + "\","
            +   "\"jsonPayload\":" + payload
            + "}]}";
    }

    private String toYandexLevel(String logbackLevel) {
        return switch (logbackLevel) {
            case "TRACE" -> "TRACE";
            case "DEBUG" -> "DEBUG";
            case "INFO"  -> "INFO";
            case "WARN"  -> "WARN";
            case "ERROR" -> "ERROR";
            default      -> "LEVEL_UNSPECIFIED";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void setApiKey(String apiKey)         { this.apiKey = apiKey; }
    public void setLogGroupId(String logGroupId) { this.logGroupId = logGroupId; }
    public void setAppName(String appName)       { this.appName = appName; }
}
