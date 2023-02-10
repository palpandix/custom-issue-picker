package com.intel.jira.plugins.jqlissuepicker.upgradetasks;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.intel.jira.plugins.jqlissuepicker.customfields.IssuePickerCFType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeTask1UnlimitedText implements PluginUpgradeTask {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeTask1UnlimitedText.class);
    private final CustomFieldManager customFieldManager;
    private final OfBizDelegator ofBizDelegator;

    public UpgradeTask1UnlimitedText(CustomFieldManager customFieldManager, OfBizDelegator ofBizDelegator) {
        this.customFieldManager = customFieldManager;
        this.ofBizDelegator = ofBizDelegator;
    }

    public int getBuildNumber() {
        return 1;
    }

    public Collection<Message> doUpgrade() throws Exception {
        LOG.info("upgrading custom field values to unlimited text");
        List<CustomField> fields = this.getIssuePickerFields();
        Iterator var2 = fields.iterator();

        while(var2.hasNext()) {
            CustomField field = (CustomField)var2.next();
            LOG.info("upgrading values for custom field {} ({})", field.getId(), field.getName());
            List<GenericValue> values = this.ofBizDelegator.findByAnd("CustomFieldValue", Collections.singletonMap("customfield", field.getIdAsLong()));
            LOG.info("upgrading {} values", values.size());
            Iterator var5 = values.iterator();

            while(var5.hasNext()) {
                GenericValue gv = (GenericValue)var5.next();
                Object value = gv.get("stringvalue");
                Object issueId = gv.get("issue");
                Map<String, Object> entityFields = new HashMap();
                entityFields.put("issue", issueId);
                entityFields.put("parentkey", (Object)null);
                entityFields.put("customfield", field.getIdAsLong());
                entityFields.put("textvalue", value);
                this.ofBizDelegator.createValue("CustomFieldValue", entityFields);
            }

            this.ofBizDelegator.removeAll(values);
        }

        LOG.info("done upgrading custom field values");
        return Collections.emptyList();
    }

    private List<CustomField> getIssuePickerFields() {
        List<CustomField> fields = new ArrayList();
        Iterator var2 = this.customFieldManager.getCustomFieldObjects().iterator();

        while(var2.hasNext()) {
            CustomField field = (CustomField)var2.next();
            if (field.getCustomFieldType() instanceof IssuePickerCFType) {
                fields.add(field);
            }
        }

        return fields;
    }

    public String getShortDescription() {
        return "Migrate issue picker custom field to unlimited text";
    }

    public String getPluginKey() {
        return "com.intel.jira.plugins.cwx-issue-picker";
    }
}
