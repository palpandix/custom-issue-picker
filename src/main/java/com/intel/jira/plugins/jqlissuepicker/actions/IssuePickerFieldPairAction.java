package com.intel.jira.plugins.jqlissuepicker.actions;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldPair;
import com.intel.jira.plugins.jqlissuepicker.util.Fields;
import com.intel.jira.plugins.jqlissuepicker.util.LicensingHelper;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSudoRequired
public class IssuePickerFieldPairAction extends JiraWebActionSupport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerFieldPairAction.class);
    private final transient EntityService entityService;
    private final transient LicensingHelper licensingHelper;
    private final transient FieldManager fieldManager;
    private final transient CustomFieldManager customFieldManager;
    private final transient I18nHelper i18n;
    private String cmd;
    private String error;
    private int fieldMappingId;
    private String fieldMappingName;
    private String fromField;
    private String toField;
    private int fieldPairId;

    public IssuePickerFieldPairAction(EntityService entityService, LicensingHelper licensingHelper, FieldManager fieldManager, CustomFieldManager customFieldManager, I18nHelper i18n) {
        this.entityService = entityService;
        this.licensingHelper = licensingHelper;
        this.fieldManager = fieldManager;
        this.customFieldManager = customFieldManager;
        this.i18n = i18n;
    }

    public String execute() throws Exception {
        this.setError((String)null);
        if (!this.licensingHelper.isLicensed()) {
            this.addErrorMessage(this.i18n.getText("cwx.issue-picker.error.unlicensed"));
            return "input";
        } else {
            FieldMapping feildMapping = this.entityService.getFieldMapping(this.getFieldMappingId());
            this.setFieldMappingId(feildMapping.getId());
            this.setFieldMappingName(feildMapping.getName());
            if (StringUtils.equals(this.getCmd(), "save")) {
                LOG.debug("saving field pair for field mapping {}", this.getFieldMappingId());
                this.entityService.createOrUpdateFieldPair(this.getFieldPairId(), this.getFieldMappingId(), this.getFromField(), this.getToField());
                this.setFieldPairId(0);
                this.setFromField((String)null);
                this.setToField((String)null);
            } else if (StringUtils.equals(this.getCmd(), "edit")) {
                LOG.debug("editing field pair for field mapping {}", this.getFieldMappingId());
                FieldPair editFieldPair = this.entityService.getFieldPair(this.getFieldPairId());
                this.setFromField(editFieldPair.getFromField());
                this.setToField(editFieldPair.getToField());
            } else if (StringUtils.equals(this.getCmd(), "delete")) {
                LOG.debug("deleting field pair for field mapping {}", this.getFieldMappingId());
                this.entityService.deleteFielPair(this.getFieldPairId());
            }

            return "input";
        }
    }

    public List<FieldInfo> getListNavigableFields() {
        List<FieldInfo> infos = new ArrayList();
        Iterator var2 = Fields.BasicField.getCopyableValues().iterator();

        while(var2.hasNext()) {
            Fields.BasicField basicField = (Fields.BasicField)var2.next();
            infos.add(new FieldInfo(basicField.getFieldName(), this.i18n.getText(basicField.getI18nKey()), false));
        }

        var2 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var2.hasNext()) {
            CustomField field = (CustomField)var2.next();
            infos.add(new FieldInfo(field.getId(), field.getName(), false));
        }

        Collections.sort(infos);
        return infos;
    }

    private String getFieldName(String fieldId) {
        Field field = this.fieldManager.getField(fieldId);
        return field != null ? field.getName() : fieldId;
    }

    public List<FieldPair> getFieldPairList() {
        List<FieldPair> fieldPairMapping = this.entityService.listFieldPairMapping(this.getFieldMappingId());
        Iterator var2 = fieldPairMapping.iterator();

        while(var2.hasNext()) {
            FieldPair fieldPair = (FieldPair)var2.next();
            fieldPair.setFromFieldName(this.getFieldName(fieldPair.getFromField()));
            fieldPair.setToFieldName(this.getFieldName(fieldPair.getToField()));
        }

        return fieldPairMapping;
    }

    public String getFieldMappingName() {
        return this.fieldMappingName;
    }

    public void setFieldMappingName(String fieldMappingName) {
        this.fieldMappingName = fieldMappingName;
    }

    public String getFromField() {
        return this.fromField;
    }

    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    public String getToField() {
        return this.toField;
    }

    public void setToField(String toField) {
        this.toField = toField;
    }

    public int getFieldPairId() {
        return this.fieldPairId;
    }

    public void setFieldPairId(int fieldPairId) {
        this.fieldPairId = fieldPairId;
    }

    public List<FieldMapping> getFieldMappingList() {
        return this.entityService.listFieldMapping();
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

    public int getFieldMappingId() {
        return this.fieldMappingId;
    }

    public void setFieldMappingId(int fieldMappingId) {
        this.fieldMappingId = fieldMappingId;
    }
}
