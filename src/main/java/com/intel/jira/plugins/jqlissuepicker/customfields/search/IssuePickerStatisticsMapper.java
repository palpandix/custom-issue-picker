package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comparator.IssueKeyComparator;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchRequestAppender;
import com.atlassian.jira.issue.search.util.SearchRequestAddendumBuilder;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operator.Operator;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class IssuePickerStatisticsMapper implements StatisticsMapper<Issue>, SearchRequestAppender.Factory<Issue> {
    private final IssueManager issueManager;
    private final CustomFieldInputHelper customFieldInputHelper;
    private final CustomField customField;

    public IssuePickerStatisticsMapper(IssueManager issueManager, CustomFieldInputHelper customFieldInputHelper, CustomField customField) {
        this.issueManager = issueManager;
        this.customFieldInputHelper = customFieldInputHelper;
        this.customField = customField;
    }

    public boolean isValidValue(Issue value) {
        return value != null;
    }

    public boolean isFieldAlwaysPartOfAnIssue() {
        return false;
    }

    public SearchRequest getSearchUrlSuffix(Issue value, SearchRequest searchRequest) {
        return this.getSearchRequestAppender().appendInclusiveSingleValueClause(value, searchRequest);
    }

    public String getDocumentConstant() {
        return this.customField.getId();
    }

    public Issue getValueFromLuceneField(String documentValue) {
        List<Issue> issues = this.getIssues(IssuePickerCFType.getIssueKeys(Collections.singletonList(documentValue)));
        return issues.isEmpty() ? null : (Issue)issues.get(0);
    }

    public Comparator<Issue> getComparator() {
        return IssueKeyComparator.COMPARATOR;
    }

    public SearchRequestAppender<Issue> getSearchRequestAppender() {
        return new IssuePickerSearchRequestAppender();
    }

    private List<Issue> getIssues(List<String> keys) {
        List<Issue> issues = new ArrayList(keys.size());
        Iterator var3 = keys.iterator();

        while(var3.hasNext()) {
            String key = (String)var3.next();
            Issue issue = this.issueManager.getIssueObject(key.toUpperCase());
            if (issue != null) {
                issues.add(issue);
            }
        }

        return issues;
    }

    private class IssuePickerSearchRequestAppender implements SearchRequestAppender<Issue>, SearchRequestAddendumBuilder.AddendumCallback<Issue> {
        private final String clauseName = this.getClauseName();

        public IssuePickerSearchRequestAppender() {
        }

        public SearchRequest appendInclusiveSingleValueClause(Issue value, SearchRequest searchRequest) {
            return SearchRequestAddendumBuilder.appendAndClause(value, searchRequest, this);
        }

        public SearchRequest appendExclusiveMultiValueClause(Iterable<? extends Issue> values, SearchRequest searchRequest) {
            return SearchRequestAddendumBuilder.appendAndNotClauses(values, searchRequest, this);
        }

        public void appendNonNullItem(Issue value, JqlClauseBuilder clauseBuilder) {
            clauseBuilder.addClause(new TerminalClauseImpl(this.clauseName, Operator.LIKE, value.getKey()));
        }

        public void appendNullItem(JqlClauseBuilder clauseBuilder) {
            clauseBuilder.addEmptyCondition(this.clauseName);
        }

        private String getClauseName() {
            ClauseNames clauseNames = IssuePickerStatisticsMapper.this.customField.getClauseNames();
            ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            return IssuePickerStatisticsMapper.this.customFieldInputHelper.getUniqueClauseName(user, clauseNames.getPrimaryName(), IssuePickerStatisticsMapper.this.customField.getName());
        }
    }
}
