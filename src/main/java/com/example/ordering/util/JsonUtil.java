package com.example.ordering.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    public static JsonObject parseBody(final HttpServletRequest request) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        String rawBody = builder.toString();
        if (rawBody.isBlank()) {
            return new JsonObject();
        }
        return JsonParser.parseString(rawBody).getAsJsonObject();
    }

    public static String toJson(final Object payload) {
        return GSON.toJson(payload);
    }

    public static void writeSuccess(final HttpServletResponse response, final Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("data", data);
        response.getWriter().write(toJson(payload));
    }

    public static void writeError(
            final HttpServletResponse response,
            final int status,
            final String code,
            final String message
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", false);
        Map<String, String> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        payload.put("error", error);
        response.getWriter().write(toJson(payload));
    }

    public static Gson gson() {
        return GSON;
    }
}
