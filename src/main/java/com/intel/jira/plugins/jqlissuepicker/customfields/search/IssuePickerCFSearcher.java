package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.customfields.statistics.CustomFieldStattable;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerVelocityProvider;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;

public class IssuePickerCFSearcher extends TextBasedCustomFieldSearcher implements CustomFieldStattable {
    private final IssueManager issueManager;
    private final EntityService entityService;
    private final IssuePickerVelocityProvider issuePickerVelocityProvider;

    public IssuePickerCFSearcher(FieldVisibilityManager fieldVisibilityManager, JqlOperandResolver jqlOperandResolver, CustomFieldInputHelper customFieldInputHelper, IssueManager issueManager, EntityService entityService, IssuePickerVelocityProvider issuePickerVelocityProvider) {
        super(fieldVisibilityManager, jqlOperandResolver, customFieldInputHelper);
        this.issueManager = issueManager;
        this.entityService = entityService;
        this.issuePickerVelocityProvider = issuePickerVelocityProvider;
    }

    protected FieldIndexer createIndexer(CustomField field, String luceneSortfieldPrefix) {
        return new IssuePickerCFIndexer(this.fieldVisibilityManager, field, luceneSortfieldPrefix, this.issueManager, this.entityService, this.issuePickerVelocityProvider);
    }

    public StatisticsMapper<Issue> getStatisticsMapper(CustomField customField) {
        return new IssuePickerStatisticsMapper(this.issueManager, this.customFieldInputHelper, customField);
    }
}
