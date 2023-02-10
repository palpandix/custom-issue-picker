package com.intel.jira.plugins.jqlissuepicker.rest;

import java.util.List;
import org.codehaus.jackson.annotate.JsonProperty;

public class SearchResult {
    @JsonProperty
    private int total;
    @JsonProperty
    private List<IssueEntry> issues;

    public SearchResult() {
    }

    public SearchResult(int total, List<IssueEntry> issues) {
        this.total = total;
        this.issues = issues;
    }

    public int getTotal() {
        return this.total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<IssueEntry> getIssues() {
        return this.issues;
    }

    public void setIssues(List<IssueEntry> issues) {
        this.issues = issues;
    }
}
