package com.intel.jira.plugins.jqlissuepicker.actions;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class CwxJPickFormatHelpAction extends JiraWebActionSupport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CwxJPickFormatHelpAction.class);
    private static final String FIELD_HELPER_GET_VALUE = "$!fieldHelper.getValueAsString(\"%s\")";
    private static final List<String> ALL_STANDARD_FIELDS = Arrays.asList("affectedVersions", "assignee", "components", "created", "creator", "description", "dueDate", "environment", "estimate", "fixVersions", "id", "issueType", "key", "labels", "originalEstimate", "priority", "project", "reporter", "resolution", "securityLevel", "status", "summary", "timespent", "updated", "votes", "watches");
    private final transient CustomFieldManager customFieldManager;

    public CwxJPickFormatHelpAction(CustomFieldManager customFieldManager) {
        this.customFieldManager = customFieldManager;
    }

    protected String doExecute() {
        LOG.debug("[doExecute] called");
        return "input";
    }

    public Map<String, String> getCustomFields() {
        Map<String, String> customFields = new LinkedHashMap();
        Iterator var2 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var2.hasNext()) {
            CustomField field = (CustomField)var2.next();
            String key = field.getName() + " (" + field.getId() + ")";
            String value = String.format("$!fieldHelper.getValueAsString(\"%s\")", field.getId());
            customFields.put(key, value);
        }

        return customFields;
    }

    public Map<String, String> getStandardFields() {
        Map<String, String> standardFields = new LinkedHashMap();
        Iterator var2 = ALL_STANDARD_FIELDS.iterator();

        while(var2.hasNext()) {
            String field = (String)var2.next();
            String value = String.format("$!fieldHelper.getValueAsString(\"%s\")", field);
            standardFields.put(field, value);
        }

        return standardFields;
    }
}
