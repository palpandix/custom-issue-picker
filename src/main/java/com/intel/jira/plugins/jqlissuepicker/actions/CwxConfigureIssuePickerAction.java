package com.intel.jira.plugins.jqlissuepicker.actions;

import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.impl.NumberCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.intel.jira.plugins.jqlissuepicker.data.DisplayMode;
import com.intel.jira.plugins.jqlissuepicker.data.LinkMode;
import com.intel.jira.plugins.jqlissuepicker.data.LinkTypeConverter;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;
import com.intel.jira.plugins.jqlissuepicker.util.Fields;
import com.intel.jira.plugins.jqlissuepicker.ao.EntityService;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSudoRequired
public class CwxConfigureIssuePickerAction extends JiraWebActionSupport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CwxConfigureIssuePickerAction.class);
    private final transient CustomFieldManager customFieldManager;
    private final transient IssueLinkTypeManager issueLinkTypeManager;
    private final transient ProjectManager projectManager;
    private final transient IssueTypeManager issueTypeManager;
    private final transient EntityService entityService;
    private final transient I18nHelper i18n;
    private String action;
    private String customFieldId;
    private String customFieldName;
    private String fieldConfigId;
    private String displayMode;
    private String displayAttribute;
    private Boolean showIssueKey;
    private String customFormat;
    private Boolean presetValue;
    private Boolean indexTableFields;
    private Boolean expandIssueTable;
    private Boolean csvExportUseDisplay;
    private String selectionMode;
    private String linkMode;
    private String linkType;
    private String jql;
    private String jqlUser;
    private Integer maxSearchResults;
    private Boolean createNewValue;
    private String newIssueProject;
    private String newIssueType;
    private Boolean currentProject;
    private String[] fieldsToCopy;
    private String[] fieldsToDisplay;
    private String[] sumUpFields;
    private String[] fieldsToInit;
    private transient List<FieldInfo> copyableFieldInfos;
    private transient List<FieldInfo> fieldInfos;
    private transient List<FieldInfo> sumUpFieldInfos;
    private transient List<FieldInfo> initableFieldInfos;
    private String initFieldMapping;
    private String copyFieldMapping;

    public CwxConfigureIssuePickerAction(CustomFieldManager customFieldManager, IssueLinkTypeManager issueLinkTypeManager, ProjectManager projectManager, IssueTypeManager issueTypeManager, EntityService entityService, I18nHelper i18n) {
        this.customFieldManager = customFieldManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.projectManager = projectManager;
        this.issueTypeManager = issueTypeManager;
        this.entityService = entityService;
        this.i18n = i18n;
    }

    public String execute() throws Exception {
        String nextPage = "input";
        if (StringUtils.equals(this.getAction(), "save")) {
            this.saveValues();
            nextPage = this.returnCompleteWithInlineRedirect("ConfigureCustomField!default.jspa?customFieldId=" + this.getCustomFieldId());
        } else {
            this.loadValues();
        }

        return nextPage;
    }

    private void saveValues() {
        LOG.debug("saving field configuration ({})", this.getFieldConfigId());
        LinkTypeConverter linkTypeConverter = new LinkTypeConverter(this.getLinkType());
        List fields;
        if (this.getSelectionModeAsEnum() == SelectionMode.SINGLE && this.getFieldsToCopy() != null) {
            fields = Arrays.asList(this.getFieldsToCopy());
        } else if (this.getFieldsToDisplay() != null) {
            fields = Arrays.asList(this.getFieldsToDisplay());
        } else {
            fields = Collections.emptyList();
        }

        String displayAttributeToSave = null;
        Boolean showIssueKeyToSave = null;
        String customFormatToSave = null;
        if (this.getDisplayModeAsEnum() == DisplayMode.KEY_ONLY) {
            showIssueKeyToSave = true;
        } else if (this.getDisplayModeAsEnum() == DisplayMode.SINGLE_ATTRIBUTE) {
            displayAttributeToSave = this.getDisplayAttribute();
            showIssueKeyToSave = this.getShowIssueKey();
        } else if (this.getDisplayModeAsEnum() == DisplayMode.CUSTOM_FORMAT) {
            customFormatToSave = this.getCustomFormat();
        }

        Long configId = NumberUtils.toLong(this.getFieldConfigId());
        IssuePickerConfig config = new IssuePickerConfig(configId, this.getSelectionModeAsEnum(), displayAttributeToSave, showIssueKeyToSave, customFormatToSave, this.getLinkModeAsEnum(), linkTypeConverter.getLinkTypeId(), linkTypeConverter.isOutward(), this.getJql(), this.getJqlUser(), this.getMaxSearchResults(), fields, this.toList(this.getSumUpFields()), this.toList(this.getFieldsToInit()), this.getPresetValue(), this.getIndexTableFields(), this.getExpandIssueTable(), this.getCsvExportUseDisplay(), this.getCreateNewValue(), this.getNewIssueProject(), this.getCurrentProject(), this.getNewIssueType(), this.getInitFieldMapping(), this.getCopyFieldMapping());
        this.entityService.saveIssuePickerConfig(configId, config);
    }

    private List<String> toList(String[] array) {
        return array == null ? Collections.emptyList() : Arrays.asList(array);
    }

    private void loadValues() {
        CustomField customField = this.customFieldManager.getCustomFieldObject(NumberUtils.toLong(this.getCustomFieldId()));
        this.setCustomFieldName(customField.getName());
        IssuePickerConfig config = this.entityService.loadIssuePickerConfig(NumberUtils.toLong(this.getFieldConfigId()));
        if (config != null) {
            this.setSelectionMode(config.getSelectionMode() == null ? SelectionMode.SINGLE.name() : config.getSelectionMode().name());
            this.setDisplayAttribute(config.getDisplayAttributeFieldId());
            this.setCustomFormat(config.getCustomFormat());
            if (StringUtils.isNotBlank(config.getCustomFormat())) {
                this.setDisplayMode(DisplayMode.CUSTOM_FORMAT.name());
            } else if (StringUtils.isNotBlank(config.getDisplayAttributeFieldId())) {
                this.setDisplayMode(DisplayMode.SINGLE_ATTRIBUTE.name());
                this.setShowIssueKey(config.getShowIssueKey());
            } else {
                this.setDisplayMode(DisplayMode.KEY_ONLY.name());
            }

            this.setPresetValue(config.getPresetValue());
            this.setIndexTableFields(config.getIndexTableFields());
            this.setExpandIssueTable(config.getExpandIssueTable());
            this.setCsvExportUseDisplay(config.getCsvExportUseDisplay());
            this.setLinkMode(config.getLinkMode() == null ? null : config.getLinkMode().name());
            this.setLinkType((new LinkTypeConverter(config.getLinkTypeId(), config.getOutward())).getLinkTypeString());
            this.setJql(config.getJql());
            this.setJqlUser(config.getJqlUser());
            this.setMaxSearchResults(config.getMaxSearchResults());
            this.setCreateNewValue(config.getCreateNewValue());
            this.setNewIssueProject(config.getNewIssueProject());
            this.setCurrentProject(config.getCurrentProject());
            this.setNewIssueType(config.getNewIssueType());
            List<String> fields = config.getFieldsToCopy();
            this.setFieldsToCopy((String[])fields.toArray(new String[0]));
            this.setCopyFieldMapping(config.getCopyFieldMapping());
            this.setFieldsToDisplay((String[])fields.toArray(new String[0]));
            if (this.getSelectionModeAsEnum() == SelectionMode.SINGLE) {
                this.fieldInfos = this.initFieldInfos(Collections.emptyList(), false);
                this.copyableFieldInfos = this.initFieldInfos(fields, true);
            } else {
                this.fieldInfos = this.initFieldInfos(fields, false);
                this.copyableFieldInfos = this.initFieldInfos(Collections.emptyList(), true);
            }

            List<String> sumFields = config.getSumUpFields();
            this.setSumUpFields((String[])sumFields.toArray(new String[0]));
            this.initSumUpFieldInfos(new HashSet(sumFields));
            this.setFieldsToInit((String[])config.getFieldsToInit().toArray(new String[0]));
            this.setInitFieldMapping(config.getInitFieldMapping());
            this.initableFieldInfos = this.initFieldInfos(config.getFieldsToInit(), true);
        } else {
            this.fieldInfos = this.initFieldInfos(Collections.emptyList(), false);
            this.copyableFieldInfos = this.initFieldInfos(Collections.emptyList(), true);
            this.initSumUpFieldInfos(Collections.emptySet());
        }

    }

    public List<FieldMapping> getFieldMappings() {
        return this.entityService.listFieldMapping();
    }

    private List<FieldInfo> initFieldInfos(List<String> currentSelection, boolean onlyCopyableFields) {
        Set<String> selectedSet = new HashSet(currentSelection);
        List<FieldInfo> infos = new ArrayList();
        Iterator var5;
        if (onlyCopyableFields) {
            var5 = Fields.BasicField.getCopyableValues().iterator();

            while(var5.hasNext()) {
                Fields.BasicField basicField = (Fields.BasicField)var5.next();
                this.addField(infos, basicField.getFieldName(), this.i18n.getText(basicField.getI18nKey()), selectedSet);
            }
        } else {
            Fields.BasicField[] var9 = Fields.BasicField.values();
            int var10 = var9.length;

            for(int var7 = 0; var7 < var10; ++var7) {
                Fields.BasicField basicField = var9[var7];
                this.addField(infos, basicField.getFieldName(), this.i18n.getText(basicField.getI18nKey()), selectedSet);
            }
        }

        var5 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var5.hasNext()) {
            CustomField field = (CustomField)var5.next();
            this.addField(infos, field.getId(), field.getName(), selectedSet);
        }

        Collections.sort(infos);
        return infos;
    }

    private void initSumUpFieldInfos(Set<String> selectedFields) {
        List<FieldInfo> infos = new ArrayList();
        Iterator var3 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var3.hasNext()) {
            CustomField field = (CustomField)var3.next();
            if (field.getCustomFieldType() instanceof NumberCFType) {
                this.addField(infos, field.getId(), field.getName(), selectedFields);
            }
        }

        this.sumUpFieldInfos = infos;
    }

    private void addField(List<FieldInfo> fieldInfos, String id, String name, Set<String> selectedSet) {
        boolean selected = selectedSet.contains(id);
        fieldInfos.add(new FieldInfo(id, name, selected));
    }

    public Map<String, String> getTextFields() {
        Map<String, String> fields = new LinkedHashMap();
        fields.put(Fields.BasicField.SUMMARY.getFieldName(), this.i18n.getText(Fields.BasicField.SUMMARY.getI18nKey()));
        Iterator var2 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var2.hasNext()) {
            CustomField cf = (CustomField)var2.next();
            CustomFieldType<?, ?> cfType = cf.getCustomFieldType();
            if (cfType instanceof GenericTextCFType) {
                fields.put(cf.getId(), cf.getName());
            }
        }

        return fields;
    }

    public Map<String, String> getLinkTypes() {
        Map<String, String> linkTypes = new LinkedHashMap();
        Iterator var2 = this.issueLinkTypeManager.getIssueLinkTypes().iterator();

        while(var2.hasNext()) {
            IssueLinkType issueLinkType = (IssueLinkType)var2.next();
            String outwardName = issueLinkType.getOutward() + " (" + issueLinkType.getName() + ")";
            String inwardName = issueLinkType.getInward() + " (" + issueLinkType.getName() + ")";
            linkTypes.put((new LinkTypeConverter(issueLinkType.getId(), true)).getLinkTypeString(), outwardName);
            linkTypes.put((new LinkTypeConverter(issueLinkType.getId(), false)).getLinkTypeString(), inwardName);
        }

        return linkTypes;
    }

    public Map<String, String> getProjects() {
        Map<String, String> projects = new LinkedHashMap();
        Iterator var2 = this.projectManager.getProjects().iterator();

        while(var2.hasNext()) {
            Project project = (Project)var2.next();
            projects.put(project.getId().toString(), project.getName());
        }

        return projects;
    }

    public Map<String, String> getIssueTypes() {
        Map<String, String> issueTypes = new LinkedHashMap();
        Iterator var2 = this.issueTypeManager.getIssueTypes().iterator();

        while(var2.hasNext()) {
            IssueType issueType = (IssueType)var2.next();
            issueTypes.put(issueType.getId(), issueType.getName());
        }

        return issueTypes;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCustomFieldId() {
        return this.customFieldId;
    }

    public void setCustomFieldId(String customFieldId) {
        this.customFieldId = customFieldId;
    }

    public String getCustomFieldName() {
        return this.customFieldName;
    }

    public void setCustomFieldName(String customFieldName) {
        this.customFieldName = customFieldName;
    }

    public String getFieldConfigId() {
        return this.fieldConfigId;
    }

    public void setFieldConfigId(String fieldConfigId) {
        this.fieldConfigId = fieldConfigId;
    }

    public String getDisplayMode() {
        return StringUtils.isBlank(this.displayMode) ? DisplayMode.KEY_ONLY.name() : this.displayMode;
    }

    public DisplayMode getDisplayModeAsEnum() {
        return DisplayMode.valueOf(this.getDisplayMode());
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public String getDisplayAttribute() {
        return this.displayAttribute;
    }

    public void setDisplayAttribute(String displayAttributeFieldId) {
        this.displayAttribute = displayAttributeFieldId;
    }

    public Boolean getShowIssueKey() {
        return this.showIssueKey;
    }

    public void setShowIssueKey(Boolean showIssueKey) {
        this.showIssueKey = showIssueKey;
    }

    public String getCustomFormat() {
        return this.customFormat;
    }

    public void setCustomFormat(String customFormat) {
        this.customFormat = customFormat;
    }

    public String getLinkType() {
        return this.linkType;
    }

    public void setLinkType(String linkTypeId) {
        this.linkType = linkTypeId;
    }

    public String getJql() {
        return this.jql;
    }

    public void setJql(String jql) {
        this.jql = jql;
    }

    public String getJqlUser() {
        return this.jqlUser;
    }

    public void setJqlUser(String jqlUser) {
        this.jqlUser = jqlUser;
    }

    public Integer getMaxSearchResults() {
        return this.maxSearchResults;
    }

    public void setMaxSearchResults(Integer maxSearchResults) {
        this.maxSearchResults = maxSearchResults;
    }

    public Boolean getCreateNewValue() {
        return this.createNewValue;
    }

    public void setCreateNewValue(Boolean createNewValue) {
        this.createNewValue = createNewValue;
    }

    public String getNewIssueProject() {
        return this.newIssueProject;
    }

    public void setNewIssueProject(String newIssueProject) {
        this.newIssueProject = newIssueProject;
    }

    public String getNewIssueType() {
        return this.newIssueType;
    }

    public void setNewIssueType(String newIssueType) {
        this.newIssueType = newIssueType;
    }

    public String[] getFieldsToCopy() {
        return this.fieldsToCopy;
    }

    public void setFieldsToCopy(String[] fieldsToCopy) {
        this.fieldsToCopy = fieldsToCopy;
    }

    public String[] getFieldsToDisplay() {
        return this.fieldsToDisplay;
    }

    public void setFieldsToDisplay(String[] fieldsToDisplay) {
        this.fieldsToDisplay = fieldsToDisplay;
    }

    public String[] getSumUpFields() {
        return this.sumUpFields;
    }

    public void setSumUpFields(String[] sumUpFields) {
        this.sumUpFields = sumUpFields;
    }

    public String[] getFieldsToInit() {
        return this.fieldsToInit;
    }

    public void setFieldsToInit(String[] fieldsToInit) {
        this.fieldsToInit = fieldsToInit;
    }

    public List<FieldInfo> getCopyableFieldInfos() {
        return this.copyableFieldInfos;
    }

    public List<FieldInfo> getFieldInfos() {
        return this.fieldInfos;
    }

    public List<FieldInfo> getSumUpFieldInfos() {
        return this.sumUpFieldInfos;
    }

    public List<FieldInfo> getInitableFieldInfos() {
        return this.initableFieldInfos;
    }

    public Boolean getPresetValue() {
        if (this.presetValue == null) {
            this.presetValue = Boolean.FALSE;
        }

        return this.presetValue;
    }

    public void setPresetValue(Boolean presetValue) {
        this.presetValue = presetValue;
    }

    public Boolean getIndexTableFields() {
        return this.indexTableFields;
    }

    public void setIndexTableFields(Boolean indexTableFields) {
        this.indexTableFields = indexTableFields;
    }

    public Boolean getExpandIssueTable() {
        return this.expandIssueTable;
    }

    public void setExpandIssueTable(Boolean expandIssueTable) {
        this.expandIssueTable = expandIssueTable;
    }

    public Boolean getCsvExportUseDisplay() {
        return this.csvExportUseDisplay;
    }

    public void setCsvExportUseDisplay(Boolean csvExportUseDisplay) {
        this.csvExportUseDisplay = csvExportUseDisplay;
    }

    public String getSelectionMode() {
        return StringUtils.isBlank(this.selectionMode) ? SelectionMode.SINGLE.name() : this.selectionMode;
    }

    public SelectionMode getSelectionModeAsEnum() {
        return SelectionMode.valueOf(this.getSelectionMode());
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public String getLinkMode() {
        return StringUtils.isBlank(this.linkMode) ? LinkMode.NONE.name() : this.linkMode;
    }

    public LinkMode getLinkModeAsEnum() {
        return LinkMode.valueOf(this.getLinkMode());
    }

    public void setLinkMode(String linkMode) {
        this.linkMode = linkMode;
    }

    public String getInitFieldMapping() {
        return this.initFieldMapping;
    }

    public void setInitFieldMapping(String initFieldMapping) {
        this.initFieldMapping = initFieldMapping;
    }

    public String getCopyFieldMapping() {
        return this.copyFieldMapping;
    }

    public void setCopyFieldMapping(String copyFieldMapping) {
        this.copyFieldMapping = copyFieldMapping;
    }

    public Boolean getCurrentProject() {
        return this.currentProject;
    }

    public void setCurrentProject(Boolean currentProject) {
        this.currentProject = currentProject;
    }
}
