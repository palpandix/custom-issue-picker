package com.intel.jira.plugins.jqlissuepicker.customfields.config;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.velocity.VelocityManager;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.util.Fields;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuePickerConfigItem implements FieldConfigItemType {
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerConfigItem.class);
    private static final String VIEW_TEMPLATE = "/templates/plugins/customfields/issue-picker-configuration-view.vm";
    private static final String CONFIG = "config";
    private static final String PRESET_VALUE = "presetValue";
    private static final String INDEX_TABLE_FIELDS = "indexTableFields";
    private static final String EXPAND_ISSUE_TABLE = "expandIssueTable";
    private static final String CSV_EXPORT_USE_DISPLAY = "csvExportUseDisplay";
    private static final String DISPLAY_ATTRIBUTE = "displayAttribute";
    private static final String SHOW_ISSUE_KEY = "showIssueKey";
    private static final String LINK_TYPE = "linkType";
    private static final String FIELDS_TO_COPY = "fieldsToCopy";
    private static final String COPY_FIELD_MAPPING = "copyFieldMapping";
    private static final String SUM_UP_FIELDS = "sumUpFields";
    private static final String FIELDS_TO_INIT = "fieldsToInit";
    private static final String INIT_FIELD_MAPPING = "initFieldMapping";
    private static final String NEW_ISSUE_PROJECT = "newIssueProject";
    private static final String CURRENT_PROJECT = "currentProject";
    private static final String NEW_ISSUE_TYPE = "newIssueType";
    private static final String CREATE_NEW_VALUE = "createNewValue";
    private final EntityService entityService;
    private final CustomFieldManager customFieldManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final ProjectManager projectManager;
    private final IssueTypeManager issueTypeManager;
    private final VelocityManager velocityManager;
    private final I18nHelper i18n;

    public IssuePickerConfigItem(EntityService entityService, CustomFieldManager customFieldManager, IssueLinkTypeManager issueLinkTypeManager, ProjectManager projectManager, IssueTypeManager issueTypeManager, VelocityManager velocityManager) {
        this.entityService = entityService;
        this.customFieldManager = customFieldManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.projectManager = projectManager;
        this.issueTypeManager = issueTypeManager;
        this.velocityManager = velocityManager;
        this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    }

    public String getObjectKey() {
        return "ics.issue-picker.config";
    }

    public String getDisplayName() {
        return "issue picker configuration";
    }

    public String getDisplayNameKey() {
        return "ics.issue-picker.config.name";
    }

    public String getViewHtml(FieldConfig fieldConfig, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> context = new HashMap();
        IssuePickerConfig config = this.getConfigurationObject((Issue)null, fieldConfig);
        context.put("i18n", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper());
        this.addToVelocityContext(context, config);

        try {
            String template = this.getResource("/templates/plugins/customfields/issue-picker-configuration-view.vm");
            VelocityContext velocityContext = new VelocityContext(context);
            StringWriter stringWriter = new StringWriter();
            this.velocityManager.writeEncodedBodyForContent(stringWriter, template, velocityContext);
            return stringWriter.toString();
        } catch (Exception var8) {
            LOG.error("could not render template", var8);
            return "";
        }
    }

    private void addToVelocityContext(Map<String, Object> context, IssuePickerConfig config) {
        context.put("config", config);
        if (config != null) {
            context.put("presetValue", this.renderBoolean(config.getPresetValue()));
            context.put("indexTableFields", this.renderBoolean(config.getIndexTableFields()));
            context.put("expandIssueTable", this.renderBoolean(config.getExpandIssueTable()));
            context.put("csvExportUseDisplay", this.renderBoolean(config.getCsvExportUseDisplay()));
            String displayAttribute = this.i18n.getText("ics.issue-picker.key-only");
            if (StringUtils.isNotBlank(config.getCustomFormat())) {
                displayAttribute = config.getCustomFormat();
            } else if (StringUtils.isNotBlank(config.getDisplayAttributeFieldId())) {
                displayAttribute = Fields.getFieldName(this.customFieldManager, this.i18n, config.getDisplayAttributeFieldId());
                context.put("showIssueKey", this.renderBoolean(config.getShowIssueKey()));
            }

            context.put("displayAttribute", displayAttribute);
            context.put("linkType", this.getLinkTypeString(config));
            context.put("fieldsToCopy", this.getFieldNames(config.getFieldsToCopy()));
            context.put("sumUpFields", this.getFieldNames(config.getSumUpFields()));
            context.put("fieldsToInit", this.getFieldNames(config.getFieldsToInit()));
            context.put("createNewValue", this.renderBoolean(config.getCreateNewValue()));
            if (config.getCreateNewValue()) {
                context.put("currentProject", this.renderBoolean(config.getCurrentProject()));
                if (!config.getCurrentProject()) {
                    context.put("newIssueProject", this.getProjectName(config.getNewIssueProject()));
                }

                context.put("newIssueType", this.getIssueTypeName(config.getNewIssueType()));
            }

            context.put("copyFieldMapping", this.fieldMappingName(config.getCopyFieldMapping()));
            context.put("initFieldMapping", this.fieldMappingName(config.getInitFieldMapping()));
        }

    }

    private String fieldMappingName(String fieldMappingId) {
        if (StringUtils.isNotBlank(fieldMappingId) && !"EMPTY".equals(fieldMappingId)) {
            FieldMapping fieldMapping = this.entityService.getFieldMapping(Integer.parseInt(fieldMappingId));
            if (fieldMapping != null) {
                return fieldMapping.getName();
            }
        }

        return "";
    }

    private String getProjectName(String projectId) {
        Project project = this.projectManager.getProjectObj(NumberUtils.toLong(projectId));
        return project.getName();
    }

    private String getIssueTypeName(String issueTypeId) {
        IssueType issueType = this.issueTypeManager.getIssueType(issueTypeId);
        return issueType.getName();
    }

    private String getLinkTypeString(IssuePickerConfig config) {
        if (config.getLinkTypeId() == null) {
            return null;
        } else {
            IssueLinkType linkType = this.issueLinkTypeManager.getIssueLinkType(config.getLinkTypeId());
            return config.getOutward() ? linkType.getOutward() : linkType.getInward();
        }
    }

    private String renderBoolean(Boolean bool) {
        return bool != null && bool ? this.i18n.getText("common.words.yes") : this.i18n.getText("common.words.no");
    }

    private String getFieldNames(List<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return "";
        } else {
            List<String> names = new ArrayList();
            Iterator var3 = fields.iterator();

            while(var3.hasNext()) {
                String fieldId = (String)var3.next();
                names.add(Fields.getFieldName(this.customFieldManager, this.i18n, fieldId));
            }

            return StringUtils.join(names, ", ");
        }
    }

    private String getResource(String resource) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(resource));
    }

    public IssuePickerConfig getConfigurationObject(Issue issue, FieldConfig config) {
        return this.entityService.loadIssuePickerConfig(config.getId());
    }

    public String getBaseEditUrl() {
        return "CwxConfigureIssuePicker.jspa";
    }
}
