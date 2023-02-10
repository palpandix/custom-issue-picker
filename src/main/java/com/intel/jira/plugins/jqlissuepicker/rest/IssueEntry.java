package com.intel.jira.plugins.jqlissuepicker.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class IssueEntry {
    @JsonProperty
    private String key;
    @JsonProperty
    private String displayName;

    public IssueEntry() {
    }

    public IssueEntry(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
