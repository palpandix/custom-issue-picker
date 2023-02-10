package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.customfields.SingleValueCustomFieldValueProvider;
import com.atlassian.jira.issue.customfields.searchers.AbstractInitializationCustomFieldSearcher;
import com.atlassian.jira.issue.customfields.searchers.CustomFieldSearcherClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.SimpleAllTextCustomFieldSearcherClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.information.CustomFieldSearcherInformation;
import com.atlassian.jira.issue.customfields.searchers.renderer.CustomFieldRenderer;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.searchers.information.SearcherInformation;
import com.atlassian.jira.issue.search.searchers.renderer.SearchRenderer;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.FreeTextClauseQueryFactory;
import com.atlassian.jira.jql.validator.FreeTextFieldValidator;
import com.atlassian.jira.web.FieldVisibilityManager;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TextBasedCustomFieldSearcher extends AbstractInitializationCustomFieldSearcher {
    protected final FieldVisibilityManager fieldVisibilityManager;
    protected final JqlOperandResolver jqlOperandResolver;
    protected final CustomFieldInputHelper customFieldInputHelper;
    private SearcherInformation<CustomField> searcherInformation;
    private SearchInputTransformer searchInputTransformer;
    private CustomFieldRenderer searchRenderer;
    private CustomFieldSearcherClauseHandler customFieldSearcherClauseHandler;

    public TextBasedCustomFieldSearcher(FieldVisibilityManager fieldVisibilityManager, JqlOperandResolver jqlOperandResolver, CustomFieldInputHelper customFieldInputHelper) {
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.jqlOperandResolver = jqlOperandResolver;
        this.customFieldInputHelper = customFieldInputHelper;
    }

    public void init(CustomField field) {
        ClauseNames names = field.getClauseNames();
        FieldIndexer indexer = this.createIndexer(field, "sort_");
        CustomFieldValueProvider customFieldValueProvider = new SingleValueCustomFieldValueProvider();
        this.searcherInformation = new CustomFieldSearcherInformation(field.getId(), field.getNameKey(), Collections.singletonList(indexer), new AtomicReference(field));
        this.searchInputTransformer = new TextBasedSearchInputTransformer(field, names, this.searcherInformation.getId(), this.customFieldInputHelper);
        this.searchRenderer = new CustomFieldRenderer(names, this.getDescriptor(), field, customFieldValueProvider, this.fieldVisibilityManager);
        this.customFieldSearcherClauseHandler = new SimpleAllTextCustomFieldSearcherClauseHandler(new FreeTextFieldValidator(field.getId(), this.jqlOperandResolver), new FreeTextClauseQueryFactory(this.jqlOperandResolver, field.getId()), OperatorClasses.TEXT_OPERATORS, JiraDataTypes.TEXT);
    }

    protected abstract FieldIndexer createIndexer(CustomField var1, String var2);

    public final SearcherInformation<CustomField> getSearchInformation() {
        return this.searcherInformation;
    }

    public final SearchInputTransformer getSearchInputTransformer() {
        return this.searchInputTransformer;
    }

    public final SearchRenderer getSearchRenderer() {
        return this.searchRenderer;
    }

    public final CustomFieldSearcherClauseHandler getCustomFieldSearcherClauseHandler() {
        return this.customFieldSearcherClauseHandler;
    }
}
