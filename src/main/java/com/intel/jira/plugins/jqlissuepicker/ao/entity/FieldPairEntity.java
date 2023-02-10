package com.intel.jira.plugins.jqlissuepicker.ao.entity;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

@Preload
@Table("CWX_FIELD_PAIR")
public interface FieldPairEntity extends Entity {
    String getFromFieldId();

    String getToFieldId();

    @NotNull
    int getFieldMappingId();

    void setFromFieldId(String var1);

    void setToFieldId(String var1);

    void setFieldMappingId(int var1);

    public static class ColumnName {
        public static final String FROM_FIELD_ID = "FROM_FIELD_ID";
        public static final String ID = "ID";
        public static final String TO_FIELD_ID = "TO_FIELD_ID";
        public static final String FIELD_MAPPING_ID = "FIELD_MAPPING_ID";

        private ColumnName() {
        }
    }
}
