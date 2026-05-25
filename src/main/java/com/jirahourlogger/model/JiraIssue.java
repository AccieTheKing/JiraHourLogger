package com.jirahourlogger.model;

/**
 * JiraIssue is a lightweight data holder for a Jira issue (typically a subtask).
 *
 * key     — the issue key shown in Jira, e.g. "TITO2-8516"
 * summary — the title of the issue, e.g. "Fix login page layout"
 * self    — the full REST API URL for this issue (returned by Jira, used for direct API calls)
 */
public record JiraIssue(String key, String summary, String self) {}
