package com.intel.jira.plugins.jqlissuepicker.servicedesk;

import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.pocketknife.api.commons.error.AnError;
import com.atlassian.servicedesk.api.ServiceDesk;
import com.atlassian.servicedesk.api.ServiceDeskService;
import com.atlassian.servicedesk.api.field.CustomerRequestCreateMeta;
import com.atlassian.servicedesk.api.field.RequestTypeField;
import com.atlassian.servicedesk.api.field.RequestTypeFieldQuery;
import com.atlassian.servicedesk.api.field.RequestTypeFieldService;
import com.atlassian.servicedesk.api.portal.Portal;
import com.atlassian.servicedesk.api.portal.PortalService;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import com.atlassian.servicedesk.api.util.paging.PagedRequest;
import com.atlassian.servicedesk.api.util.paging.PagedResponse;
import com.atlassian.servicedesk.api.util.paging.SimplePagedRequest;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDeskHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeskHelper.class);
    private static Cache<Integer, Optional<Pair<Long, Long>>> requestTypeCache;
    private final ProjectManager projectManager;
    private final ServiceDeskService serviceDeskService;
    private final RequestTypeService requestTypeService;
    private final RequestTypeFieldService requestTypeFieldService;
    private final PortalService portalService;

    public ServiceDeskHelper(ProjectManager projectManager, Object serviceDeskService, Object requestTypeService, Object requestTypeFieldService, Object portalService) {
        this.projectManager = projectManager;
        this.serviceDeskService = (ServiceDeskService)serviceDeskService;
        this.requestTypeService = (RequestTypeService)requestTypeService;
        this.requestTypeFieldService = (RequestTypeFieldService)requestTypeFieldService;
        this.portalService = (PortalService)portalService;
    }

    @Nullable
    public Pair<Long, Long> getProjectAndIssueTypeId(ApplicationUser user, int requestTypeId) {
        try {
            return (Pair)((Optional)requestTypeCache.get(requestTypeId, () -> {
                return Optional.ofNullable(this.loadProjectAndIssueTypeId(user, requestTypeId));
            })).orElse((Object)null);
        } catch (ExecutionException var4) {
            LOG.error("error getting project and issue type id", var4);
            return null;
        }
    }

    @Nullable
    private Pair<Long, Long> loadProjectAndIssueTypeId(ApplicationUser user, int requestTypeId) {
        List<RequestType> allRequestTypes = this.getAllRequestTypes(user);
        if (!allRequestTypes.isEmpty()) {
            Iterator var4 = allRequestTypes.iterator();

            while(var4.hasNext()) {
                RequestType type = (RequestType)var4.next();
                if (type.getId() == requestTypeId) {
                    LOG.trace("found request type {} for id {}", type.getName(), requestTypeId);
                    Long projectId = this.getProjectForPortal(user, type);
                    if (projectId == null) {
                        return null;
                    }

                    return Pair.of(projectId, type.getIssueTypeId());
                }
            }

            LOG.warn("no request type with id {}", requestTypeId);
        } else {
            LOG.error("could not get request types");
        }

        return null;
    }

    private Long getProjectForPortal(ApplicationUser user, RequestType requestType) {
        int portalId = this.getPortalId(requestType);
        Iterator var4 = this.projectManager.getProjects().iterator();

        while(var4.hasNext()) {
            Project project = (Project)var4.next();
            LOG.trace("project {} of type {}", project.getName(), project.getProjectTypeKey());
            if (StringUtils.equals(project.getProjectTypeKey().getKey(), "service_desk")) {
                Either<AnError, Portal> portal = this.portalService.getPortalForProject(user, project);
                if (portal.isRight() && ((Portal)portal.getOrNull()).getId() == portalId) {
                    LOG.debug("found project {} for request type {}", project.getKey(), requestType.getId());
                    return project.getId();
                }
            }
        }

        LOG.warn("no project for request type {}", requestType.getId());
        return null;
    }

    private List<RequestType> getAllRequestTypes(ApplicationUser user) {
        List<RequestType> allRequestTypes = new ArrayList();
        int start = 0;
        int limit = 100;
        boolean hasMore = true;

        do {
            PagedRequest pagedRequest = new SimplePagedRequest(start, limit);
            RequestTypeQuery query = this.requestTypeService.newQueryBuilder().pagedRequest(pagedRequest).build();
            Either<AnError, PagedResponse<RequestType>> result = this.requestTypeService.getRequestTypes(user, query);
            if (result.isRight()) {
                allRequestTypes.addAll(((PagedResponse)result.getOrNull()).getResults());
                hasMore = ((PagedResponse)result.getOrNull()).hasNextPage();
                start += limit;
            } else {
                LOG.warn("could not get request types: {}", result.left().get());
                hasMore = false;
            }
        } while(hasMore);

        return allRequestTypes;
    }

    public Map<String, String> getIssuePickerFieldNames(ApplicationUser user, Issue issue) {
        RequestTypeQuery query = this.requestTypeService.newQueryBuilder().issue(issue.getId()).build();
        Either<AnError, PagedResponse<RequestType>> requestTypes = this.requestTypeService.getRequestTypes(user, query);
        if (requestTypes.getOrNull() != null) {
            Optional<RequestType> first = ((PagedResponse)requestTypes.getOrNull()).findFirst();
            if (first.isPresent()) {
                return this.getIssuePickerFieldNames(user, issue.getProjectObject(), ((RequestType)first.get()).getId());
            }
        }

        return Collections.emptyMap();
    }

    public Map<String, String> getIssuePickerFieldNames(ApplicationUser user, Project project, int requestTypeId) {
        Either<AnError, ServiceDesk> desk = this.serviceDeskService.getServiceDeskForProject(user, project);
        if (!desk.isLeft() && desk.getOrNull() != null) {
            RequestTypeFieldQuery query = this.requestTypeFieldService.newQueryBuilder().requestType(requestTypeId).serviceDesk(((ServiceDesk)desk.getOrNull()).getId()).build();
            Either<AnError, CustomerRequestCreateMeta> result = this.requestTypeFieldService.getCustomerRequestCreateMeta(user, query);
            if (!result.isLeft() && result.getOrNull() != null) {
                Map<String, String> map = new HashMap();
                Iterator var8 = ((CustomerRequestCreateMeta)result.getOrNull()).requestTypeFields().iterator();

                while(var8.hasNext()) {
                    RequestTypeField field = (RequestTypeField)var8.next();
                    String fieldId = field.fieldId().value().replace("customfield_", "");
                    LOG.trace("field {}: {}", fieldId, field.name());
                    map.put(fieldId, field.name());
                }

                return map;
            } else {
                return Collections.emptyMap();
            }
        } else {
            LOG.warn("could not get service desk for project {}", project.getKey());
            return Collections.emptyMap();
        }
    }

    private int getPortalId(RequestType requestType) {
        try {
            Method method = RequestType.class.getMethod("getPortalId");
            Number result = (Number)method.invoke(requestType);
            return result.intValue();
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException var4) {
            LOG.error("could not get portal id from request type", var4);
            return 0;
        }
    }

    static {
        requestTypeCache = CacheBuilder.newBuilder().expireAfterAccess(60L, TimeUnit.MINUTES).build();
    }
}
