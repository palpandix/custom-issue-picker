package com.intel.jira.plugins.jqlissuepicker.ao.dto;

public class FieldPair {
    private int id;
    private String fromField;
    private String toField;
    private String fromFieldName;
    private String toFieldName;

    public FieldPair() {
    }

    public String getFromFieldName() {
        return this.fromFieldName;
    }

    public void setFromFieldName(String fromFieldName) {
        this.fromFieldName = fromFieldName;
    }

    public String getToFieldName() {
        return this.toFieldName;
    }

    public void setToFieldName(String toFieldName) {
        this.toFieldName = toFieldName;
    }

    public int getId() {
        return this.id;
    }

    public FieldPair setId(int id) {
        this.id = id;
        return this;
    }

    public String getFromField() {
        return this.fromField;
    }

    public FieldPair setFromField(String fromField) {
        this.fromField = fromField;
        return this;
    }

    public String getToField() {
        return this.toField;
    }

    public FieldPair setToField(String toField) {
        this.toField = toField;
        return this;
    }
}
