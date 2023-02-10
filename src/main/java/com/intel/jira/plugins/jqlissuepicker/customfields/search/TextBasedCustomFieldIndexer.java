package com.intel.jira.plugins.jqlissuepicker.customfields.search;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.impl.AbstractCustomFieldIndexer;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextBasedCustomFieldIndexer extends AbstractCustomFieldIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(TextBasedCustomFieldIndexer.class);
    private final String sortFieldPrefix;

    public TextBasedCustomFieldIndexer(FieldVisibilityManager fieldVisibilityManager, CustomField customField, String sortFieldPrefix) {
        super(fieldVisibilityManager, customField);
        this.sortFieldPrefix = sortFieldPrefix;
    }

    public final void addDocumentFieldsSearchable(Document doc, Issue issue) {
        this.addDocumentFields(doc, issue, Index.ANALYZED, true);
    }

    public final void addDocumentFieldsNotSearchable(Document doc, Issue issue) {
        this.addDocumentFields(doc, issue, Index.NO, false);
    }

    private void addDocumentFields(Document doc, Issue issue, Field.Index fieldIndexType, boolean sortable) {
        Object fieldValue = this.customField.getValue(issue);
        LOG.trace("indexing field {} for issue {}, value {}", new Object[]{this.customField.getName(), issue.getKey(), fieldValue});
        if (fieldValue != null) {
            List<String> issueKeys = Collections.emptyList();
            if (fieldValue instanceof String) {
                issueKeys = IssuePickerCFType.getIssueKeys(Collections.singletonList((String)fieldValue));
            } else if (fieldValue instanceof Collection) {
                issueKeys = IssuePickerCFType.getIssueKeys((Collection)fieldValue);
            } else {
                LOG.warn("unsupported type in issue {}, field {}: {}", new Object[]{issue.getKey(), this.customField.getName(), fieldValue});
            }

            Iterator var7 = issueKeys.iterator();

            while(var7.hasNext()) {
                String key = (String)var7.next();
                this.indexValue(issue, doc, key, fieldIndexType, sortable);
            }

        }
    }

    private void indexValue(Issue issue, Document doc, @Nonnull String value, Field.Index fieldIndexType, boolean sortable) {
        if (StringUtils.isNotBlank(value)) {
            Iterator var6 = this.getIndexValuesForIssueValue(issue, value).iterator();

            while(var6.hasNext()) {
                String indexValue = (String)var6.next();
                LOG.trace("indexing value {}", indexValue);
                doc.add(new Field(this.getDocumentFieldId(), indexValue, Store.YES, fieldIndexType));
            }

            if (sortable) {
                String sortText = this.getSortIndexTextForValue(value);
                doc.add(new Field(this.sortFieldPrefix + this.getDocumentFieldId(), sortText, Store.NO, Index.NOT_ANALYZED_NO_NORMS));
            }
        }

    }

    protected String getSortIndexTextForValue(@Nonnull String value) {
        return value;
    }

    protected List<String> getIndexValuesForIssueValue(Issue issue, @Nonnull String value) {
        return Collections.singletonList(value);
    }
}
