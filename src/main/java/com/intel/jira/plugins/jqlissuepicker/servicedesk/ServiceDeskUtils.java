package com.intel.jira.plugins.jqlissuepicker.servicedesk;


import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.context.BundleContextAware;

public class ServiceDeskUtils implements BundleContextAware, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeskUtils.class);
    private static final String SERVICE_DESK_SERVICE = "com.atlassian.servicedesk.api.ServiceDeskService";
    private static final String REQUEST_TYPE_SERVICE = "com.atlassian.servicedesk.api.requesttype.RequestTypeService";
    private static final String REQUEST_TYPE_FIELD_SERVICE = "com.atlassian.servicedesk.api.field.RequestTypeFieldService";
    private static final String PORTAL_SERVICE = "com.atlassian.servicedesk.api.portal.PortalService";
    private ServiceTracker<?, ?> serviceDeskServiceTracker;
    private ServiceTracker<?, ?> requestTypeServiceTracker;
    private ServiceTracker<?, ?> requestTypeFieldServiceTracker;
    private ServiceTracker<?, ?> portalServiceTracker;

    public ServiceDeskUtils() {
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.serviceDeskServiceTracker = this.initServiceTracker(bundleContext, "com.atlassian.servicedesk.api.ServiceDeskService");
        this.requestTypeServiceTracker = this.initServiceTracker(bundleContext, "com.atlassian.servicedesk.api.requesttype.RequestTypeService");
        this.requestTypeFieldServiceTracker = this.initServiceTracker(bundleContext, "com.atlassian.servicedesk.api.field.RequestTypeFieldService");
        this.portalServiceTracker = this.initServiceTracker(bundleContext, "com.atlassian.servicedesk.api.portal.PortalService");
    }

    private ServiceTracker<?, ?> initServiceTracker(BundleContext bundleContext, String key) {
        ServiceTracker<?, ?> tracker = new ServiceTracker(bundleContext, key, (ServiceTrackerCustomizer)null);
        tracker.open();
        return tracker;
    }

    public void destroy() throws Exception {
        this.serviceDeskServiceTracker.close();
        this.serviceDeskServiceTracker = null;
        this.requestTypeServiceTracker.close();
        this.requestTypeServiceTracker = null;
        this.requestTypeFieldServiceTracker.close();
        this.requestTypeFieldServiceTracker = null;
        this.portalServiceTracker.close();
        this.portalServiceTracker = null;
    }

    @Nullable
    public Pair<Long, Long> getProjectAndIssueTypeId(ApplicationUser user, int requestTypeId) {
        ServiceDeskHelper helper = this.getServiceDeskHelper();
        return helper != null ? helper.getProjectAndIssueTypeId(user, requestTypeId) : null;
    }

    public Map<String, String> getIssuePickerFieldNames(ApplicationUser user, Issue issue) {
        ServiceDeskHelper helper = this.getServiceDeskHelper();
        return helper != null ? helper.getIssuePickerFieldNames(user, issue) : Collections.emptyMap();
    }

    public Map<String, String> getIssuePickerFieldNames(ApplicationUser user, Project project, int requestTypeId) {
        ServiceDeskHelper helper = this.getServiceDeskHelper();
        return helper != null ? helper.getIssuePickerFieldNames(user, project, requestTypeId) : Collections.emptyMap();
    }

    private ServiceDeskHelper getServiceDeskHelper() {
        boolean isServiceDeskActive = this.serviceDeskServiceTracker != null && this.serviceDeskServiceTracker.getService() != null && this.requestTypeServiceTracker != null && this.requestTypeServiceTracker.getService() != null && this.requestTypeFieldServiceTracker != null && this.requestTypeFieldServiceTracker.getService() != null && this.portalServiceTracker != null && this.portalServiceTracker.getService() != null;
        LOG.debug("JIRA Service Desk is active: {}", isServiceDeskActive);
        return isServiceDeskActive ? new ServiceDeskHelper(this.serviceDeskServiceTracker.getService(), this.requestTypeServiceTracker.getService(), this.requestTypeFieldServiceTracker.getService(), this.portalServiceTracker.getService()) : null;
    }
}
