package com.intel.jira.plugins.jqlissuepicker.ao.dto;

public class FieldMapping {
    private int id;
    private String name;
    private String description;

    public FieldMapping() {
    }

    public String getDescription() {
        return this.description;
    }

    public FieldMapping setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getId() {
        return this.id;
    }

    public FieldMapping setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public FieldMapping setName(String name) {
        this.name = name;
        return this;
    }
}
