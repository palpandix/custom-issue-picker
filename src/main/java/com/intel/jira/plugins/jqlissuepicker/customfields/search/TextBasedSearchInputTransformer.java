package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.customfields.searchers.transformer.FreeTextCustomFieldSearchInputTransformer;
import com.atlassian.jira.issue.customfields.searchers.transformer.TextQueryValidator;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.lucene.parsing.LuceneQueryParserFactory;
import java.util.Collection;
import java.util.Iterator;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextBasedSearchInputTransformer extends FreeTextCustomFieldSearchInputTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(TextBasedSearchInputTransformer.class);
    private final TextQueryValidator textQueryValidator = new TextQueryValidator();

    public TextBasedSearchInputTransformer(CustomField customField, ClauseNames clauseNames, String urlParameterName, CustomFieldInputHelper customFieldInputHelper) {
        super(customField, clauseNames, urlParameterName, customFieldInputHelper);
    }

    public void validateParams(ApplicationUser user, SearchContext searchContext, FieldValuesHolder fieldValuesHolder, I18nHelper i18nHelper, ErrorCollection errors) {
        if (fieldValuesHolder.containsKey(this.getCustomField().getId())) {
            CustomFieldParams customFieldParams = (CustomFieldParams)fieldValuesHolder.get(this.getCustomField().getId());
            String paramValue = this.getFieldValue(this.getCustomField().getCustomFieldType(), customFieldParams);
            if (paramValue != null) {
                MessageSet validationResult = this.textQueryValidator.validate(this.getQueryParserForField(this.getCustomField()), paramValue, this.getCustomField().getFieldName(), (String)null, true, i18nHelper);
                Iterator var9 = validationResult.getErrorMessages().iterator();

                while(var9.hasNext()) {
                    String errorMessage = (String)var9.next();
                    errors.addError(this.getCustomField().getId(), errorMessage);
                }
            }
        }

    }

    private QueryParser getQueryParserForField(CustomField customField) {
        return ((LuceneQueryParserFactory)ComponentAccessor.getComponent(LuceneQueryParserFactory.class)).createParserFor(customField.getId());
    }

    private String getFieldValue(CustomFieldType<?, ?> customFieldType, CustomFieldParams customFieldParams) {
        Object value = customFieldType.getValueFromCustomFieldParams(customFieldParams);
        if (value instanceof Collection) {
            Collection<?> collection = (Collection)value;
            if (!collection.isEmpty()) {
                Object first = collection.iterator().next();
                if (first instanceof String) {
                    return (String)first;
                }

                if (first != null) {
                    LOG.warn("unsupported type of value in CF {}", this.getCustomField().getName(), first);
                }
            }
        } else {
            if (value instanceof String) {
                return (String)value;
            }

            if (value != null) {
                LOG.warn("unsupported type of value in CF {}", this.getCustomField().getName(), value);
            }
        }

        return null;
    }
}
