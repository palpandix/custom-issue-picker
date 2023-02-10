package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

public class FieldUtil {
    private static final Logger LOG = Logger.getLogger(FieldUtil.class);
    private static final String CUSTOMFIELD_PREFIX = "customfield_";
    private static final String LIST_SEPARATOR = ", ";
    private static final String CASCADING_SELECT_SEPARATOR = ":";

    private FieldUtil() {
    }

    public static CustomField getCustomFieldById(String fieldId) {
        CustomField customField = null;
        if (NumberUtils.isDigits(fieldId)) {
            customField = getCustomFieldManager().getCustomFieldObject(NumberUtils.toLong(fieldId));
        } else if (StringUtils.startsWith(fieldId, "customfield_")) {
            customField = getCustomFieldManager().getCustomFieldObject(fieldId);
        } else {
            LOG.trace("[getCustomFieldById] The given String is not a customfield ID: " + fieldId);
        }

        return customField;
    }

    public static String getFieldValueAsString(Issue issue, String field) {
        return getCollectionAsSingleString(getFieldValueAsListOfString(issue, field));
    }

    public static String getCollectionAsSingleString(Collection<String> collection) {
        return StringUtils.join(collection, ", ");
    }

    public static List<String> getFieldValueAsListOfString(Issue issue, String field) {
        CustomField customField = getCustomFieldById(field);
        List fieldValue;
        if (customField != null) {
            fieldValue = getCustomFieldValueAsListOfString(issue, customField);
        } else {
            fieldValue = getStandardFieldValueAsListOfString(issue, field);
        }

        LOG.debug("[getFieldValueAsListOfString] field=" + field + " value=" + fieldValue);
        return fieldValue;
    }

    public static List<String> getStandardFieldValueAsListOfString(Issue issue, String fieldName) {
        if (issue != null && !StringUtils.isBlank(fieldName)) {
            JiraStandardFields field;
            try {
                field = JiraStandardFields.valueOf(fieldName.toUpperCase());
            } catch (IllegalArgumentException var6) {
                LOG.warn("[getStandardFieldValueAsListOfString] Not a standard field: " + fieldName);
                LOG.debug("[getStandardFieldValueAsListOfString] Full error: ", var6);
                return null;
            }

            List value;
            try {
                switch (field) {
                    case AFFECTEDVERSION:
                    case AFFECTEDVERSIONS:
                        value = getObjectAsListOfString(issue.getAffectedVersions());
                        break;
                    case ASSIGNEE:
                        value = inList(getSingleObjectAsString(issue.getAssignee()));
                        break;
                    case COMPONENTS:
                        value = getObjectAsListOfString(issue.getComponents());
                        break;
                    case CREATED:
                        value = inList(DateTimeUtils.getDateTimeAsString(issue.getCreated()));
                        break;
                    case CREATOR:
                        value = inList(getSingleObjectAsString(issue.getCreator()));
                        break;
                    case DESCRIPTION:
                        value = inList(issue.getDescription());
                        break;
                    case DUEDATE:
                        value = inList(DateTimeUtils.getDateOnlyAsString(issue.getDueDate()));
                        break;
                    case ENVIRONMENT:
                        value = inList(issue.getEnvironment());
                        break;
                    case ESTIMATE:
                        value = inList(getSingleObjectAsString(issue.getEstimate()));
                        break;
                    case FIXVERSION:
                    case FIXVERSIONS:
                        value = getObjectAsListOfString(issue.getFixVersions());
                        break;
                    case ID:
                        value = inList(getSingleObjectAsString(issue.getId()));
                        break;
                    case ISSUETYPE:
                        value = inList(issue.getIssueType().getName());
                        break;
                    case KEY:
                        value = inList(issue.getKey());
                        break;
                    case LABELS:
                        value = getObjectAsListOfString(issue.getLabels());
                        break;
                    case ORIGINALESTIMATE:
                        value = inList(getSingleObjectAsString(issue.getOriginalEstimate()));
                        break;
                    case PRIORITY:
                        value = inList(issue.getPriority().getName());
                        break;
                    case PROJECT:
                        value = inList(issue.getProjectObject().getName());
                        break;
                    case REPORTER:
                        value = inList(getSingleObjectAsString(issue.getReporter()));
                        break;
                    case RESOLUTION:
                        value = inList(issue.getResolution().getName());
                        break;
                    case SECURITYLEVEL:
                        value = inList(getSingleObjectAsString(issue.getSecurityLevelId()));
                        break;
                    case STATUS:
                        value = inList(issue.getStatus().getName());
                        break;
                    case SUMMARY:
                        value = inList(issue.getSummary());
                        break;
                    case TIMESPENT:
                        value = inList(getSingleObjectAsString(issue.getTimeSpent()));
                        break;
                    case UPDATED:
                        value = inList(DateTimeUtils.getDateTimeAsString(issue.getUpdated()));
                        break;
                    case VOTES:
                        value = inList(getSingleObjectAsString(issue.getVotes()));
                        break;
                    case WATCHES:
                        value = inList(getSingleObjectAsString(issue.getWatches()));
                        break;
                    default:
                        value = null;
                }
            } catch (Exception var5) {
                LOG.debug("[getStandardFieldValueAsListOfString] error", var5);
                value = null;
            }

            return value;
        } else {
            LOG.debug("[getStandardFieldValueAsListOfString] missing parameter: issue=" + issue + " fieldName=" + fieldName);
            return null;
        }
    }

    public static List<String> getCustomFieldValueAsListOfString(Issue issue, CustomField customField) {
        if (issue != null && customField != null) {
            Object value = issue.getCustomFieldValue(customField);
            if (value == null) {
                return null;
            } else {
                List<String> valueAsListOfString = new ArrayList();
                CustomFieldType<?, ?> customFieldType = customField.getCustomFieldType();
                Date date;
                if (customFieldType instanceof DateCFType) {
                    date = (Date)value;
                    ((List)valueAsListOfString).add(DateTimeUtils.getDateOnlyAsString(date));
                } else if (customFieldType instanceof DateTimeCFType) {
                    date = (Date)value;
                    ((List)valueAsListOfString).add(DateTimeUtils.getDateTimeAsString(date));
                } else if (customFieldType instanceof SelectCFType) {
                    ((List)valueAsListOfString).add(((Option)value).getValue());
                } else if (customFieldType instanceof MultiSelectCFType) {
                    List<Option> values = (List)value;
                    valueAsListOfString = getCollectionAsListOfString(values);
                } else if (customFieldType instanceof CascadingSelectCFType) {
                    Map<String, Object> map = (Map)value;
                    Option mainCategory = (Option)map.get((Object)null);
                    Option subCategory = (Option)map.get("1");
                    ((List)valueAsListOfString).add(mainCategory + ":" + subCategory);
                } else {
                    valueAsListOfString = getObjectAsListOfString(value);
                }

                return (List)valueAsListOfString;
            }
        } else {
            LOG.debug("[getCustomFieldValueAsListOfString] missing parameter: issue=" + issue + " customField=" + customField);
            return null;
        }
    }

    public static List<String> getObjectAsListOfString(Object objectToBeCast) {
        LOG.debug("[getObjectAsListOfString] objectToBeCast=" + objectToBeCast);
        List<String> listOfString = null;
        if (objectToBeCast == null) {
            return listOfString;
        } else {
            if (objectToBeCast instanceof Map) {
                Map<Object, Object> map = (Map)objectToBeCast;
                listOfString = getMapAsListOfString(map);
            } else if (objectToBeCast instanceof Collection) {
                Collection<Object> collection = (Collection)objectToBeCast;
                listOfString = getCollectionAsListOfString(collection);
            } else {
                listOfString = inList(getSingleObjectAsString(objectToBeCast));
            }

            LOG.debug("[getObjectAsListOfString] value=" + listOfString);
            return listOfString;
        }
    }

    public static List<String> inList(String string) {
        if (string == null) {
            return null;
        } else {
            ArrayList<String> list = new ArrayList();
            list.add(string);
            return list;
        }
    }

    public static List<String> getMapAsListOfString(Map<Object, Object> map) {
        List<String> listOfStrings = null;
        if (map != null && map.size() > 0) {
            listOfStrings = new ArrayList();
            Iterator var2 = map.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<Object, Object> entry = (Map.Entry)var2.next();
                String string = getSingleObjectAsString(entry.getValue());
                if (string != null) {
                    listOfStrings.add(string);
                }
            }
        }

        return listOfStrings;
    }

    public static List<String> getCollectionAsListOfString(Collection<?> collection) {
        List<String> listOfStrings = null;
        if (collection != null) {
            listOfStrings = new ArrayList();
            Iterator var2 = collection.iterator();

            while(var2.hasNext()) {
                Object object = var2.next();
                String string = getSingleObjectAsString(object);
                if (string != null) {
                    listOfStrings.add(string);
                }
            }
        }

        return listOfStrings;
    }

    public static String getSingleObjectAsString(Object objectToBeCast) {
        String valueAsString;
        if (objectToBeCast == null) {
            valueAsString = null;
        } else if (objectToBeCast instanceof ApplicationUser) {
            ApplicationUser applicationUser = (ApplicationUser)objectToBeCast;
            valueAsString = applicationUser.getName();
        } else if (objectToBeCast instanceof Principal) {
            Principal principalUser = (Principal)objectToBeCast;
            valueAsString = principalUser.getName();
        } else if (objectToBeCast instanceof Group) {
            Group group = (Group)objectToBeCast;
            valueAsString = group.getName();
        } else if (objectToBeCast instanceof Project) {
            Project project = (Project)objectToBeCast;
            valueAsString = project.getName();
        } else if (objectToBeCast instanceof ProjectComponent) {
            ProjectComponent projectComponent = (ProjectComponent)objectToBeCast;
            valueAsString = projectComponent.getName();
        } else if (objectToBeCast instanceof Date) {
            Date date = (Date)objectToBeCast;
            valueAsString = DateTimeUtils.getDateTimeAsString(date);
        } else {
            LOG.debug("getSingleObjectAsString: handling class: " + objectToBeCast.getClass());
            valueAsString = objectToBeCast.toString();
        }

        return valueAsString;
    }

    public static String getDateOnlyAsString(Date date) {
        return DateTimeUtils.getDateOnlyAsString(date);
    }

    public static String getDateTimeAsString(Date date) {
        return DateTimeUtils.getDateTimeAsString(date);
    }

    public static CustomFieldManager getCustomFieldManager() {
        return ComponentAccessor.getCustomFieldManager();
    }
}
