package com.intel.jira.plugins.jqlissuepicker.data;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class LinkTypeConverter {
    private Long linkTypeId;
    private boolean outward;

    public LinkTypeConverter(String linkTypeString) {
        if (StringUtils.isNotBlank(linkTypeString)) {
            String[] s = StringUtils.split(linkTypeString, '|');
            if (s.length > 1) {
                this.linkTypeId = NumberUtils.toLong(s[0]);
                this.outward = BooleanUtils.toBoolean(s[1]);
            }
        }

    }

    public LinkTypeConverter(Long linkTypeId, boolean outward) {
        this.linkTypeId = linkTypeId;
        this.outward = outward;
    }

    public Long getLinkTypeId() {
        return this.linkTypeId;
    }

    public boolean isOutward() {
        return this.outward;
    }

    public String getLinkTypeString() {
        return this.linkTypeId == null ? null : this.linkTypeId + "|" + this.outward;
    }
}
