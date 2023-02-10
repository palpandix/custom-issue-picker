package com.intel.jira.plugins.jqlissuepicker.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerVelocityProvider;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;
import com.intel.jira.plugins.jqlissuepicker.servicedesk.ServiceDeskUtils;
import com.intel.jira.plugins.jqlissuepicker.util.QueryUtil;
import com.intel.jira.plugins.jqlissuepicker.util.TemplateUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/issuepicker")
@Consumes({"application/json"})
@Produces({"application/json"})
public class IssuePickerRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerRestResource.class);
    private static final String COULD_NOT_DETERMINE_PROJECT_OR_ISSUE_TYPE_ID = "could not determine project or issue type id";
    private static final int DEFAULT_MAX_SEARCH_RESULTS = 20;
    private final ServiceDeskUtils serviceDeskUtils;
    private final CustomFieldManager customFieldManager;
    private final IssueManager issueManager;
    private final FieldConfigSchemeManager fieldConfigSchemeManager;
    private final SearchService searchService;
    private final ProjectManager projectManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final EntityService entityService;
    private final IssuePickerVelocityProvider velocityProvider;

    public IssuePickerRestResource(ServiceDeskUtils serviceDeskUtils, CustomFieldManager customFieldManager, IssueManager issueManager, FieldConfigSchemeManager fieldConfigSchemeManager, SearchService searchService, ProjectManager projectManager, JiraAuthenticationContext jiraAuthenticationContext, EntityService entityService, IssuePickerVelocityProvider velocityProvider) {
        this.serviceDeskUtils = serviceDeskUtils;
        this.customFieldManager = customFieldManager;
        this.issueManager = issueManager;
        this.fieldConfigSchemeManager = fieldConfigSchemeManager;
        this.searchService = searchService;
        this.projectManager = projectManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.entityService = entityService;
        this.velocityProvider = velocityProvider;
    }

    @GET
    @Path("/search")
    public Response search(@QueryParam("customFieldId") String customFieldId, @QueryParam("issue") String issueId, @QueryParam("project") String projectId, @QueryParam("issueType") String issueTypeId, @QueryParam("cfConfigId") String cfConfigId, @QueryParam("requestType") String requestTypeId, @QueryParam("query") String query) {
        Issue currentIssue = null;
        IssueContext issueContext = null;
        Long fieldConfigId = null;
        CustomField field = this.customFieldManager.getCustomFieldObject(customFieldId);
        if (StringUtils.isNumeric(issueId)) {
            issueContext = currentIssue = this.issueManager.getIssueObject(NumberUtils.toLong(issueId));
        } else if (StringUtils.isNotBlank(requestTypeId)) {
            ApplicationUser user = this.jiraAuthenticationContext.getLoggedInUser();
            Pair<Long, Long> pair = this.serviceDeskUtils.getProjectAndIssueTypeId(user, NumberUtils.toInt(requestTypeId));
            if (pair == null) {
                LOG.error("could not determine project or issue type id for request type {}", requestTypeId);
                return Response.status(Status.BAD_REQUEST).entity("could not determine project or issue type id").build();
            }

            issueContext = new IssueContextImpl((Long)pair.getLeft(), String.valueOf(pair.getRight()));
        } else if (StringUtils.isNoneBlank(new CharSequence[]{projectId, issueTypeId})) {
            issueContext = new IssueContextImpl(NumberUtils.toLong(projectId), issueTypeId);
        } else {
            if (!StringUtils.isNotBlank(cfConfigId)) {
                return Response.status(Status.BAD_REQUEST).entity("missing issue or project and issue type or request type").build();
            }

            fieldConfigId = NumberUtils.toLong(cfConfigId);
        }

        if (issueContext != null) {
            FieldConfig fieldConfig = field.getRelevantConfig((IssueContext)issueContext);
            if (fieldConfig != null) {
                fieldConfigId = fieldConfig.getId();
            }
        }

        IssuePickerConfig config = fieldConfigId == null ? null : this.entityService.loadIssuePickerConfig(fieldConfigId);
        if (config != null && StringUtils.isNotBlank(config.getJql())) {
            String jql = TemplateUtils.replaceVariables(config.getJql(), (IssueContext)issueContext);
            LOG.trace("{}: {}: querying issues", issueId, field.getName());
            List<Issue> jqlIssues = QueryUtil.queryIssues(this.searchService, jql, currentIssue, config.getJqlUser());
            List<IssueEntry> results = new ArrayList(jqlIssues.size());
            int numResults = 0;
            int maxSearchResults = (Integer)ObjectUtils.defaultIfNull(config.getMaxSearchResults(), 20);
            Iterator var18 = jqlIssues.iterator();

            while(var18.hasNext()) {
                Issue issue = (Issue)var18.next();
                String displayName = this.velocityProvider.getIssueDisplayValue(config, issue, false);
                if (StringUtils.containsIgnoreCase(displayName, query)) {
                    if (numResults < maxSearchResults) {
                        results.add(new IssueEntry(issue.getKey(), displayName));
                    }

                    ++numResults;
                }
            }

            LOG.trace("{} issues match query: {}", results.size(), query);
            return Response.ok(new SearchResult(numResults, results)).build();
        } else {
            return Response.ok((Object)null).build();
        }
    }

    @GET
    @Path("/jsd-viewdata")
    public Response getJsdViewData(@QueryParam("issueKey") String issueKey) {
        ApplicationUser user = this.jiraAuthenticationContext.getLoggedInUser();
        if (StringUtils.isBlank(issueKey)) {
            return Response.status(Status.BAD_REQUEST).entity("Missing issue key").build();
        } else {
            MutableIssue issue = this.issueManager.getIssueObject(issueKey);
            Map<String, String> fieldNames = this.serviceDeskUtils.getIssuePickerFieldNames(user, issue);
            return Response.ok(new IssuePickerModel(this.getConfigurations(fieldNames, issue.getProjectId(), issue.getIssueTypeId(), issue, true, false))).build();
        }
    }

    @GET
    @Path("/resolve-keys")
    public Response resolveKeys(@QueryParam("fieldConfigId") Long fieldConfigId, @QueryParam("keys") String unresolvedKeyString) {
        IssuePickerConfig config = fieldConfigId == null ? null : this.entityService.loadIssuePickerConfig(fieldConfigId);
        List<String> keys = Arrays.asList(StringUtils.split(unresolvedKeyString, ','));
        Stream var10000 = keys.stream();
        IssueManager var10001 = this.issueManager;
        var10001.getClass();
        //List<IssueEntry> entries = (List)var10000.map(var10001::getIssueObject).map((issue) -> {
        /*List<IssueEntry> entries = (List)var10000.map((i)->var10001.getIssueObject(i.toString())).map(((issue) -> {
            return new IssueEntry(issue.getKey(), this.velocityProvider.getIssueDisplayValue(config, issue, false));
        }).collect(Collectors.toList());*/
        List<IssueEntry> entries = (List)var10000.map((i)->var10001.getIssueObject(i.toString())).map((issue) -> {
            return new IssueEntry("test", this.velocityProvider.getIssueDisplayValue(config, (Issue) issue, false));
        }).collect(Collectors.toList());
        return Response.ok(entries).build();
    }

    @GET
    @Path("/configuration")
    public Response getConfiguration(@QueryParam("requestType") String requestTypeId) {
        LOG.trace("loading configurations for request type {}", requestTypeId);
        ApplicationUser user = this.jiraAuthenticationContext.getLoggedInUser();
        Pair<Long, Long> pair = this.serviceDeskUtils.getProjectAndIssueTypeId(user, NumberUtils.toInt(requestTypeId));
        if (pair == null) {
            LOG.error("could not determine project or issue type id for request type {}", requestTypeId);
            return Response.status(Status.BAD_REQUEST).entity("could not determine project or issue type id").build();
        } else {
            Project project = this.projectManager.getProjectObj((Long)pair.getLeft());
            if (project == null) {
                LOG.error("no project with id {}", pair.getLeft());
                return Response.status(Status.BAD_REQUEST).entity("could not determine project or issue type id").build();
            } else {
                Map<String, String> fieldNames = this.serviceDeskUtils.getIssuePickerFieldNames(user, project, NumberUtils.toInt(requestTypeId));
                LOG.debug("found project id {} and issue type id {}", pair.getLeft(), pair.getRight());
                return Response.ok(new IssuePickerModel(this.getConfigurations(fieldNames, (Long)pair.getLeft(), String.valueOf(pair.getRight()), (Issue)null, false, true))).build();
            }
        }
    }

    private Map<String, IssuePickerConfiguration> getConfigurations(Map<String, String> fieldNames, Long projectId, String issueTypeId, Issue currentIssue, boolean jsdViewData, boolean jsdEditData) {
        Map<String, IssuePickerConfiguration> configurations = new HashMap();
        Iterator var8 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(true) {
            CustomField field;
            String serviceDeskFieldName;
            do {
                if (!var8.hasNext()) {
                    return configurations;
                }

                field = (CustomField)var8.next();
                serviceDeskFieldName = (String)fieldNames.get(field.getIdAsLong().toString());
            } while(!(field.getCustomFieldType() instanceof IssuePickerCFType));

            Iterator var11 = this.fieldConfigSchemeManager.getConfigSchemesForField(field).iterator();

            while(var11.hasNext()) {
                FieldConfigScheme scheme = (FieldConfigScheme)var11.next();
                boolean typeMatch = scheme.isAllIssueTypes() || scheme.getAssociatedIssueTypeIds().contains(issueTypeId);
                boolean projMatch = scheme.isAllProjects() || scheme.getAssociatedProjectIds().contains(projectId);
                if (typeMatch && projMatch) {
                    this.addConfiguration(scheme.getOneAndOnlyConfig().getId(), configurations, field, serviceDeskFieldName, currentIssue, jsdViewData, jsdEditData);
                }
            }
        }
    }

    private void addConfiguration(Long fieldConfigId, Map<String, IssuePickerConfiguration> configurations, CustomField field, String serviceDeskFieldName, Issue currentIssue, boolean jsdViewData, boolean jsdEditData) {
        LOG.trace("getting issue picker configuration for field {}: {}", field.getId(), field.getName());
        LOG.trace("field config id: {}", fieldConfigId);
        configurations.put(field.getIdAsLong().toString(), this.getIssuePickerConfig(field, fieldConfigId, serviceDeskFieldName, currentIssue, jsdViewData, jsdEditData));
    }

    private IssuePickerConfiguration getIssuePickerConfig(CustomField field, Long fieldConfigId, String serviceDeskFieldName, Issue currentIssue, boolean jsdViewData, boolean jsdEditData) {
        IssuePickerConfig config = this.entityService.loadIssuePickerConfig(fieldConfigId);
        IssuePickerConfiguration ipConfig = new IssuePickerConfiguration();
        if (jsdViewData) {
            String fieldValue = null;
            if (currentIssue != null) {
                fieldValue = field.getColumnViewHtml((FieldLayoutItem)null, Collections.emptyMap(), currentIssue);
            }

            ipConfig.setServiceDeskFieldName(serviceDeskFieldName);
            ipConfig.setCurrentIssueFieldValue(fieldValue);
        }

        if (jsdEditData && config != null && config.getSelectionMode() != null) {
            ipConfig.setPresetValue(config.getPresetValue());
            ipConfig.setSelectionMode(config.getSelectionMode().name());
        }

        return ipConfig;
    }
}
