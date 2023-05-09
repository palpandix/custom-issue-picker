package com.intel.jira.plugins.jqlissuepicker.customfields.search;



import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.impl.AbstractCustomFieldIndexer;
import com.atlassian.jira.issue.index.indexers.impl.FieldIndexerUtil;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.util.BytesRef;
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
        this.addDocumentFields(doc, issue, true);
    }

    public final void addDocumentFieldsNotSearchable(Document doc, Issue issue) {
        this.addDocumentFields(doc, issue, false);
    }

    private void addDocumentFields(Document doc, Issue issue, boolean searchable) {
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

            boolean first = true;

            for(Iterator var7 = issueKeys.iterator(); var7.hasNext(); first = false) {
                String key = (String)var7.next();
                this.indexValue(issue, doc, key, searchable && first);
            }

        }
    }

    private void indexValue(Issue issue, Document doc, @Nonnull String value, boolean searchable) {
        if (StringUtils.isNotBlank(value)) {
            Iterator var5 = this.getIndexValuesForIssueValue(issue, value).iterator();

            String valueForSorting;
            while(var5.hasNext()) {
                valueForSorting = (String)var5.next();
                LOG.trace("indexing value {}", valueForSorting);
                doc.add(new TextField(this.getDocumentFieldId(), valueForSorting, Store.YES));
                doc.add(new SortedSetDocValuesField(this.getDocumentFieldId(), new BytesRef(valueForSorting)));
            }

            if (searchable) {
                String sortText = this.getSortIndexTextForValue(issue, value);
                valueForSorting = FieldIndexerUtil.getValueForSorting(sortText);
                LOG.trace("sort text: {}", sortText);
                doc.add(new SortedDocValuesField(this.sortFieldPrefix + this.getDocumentFieldId(), new BytesRef(valueForSorting)));
            }
        }

    }

    protected String getSortIndexTextForValue(Issue issue, @Nonnull String value) {
        return value;
    }

    protected List<String> getIndexValuesForIssueValue(Issue issue, @Nonnull String value) {
        return Collections.singletonList(value);
    }
}

