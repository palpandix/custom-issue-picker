package com.intel.jira.plugins.jqlissuepicker.customfields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.GroupSelectorField;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.export.FieldExportParts;
import com.atlassian.jira.issue.export.FieldExportPartsBuilder;
import com.atlassian.jira.issue.export.customfield.CustomFieldExportContext;
import com.atlassian.jira.issue.export.customfield.ExportableCustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.velocity.VelocityManager;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.customfields.config.IssuePickerConfigItem;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuePickerCFType extends GenericTextCFType  {
    public static final String NEW_ISSUE_KEY_PREFIX = "new-";
    public static final String NEW_ISSUE_KEY_DISPALY_POSTFIX = "(new)";
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerCFType.class);
    private static final String SHOW_NOTHING_IN_VIEW = "showNothingInView";
    private static final String ISSUE_ID = "issueId";
    private static final String CURRENT_PROJECT_ID = "currentProjectId";
    private static final String CURRENT_ISSUE_TYPE_ID = "currentIssueTypeId";
    private static final String IP_CONFIG = "ipConfig";
    private static final String CF_CONFIG_ID = "cfConfigId";
    private static final String UNLICENSED = "unlicensed";
    private final TextFieldCharacterLengthValidator textFieldCharacterLengthValidator;
    private final CustomFieldManager customFieldManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final IssueManager issueManager;
    private final ProjectManager projectManager;
    private final IssueTypeManager issueTypeManager;
    private final VelocityManager velocityManager;
    private final EntityService entityService;
    private final IssuePickerVelocityProvider velocityProvider;
    private final I18nHelper i18n;

    public IssuePickerCFType(CustomFieldValuePersister customFieldValuePersister, GenericConfigManager genericConfigManager, TextFieldCharacterLengthValidator textFieldCharacterLengthValidator, JiraAuthenticationContext jiraAuthenticationContext, CustomFieldManager customFieldManager, IssueLinkTypeManager issueLinkTypeManager, IssueManager issueManager, ProjectManager projectManager, IssueTypeManager issueTypeManager, VelocityManager velocityManager, EntityService entityService, IssuePickerVelocityProvider velocityProvider) {
        super(customFieldValuePersister, genericConfigManager, textFieldCharacterLengthValidator, jiraAuthenticationContext);
        this.textFieldCharacterLengthValidator = textFieldCharacterLengthValidator;
        this.customFieldManager = customFieldManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.issueManager = issueManager;
        this.projectManager = projectManager;
        this.issueTypeManager = issueTypeManager;
        this.velocityManager = velocityManager;
        this.entityService = entityService;
        this.velocityProvider = velocityProvider;
        this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    }

    @Nullable
    public String getValueFromIssue(CustomField field, Issue issue) {
        if (issue != null) {
            FieldConfig fieldConfig = field.getRelevantConfig(issue);
            IssuePickerConfig config = fieldConfig == null ? null : this.entityService.loadIssuePickerConfig(fieldConfig.getId());
            if (config != null && config.getSelectionMode() == SelectionMode.NONE) {
                return "";
            }
        }

        return (String)super.getValueFromIssue(field, issue);
    }

    @Nonnull
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);

        try {
            if (issue == null) {
                params.put("showNothingInView", true);
                params.put("issueId", (Object)null);
                return params;
            }

            params.put("issueId", issue.getId());
            params.put("currentProjectId", issue.getProjectId());
            params.put("currentIssueTypeId", issue.getIssueTypeId());
            
            FieldConfig fieldConfig = field.getRelevantConfig(issue);
            params.put("cfConfigId", fieldConfig.getId());
            IssuePickerConfig config = fieldConfig == null ? null : this.entityService.loadIssuePickerConfig(fieldConfig.getId());
            if (config != null && StringUtils.isNotBlank(config.getJql())) {
                params.put("ipConfig", config);
                List<String> selectedIssueKeys = this.getIssueKeys(issue.getCustomFieldValue(field));
                this.velocityProvider.updateContext(params, config, issue, field, selectedIssueKeys);
            }

            LOG.trace("params: {}", params);
        } catch (Exception var8) {
            LOG.error("[getVelocityParameters] ERROR: ", var8);
        }

        return params;
    }

    private List<String> getIssueKeys(Object customFieldValue) {
        if (customFieldValue instanceof String && StringUtils.isNotBlank((String)customFieldValue)) {
            String[] issueKeys = StringUtils.split((String)customFieldValue, ',');
            return (List)Arrays.asList(issueKeys).stream().map(StringUtils::trim).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public void validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config) {
        super.validateFromParams(relevantParams, errorCollectionToAddTo, config);
        Collection<String> values = relevantParams.getValuesForNullKey();
        LOG.debug("validating: {}", values);
        Iterator var5 = getIssueKeys(values).iterator();

        while(var5.hasNext()) {
            String key = (String)var5.next();
            if (!StringUtils.startsWith(key, "new-")) {
                Issue issue = this.issueManager.getIssueObject(key);
                if (issue == null) {
                    LOG.warn("trying to save issue with unknown key {}", key);
                    errorCollectionToAddTo.addError(config.getFieldId(), "No issue with key " + key);
                }
            }
        }

    }

    @Nonnull
    public static List<String> getIssueKeys(Collection<String> values) {
        List<String> keys = new ArrayList();
        if (CollectionUtils.isNotEmpty(values)) {
            Iterator var2 = values.iterator();

            while(true) {
                String value;
                do {
                    if (!var2.hasNext()) {
                        return keys;
                    }

                    value = (String)var2.next();
                } while(!StringUtils.isNotBlank(value));

                String[] strings = StringUtils.split(value, ',');
                String[] var5 = strings;
                int var6 = strings.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    String string = var5[var7];
                    keys.add(StringUtils.trim(string));
                }
            }
        } else {
            return keys;
        }
    }

    public String getValueFromCustomFieldParams(CustomFieldParams relevantParams) {
        Collection<String> values = relevantParams.getValuesForNullKey();
        if (CollectionUtils.isNotEmpty(values)) {
            String value = StringUtils.join(values, ',');
            if (this.textFieldCharacterLengthValidator.isTextTooLong(value)) {
                throw new FieldValidationException(this.i18n.getText("field.error.text.toolong", this.textFieldCharacterLengthValidator.getMaximumNumberOfCharacters()));
            } else {
                return value;
            }
        } else {
            return null;
        }
    }

    @Nonnull
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_UNLIMITED_TEXT;
    }

    @Nonnull
    public List<FieldConfigItemType> getConfigurationItemTypes() {
        List<FieldConfigItemType> configurationItemTypes = super.getConfigurationItemTypes();
        configurationItemTypes.add(new IssuePickerConfigItem(this.entityService, this.customFieldManager, this.issueLinkTypeManager, this.projectManager, this.issueTypeManager, this.velocityManager));
        return configurationItemTypes;
    }

    public Query getQueryForGroup(String fieldID, String groupName) {
        return new TermQuery(new Term(fieldID, groupName));
    }

    public FieldExportParts getRepresentationFromIssue(Issue issue, CustomFieldExportContext customFieldExportContext) {
        CustomField field = customFieldExportContext.getCustomField();
        FieldConfig fieldConfig = field.getRelevantConfig(issue);
        IssuePickerConfig config = fieldConfig == null ? null : this.entityService.loadIssuePickerConfig(fieldConfig.getId());
        FieldExportParts fieldExportParts;
        if (config != null && Boolean.TRUE.equals(config.getCsvExportUseDisplay())) {
            List<String> selectedIssueKeys = this.getIssueKeys(issue.getCustomFieldValue(field));
            List<Issue> selectedIssues = new ArrayList();
            Iterator var9 = selectedIssueKeys.iterator();

            while(var9.hasNext()) {
                String key = (String)var9.next();
                MutableIssue issueObject = this.issueManager.getIssueObject(key);
                if (issueObject != null) {
                    selectedIssues.add(issueObject);
                }
            }

            Stream<String> displayNames = selectedIssues.stream().map((selectedIssue) -> {
                return this.velocityProvider.getIssueDisplayValue(config, selectedIssue, false);
            });
            fieldExportParts = FieldExportPartsBuilder.buildSinglePartRepresentation(customFieldExportContext.getCustomField().getId(), customFieldExportContext.getDefaultColumnHeader(), displayNames);
        } else {
            String valueFromIssue = this.getValueFromIssue(field, issue);
            fieldExportParts = FieldExportPartsBuilder.buildSinglePartRepresentation(customFieldExportContext.getCustomField().getId(), customFieldExportContext.getDefaultColumnHeader(), valueFromIssue);
        }

        return fieldExportParts;
    }
}
