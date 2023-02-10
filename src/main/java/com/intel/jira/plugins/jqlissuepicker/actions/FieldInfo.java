package com.intel.jira.plugins.jqlissuepicker.actions;

public class FieldInfo implements Comparable<FieldInfo> {
    private final String id;
    private final String name;
    private final boolean selected;

    public FieldInfo(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public int hashCode() {
        boolean prime = true;
        int result = 1;
        result = 31 * result + (this.id == null ? 0 : this.id.hashCode());
        result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof FieldInfo)) {
            return false;
        } else {
            FieldInfo other = (FieldInfo)obj;
            if (this.id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!this.id.equals(other.id)) {
                return false;
            }

            if (this.name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!this.name.equals(other.name)) {
                return false;
            }

            return true;
        }
    }

    public int compareTo(FieldInfo o) {
        return this.name.compareToIgnoreCase(o.name);
    }
}
