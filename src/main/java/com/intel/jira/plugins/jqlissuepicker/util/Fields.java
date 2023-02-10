package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.I18nHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fields {
    public static final String CUSTOMFIELD_PREFIX = "customfield";
    private static final Logger LOG = LoggerFactory.getLogger(Fields.class);

    private Fields() {
    }

    public static String getFieldName(CustomFieldManager customFieldManager, I18nHelper i18n, String fieldId) {
        if (StringUtils.isBlank(fieldId)) {
            return null;
        } else if (StringUtils.startsWith(fieldId, "customfield")) {
            CustomField displayValueCustomField = customFieldManager.getCustomFieldObject(fieldId);
            if (displayValueCustomField == null) {
                LOG.warn("could not find custom field with id {}", fieldId);
                return fieldId;
            } else {
                return displayValueCustomField.getName();
            }
        } else {
            BasicField field = Fields.BasicField.forName(fieldId);
            return field != null ? i18n.getText(field.getI18nKey()) : null;
        }
    }

    public static String getStringFieldValue(CustomFieldManager customFieldManager, Issue issue, String fieldId) {
        Object value = getFieldValue(customFieldManager, issue, fieldId);
        return value == null ? null : value.toString();
    }

    public static Object getFieldValue(CustomFieldManager customFieldManager, Issue issue, String fieldId) {
        Object value;
        if (StringUtils.isBlank(fieldId)) {
            value = null;
        } else if (StringUtils.startsWith(fieldId, "customfield")) {
            CustomField displayValueCustomField = customFieldManager.getCustomFieldObject(fieldId);
            value = displayValueCustomField.getValue(issue);
        } else {
            value = Fields.BasicField.getValue(issue, fieldId);
        }

        return value;
    }

    public static enum BasicField {
        SUMMARY("summary"),
        DESCRIPTION("description"),
        PRIORITY("priority", true, true),
        LABELS("labels"),
        AFFECTED_VERSIONS("versions", "affectsversions", true, false),
        FIX_VERSIONS("fixVersions", "fixversions", true, false),
        DUE_DATE("duedate"),
        REPORTER("reporter"),
        ASSIGNEE("assignee"),
        STATUS("status", false),
        PROJECT("project", false),
        COMPONENTS("components", false),
        ENVIRONMENT("environment", false),
        ISSUE_TYPE("issuetype", false, true),
        RESOLUTION("resolution", false);

        private String fieldName;
        private String i18nKey;
        private boolean copyable;
        private boolean rawSort;

        private BasicField(String fieldName) {
            this(fieldName, fieldName, true, false);
        }

        private BasicField(String fieldName, boolean copyable) {
            this(fieldName, fieldName, copyable, false);
        }

        private BasicField(String fieldName, boolean copyable, boolean rawSort) {
            this(fieldName, fieldName, copyable, rawSort);
        }

        private BasicField(String fieldName, String i18nKey, boolean copyable, boolean rawSort) {
            this.fieldName = fieldName;
            this.i18nKey = "issue.field." + i18nKey;
            this.copyable = copyable;
            this.rawSort = rawSort;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public String getI18nKey() {
            return this.i18nKey;
        }

        public boolean isCopyable() {
            return this.copyable;
        }

        public boolean isRawSort() {
            return this.rawSort;
        }

        public static Collection<BasicField> getCopyableValues() {
            List<BasicField> fields = new ArrayList(values().length);
            BasicField[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BasicField field = var1[var3];
                if (field.isCopyable()) {
                    fields.add(field);
                }
            }

            return fields;
        }

        public static BasicField forName(String name) {
            BasicField[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BasicField field = var1[var3];
                if (StringUtils.equals(field.getFieldName(), name)) {
                    return field;
                }
            }

            return null;
        }

        public static String getRawValue(Issue issue, String fieldId) {
            BasicField basicField = forName(fieldId);
            if (basicField == null) {
                Fields.LOG.error("unsupported basic field {}", fieldId);
                return null;
            } else {
                return basicField.getRawValue(issue);
            }
        }

        public String getRawValue(Issue issue) {
            switch (this) {
                case PRIORITY:
                    return issue.getPriority() == null ? null : issue.getPriority().getNameTranslation();
                case ISSUE_TYPE:
                    return issue.getIssueType().getNameTranslation();
                default:
                    Fields.LOG.error("unsupported basic field {}", this.name());
                    return null;
            }
        }

        public static Object getValue(Issue issue, String fieldId) {
            BasicField basicField = forName(fieldId);
            if (basicField == null) {
                Fields.LOG.error("unsupported basic field {}", fieldId);
                return null;
            } else {
                return basicField.getValue(issue);
            }
        }

        public Object getValue(Issue issue) {
            switch (this) {
                case PRIORITY:
                    return issue.getPriority();
                case ISSUE_TYPE:
                    return issue.getIssueType();
                case SUMMARY:
                    return issue.getSummary();
                case DESCRIPTION:
                    return issue.getDescription();
                case LABELS:
                    return issue.getLabels();
                case AFFECTED_VERSIONS:
                    return issue.getAffectedVersions();
                case FIX_VERSIONS:
                    return issue.getFixVersions();
                case DUE_DATE:
                    return issue.getDueDate();
                case REPORTER:
                    return issue.getReporter();
                case ASSIGNEE:
                    return issue.getAssignee();
                case STATUS:
                    return issue.getStatus();
                case COMPONENTS:
                    return issue.getComponents();
                case ENVIRONMENT:
                    return issue.getEnvironment();
                case PROJECT:
                    return issue.getProjectObject();
                case RESOLUTION:
                    return issue.getResolution();
                default:
                    Fields.LOG.error("unsupported basic field {}", this.name());
                    return null;
            }
        }
    }
}
