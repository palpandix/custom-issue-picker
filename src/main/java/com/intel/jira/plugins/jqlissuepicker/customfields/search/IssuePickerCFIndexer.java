package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerVelocityProvider;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;
import com.intel.jira.plugins.jqlissuepicker.util.FieldUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuePickerCFIndexer extends TextBasedCustomFieldIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerCFIndexer.class);
    private final IssueManager issueManager;
    private final EntityService entityService;
    private final IssuePickerVelocityProvider issuePickerVelocityProvider;

    public IssuePickerCFIndexer(FieldVisibilityManager fieldVisibilityManager, CustomField customField, String sortFieldPrefix, IssueManager issueManager, EntityService entityService, IssuePickerVelocityProvider issuePickerVelocityProvider) {
        super(fieldVisibilityManager, customField, sortFieldPrefix);
        this.issueManager = issueManager;
        this.entityService = entityService;
        this.issuePickerVelocityProvider = issuePickerVelocityProvider;
    }

    protected List<String> getIndexValuesForIssueValue(Issue issue, @Nonnull String issueKey) {
        if (issueKey.startsWith("new-")) {
            return Collections.emptyList();
        } else {
            List<String> indexValues = new ArrayList();
            indexValues.add(issueKey);
            FieldConfig fieldConfig = this.customField.getRelevantConfig(issue);
            if (fieldConfig != null) {
                IssuePickerConfig config = this.entityService.loadIssuePickerConfig(fieldConfig.getId());
                if (config != null) {
                    Issue targetIssue = this.issueManager.getIssueObject(issueKey);
                    if (targetIssue == null) {
                        LOG.info("{}: skipping issue with key {}", issue.getKey(), issueKey);
                    } else {
                        if (!StringUtils.equals(issueKey, targetIssue.getKey())) {
                            indexValues.add(targetIssue.getKey());
                        }

                        String displayValue = this.issuePickerVelocityProvider.getIssueDisplayValue(config, targetIssue, true);
                        if (StringUtils.isNotBlank(displayValue)) {
                            indexValues.add(displayValue);
                        }

                        this.indexTableFields(indexValues, config, targetIssue);
                    }
                }
            }

            return indexValues;
        }
    }

    private void indexTableFields(List<String> indexValues, IssuePickerConfig config, Issue issue) {
        if (config.getSelectionMode() != SelectionMode.SINGLE && config.getIndexTableFields()) {
            LOG.trace("indexing table fields for referenced issue {}", issue.getKey());
            Iterator var4 = config.getFieldsToCopy().iterator();

            while(true) {
                List values;
                do {
                    if (!var4.hasNext()) {
                        return;
                    }

                    String fieldId = (String)var4.next();
                    values = FieldUtil.getFieldValueAsListOfString(issue, fieldId);
                } while(values == null);

                Iterator var7 = values.iterator();

                while(var7.hasNext()) {
                    String value = (String)var7.next();
                    if (StringUtils.isNotBlank(value)) {
                        indexValues.add(value);
                    }
                }
            }
        }
    }
}
