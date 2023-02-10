package com.intel.jira.plugins.jqlissuepicker.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldMapping;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.FieldPair;
import com.intel.jira.plugins.jqlissuepicker.ao.dto.IssuePickerConfig;
import com.intel.jira.plugins.jqlissuepicker.ao.entity.FieldFormatEntity;
import com.intel.jira.plugins.jqlissuepicker.ao.entity.FieldMappingEntity;
import com.intel.jira.plugins.jqlissuepicker.ao.entity.FieldPairEntity;
import com.intel.jira.plugins.jqlissuepicker.ao.entity.IssuePickerConfigEntity;
import com.intel.jira.plugins.jqlissuepicker.data.LinkMode;
import com.intel.jira.plugins.jqlissuepicker.data.SelectionMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.java.ao.DBParam;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityServiceImpl implements EntityService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityServiceImpl.class);
    private final ActiveObjects ao;

    public EntityServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    public IssuePickerConfig loadIssuePickerConfig(Long fieldConfigId) {
        LOG.trace("loading issue picker config for field config {}", fieldConfigId);
        if (fieldConfigId == null) {
            return null;
        } else {
            IssuePickerConfigEntity[] entities = (IssuePickerConfigEntity[])this.ao.find(IssuePickerConfigEntity.class, Query.select().where("FIELD_CONFIG_ID = ?", new Object[]{fieldConfigId.toString()}));
            if (entities != null && entities.length != 0 && entities.length <= 1) {
                IssuePickerConfigEntity entity = entities[0];
                Long linkTypeNumber = entity.getLinkType() == null ? null : Long.valueOf(entity.getLinkType());
                List<String> fieldsToCopy = this.toList(entity.getFieldsToCopy());
                List<String> sumUpFields = this.toList(entity.getSumUpFields());
                List<String> fieldsToInit = this.toList(entity.getFieldsToInit());
                SelectionMode selectionMode = StringUtils.isBlank(entity.getSelectionMode()) ? null : SelectionMode.valueOf(entity.getSelectionMode());
                LinkMode linkMode = StringUtils.isBlank(entity.getLinkMode()) ? null : LinkMode.valueOf(entity.getLinkMode());
                IssuePickerConfig config = new IssuePickerConfig(fieldConfigId, selectionMode, entity.getDisplayAttributeField(), entity.isShowIssueKey(), entity.getCustomFormat(), linkMode, linkTypeNumber, entity.isOutward(), entity.getJql(), entity.getJqlUser(), entity.getMaxSearchResults(), fieldsToCopy, sumUpFields, fieldsToInit, entity.isPresetValue(), entity.isIndexTableFields(), entity.isExpandIssueTable(), entity.isCsvExportUseDisplay(), entity.isCreateNewValue(), entity.getNewIssueProject(), entity.isCurrentProject(), entity.getNewIssueType(), entity.getInitFieldMapping(), entity.getCopyFieldMapping());
                LOG.trace("found config: {}", config);
                return config;
            } else {
                LOG.debug("No issue picker config for field config {}", fieldConfigId);
                return null;
            }
        }
    }

    public void saveIssuePickerConfig(Long fieldConfigId, IssuePickerConfig config) {
        IssuePickerConfigEntity[] entities = (IssuePickerConfigEntity[])this.ao.find(IssuePickerConfigEntity.class, Query.select().where("FIELD_CONFIG_ID = ?", new Object[]{fieldConfigId.toString()}));
        IssuePickerConfigEntity entity;
        if (entities != null && entities.length != 0 && entities.length <= 1) {
            entity = entities[0];
        } else {
            entity = (IssuePickerConfigEntity)this.ao.create(IssuePickerConfigEntity.class, new DBParam[0]);
            entity.setFieldConfigId(fieldConfigId.toString());
        }

        entity.setSelectionMode(config.getSelectionMode() == null ? null : config.getSelectionMode().name());
        entity.setDisplayAttributeField(config.getDisplayAttributeFieldId());
        entity.setShowIssueKey(config.getShowIssueKey());
        entity.setCustomFormat(config.getCustomFormat());
        entity.setLinkMode(config.getLinkMode() == null ? null : config.getLinkMode().name());
        entity.setLinkType(config.getLinkTypeId() == null ? null : config.getLinkTypeId().toString());
        entity.setOutward(config.getOutward());
        entity.setJql(config.getJql());
        entity.setJqlUser(config.getJqlUser());
        entity.setMaxSearchResults(config.getMaxSearchResults());
        entity.setFieldsToCopy(this.fromList(config.getFieldsToCopy()));
        entity.setSumUpFields(this.fromList(config.getSumUpFields()));
        entity.setFieldsToInit(this.fromList(config.getFieldsToInit()));
        entity.setPresetValue(config.getPresetValue());
        entity.setIndexTableFields(config.getIndexTableFields());
        entity.setExpandIssueTable(config.getExpandIssueTable());
        entity.setCsvExportUseDisplay(config.getCsvExportUseDisplay());
        entity.setCreateNewValue(config.getCreateNewValue());
        entity.setNewIssueProject(config.getNewIssueProject());
        entity.setCurrentProject(config.getCurrentProject());
        entity.setNewIssueType(config.getNewIssueType());
        entity.setInitFieldMapping(config.getInitFieldMapping());
        entity.setCopyFieldMapping(config.getCopyFieldMapping());
        entity.save();
    }

    public void saveNumberFormat(String customFieldId, String numberFormat) {
        FieldFormatEntity entity = this.getNumberFormatEntity(customFieldId);
        if (entity == null) {
            entity = (FieldFormatEntity)this.ao.create(FieldFormatEntity.class, new DBParam[0]);
            entity.setFieldId(customFieldId);
        }

        entity.setNumberFormat(numberFormat);
        entity.save();
    }

    public void deleteNumberFormat(String customFieldId) {
        this.ao.deleteWithSQL(FieldFormatEntity.class, "FIELD_ID = ?", new Object[]{customFieldId});
    }

    @Nullable
    public String getNumberFormat(String customFieldId) {
        FieldFormatEntity entity = this.getNumberFormatEntity(customFieldId);
        return entity == null ? null : entity.getNumberFormat();
    }

    private FieldFormatEntity getNumberFormatEntity(String customFieldId) {
        FieldFormatEntity[] entities;
        if (StringUtils.isBlank(customFieldId)) {
            entities = (FieldFormatEntity[])this.ao.find(FieldFormatEntity.class, Query.select().where("FIELD_ID IS NULL", new Object[0]));
        } else {
            entities = (FieldFormatEntity[])this.ao.find(FieldFormatEntity.class, Query.select().where("FIELD_ID = ?", new Object[]{customFieldId}));
        }

        return entities != null && entities.length != 0 ? entities[0] : null;
    }

    @Nonnull
    public Map<String, String> getNumberFormats() {
        FieldFormatEntity[] entities = (FieldFormatEntity[])this.ao.find(FieldFormatEntity.class, Query.select().where("FIELD_ID IS NOT NULL", new Object[0]));
        if (entities == null) {
            return Collections.emptyMap();
        } else {
            Map<String, String> formats = new HashMap();
            FieldFormatEntity[] var3 = entities;
            int var4 = entities.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                FieldFormatEntity entity = var3[var5];
                formats.put(entity.getFieldId(), entity.getNumberFormat());
            }

            return formats;
        }
    }

    @Nonnull
    private List<String> toList(String string) {
        if (StringUtils.isBlank(string)) {
            return Collections.emptyList();
        } else {
            String[] strings = StringUtils.split(string, ',');
            return Arrays.asList(strings);
        }
    }

    private String fromList(List<String> list) {
        return CollectionUtils.isEmpty(list) ? null : StringUtils.join(list, ',');
    }

    public int createOrUpdateFieldMapping(int fieldMappingId, String name, String description) {
        return (Integer)this.ao.executeInTransaction(() -> {
            FieldMappingEntity entity = this.getFieldMappingEntity(fieldMappingId);
            if (entity == null) {
                entity = this.createFieldMapping(name, description);
            } else {
                entity.setName(name);
                entity.setDescription(description);
            }

            entity.save();
            this.ao.flushAll();
            return entity.getID();
        });
    }

    private FieldMappingEntity createFieldMapping(String name, String description) {
        DBParam nameParam = new DBParam("NAME", name);
        DBParam descsriptionParam = new DBParam("DESCRIPTION", description);
        return (FieldMappingEntity)this.ao.create(FieldMappingEntity.class, new DBParam[]{nameParam, descsriptionParam});
    }

    public void deleteFieldMapping(int fieldMappingId) {
        this.ao.executeInTransaction(() -> {
            FieldMappingEntity filterMapping = this.getFieldMappingEntity(fieldMappingId);
            this.ao.delete(new RawEntity[]{filterMapping});
            this.deleteFieldPairs(fieldMappingId);
            return null;
        });
    }

    public List<FieldMapping> listFieldMapping() {
        return (List)this.ao.executeInTransaction(() -> {
            FieldMappingEntity[] fieldMappingEntities = (FieldMappingEntity[])this.ao.find(FieldMappingEntity.class, Query.select());
            List<FieldMapping> result = new ArrayList();
            FieldMappingEntity[] var3 = fieldMappingEntities;
            int var4 = fieldMappingEntities.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                FieldMappingEntity fieldMappingEntity = var3[var5];
                FieldMapping fieldMapping = this.convertFieldMappingEntity(fieldMappingEntity);
                result.add(fieldMapping);
            }

            return result;
        });
    }

    private FieldMapping convertFieldMappingEntity(FieldMappingEntity fieldMappingEntity) {
        return fieldMappingEntity == null ? null : (new FieldMapping()).setId(fieldMappingEntity.getID()).setName(fieldMappingEntity.getName()).setDescription(fieldMappingEntity.getDescription());
    }

    public FieldMapping getFieldMapping(int fieldMappingId) {
        return this.convertFieldMappingEntity(this.getFieldMappingEntity(fieldMappingId));
    }

    private FieldMappingEntity getFieldMappingEntity(int fieldMappingId) {
        return (FieldMappingEntity)this.ao.get(FieldMappingEntity.class, fieldMappingId);
    }

    private FieldPairEntity getFieldPairEntity(int fieldPairId) {
        return (FieldPairEntity)this.ao.get(FieldPairEntity.class, fieldPairId);
    }

    public int createOrUpdateFieldPair(int fieldPairId, int fieldMappingId, String fromField, String toField) {
        return (Integer)this.ao.executeInTransaction(() -> {
            FieldPairEntity entity = this.getFieldPairEntity(fieldPairId);
            if (entity == null) {
                entity = this.createFieldPair(fieldMappingId, fromField, toField);
            } else {
                entity.setFieldMappingId(fieldMappingId);
                entity.setFromFieldId(fromField);
                entity.setToFieldId(toField);
            }

            entity.save();
            this.ao.flushAll();
            return entity.getID();
        });
    }

    private FieldPairEntity createFieldPair(int fieldMappingId, String fromField, String toField) {
        DBParam fieldMappingParam = new DBParam("FIELD_MAPPING_ID", fieldMappingId);
        DBParam fromFieldParam = new DBParam("FROM_FIELD_ID", fromField);
        DBParam toFieldParam = new DBParam("TO_FIELD_ID", toField);
        return (FieldPairEntity)this.ao.create(FieldPairEntity.class, new DBParam[]{fieldMappingParam, fromFieldParam, toFieldParam});
    }

    public void deleteFielPair(int fieldPairId) {
        FieldPairEntity fieldPairEntity = this.getFieldPairEntity(fieldPairId);
        this.ao.delete(new RawEntity[]{fieldPairEntity});
    }

    private void deleteFieldPairs(int fieldMappingId) {
        FieldPairEntity[] fieldPairEntities = this.getFieldPairEntites(fieldMappingId);
        if (fieldPairEntities != null) {
            this.ao.delete(fieldPairEntities);
        }

    }

    private FieldPairEntity[] getFieldPairEntites(int fieldMappingId) {
        return (FieldPairEntity[])this.ao.find(FieldPairEntity.class, Query.select().where("FIELD_MAPPING_ID = ?", new Object[]{fieldMappingId}));
    }

    public List<FieldPair> listFieldPairMapping(int fieldMappingId) {
        return (List)this.ao.executeInTransaction(() -> {
            FieldPairEntity[] fieldPairEntities = this.getFieldPairEntites(fieldMappingId);
            List<FieldPair> result = new ArrayList();
            FieldPairEntity[] var4 = fieldPairEntities;
            int var5 = fieldPairEntities.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                FieldPairEntity fieldPairEntity = var4[var6];
                FieldPair fieldPair = this.convertFieldPairEntity(fieldPairEntity);
                result.add(fieldPair);
            }

            return result;
        });
    }

    private FieldPair convertFieldPairEntity(FieldPairEntity fieldPairEntity) {
        return fieldPairEntity == null ? null : (new FieldPair()).setId(fieldPairEntity.getID()).setFromField(fieldPairEntity.getFromFieldId()).setToField(fieldPairEntity.getToFieldId());
    }

    public FieldPair getFieldPair(int fieldPairId) {
        return this.convertFieldPairEntity(this.getFieldPairEntity(fieldPairId));
    }
}
