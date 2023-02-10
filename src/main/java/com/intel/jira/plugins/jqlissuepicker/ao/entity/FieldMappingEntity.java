package com.intel.jira.plugins.jqlissuepicker.ao.entity;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("CWX_FIELD_MAPPING")
public interface FieldMappingEntity extends Entity {
    String getName();

    void setName(String var1);

    String getDescription();

    void setDescription(String var1);

    public static class ColumnName {
        public static final String ID = "ID";
        public static final String NAME = "NAME";
        public static final String DESCRIPTION = "DESCRIPTION";

        private ColumnName() {
        }
    }
}
