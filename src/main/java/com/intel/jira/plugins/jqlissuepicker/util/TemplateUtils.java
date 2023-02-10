package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.velocity.VelocityManager;
import com.opensymphony.util.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateUtils.class);

    private TemplateUtils() {
    }

    public static String getAppilcationBaseURL() {
        return getApplicationProperties().getString("jira.baseurl");
    }

    public static String replaceVariables(String text, IssueContext issue) {
        LOG.trace("[replaceAllVariables] original text: " + text);
        Map<String, Object> contextParameters = getVelocityContext(issue);
        String baseUrl = getAppilcationBaseURL();
        String newText = null;

        try {
            newText = getVelocityManager().getEncodedBodyForContent(text, baseUrl, contextParameters);
        } catch (Exception var6) {
            LOG.error("[replaceAllVariables] Error while replacing variables.", var6);
        }

        LOG.trace("[replaceAllVariables] new text: " + newText);
        return newText;
    }

    public static Map<String, Object> getVelocityContext(IssueContext issue) {
        Map<String, Object> context = new HashMap();
        context.put("stringUtils", new StringUtils());
        context.put("textUtils", new TextUtils());
        context.put("projectHelper", new ProjectHelper(issue == null ? null : issue.getProjectId()));
        context.put("customFieldManager", getCustomFieldManager());
        context.put("issue", issue);
        if (issue instanceof Issue) {
            context.put("fieldHelper", new FieldHelper((Issue)issue));
            List<CustomField> customFields = getCustomFieldManager().getCustomFieldObjects((Issue)issue);
            if (customFields != null) {
                Iterator var3 = customFields.iterator();

                while(var3.hasNext()) {
                    CustomField customField = (CustomField)var3.next();
                    context.put(customField.getId(), customField);
                }
            }
        }

        return context;
    }

    public static VelocityManager getVelocityManager() {
        return ComponentAccessor.getVelocityManager();
    }

    public static CustomFieldManager getCustomFieldManager() {
        return ComponentAccessor.getCustomFieldManager();
    }

    public static ApplicationProperties getApplicationProperties() {
        return ComponentAccessor.getApplicationProperties();
    }
}
