package com.intel.jira.plugins.jqlissuepicker.customfields;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.entity.WithKey;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comparator.KeyComparator;
import com.atlassian.jira.issue.customfields.impl.NumberCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.renderer.v2.components.HtmlEscaper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;
import com.intel.jira.plugins.jqlissuepicker.rest.IssueEntry;
import com.intel.jira.plugins.jqlissuepicker.util.Fields;
import com.intel.jira.plugins.jqlissuepicker.util.NumberFormatter;
import com.intel.jira.plugins.jqlissuepicker.util.QueryUtil;
import com.intel.jira.plugins.jqlissuepicker.util.TemplateUtils;
import com.intel.jira.plugins.jqlissuepicker.util.Fields.BasicField;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuePickerVelocityProvider {
    private static final String INITIAL_VALUES_JSON = "initialValuesJson";
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerVelocityProvider.class);
    private static final String ISSUE_PICKER_INFO = "cwxip";
    private static Cache<String, Optional<String>> cache;
    private final CustomFieldManager customFieldManager;
    private final FieldManager fieldManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final PermissionManager permissionManager;
    private final IssueManager issueManager;
    private final SearchService searchService;
    private final EntityService entityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IssuePickerVelocityProvider(CustomFieldManager customFieldManager, FieldManager fieldManager, FieldLayoutManager fieldLayoutManager, PermissionManager permissionManager, IssueManager issueManager, SearchService searchService, EntityService entityService) {
        this.customFieldManager = customFieldManager;
        this.fieldManager = fieldManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.permissionManager = permissionManager;
        this.issueManager = issueManager;
        this.searchService = searchService;
        this.entityService = entityService;
    }

    public void updateContext(Map<String, Object> params, IssuePickerConfig config, Issue issue, CustomField field, List<String> selectedIssueKeyList) {
        try {
            List<String> selectedIssueKeys = selectedIssueKeyList;
            String jqlToShow = null;
            Pair issuesAndKeys;
            List issueInfos;
            if (config.getSelectionMode() == SelectionMode.NONE) {
                String jql = TemplateUtils.replaceVariables(config.getJql(), issue);
                LOG.trace("{}: {}: querying issues", issue.getKey(), field.getName());
                issueInfos = QueryUtil.queryIssues(this.searchService, jql, issue, config.getJqlUser());
                //selectedIssueKeys = (List)issueInfos.stream().map(Issue::getKey).collect(Collectors.toList());
                selectedIssueKeys = null;
                issuesAndKeys = Pair.of(issueInfos, Collections.emptyList());
                jqlToShow = jql;
            } else {
                issuesAndKeys = this.checkExistingIssues(issue.getKey(), field.getName(), selectedIssueKeyList);
            }

            Map<String, Double> sumsByFieldId = new HashMap();
            issueInfos = this.getIssueInfos(config, (List)issuesAndKeys.getLeft(), (List)issuesAndKeys.getRight(), sumsByFieldId);
            params.put("cwxip", new CwxIpInfo(StringUtils.join(selectedIssueKeys, ","), issueInfos, this.getDisplayFieldName(config.getDisplayAttributeFieldId()), this.getJqlLink(jqlToShow), this.getFieldInfos(config), this.formatSums(sumsByFieldId)));
            params.put("initialValuesJson", this.getInitialValuesJson(issueInfos));
        } catch (Exception var11) {
            LOG.error("[updateContext] ERROR: ", var11);
        }

    }

    private String getInitialValuesJson(List<IssueInfo> issueInfos) throws IOException {
        List<IssueEntry> entries = (List)issueInfos.stream().map((info) -> {
            return new IssueEntry(info.getKey(), info.getDisplayName());
        }).collect(Collectors.toList());
        return this.objectMapper.writeValueAsString(entries);
    }

    private List<IssueInfo> getIssueInfos(IssuePickerConfig config, List<Issue> selectedIssues, List<String> selectedDeletedIssueKeys, Map<String, Double> sumsByFieldId) {
        List<IssueInfo> infos = new ArrayList();
        ApplicationUser user = QueryUtil.getUser(config.getJqlUser());
        Iterator var7 = selectedDeletedIssueKeys.iterator();

        while(var7.hasNext()) {
            String key = (String)var7.next();
            infos.add(this.createdDeletedIssueInfo(key));
        }

        var7 = selectedIssues.iterator();

        while(var7.hasNext()) {
            Issue issue = (Issue)var7.next();
            infos.add(this.createIssueInfo(config, user, issue, true, sumsByFieldId));
        }

        Collections.sort(infos, IssuePickerVelocityProvider.DisplayNameComparator.getInstance(user));
        return infos;
    }

    private IssueInfo createIssueInfo(IssuePickerConfig config, ApplicationUser user, Issue issue, boolean selected, Map<String, Double> sumsByFieldId) {
        String key = issue.getKey();
        String hyperLink;
        if (this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
            hyperLink = this.getIssueDisplayValue(config, issue, false);
            hyperLink = this.makeHyperLink(issue, hyperLink);
            this.processSums(config, issue, sumsByFieldId);
            return new IssueInfo(key, hyperLink, hyperLink, this.getFields(config, issue), selected);
        } else {
            hyperLink = this.makeHyperLink(issue, key);
            return new IssueInfo(key, key, hyperLink, Collections.emptyMap(), selected);
        }
    }

    private void processSums(IssuePickerConfig config, Issue issue, Map<String, Double> sumsByFieldId) {
        Iterator var4 = config.getSumUpFields().iterator();

        while(var4.hasNext()) {
            String field = (String)var4.next();
            Double currentValue = (Double)Fields.getFieldValue(this.customFieldManager, issue, field);
            LOG.trace("{}: {} has value {}", new Object[]{issue.getKey(), field, currentValue});
            if (currentValue != null) {
                Double value = (Double)sumsByFieldId.get(field);
                if (value == null) {
                    value = currentValue;
                } else {
                    value = value + currentValue;
                }

                sumsByFieldId.put(field, value);
            }
        }

    }

    private String newIssueKeyDisplayName(String issueKey) {
        String suffix = issueKey.substring("new-".length());
        return suffix + " " + "(new)";
    }

    private IssueInfo createdDeletedIssueInfo(String issueKey) {
        String formattedIssueKey = "<span class=\"cwx-deleted\">" + issueKey + "</span>";
        String dispalyName = issueKey.startsWith("new-") ? this.newIssueKeyDisplayName(issueKey) : issueKey;
        return new IssueInfo(issueKey, dispalyName, formattedIssueKey, (Map)null, true);
    }

    private Pair<List<Issue>, List<String>> checkExistingIssues(String issueKey, String fieldName, List<String> keys) {
        List<Issue> issues = new ArrayList();
        List<String> deletedIssueKeys = new ArrayList();
        Iterator var6 = keys.iterator();

        while(var6.hasNext()) {
            String key = (String)var6.next();
            MutableIssue issue = this.issueManager.getIssueObject(key);
            if (issue == null) {
                LOG.info("issue {}[{}] references non-existing issue {}", new Object[]{issueKey, fieldName, key});
                deletedIssueKeys.add(key);
            } else {
                issues.add(issue);
            }
        }

        return Pair.of(issues, deletedIssueKeys);
    }

    private String getJqlLink(String jql) {
        if (StringUtils.isBlank(jql)) {
            return null;
        } else {
            String baseUrl = TemplateUtils.getAppilcationBaseURL();
            return baseUrl + "/issues/?jql=" + this.escapeURLParam(jql);
        }
    }

    private String getDisplayFieldName(String fieldId) {
        Field field = this.fieldManager.getField(fieldId);
        return field != null ? field.getName() : null;
    }

    private String escapeURLParam(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    @Nullable
    private List<FieldInfo> getFieldInfos(IssuePickerConfig config) {
        if (config.getSelectionMode() == SelectionMode.SINGLE) {
            return null;
        } else {
            I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            List<String> fieldIds = config.getFieldsToCopy();
            List<FieldInfo> fieldNames = new ArrayList(fieldIds.size());
            Iterator var5 = fieldIds.iterator();

            while(var5.hasNext()) {
                String fieldId = (String)var5.next();
                String fieldName = Fields.getFieldName(this.customFieldManager, i18n, fieldId);
                boolean rightAlign = this.isRightAlign(fieldId);
                boolean rawSort = this.isRawSort(fieldId);
                if (StringUtils.isNotBlank(fieldName)) {
                    fieldNames.add(new FieldInfo(fieldId, fieldName, rightAlign, rawSort));
                }
            }

            Collections.sort(fieldNames, new FieldComparator());
            return fieldNames;
        }
    }

    private boolean isRawSort(String fieldId) {
        if (StringUtils.startsWith(fieldId, "customfield")) {
            return false;
        } else {
            Fields.BasicField basicField = BasicField.forName(fieldId);
            return basicField.isRawSort();
        }
    }

    private boolean isRightAlign(String fieldId) {
        if (!StringUtils.startsWith(fieldId, "customfield")) {
            return false;
        } else {
            CustomField field = this.customFieldManager.getCustomFieldObject(fieldId);
            return field != null ? field.getCustomFieldType() instanceof NumberCFType : false;
        }
    }

    private Map<String, String> formatSums(Map<String, Double> sums) {
        Map<String, String> formattedSums = new HashMap();
        Iterator var3 = sums.entrySet().iterator();

        while(var3.hasNext()) {
            Map.Entry<String, Double> entry = (Map.Entry)var3.next();
            Double value = (Double)entry.getValue();
            String formattedValue = "";
            if (value != null) {
                formattedValue = NumberFormatter.formatNumber(this.entityService, (String)entry.getKey(), value);
            }

            LOG.trace("sum for {} is {}", entry.getKey(), formattedValue);
            formattedSums.put(entry.getKey(), formattedValue);
        }

        return formattedSums;
    }

    private String makeHyperLink(Issue issue, String displayValue) {
        String baseUrl = TemplateUtils.getAppilcationBaseURL();
        String link = baseUrl + "/browse/" + issue.getKey();
        return String.format("<a href=\"%s\">%s</a>", link, StringEscapeUtils.escapeHtml4(displayValue));
    }

    public String getIssueDisplayValue(IssuePickerConfig config, Issue issue, boolean forIndexing) {
        ApplicationUser user;
        if (forIndexing) {
            user = QueryUtil.getUserForIndexing(config.getJqlUser());
        } else {
            user = QueryUtil.getUser(config.getJqlUser());
        }

        try {
            String userKey = user == null ? "null" : user.getKey();
            String key = userKey + " -- " + config.getFieldConfigId() + " -- " + issue.getKey();
            Optional<String> result = (Optional)cache.get(key, () -> {
                LOG.trace("cache miss; calculating issue display value (user {}, issue {})", userKey, issue.getKey());
                String displayValue = this.getNewIssueDisplayValue(config, issue, user, forIndexing);
                return Optional.ofNullable(displayValue);
            });
            return result.isPresent() ? (String)result.get() : " NO DISPLAY VALUE (Issue: " + issue.getKey() + ")";
        } catch (ExecutionException var8) {
            LOG.error("could not get value from cache", var8);
            return null;
        }
    }

    private String getNewIssueDisplayValue(IssuePickerConfig config, Issue issue, ApplicationUser user, boolean forIndexing) {
        String fieldId = config.getDisplayAttributeFieldId();
        String customFormat = config.getCustomFormat();
        boolean showKeyOnly = StringUtils.isBlank(fieldId) && StringUtils.isBlank(customFormat) || !forIndexing && !this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
        if (!config.getShowIssueKey() && !showKeyOnly) {
            return this.getDisplayValue(issue, fieldId, customFormat);
        } else {
            return showKeyOnly ? issue.getKey() : issue.getKey() + ": " + this.getDisplayValue(issue, fieldId, customFormat);
        }
    }

    private String getDisplayValue(Issue issue, String fieldId, String customFormat) {
        return StringUtils.isNotBlank(customFormat) ? TemplateUtils.replaceVariables(customFormat, issue) : Fields.getStringFieldValue(this.customFieldManager, issue, fieldId);
    }

    @Nullable
    private Map<String, FieldValue> getFields(IssuePickerConfig config, Issue issue) {
        if (config.getSelectionMode() == SelectionMode.SINGLE) {
            return null;
        } else {
            List<String> fieldIds = config.getFieldsToCopy();
            Map<String, FieldValue> fields = new HashMap(fieldIds.size());
            Iterator var5 = fieldIds.iterator();

            while(var5.hasNext()) {
                String fieldId = (String)var5.next();
                if (StringUtils.startsWith(fieldId, "customfield")) {
                    fields.put(fieldId, new FieldValue(this.getCustomFieldHtml(issue, fieldId), (String)null));
                } else {
                    fields.put(fieldId, new FieldValue(this.getBasicFieldHtml(issue, fieldId), this.getBasicFieldRaw(issue, fieldId)));
                }
            }

            return fields;
        }
    }

    private String getBasicFieldRaw(Issue issue, String fieldId) {
        LOG.debug("non-navigable field {}", fieldId);
        if (!BasicField.forName(fieldId).isRawSort()) {
            return null;
        } else {
            String value = BasicField.getRawValue(issue, fieldId);
            String string = value == null ? "" : value;
            return HtmlEscaper.escapeAll(string, false);
        }
    }

    private String getBasicFieldHtml(Issue issue, String fieldId) {
        Field field = this.fieldManager.getField(fieldId);
        if (field instanceof NavigableField) {
            FieldLayoutItem fieldLayoutItem = this.fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(fieldId);
            return ((NavigableField)field).getColumnViewHtml(fieldLayoutItem, Collections.emptyMap(), issue);
        } else {
            LOG.debug("non-navigable field {}", fieldId);
            Object value = BasicField.getValue(issue, fieldId);
            String string = value == null ? "" : value.toString();
            return HtmlEscaper.escapeAll(string, false);
        }
    }

    private String getCustomFieldHtml(Issue issue, String fieldId) {
        CustomField field = this.customFieldManager.getCustomFieldObject(fieldId);
        if (field.getCustomFieldType() instanceof NumberCFType) {
            Double value = (Double)issue.getCustomFieldValue(field);
            return value == null ? "" : NumberFormatter.formatNumber(this.entityService, fieldId, value);
        } else {
            FieldLayoutItem fieldLayoutItem = this.fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(field);
            return field.getColumnViewHtml(fieldLayoutItem, Collections.emptyMap(), issue);
        }
    }

    static {
        cache = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();
    }

    public static class CwxIpInfo {
        private final String selectedIssueKeys;
        private final List<IssueInfo> issueInfos;
        private final String displayFieldName;
        private final String jqlLink;
        private final List<FieldInfo> fieldInfos;
        private final Map<String, String> sumsByFieldId;

        public CwxIpInfo(String selectedIssueKeys, List<IssueInfo> issueInfos, String displayFieldName, String jqlLink, List<FieldInfo> fieldInfos, Map<String, String> sumsByFieldId) {
            this.selectedIssueKeys = selectedIssueKeys;
            this.issueInfos = issueInfos;
            this.displayFieldName = displayFieldName;
            this.jqlLink = jqlLink;
            this.fieldInfos = fieldInfos;
            this.sumsByFieldId = sumsByFieldId;
        }

        public String getSelectedIssueKeys() {
            return this.selectedIssueKeys;
        }

        public List<IssueInfo> getIssueInfos() {
            return this.issueInfos;
        }

        public String getDisplayFieldName() {
            return this.displayFieldName;
        }

        public String getJqlLink() {
            return this.jqlLink;
        }

        public List<FieldInfo> getFieldInfos() {
            return this.fieldInfos;
        }

        public Map<String, String> getSumsByFieldId() {
            return this.sumsByFieldId;
        }
    }

    public static class DisplayNameComparator implements Comparator<IssueInfo> {
        private final Locale locale;

        private DisplayNameComparator(Locale locale) {
            this.locale = locale;
        }

        private static DisplayNameComparator getInstance(ApplicationUser user) {
            Locale locale = ComponentAccessor.getLocaleManager().getLocaleFor(user);
            return new DisplayNameComparator(locale);
        }

        public int compare(IssueInfo issueInfo1, IssueInfo issueInfo2) {
            if (issueInfo1 == null) {
                return issueInfo2 != null ? -1 : 0;
            } else if (issueInfo2 == null) {
                return 1;
            } else {
                String displayValue1 = issueInfo1.getDisplayName();
                String displayValue2 = issueInfo2.getDisplayName();
                if (displayValue1 == null) {
                    return displayValue2 != null ? -1 : 0;
                } else if (displayValue2 == null) {
                    return 1;
                } else if (this.locale != null) {
                    Collator collator = Collator.getInstance(this.locale);
                    return collator.compare(displayValue1, displayValue2);
                } else {
                    return displayValue1.compareTo(displayValue2);
                }
            }
        }
    }

    public static class WithKeyComparator<T extends WithKey> implements Comparator<T> {
        public WithKeyComparator() {
        }

        public int compare(T o1, T o2) {
            return KeyComparator.COMPARATOR.compare(o1.getKey(), o2.getKey());
        }
    }

    public static class FieldComparator implements Comparator<FieldInfo> {
        public FieldComparator() {
        }

        public int compare(FieldInfo o1, FieldInfo o2) {
            int result = o1.getFieldName().compareToIgnoreCase(o2.getFieldName());
            return result != 0 ? result : o1.getFieldId().compareTo(o2.getFieldId());
        }
    }

    public static class IssueInfo implements WithKey {
        private final String key;
        private final String displayName;
        private final String hyperLink;
        private final Map<String, FieldValue> fieldValues;
        private final boolean selected;

        public IssueInfo(String key, String displayName, String hyperLink, Map<String, FieldValue> fieldValues, boolean selected) {
            this.key = key;
            this.displayName = displayName;
            this.hyperLink = hyperLink;
            this.fieldValues = fieldValues;
            this.selected = selected;
        }

        public String getKey() {
            return this.key;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getHyperLink() {
            return this.hyperLink;
        }

        public Map<String, FieldValue> getFieldValues() {
            return this.fieldValues;
        }

        public boolean isSelected() {
            return this.selected;
        }
    }

    public static class FieldValue {
        private final String htmlValue;
        private final String rawValue;

        public FieldValue(String htmlValue, String rawValue) {
            this.htmlValue = htmlValue;
            this.rawValue = rawValue;
        }

        public String getHtmlValue() {
            return this.htmlValue;
        }

        public String getRawValue() {
            return this.rawValue;
        }
    }

    public static class FieldInfo {
        private final String fieldId;
        private final String fieldName;
        private final boolean rightAlign;
        private final boolean rawSort;

        public FieldInfo(String fieldId, String fieldName, boolean rightAlign, boolean rawSort) {
            this.fieldId = fieldId;
            this.fieldName = fieldName;
            this.rightAlign = rightAlign;
            this.rawSort = rawSort;
        }

        public String getFieldId() {
            return this.fieldId;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public boolean isRightAlign() {
            return this.rightAlign;
        }

        public boolean isRawSort() {
            return this.rawSort;
        }
    }
}
