package com.intel.jira.plugins.jqlissuepicker.rest;

import java.util.Map;
import org.codehaus.jackson.annotate.JsonProperty;

public class IssuePickerModel {
    @JsonProperty
    private Map<String, IssuePickerConfiguration> configurations;

    public IssuePickerModel() {
    }

    public IssuePickerModel(Map<String, IssuePickerConfiguration> configurations) {
        this.configurations = configurations;
    }

    public Map<String, IssuePickerConfiguration> getConfigurations() {
        return this.configurations;
    }

    public void setConfigurations(Map<String, IssuePickerConfiguration> configurations) {
        this.configurations = configurations;
    }
}
