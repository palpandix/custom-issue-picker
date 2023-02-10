package com.intel.jira.plugins.jqlissuepicker.ao.entity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("FIELD_FORMAT")
public interface FieldFormatEntity extends Entity {
    String getFieldId();

    void setFieldId(String var1);

    String getNumberFormat();

    void setNumberFormat(String var1);
}
