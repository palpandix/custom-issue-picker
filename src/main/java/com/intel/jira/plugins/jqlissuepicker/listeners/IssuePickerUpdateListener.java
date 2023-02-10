package com.intel.jira.plugins.jqlissuepicker.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.google.common.collect.ImmutableSet;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.data.LinkMode;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;
import com.intel.jira.plugins.jqlissuepicker.util.Fields;
import com.intel.jira.plugins.jqlissuepicker.util.QueryUtil;
import com.intel.jira.plugins.jqlissuepicker.util.TemplateUtils;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldPair;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuePickerUpdateListener implements LifecycleAware {
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerUpdateListener.class);
    private final EventPublisher eventPublisher;
    private final CustomFieldManager customFieldManager;
    private final IssueService issueService;
    private final IssueIndexingService indexingService;
    private final IssueLinkManager issueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final ProjectManager projectManager;
    private final IssueTypeManager issueTypeManager;
    private final SearchService searchService;
    private final EntityService entityService;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public IssuePickerUpdateListener(EventPublisher eventPublisher, CustomFieldManager customFieldManager, IssueManager issueManager, IssueIndexingService indexingService, IssueLinkManager issueLinkManager, IssueLinkTypeManager issueLinkTypeManager, ProjectManager projectManager, IssueTypeManager issueTypeManager, SearchService searchService, EntityService entityService, IssueService issueService, JiraAuthenticationContext jiraAuthenticationContext) {
        this.eventPublisher = eventPublisher;
        this.customFieldManager = customFieldManager;
        this.indexingService = indexingService;
        this.issueLinkManager = issueLinkManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.projectManager = projectManager;
        this.issueTypeManager = issueTypeManager;
        this.searchService = searchService;
        this.entityService = entityService;
        this.issueService = issueService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public void onStart() {
        LOG.debug("registering listener");
        this.eventPublisher.register(this);
    }

    public void onStop() {
        LOG.debug("unregistering listener");
        this.eventPublisher.unregister(this);
    }

    @EventListener
    public void onEvent(IssueEvent event) {
        Set<String> changedFields = this.getChangedFields(event);
        if (event.getEventTypeId().equals(EventType.ISSUE_CREATED_ID) || CollectionUtils.isNotEmpty(changedFields)) {
            ApplicationUser user = this.jiraAuthenticationContext.getLoggedInUser();
            IssueService.IssueResult issueResult = this.issueService.getIssue(user, event.getIssue().getId());
            if (!issueResult.isValid()) {
                LOG.error("error getting issue with id {} and key {}", event.getIssue().getId(), event.getIssue().getKey());
                return;
            }

            MutableIssue issue = issueResult.getIssue();
            Map<String, CustomField> fieldsByName = this.getIssuePickerFields(issue);
            Iterator var7 = fieldsByName.entrySet().iterator();

            while(var7.hasNext()) {
                Map.Entry<String, CustomField> e = (Map.Entry)var7.next();
                this.processFieldChange(event, issue, changedFields, (String)e.getKey(), (CustomField)e.getValue());
            }
        }

    }

    private void processFieldChange(IssueEvent event, MutableIssue issue, Set<String> changedFields, String fieldName, CustomField field) {
        boolean isCreatedEvent = event.getEventTypeId().equals(EventType.ISSUE_CREATED_ID);
        if (isCreatedEvent || changedFields.contains(fieldName)) {
            CustomFieldType<?, ?> type = field.getCustomFieldType();
            if (type instanceof IssuePickerCFType) {
                FieldConfig fieldConfig = field.getRelevantConfig(issue);
                IssuePickerConfig config = fieldConfig == null ? null : this.entityService.loadIssuePickerConfig(fieldConfig.getId());
                if (config != null) {
                    this.updateIssuesFromIssuePicker(event.getUser(), field, issue, config, isCreatedEvent);
                }
            }
        }

    }

    private Map<String, CustomField> getIssuePickerFields(Issue issue) {
        List<CustomField> fields = this.customFieldManager.getCustomFieldObjects(issue);
        Map<String, CustomField> fieldsByName = new HashMap();
        Iterator var4 = fields.iterator();

        while(var4.hasNext()) {
            CustomField field = (CustomField)var4.next();
            CustomFieldType<?, ?> type = field.getCustomFieldType();
            if (type instanceof IssuePickerCFType) {
                fieldsByName.put(field.getName(), field);
            }
        }

        return fieldsByName;
    }

    private Set<String> getChangedFields(IssueEvent event) {
        Set<String> changedFields = new HashSet();
        GenericValue changeLog = event.getChangeLog();
        if (changeLog != null) {
            try {
                List<GenericValue> changeItems = changeLog.getRelated("ChildChangeItem");
                if (CollectionUtils.isNotEmpty(changeItems)) {
                    Iterator var5 = changeItems.iterator();

                    while(var5.hasNext()) {
                        GenericValue changeItem = (GenericValue)var5.next();
                        String fieldName = changeItem.getString("field");
                        LOG.debug("changed field: {}", fieldName);
                        changedFields.add(fieldName);
                    }
                }
            } catch (GenericEntityException var8) {
                LOG.error("could not get change items", var8);
            }
        }

        return changedFields;
    }

    private void updateIssuesFromIssuePicker(ApplicationUser user, CustomField field, MutableIssue issue, IssuePickerConfig config, boolean isCreatedEvent) {
        LOG.debug("updating issue {} for field {}", issue.getKey(), field.getName());
        boolean forceLinkIssue = config.getLinkMode() == LinkMode.LINK_ONLY;
        boolean shouldCopyFields = config.getSelectionMode() == SelectionMode.SINGLE;
        boolean autoselectValue = isCreatedEvent && config.getPresetValue();
        Collection<String> targetIssueKeys = this.getTargetIssueKeys(issue, field);
        String newIssueKey = this.createNewIssue(user, issue, field, config, targetIssueKeys);
        IssueInputParameters issueInputParameters = this.issueService.newIssueInputParameters();
        if (newIssueKey != null) {
            LOG.debug("replacing new issue key in field {} in {}", field.getName(), issue.getKey());
            targetIssueKeys = this.replaceNewIssueKey(targetIssueKeys, newIssueKey);
            this.updateCustomField(issueInputParameters, field, StringUtils.join(targetIssueKeys, ','));
            this.updateAndReindex(issueInputParameters, issue.getId(), user);
        }

        if (autoselectValue && targetIssueKeys.isEmpty()) {
            targetIssueKeys = this.autoselectSingleValue(user, field, issueInputParameters, issue, config);
        }

        Iterator var12 = targetIssueKeys.iterator();

        while(true) {
            while(var12.hasNext()) {
                String targetIssueKey = (String)var12.next();
                ApplicationUser jqlUser = QueryUtil.getUser(config.getJqlUser());
                IssueService.IssueResult issueResult = this.issueService.getIssue(jqlUser, targetIssueKey);
                Issue targetIssue = issueResult.getIssue();
                if (targetIssue == null) {
                    LOG.warn("target issue with key {} no longer exists", targetIssueKey);
                } else {
                    if (shouldCopyFields && (CollectionUtils.isNotEmpty(config.getFieldsToCopy()) || !StringUtils.isBlank(config.getCopyFieldMapping()) && !"EMPTY".equals(config.getCopyFieldMapping()))) {
                        this.copyFields(config, issueInputParameters, issue, targetIssue);
                    }

                    if (forceLinkIssue) {
                        this.linkIssue(user, config, issue, targetIssue);
                    }
                }
            }

            if (!forceLinkIssue) {
                this.syncIssueLinks(user, config, issue, targetIssueKeys, isCreatedEvent);
            } else if (CollectionUtils.isNotEmpty(targetIssueKeys)) {
                this.updateCustomField(issueInputParameters, field, (Object)null);
                this.updateAndReindex(issueInputParameters, issue.getId(), user);
            }

            return;
        }
    }

    private Collection<String> autoselectSingleValue(ApplicationUser user, CustomField field, IssueInputParameters issueInputParameters, MutableIssue issue, IssuePickerConfig config) {
        String jql = TemplateUtils.replaceVariables(config.getJql(), issue);
        LOG.trace("{}: querying issues to autoselect", field.getName());
        List<Issue> allIssues = QueryUtil.queryIssues(this.searchService, jql, issue, config.getJqlUser());
        if (allIssues.size() == 1) {
            String singleIssueKey = ((Issue)allIssues.get(0)).getKey();
            LOG.debug("autoselecting single issue {}", singleIssueKey);
            this.updateCustomField(issueInputParameters, field, singleIssueKey);
            this.updateAndReindex(issueInputParameters, issue.getId(), user);
            return Collections.singletonList(singleIssueKey);
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<String> replaceNewIssueKey(Collection<String> keys, String newIssueKey) {
        List<String> list = new ArrayList(keys.size());
        Iterator var4 = keys.iterator();

        while(var4.hasNext()) {
            String issueKey = (String)var4.next();
            if (StringUtils.startsWith(issueKey, "new-")) {
                list.add(newIssueKey);
            } else {
                list.add(issueKey);
            }
        }

        return list;
    }

    @Nullable
    private String createNewIssue(ApplicationUser user, MutableIssue issue, CustomField field, IssuePickerConfig config, Collection<String> targetIssueKeys) {
        if (config.getCreateNewValue()) {
            String summary = this.getNewIssueSummary(targetIssueKeys);
            if (StringUtils.isNotBlank(summary)) {
                if (StringUtils.isNotBlank(config.getNewIssueType()) && (config.getCurrentProject() || StringUtils.isNotBlank(config.getNewIssueProject()))) {
                    Project project;
                    if (config.getCurrentProject()) {
                        project = this.projectManager.getProjectObj(issue.getProjectId());
                    } else {
                        project = this.projectManager.getProjectObj(NumberUtils.toLong(config.getNewIssueProject()));
                    }

                    if (project == null) {
                        LOG.error("error getting project with id {}", config.getNewIssueProject());
                        return null;
                    }

                    IssueType issueType = this.issueTypeManager.getIssueType(config.getNewIssueType());
                    if (issueType == null) {
                        LOG.error("error getting issue type with id {}", config.getNewIssueType());
                        return null;
                    }

                    LOG.info("creating new {} in project {} with summary {}", new Object[]{issueType.getName(), project.getKey(), summary});
                    IssueInputParameters issueInputParameters = this.issueService.newIssueInputParameters();
                    issueInputParameters.setProjectId(project.getId());
                    issueInputParameters.setIssueTypeId(issueType.getId());
                    issueInputParameters.setSummary(summary);
                    issueInputParameters.setReporterId(user.getKey());
                    this.copyFields(issue, issueInputParameters, "new-issue", config.getFieldsToInit(), config.getInitFieldMapping());
                    IssueService.CreateValidationResult validateCreate = this.issueService.validateCreate(user, issueInputParameters);
                    if (!validateCreate.isValid()) {
                        this.logErrorMessages("could not create new issue", validateCreate.getErrorCollection());
                        return null;
                    }

                    IssueService.IssueResult issueResult = this.issueService.create(user, validateCreate);
                    Issue resultIssue = issueResult.getIssue();
                    LOG.debug("created new issue with key {}", resultIssue.getKey());
                    return resultIssue.getKey();
                }

                LOG.error("issue {}, field {}: cannot create new issue without project or issue type", issue.getKey(), field.getName());
            }
        }

        return null;
    }

    private void logErrorMessages(String msgPrefix, ErrorCollection errorCollection) {
        Map<String, String> errors = errorCollection.getErrors();
        StringBuilder sb = new StringBuilder();
        sb.append(msgPrefix);
        sb.append("; \n");
        Iterator var5 = errors.entrySet().iterator();

        while(var5.hasNext()) {
            Map.Entry<String, String> error = (Map.Entry)var5.next();
            sb.append((String)error.getKey());
            sb.append(" : ");
            sb.append((String)error.getValue());
            sb.append("; \n");
        }

        LOG.error(sb.toString());
    }

    @Nullable
    private String getNewIssueSummary(Collection<String> targetIssueKeys) {
        Iterator var2 = targetIssueKeys.iterator();

        while(true) {
            String key;
            do {
                if (!var2.hasNext()) {
                    return null;
                }

                key = (String)var2.next();
            } while(!StringUtils.startsWith(key, "new-"));

            String suffix = key.substring("new-".length());

            try {
                return URLDecoder.decode(suffix, "UTF-8");
            } catch (UnsupportedEncodingException var6) {
                LOG.error("could not decode new issue summary", var6);
            }
        }
    }

    @Nonnull
    private Collection<String> getTargetIssueKeys(MutableIssue issue, CustomField field) {
        Object value = issue.getCustomFieldValue(field);
        if (value instanceof Collection) {
            return IssuePickerCFType.getIssueKeys((Collection)value);
        } else if (value instanceof String) {
            return IssuePickerCFType.getIssueKeys(Collections.singletonList((String)value));
        } else {
            if (value != null) {
                LOG.error("unexpected issue custom field value of type {}", value.getClass());
            }

            return Collections.emptyList();
        }
    }

    private void syncIssueLinks(ApplicationUser user, IssuePickerConfig config, MutableIssue issue, @Nonnull Collection<String> targetIssueKeys, boolean isCreatedEvent) {
        if (config.getLinkTypeId() == null) {
            LOG.debug("no linking configured, not syncing issue links for issue {}", issue.getKey());
        } else if (isCreatedEvent && targetIssueKeys.isEmpty()) {
            LOG.debug("not syncing links for empty field in newly created issue {}", issue.getKey());
        } else {
            IssueLinkType linkType = this.issueLinkTypeManager.getIssueLinkType(config.getLinkTypeId());
            Long issueId = issue.getId();
            Long linkTypeId = linkType.getId();
            boolean outward = config.getOutward();
            Iterator var10 = targetIssueKeys.iterator();

            while(var10.hasNext()) {
                String targetIssueKey = (String)var10.next();
                IssueService.IssueResult issueResult = this.issueService.getIssue(user, targetIssueKey);
                Issue targetIssue = issueResult.getIssue();
                if (targetIssue != null && !this.hasLink(issueId, targetIssue.getId(), outward, linkTypeId)) {
                    this.linkIssue(user, config, issue, targetIssue);
                }
            }

            Set<String> newIssueKeys = ImmutableSet.copyOf(targetIssueKeys);
            List<IssueLink> links = this.getIssueLinks(issueId, linkTypeId, outward);
            Iterator var17 = links.iterator();

            while(var17.hasNext()) {
                IssueLink link = (IssueLink)var17.next();
                Issue targetIssue = outward ? link.getDestinationObject() : link.getSourceObject();
                if (!newIssueKeys.contains(targetIssue.getKey())) {
                    LOG.info("removing issue link from {} to {} using link type {}", new Object[]{issue.getKey(), targetIssue.getKey(), outward ? linkType.getOutward() : linkType.getInward()});
                    this.issueLinkManager.removeIssueLink(link, user);
                }
            }

        }
    }

    private List<IssueLink> getIssueLinks(Long issueId, Long linkTypeId, boolean outward) {
        List<IssueLink> links = outward ? this.issueLinkManager.getOutwardLinks(issueId) : this.issueLinkManager.getInwardLinks(issueId);
        List<IssueLink> result = new ArrayList();
        Iterator var6 = links.iterator();

        while(var6.hasNext()) {
            IssueLink link = (IssueLink)var6.next();
            if (link.getLinkTypeId().equals(linkTypeId)) {
                result.add(link);
            }
        }

        return result;
    }

    private boolean hasLink(Long sourceIssueId, Long targetIssueId, boolean outward, Long linkTypeId) {
        IssueLink link = outward ? this.issueLinkManager.getIssueLink(sourceIssueId, targetIssueId, linkTypeId) : this.issueLinkManager.getIssueLink(targetIssueId, sourceIssueId, linkTypeId);
        return link != null;
    }

    private void linkIssue(ApplicationUser user, IssuePickerConfig config, MutableIssue issue, Issue targetIssue) {
        if (config.getLinkTypeId() == null) {
            LOG.debug("no linking configured, not syncing issue links for issue {}", issue.getKey());
        } else {
            IssueLinkType linkType = this.issueLinkTypeManager.getIssueLinkType(config.getLinkTypeId());

            try {
                LOG.info("linking issue {} to {} using link type {}", new Object[]{issue.getKey(), targetIssue.getKey(), config.getOutward() ? linkType.getOutward() : linkType.getInward()});
                if (config.getOutward()) {
                    this.issueLinkManager.createIssueLink(issue.getId(), targetIssue.getId(), linkType.getId(), (Long)null, user);
                } else {
                    this.issueLinkManager.createIssueLink(targetIssue.getId(), issue.getId(), linkType.getId(), (Long)null, user);
                }
            } catch (CreateException var7) {
                LOG.error("could not link issues", var7);
            }

        }
    }

    private void copyFields(IssuePickerConfig config, IssueInputParameters issueInputParameters, MutableIssue issue, Issue targetIssue) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        this.copyFields(targetIssue, issueInputParameters, issue.getKey(), config.getFieldsToCopy(), config.getCopyFieldMapping());
        this.updateAndReindex(issueInputParameters, issue.getId(), user);
    }

    private void copyFields(Issue fromIssue, IssueInputParameters toIssue, String toIssueKey, List<String> fields, String mappingId) {
        if (StringUtils.isNotBlank(mappingId) && !"EMPTY".equals(mappingId)) {
            LOG.debug("copying fields from {} to {} with field mapping {}", new Object[]{fromIssue.getKey(), toIssueKey, mappingId});
            List<FieldPair> listFieldPairMapping = this.entityService.listFieldPairMapping(Integer.parseInt(mappingId));
            Iterator var12 = listFieldPairMapping.iterator();

            while(var12.hasNext()) {
                FieldPair fieldPair = (FieldPair)var12.next();

                try {
                    Object fromFieldValue = this.getFromValue(fromIssue, fieldPair.getFromField());
                    this.setToFieldValue(toIssue, fieldPair.getToField(), fromFieldValue);
                } catch (Exception var10) {
                    LOG.error("Exception thrown when copying {} field from {} to {} field in {}  by field mapping {}", new Object[]{fieldPair.getFromField(), fromIssue.getKey(), fieldPair.getToField(), toIssueKey, mappingId, var10});
                }
            }
        } else {
            LOG.debug("copying {} fields from {} to {}", new Object[]{fields.size(), fromIssue.getKey(), toIssueKey});
            Iterator var6 = fields.iterator();

            while(var6.hasNext()) {
                String fieldName = (String)var6.next();
                Object fromFieldValue = this.getFromValue(fromIssue, fieldName);
                this.setToFieldValue(toIssue, fieldName, fromFieldValue);
            }
        }

    }

    private void setToFieldValue(IssueInputParameters toIssue, String toField, Object fromFieldValue) {
        if (StringUtils.startsWith(toField, "customfield")) {
            CustomField field = this.customFieldManager.getCustomFieldObject(toField);
            this.updateCustomField(toIssue, field, fromFieldValue);
        } else {
            this.updateBasicField(toIssue, toField, fromFieldValue);
        }

    }

    private Object getFromValue(Issue fromIssue, String fromField) {
        if (StringUtils.startsWith(fromField, "customfield")) {
            CustomField field = this.customFieldManager.getCustomFieldObject(fromField);
            field.hasValue(fromIssue);
            field.getValue(fromIssue);
            field.getValueFromIssue(fromIssue);
            fromIssue.getCustomFieldValue(field);
            return fromIssue.getCustomFieldValue(field);
        } else {
            return this.getBasicFieldValue(fromIssue, fromField);
        }
    }

    private Object getBasicFieldValue(Issue fromIssue, String fieldName) {
        LOG.trace("updating basic field {}", fieldName);
        Fields.BasicField basicField = Fields.BasicField.forName(fieldName);
        if (basicField == null) {
            LOG.error("unsupported basic field {}", fieldName);
            return null;
        } else {
            switch (basicField) {
                case SUMMARY:
                    return fromIssue.getSummary();
                case DESCRIPTION:
                    return fromIssue.getDescription();
                case PRIORITY:
                    return fromIssue.getPriority();
                case LABELS:
                    return fromIssue.getLabels();
                case AFFECTED_VERSIONS:
                    return fromIssue.getAffectedVersions();
                case FIX_VERSIONS:
                    return fromIssue.getFixVersions();
                case DUE_DATE:
                    return fromIssue.getDueDate();
                case REPORTER:
                    return fromIssue.getReporter();
                case ASSIGNEE:
                    return fromIssue.getAssignee();
                case STATUS:
                    return fromIssue.getStatus();
                case COMPONENTS:
                    return fromIssue.getComponents();
                case ENVIRONMENT:
                    return fromIssue.getEnvironment();
                case ISSUE_TYPE:
                    return fromIssue.getIssueType();
                case PROJECT:
                    return fromIssue.getProjectObject();
                case RESOLUTION:
                    return fromIssue.getResolution();
                default:
                    LOG.error("unsupported basic field {}", basicField.name());
                    return null;
            }
        }
    }

    private void updateBasicField(IssueInputParameters toIssue, String fieldName, Object value) {
        LOG.trace("updating basic field {}", fieldName);
        Fields.BasicField basicField = Fields.BasicField.forName(fieldName);
        if (basicField == null) {
            LOG.error("unsupported basic field {}", fieldName);
        } else {
            switch (basicField) {
                case SUMMARY:
                    toIssue.setSummary((String)value);
                    break;
                case DESCRIPTION:
                    toIssue.setDescription((String)value);
                    break;
                case PRIORITY:
                    Priority priority = (Priority)value;
                    String priorityId = priority != null ? priority.getId() : null;
                    toIssue.setPriorityId(priorityId);
                    break;
                case LABELS:
                    Set<Label> labels = (Set)value;
                    String[] labelsArray = (String[])labels.stream().map((l) -> {
                        return l.getLabel();
                    }).toArray((size) -> {
                        return new String[size];
                    });
                    toIssue.addCustomFieldValue("labels", labelsArray);
                    break;
                case AFFECTED_VERSIONS:
                    Collection<Version> affectedVersions = (Collection)value;
                    Long[] affectedVersionIds = (Long[])((List)affectedVersions.stream().map(Version::getId).collect(Collectors.toList())).toArray(new Long[affectedVersions.size()]);
                    toIssue.setAffectedVersionIds(affectedVersionIds);
                    break;
                case FIX_VERSIONS:
                    Collection<Version> fixVersions = (Collection)value;
                    Long[] fixVersionIds = (Long[])((List)fixVersions.stream().map(Version::getId).collect(Collectors.toList())).toArray(new Long[fixVersions.size()]);
                    toIssue.setFixVersionIds(fixVersionIds);
                    break;
                case DUE_DATE:
                    Timestamp dueDate = (Timestamp)value;
                    DateTimeFormatter dateTimeFormatter = this.getDateTimeFormatter();
                    toIssue.setDueDate(dateTimeFormatter.format(dueDate));
                    break;
                case REPORTER:
                    ApplicationUser reporter = (ApplicationUser)value;
                    String reporterName = reporter != null ? reporter.getName() : null;
                    toIssue.setReporterId(reporterName);
                    break;
                case ASSIGNEE:
                    ApplicationUser assignee = (ApplicationUser)value;
                    String assigneeName = assignee != null ? assignee.getName() : null;
                    toIssue.setAssigneeId(assigneeName);
                    break;
                case STATUS:
                case COMPONENTS:
                case ENVIRONMENT:
                case ISSUE_TYPE:
                case PROJECT:
                case RESOLUTION:
                    LOG.error("field {} cannot be copied", basicField.name());
                    break;
                default:
                    LOG.error("unsupported basic field {}", basicField.name());
            }

        }
    }

    private DateTimeFormatter getDateTimeFormatter() {
        DateTimeFormatterFactory dateTimeFormatterFactory = (DateTimeFormatterFactory)ComponentAccessor.getComponent(DateTimeFormatterFactory.class);
        return dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE);
    }

    private void updateCustomField(IssueInputParameters issue, CustomField field, Object value) {
        LOG.trace("updating field {} with value {}", field.getName(), value);
        CustomFieldType customFieldType = field.getCustomFieldType();
        if (value instanceof Collection) {
            Collection<?> valueList = (Collection)value;
            Stream var10000 = valueList.stream();
            customFieldType.getClass();
            String[] args = (String[])var10000.map(customFieldType::getStringFromSingularObject).toArray((size) -> {
                return new String[size];
            });
            issue.addCustomFieldValue(field.getId(), args);
        } else {
            String stringFromSingularObject = customFieldType.getStringFromSingularObject(value);
            issue.addCustomFieldValue(field.getId(), new String[]{stringFromSingularObject});
        }

    }

    private void updateAndReindex(IssueInputParameters issueInputParameters, Long issueId, ApplicationUser user) {
        try {
            IssueService.UpdateValidationResult validateUpdate = this.issueService.validateUpdate(user, issueId, issueInputParameters);
            if (!validateUpdate.isValid()) {
                this.logErrorMessages("could not update and reindex", validateUpdate.getErrorCollection());
                return;
            }

            IssueService.IssueResult updateIssueResult = this.issueService.update(user, validateUpdate);
            Issue newIssue = updateIssueResult.getIssue();
            this.indexingService.reIndex(newIssue);
        } catch (IndexException var7) {
            LOG.error("could not index issue", var7);
        }

    }
}
