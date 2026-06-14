package com.example.sales_test_1.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class LogglyAppender extends AppenderBase<ILoggingEvent> {

    private String token;
    private String appName = "vault";

    @Override
    protected void append(ILoggingEvent event) {
        if (token == null || token.isBlank()) return;

        try {
            String url = "https://logs-01.loggly.com/inputs/" + token + "/tag/http,java," + appName + "/";
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            String json = buildJson(event);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            conn.getResponseCode(); // fire and read response to complete the request
            conn.disconnect();
        } catch (Exception e) {
            addError("Failed to send log to Loggly", e);
        }
    }

    private String buildJson(ILoggingEvent event) {
        String message = escape(event.getFormattedMessage());
        String logger  = escape(event.getLoggerName());
        String thread  = escape(event.getThreadName());
        String level   = event.getLevel().toString();
        String ts      = Instant.ofEpochMilli(event.getTimeStamp()).toString();

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"timestamp\":\"").append(ts).append("\",");
        sb.append("\"level\":\"").append(level).append("\",");
        sb.append("\"app\":\"").append(appName).append("\",");
        sb.append("\"logger\":\"").append(logger).append("\",");
        sb.append("\"thread\":\"").append(thread).append("\",");
        sb.append("\"message\":\"").append(message).append("\"");

        if (event.getThrowableProxy() != null) {
            sb.append(",\"exception\":\"")
              .append(escape(event.getThrowableProxy().getClassName() + ": " + event.getThrowableProxy().getMessage()))
              .append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void setToken(String token) { this.token = token; }
    public void setAppName(String appName) { this.appName = appName; }
}
