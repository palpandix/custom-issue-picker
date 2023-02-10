package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.issue.Issue;
import java.util.Date;
import java.util.List;

public class FieldHelper {
    private Issue issue;

    public FieldHelper() {
    }

    public FieldHelper(Issue issue) {
        this.setIssue(issue);
    }

    public Issue getIssue() {
        return this.issue;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }

    public String getValueAsString(String fieldName) {
        return FieldUtil.getFieldValueAsString(this.issue, fieldName);
    }

    public List<String> getValueAsListOfString(String fieldName) {
        return FieldUtil.getFieldValueAsListOfString(this.issue, fieldName);
    }

    public static String getDateOnlyAsString(Date date) {
        return DateTimeUtils.getDateOnlyAsString(date);
    }

    public static String getDateStringWithFormat(Date date, String format) {
        return DateTimeUtils.getDateStringWithFormat(date, format);
    }

    public static String getDateTimeAsString(Date date) {
        return DateTimeUtils.getDateTimeAsString(date);
    }
}
