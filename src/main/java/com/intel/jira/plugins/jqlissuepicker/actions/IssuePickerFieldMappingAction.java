package com.intel.jira.plugins.jqlissuepicker.actions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSudoRequired
public class IssuePickerFieldMappingAction extends JiraWebActionSupport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(IssuePickerFieldMappingAction.class);
    private final transient EntityService entityService;
    private String cmd;
    private String error;
    private int fieldMappingId;
    private String fieldName;
    private String fieldDescription;

    public IssuePickerFieldMappingAction(EntityService entityService) {
        this.entityService = entityService;
    }

    public String execute() throws Exception {
        this.setError((String)null);
        
        if (StringUtils.equals(this.getCmd(), "save")) {
            LOG.debug("saving field mapping for {}", this.getFieldMappingId());
            this.entityService.createOrUpdateFieldMapping(this.getFieldMappingId(), this.getFieldName(), this.getFieldDescription());
            this.setFieldMappingId(0);
            this.setFieldName((String)null);
            this.setFieldDescription((String)null);
        } else if (StringUtils.equals(this.getCmd(), "edit")) {
            LOG.debug("editing field mapping for {}", this.getFieldMappingId());
            FieldMapping editFieldMapping = this.entityService.getFieldMapping(this.getFieldMappingId());
            this.setFieldName(editFieldMapping.getName());
            this.setFieldDescription(editFieldMapping.getDescription());
        } else if (StringUtils.equals(this.getCmd(), "delete")) {
            LOG.debug("deleting field mapping for {}", this.getFieldMappingId());
            this.entityService.deleteFieldMapping(this.getFieldMappingId());
        }

        return "input";
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

    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldDescription() {
        return this.fieldDescription;
    }

    public void setFieldDescription(String fieldDescription) {
        this.fieldDescription = fieldDescription;
    }
}
