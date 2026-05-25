package com.jirahourlogger.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jirahourlogger.model.JiraIssue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JiraClient handles all communication with the Jira REST API.
 *
 * Authentication uses HTTP Basic Auth — Jira expects:
 *   Authorization: Basic Base64(email:apiToken)
 *
 * Credentials are loaded from ~/.jirahourlogger/.env, with system
 * environment variables taking priority if both are present.
 *
 * The .env file format (one KEY=VALUE per line, # for comments):
 *   JIRA_EMAIL=you@company.com
 *   JIRA_API_TOKEN=yourtoken
 *
 * Java 21's built-in java.net.http.HttpClient is used — no external library needed.
 * Gson is used to parse the JSON responses from the API.
 */
public class JiraClient {

    private static final Path ENV_FILE =
            Path.of(System.getProperty("user.dir"), ".env");

    // Loaded once when the class is first used — shared across all JiraClient instances
    private static final Map<String, String> ENV_VALUES = loadEnvFile();

    // Format Jira expects for the "started" field: 2026-05-07T09:00:00.000+0000
    private static final DateTimeFormatter JIRA_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final String authHeader;
    private final HttpClient http = HttpClient.newHttpClient();

    public JiraClient() {
        String email    = requireEnv("JIRA_EMAIL");
        String apiToken = requireEnv("JIRA_API_TOKEN");
        // Encode "email:token" as Base64 for the Basic Auth header
        String credentials = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // -------------------------------------------------------------------------
    // URL helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the base URL from a Jira browse URL.
     * "https://company.atlassian.net/browse/PROJ-123" → "https://company.atlassian.net"
     */
    public static String extractBaseUrl(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getHost();
    }

    /**
     * Extracts the issue key from a Jira browse URL.
     * "https://company.atlassian.net/browse/PROJ-123" → "PROJ-123"
     */
    public static String extractKey(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath(); // e.g. "/browse/PROJ-123"
        return path.substring(path.lastIndexOf('/') + 1);
    }

    // -------------------------------------------------------------------------
    // API calls
    // -------------------------------------------------------------------------

    /**
     * Fetches the subtasks of the given issue and returns them as a list of JiraIssue records.
     * Returns an empty list if the issue has no subtasks.
     *
     * API used: GET /rest/api/3/issue/{key}?fields=subtasks
     */
    public List<JiraIssue> fetchSubtasks(String issueKey, String baseUrl) throws Exception {
        String endpoint = baseUrl + "/rest/api/3/issue/" + issueKey + "?fields=subtasks";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Jira API error " + response.statusCode() + ": " + response.body());
        }

        // Parse: { "fields": { "subtasks": [ { "key": "...", "self": "...", "fields": { "summary": "..." } } ] } }
        JsonObject root      = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray  subtasks  = root.getAsJsonObject("fields").getAsJsonArray("subtasks");

        List<JiraIssue> result = new ArrayList<>();
        for (var element : subtasks) {
            JsonObject subtask  = element.getAsJsonObject();
            String key          = subtask.get("key").getAsString();
            String self         = subtask.get("self").getAsString();
            String summary      = subtask.getAsJsonObject("fields").get("summary").getAsString();
            result.add(new JiraIssue(key, summary, self));
        }
        return result;
    }

    /**
     * Posts a worklog entry to the given issue in Jira.
     *
     * The duration is calculated from start to end (e.g. 09:00 → 11:00 = 7200 seconds).
     * The "started" timestamp combines the note's date with the start time, in UTC.
     *
     * API used: POST /rest/api/3/issue/{key}/worklog
     */
    public void logWork(String issueKey, String baseUrl,
                        LocalDate date, LocalTime start, LocalTime end,
                        java.util.Optional<String> description) throws Exception {

        long timeSpentSeconds = Duration.between(start, end).getSeconds();

        ZoneOffset localOffset = ZoneId.systemDefault().getRules().getOffset(date.atTime(start));
        String started = OffsetDateTime.of(date, start, localOffset).format(JIRA_DATE_FMT);

        JsonObject body = new JsonObject();
        body.addProperty("started", started);
        body.addProperty("timeSpentSeconds", timeSpentSeconds);

        // Jira's comment field uses Atlassian Document Format (ADF) — a nested JSON structure.
        // A plain-text comment requires wrapping the text in: doc → paragraph → text
        description.ifPresent(desc -> {
            JsonObject textNode = new JsonObject();
            textNode.addProperty("type", "text");
            textNode.addProperty("text", desc);

            JsonArray textContent = new JsonArray();
            textContent.add(textNode);

            JsonObject paragraph = new JsonObject();
            paragraph.addProperty("type", "paragraph");
            paragraph.add("content", textContent);

            JsonArray paragraphContent = new JsonArray();
            paragraphContent.add(paragraph);

            JsonObject comment = new JsonObject();
            comment.addProperty("type", "doc");
            comment.addProperty("version", 1);
            comment.add("content", paragraphContent);

            body.add("comment", comment);
        });

        String endpoint = baseUrl + "/rest/api/3/issue/" + issueKey + "/worklog";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) { // Jira returns 201 Created on success
            throw new RuntimeException("Jira worklog error " + response.statusCode() + ": " + response.body());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Looks up a credential value. Priority order:
     *   1. System environment variable (set via `export KEY=value` in the shell)
     *   2. ~/.jirahourlogger/.env file
     *
     * Throws a clear error if the key is missing from both sources.
     */
    private static String requireEnv(String name) {
        // System env takes priority — useful for CI or when overriding the file
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) return value;

        // Fall back to the .env file
        value = ENV_VALUES.get(name);
        if (value != null && !value.isBlank()) return value;

        throw new IllegalStateException(
                "Missing credential: " + name + ". " +
                "Add it to " + ENV_FILE + ":\n  " + name + "=yourvalue"
        );
    }

    /**
     * Parses ~/.jirahourlogger/.env into a Map<String, String>.
     *
     * Each line is expected to be KEY=VALUE.
     * Lines starting with # and blank lines are ignored.
     * If the file doesn't exist yet, an empty map is returned silently.
     *
     * static initializer: this method runs once when the class is loaded,
     * before any constructor is called. The result is stored in ENV_VALUES.
     */
    private static Map<String, String> loadEnvFile() {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(ENV_FILE)) return values;

        try {
            for (String line : Files.readAllLines(ENV_FILE)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) continue; // skip comments

                int separator = trimmed.indexOf('=');
                if (separator <= 0) continue; // skip malformed lines

                String key   = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                values.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read " + ENV_FILE + ": " + e.getMessage());
        }

        return values;
    }
}
