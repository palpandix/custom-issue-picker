package com.intel.jira.plugins.jqlissuepicker.actions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.intel.jira.plugins.jqlissuepicker.util.LicensingHelper;
import com.intel.jira.plugins.jqlissuepicker.util.NumberFormatter;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSudoRequired
public class IssuePickerFormatSettingsAction extends JiraWebActionSupport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerFormatSettingsAction.class);
    private static final double EXAMPLE_NUMBER = 1234.56;
    private final transient CustomFieldManager customFieldManager;
    private final transient EntityService entityService;
    private final transient LicensingHelper licensingHelper;
    private String cmd;
    private String error;
    private String defaultNumberFormat;
    private String field;
    private String format;

    public IssuePickerFormatSettingsAction(CustomFieldManager customFieldManager, EntityService entityService, LicensingHelper licensingHelper) {
        this.customFieldManager = customFieldManager;
        this.entityService = entityService;
        this.licensingHelper = licensingHelper;
    }

    public String execute() throws Exception {
        this.setError((String)null);
        if (!this.licensingHelper.isLicensed()) {
            I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            this.addErrorMessage(i18n.getText("cwx.issue-picker.error.unlicensed"));
            this.setDefaultNumberFormat(this.convertSeparators(this.entityService.getNumberFormat((String)null)));
            return "input";
        } else {
            if (StringUtils.equals(this.getCmd(), "saveDefaultNumberFormat")) {
                LOG.debug("saving default number format: {}", this.getDefaultNumberFormat());
                this.entityService.saveNumberFormat((String)null, this.convertSeparators(this.getDefaultNumberFormat()));
            } else if (StringUtils.equals(this.getCmd(), "save")) {
                LOG.debug("saving format for field {}", this.getField());
                this.entityService.saveNumberFormat(this.getField(), this.convertSeparators(this.getFormat()));
                this.setField((String)null);
                this.setFormat((String)null);
            } else if (StringUtils.equals(this.getCmd(), "edit")) {
                LOG.debug("editing format for field {}", this.getField());
            } else if (StringUtils.equals(this.getCmd(), "delete")) {
                LOG.debug("deleting format for field {}", this.getField());
                this.entityService.deleteNumberFormat(this.getField());
            }

            this.setDefaultNumberFormat(this.convertSeparators(this.entityService.getNumberFormat((String)null)));
            return "input";
        }
    }

    public String getCmd() {
        return this.cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDefaultNumberFormat() {
        return this.defaultNumberFormat;
    }

    public void setDefaultNumberFormat(String defaultNumberFormat) {
        this.defaultNumberFormat = defaultNumberFormat;
    }

    public String getField() {
        return this.field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, String> getCustomFields() {
        Map<String, String> fields = new LinkedHashMap();
        Iterator var2 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var2.hasNext()) {
            CustomField customField = (CustomField)var2.next();
            fields.put(customField.getId(), customField.getName());
        }

        return fields;
    }

    public List<FieldFormat> getFieldFormats() {
        List<FieldFormat> formats = new ArrayList();
        Iterator var2 = this.entityService.getNumberFormats().entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry)var2.next();
            if (StringUtils.isNotBlank((CharSequence)entry.getKey())) {
                CustomField customField = this.customFieldManager.getCustomFieldObject((String)entry.getKey());
                if (customField != null) {
                    formats.add(new FieldFormat(customField.getId(), customField.getName(), this.convertSeparators((String)entry.getValue())));
                }
            }
        }

        Collections.sort(formats, new FieldComparator());
        return formats;
    }

    public String escape(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    public String formatExample(String fieldId) {
        String formatted = NumberFormatter.formatNumber(this.entityService, fieldId, 1234.56);
        return formatted == null ? "ERROR" : formatted;
    }

    private String convertSeparators(String format) {
        return format != null && this.getUsersDecimalSeparator() == ',' ? format.replace(',', ';').replace('.', ',').replace(';', '.') : format;
    }

    private char getUsersDecimalSeparator() {
        Locale locale = ComponentAccessor.getJiraAuthenticationContext().getLocale();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        return symbols.getDecimalSeparator();
    }

    public static class FieldComparator implements Comparator<FieldFormat> {
        public FieldComparator() {
        }

        public int compare(FieldFormat o1, FieldFormat o2) {
            int result = o1.getName().compareToIgnoreCase(o2.getName());
            return result != 0 ? result : o1.getFieldId().compareTo(o2.getFieldId());
        }
    }

    public static class FieldFormat {
        private final String fieldId;
        private final String name;
        private final String format;

        public FieldFormat(String fieldId, String name, String format) {
            this.fieldId = fieldId;
            this.name = name;
            this.format = format;
        }

        public String getFieldId() {
            return this.fieldId;
        }

        public String getName() {
            return this.name;
        }

        public String getFormat() {
            return this.format;
        }
    }
}
