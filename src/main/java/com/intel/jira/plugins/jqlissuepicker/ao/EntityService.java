package com.intel.jira.plugins.jqlissuepicker.ao;

import com.atlassian.activeobjects.tx.Transactional;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldPair;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Transactional
public interface EntityService {
    IssuePickerConfig loadIssuePickerConfig(Long var1);

    void saveIssuePickerConfig(Long var1, IssuePickerConfig var2);

    void saveNumberFormat(String var1, String var2);

    void deleteNumberFormat(String var1);

    @Nullable
    String getNumberFormat(String var1);

    @Nonnull
    Map<String, String> getNumberFormats();

    int createOrUpdateFieldMapping(int var1, String var2, String var3);

    void deleteFieldMapping(int var1);

    List<FieldMapping> listFieldMapping();

    FieldMapping getFieldMapping(int var1);

    int createOrUpdateFieldPair(int var1, int var2, String var3, String var4);

    void deleteFielPair(int var1);

    FieldPair getFieldPair(int var1);

    List<FieldPair> listFieldPairMapping(int var1);
}
