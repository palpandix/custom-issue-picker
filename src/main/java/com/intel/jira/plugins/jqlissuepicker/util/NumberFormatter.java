package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumberFormatter {
    private static final Logger LOG = LoggerFactory.getLogger(NumberFormatter.class);

    private NumberFormatter() {
    }

    public static String formatNumber(EntityService entityService, String fieldId, double number) {
        Locale locale = ComponentAccessor.getJiraAuthenticationContext().getLocale();
        DecimalFormatSymbols localeSymbols = new DecimalFormatSymbols(locale);
        char decimalSeparator = localeSymbols.getDecimalSeparator();
        char groupingSeparator = localeSymbols.getGroupingSeparator();
        String numberFormatString = entityService.getNumberFormat(fieldId);
        if (StringUtils.isBlank(numberFormatString)) {
            numberFormatString = entityService.getNumberFormat((String)null);
        }

        try {
            Object numberFormat;
            if (StringUtils.isNotBlank(numberFormatString)) {
                DecimalFormat format = new DecimalFormat(numberFormatString);
                DecimalFormatSymbols decimalFormatSymbols = format.getDecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator(decimalSeparator);
                decimalFormatSymbols.setGroupingSeparator(groupingSeparator);
                format.setDecimalFormatSymbols(decimalFormatSymbols);
                numberFormat = format;
            } else {
                numberFormat = NumberFormat.getInstance(locale);
            }

            return ((NumberFormat)numberFormat).format(number);
        } catch (Exception var12) {
            LOG.debug("exception formatting number with number format {}", numberFormatString);
            LOG.trace("exception", var12);
            return null;
        }
    }
}
