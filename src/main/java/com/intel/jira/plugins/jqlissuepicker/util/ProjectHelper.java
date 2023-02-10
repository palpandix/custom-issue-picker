package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.opensymphony.module.propertyset.PropertySet;

public class ProjectHelper {
    private static final String PROJECT_METADATA_NAME = "cwx.project.metadata";
    private static final String PROJECT_METADATA_PREFIX = "project.metadata.";
    private final JiraPropertySetFactory propertySetFactory = (JiraPropertySetFactory)ComponentAccessor.getComponent(JiraPropertySetFactory.class);
    private final Long projectId;

    public ProjectHelper(Long projectId) {
        this.projectId = projectId;
    }

    public String getMetadata(String key) {
        if (this.projectId == null) {
            return null;
        } else {
            PropertySet propertySet = this.propertySetFactory.buildNoncachingPropertySet("cwx.project.metadata", this.projectId);
            return propertySet.getString("project.metadata." + key);
        }
    }
}
