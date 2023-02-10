package com.intel.jira.plugins.jqlissuepicker.ao.entity;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("ISSUE_PICKER_CONFIG")
public interface IssuePickerConfigEntity extends Entity {
    String getFieldConfigId();

    void setFieldConfigId(String var1);

    String getSelectionMode();

    void setSelectionMode(String var1);

    String getDisplayAttributeField();

    void setDisplayAttributeField(String var1);

    Boolean isShowIssueKey();

    void setShowIssueKey(Boolean var1);

    @StringLength(-1)
    String getCustomFormat();

    void setCustomFormat(String var1);

    String getLinkMode();

    void setLinkMode(String var1);

    String getLinkType();

    void setLinkType(String var1);

    Boolean isOutward();

    void setOutward(Boolean var1);

    @StringLength(-1)
    String getJql();

    void setJql(String var1);

    String getJqlUser();

    void setJqlUser(String var1);

    Integer getMaxSearchResults();

    void setMaxSearchResults(Integer var1);

    @StringLength(-1)
    String getFieldsToCopy();

    void setFieldsToCopy(String var1);

    String getCopyFieldMapping();

    void setCopyFieldMapping(String var1);

    @StringLength(-1)
    String getSumUpFields();

    void setSumUpFields(String var1);

    @StringLength(-1)
    String getFieldsToInit();

    void setFieldsToInit(String var1);

    String getInitFieldMapping();

    void setInitFieldMapping(String var1);

    Boolean isPresetValue();

    void setPresetValue(Boolean var1);

    Boolean isExpandIssueTable();

    void setExpandIssueTable(Boolean var1);

    Boolean isCsvExportUseDisplay();

    void setCsvExportUseDisplay(Boolean var1);

    Boolean isIndexTableFields();

    void setIndexTableFields(Boolean var1);

    Boolean isCurrentProject();

    void setCurrentProject(Boolean var1);

    String getNewIssueProject();

    void setNewIssueProject(String var1);

    Boolean isCreateNewValue();

    void setCreateNewValue(Boolean var1);

    String getNewIssueType();

    void setNewIssueType(String var1);
}
