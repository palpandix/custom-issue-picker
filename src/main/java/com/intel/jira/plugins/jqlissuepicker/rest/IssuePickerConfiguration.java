package com.intel.jira.plugins.jqlissuepicker.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class IssuePickerConfiguration {
    @JsonProperty
    private String selectionMode;
    @JsonProperty
    private Boolean presetValue;
    @JsonProperty
    private String serviceDeskFieldName;
    @JsonProperty
    private String currentIssueFieldValue;

    public IssuePickerConfiguration() {
    }

    public String getSelectionMode() {
        return this.selectionMode;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public Boolean getPresetValue() {
        return this.presetValue;
    }

    public void setPresetValue(Boolean presetValue) {
        this.presetValue = presetValue;
    }

    public String getServiceDeskFieldName() {
        return this.serviceDeskFieldName;
    }

    public void setServiceDeskFieldName(String serviceDeskFieldName) {
        this.serviceDeskFieldName = serviceDeskFieldName;
    }

    public String getCurrentIssueFieldValue() {
        return this.currentIssueFieldValue;
    }

    public void setCurrentIssueFieldValue(String currentIssueFieldValue) {
        this.currentIssueFieldValue = currentIssueFieldValue;
    }
}
