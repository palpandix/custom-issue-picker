package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {
    private DateTimeUtils() {
    }

    public static String getDateOnlyAsString(Date date) {
        if (date != null) {
            DateTimeFormatter formatter = getDateTimeFormatterFactory().formatter().withStyle(DateTimeStyle.DATE);
            return formatter.format(date);
        } else {
            return null;
        }
    }

    public static String getDateStringWithFormat(Date date, String format) {
        if (date != null) {
            DateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.format(date);
        } else {
            return null;
        }
    }

    public static String getDateTimeAsString(Date date) {
        if (date != null) {
            DateTimeFormatter formatter = getDateTimeFormatterFactory().formatter().withStyle(DateTimeStyle.COMPLETE);
            return formatter.format(date);
        } else {
            return null;
        }
    }

    public static DateTimeFormatterFactory getDateTimeFormatterFactory() {
        return (DateTimeFormatterFactory)ComponentAccessor.getComponent(DateTimeFormatterFactory.class);
    }
}
