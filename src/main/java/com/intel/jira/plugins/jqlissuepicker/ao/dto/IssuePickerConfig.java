package com.intel.jira.plugins.jqlissuepicker.ao.dto;

import com.intel.jira.plugins.jqlissuepicker.data.LinkMode;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.BooleanUtils;
import org.codehaus.jackson.annotate.JsonProperty;

public class IssuePickerConfig {
    @JsonProperty
    private Long fieldConfigId;
    @JsonProperty
    private SelectionMode selectionMode;
    @JsonProperty
    private String displayAttributeFieldId;
    @JsonProperty
    private Boolean showIssueKey;
    @JsonProperty
    private LinkMode linkMode;
    @JsonProperty
    private String customFormat;
    @JsonProperty
    private Long linkTypeId;
    @JsonProperty
    private Boolean outward;
    @JsonProperty
    private String jql;
    @JsonProperty
    private String jqlUser;
    @JsonProperty
    private Integer maxSearchResults;
    @JsonProperty
    private List<String> fieldsToCopy;
    @JsonProperty
    private String copyFieldMapping;
    @JsonProperty
    private List<String> sumUpFields;
    @JsonProperty
    private List<String> fieldsToInit;
    @JsonProperty
    private String initFieldMapping;
    @JsonProperty
    private Boolean presetValue;
    @JsonProperty
    private Boolean indexTableFields;
    @JsonProperty
    private Boolean expandIssueTable;
    @JsonProperty
    private Boolean csvExportUseDisplay;
    @JsonProperty
    private Boolean createNewValue;
    @JsonProperty
    private String newIssueProject;
    @JsonProperty
    private Boolean currentProject;
    @JsonProperty
    private String newIssueType;

    public IssuePickerConfig() {
    }

    public IssuePickerConfig(Long fieldConfigId, SelectionMode selectionMode, String displayAttributeFieldId, Boolean showIssueKey, String customFormat, LinkMode linkMode, Long linkTypeId, Boolean outward, String jql, String jqlUser, Integer maxSearchResults, @Nonnull List<String> fieldsToCopy, @Nonnull List<String> sumUpFields, @Nonnull List<String> fieldsToInit, Boolean presetValue, Boolean indexTableFields, Boolean expandIssueTable, Boolean csvExportUseDisplay, Boolean createNewValue, String newIssueProject, Boolean currentProject, String newIssueType, String initFieldMapping, String copyFieldMapping) {
        this.fieldConfigId = fieldConfigId;
        this.selectionMode = selectionMode;
        this.displayAttributeFieldId = displayAttributeFieldId;
        this.showIssueKey = BooleanUtils.isTrue(showIssueKey);
        this.customFormat = customFormat;
        this.linkMode = linkMode;
        this.linkTypeId = linkTypeId;
        this.outward = BooleanUtils.isTrue(outward);
        this.jql = jql;
        this.jqlUser = jqlUser;
        this.maxSearchResults = maxSearchResults;
        this.fieldsToCopy = fieldsToCopy;
        this.sumUpFields = sumUpFields;
        this.fieldsToInit = fieldsToInit;
        this.presetValue = presetValue;
        this.indexTableFields = indexTableFields;
        this.expandIssueTable = expandIssueTable;
        this.csvExportUseDisplay = csvExportUseDisplay;
        this.createNewValue = createNewValue;
        this.newIssueProject = newIssueProject;
        this.currentProject = currentProject;
        this.newIssueType = newIssueType;
        this.initFieldMapping = initFieldMapping;
        this.copyFieldMapping = copyFieldMapping;
    }

    public Long getFieldConfigId() {
        return this.fieldConfigId;
    }

    public void setFieldConfigId(Long fieldConfigId) {
        this.fieldConfigId = fieldConfigId;
    }

    public SelectionMode getSelectionMode() {
        return this.selectionMode;
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public String getDisplayAttributeFieldId() {
        return this.displayAttributeFieldId;
    }

    public void setDisplayAttributeFieldId(String displayAttributeFieldId) {
        this.displayAttributeFieldId = displayAttributeFieldId;
    }

    public boolean getShowIssueKey() {
        return this.showIssueKey != null && this.showIssueKey;
    }

    public void setShowIssueKey(Boolean showIssueKey) {
        this.showIssueKey = showIssueKey;
    }

    public String getCustomFormat() {
        return this.customFormat;
    }

    public void setCustomFormat(String customFormat) {
        this.customFormat = customFormat;
    }

    public LinkMode getLinkMode() {
        return this.linkMode;
    }

    public void setLinkMode(LinkMode linkMode) {
        this.linkMode = linkMode;
    }

    public Long getLinkTypeId() {
        return this.linkTypeId;
    }

    public void setLinkTypeId(Long linkTypeId) {
        this.linkTypeId = linkTypeId;
    }

    public boolean getOutward() {
        return this.outward != null && this.outward;
    }

    public void setOutward(Boolean outward) {
        this.outward = outward;
    }

    public String getJql() {
        return this.jql;
    }

    public void setJql(String jql) {
        this.jql = jql;
    }

    public String getJqlUser() {
        return this.jqlUser;
    }

    public void setJqlUser(String jqlUser) {
        this.jqlUser = jqlUser;
    }

    public Integer getMaxSearchResults() {
        return this.maxSearchResults;
    }

    public void setMaxSearchResults(Integer maxSearchResults) {
        this.maxSearchResults = maxSearchResults;
    }

    public String toString() {
        return "displayAttr: " + this.displayAttributeFieldId + ", linkType: " + this.linkTypeId + ", jql: " + this.jql;
    }

    @Nonnull
    public List<String> getFieldsToCopy() {
        return this.fieldsToCopy == null ? Collections.emptyList() : this.fieldsToCopy;
    }

    public void setFieldsToCopy(List<String> fieldsToCopy) {
        this.fieldsToCopy = fieldsToCopy;
    }

    @Nonnull
    public List<String> getSumUpFields() {
        return this.sumUpFields == null ? Collections.emptyList() : this.sumUpFields;
    }

    public void setSumUpFields(List<String> sumUpFields) {
        this.sumUpFields = sumUpFields;
    }

    @Nonnull
    public List<String> getFieldsToInit() {
        return this.fieldsToInit == null ? Collections.emptyList() : this.fieldsToInit;
    }

    public void setFieldsToInit(List<String> fieldsToInit) {
        this.fieldsToInit = fieldsToInit;
    }

    public boolean getPresetValue() {
        return this.presetValue != null && this.presetValue;
    }

    public void setPresetValue(Boolean presetValue) {
        this.presetValue = presetValue;
    }

    public boolean getIndexTableFields() {
        return this.indexTableFields != null && this.indexTableFields;
    }

    public void setIndexTableFields(Boolean indexTableFields) {
        this.indexTableFields = indexTableFields;
    }

    public Boolean getExpandIssueTable() {
        return this.expandIssueTable;
    }

    public void setExpandIssueTable(Boolean expandIssueTable) {
        this.expandIssueTable = expandIssueTable;
    }

    public boolean getCreateNewValue() {
        return this.createNewValue != null && this.createNewValue;
    }

    public boolean getCurrentProject() {
        return this.currentProject != null && this.currentProject;
    }

    public void setCurrentProject(Boolean currentProject) {
        this.currentProject = currentProject;
    }

    public void setCreateNewValue(Boolean createNewValue) {
        this.createNewValue = createNewValue;
    }

    public String getNewIssueProject() {
        return this.newIssueProject;
    }

    public void setNewIssueProject(String newIssueProject) {
        this.newIssueProject = newIssueProject;
    }

    public String getNewIssueType() {
        return this.newIssueType;
    }

    public void setNewIssueType(String newIssueType) {
        this.newIssueType = newIssueType;
    }

    public Boolean getCsvExportUseDisplay() {
        return this.csvExportUseDisplay;
    }

    public void setCsvExportUseDisplay(Boolean csvExportUseDisplay) {
        this.csvExportUseDisplay = csvExportUseDisplay;
    }

    public String getCopyFieldMapping() {
        return this.copyFieldMapping;
    }

    public void setCopyFieldMapping(String copyFieldMapping) {
        this.copyFieldMapping = copyFieldMapping;
    }

    public String getInitFieldMapping() {
        return this.initFieldMapping;
    }

    public void setInitFieldMapping(String initFieldMapping) {
        this.initFieldMapping = initFieldMapping;
    }
}
