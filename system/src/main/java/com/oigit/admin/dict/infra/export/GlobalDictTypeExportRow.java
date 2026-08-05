package com.oigit.admin.dict.infra.export;

import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.translation.MissingTranslationPolicy;
import com.oigit.admin.core.translation.Translate;
import com.oigit.admin.core.translation.TranslationScene;
import com.oigit.admin.core.translation.TranslationTypes;

import java.time.LocalDateTime;

/** Export-specific projection. Dictionary names are translated only in export. */
public class GlobalDictTypeExportRow {

    private Long id;
    private String dictTypeCode;
    private String dictTypeName;

    @Translate(
            type = TranslationTypes.DICT_ITEM_NAME,
            qualifier = "ENABLE_STATUS",
            targetField = "statusName",
            scenes = TranslationScene.EXPORT,
            missing = MissingTranslationPolicy.SOURCE
    )
    private EnableStatusEnum status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Translate(type = TranslationTypes.USER_NAME, targetField = "createByName", scenes = TranslationScene.EXPORT)
    private Long createById;
    private String createByName;

    @Translate(type = TranslationTypes.USER_NAME, targetField = "updateByName", scenes = TranslationScene.EXPORT)
    private Long updateById;
    private String updateByName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }

    public String getDictTypeName() {
        return dictTypeName;
    }

    public void setDictTypeName(String dictTypeName) {
        this.dictTypeName = dictTypeName;
    }

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public String getCreateByName() {
        return createByName;
    }

    public void setCreateByName(String createByName) {
        this.createByName = createByName;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }

    public String getUpdateByName() {
        return updateByName;
    }

    public void setUpdateByName(String updateByName) {
        this.updateByName = updateByName;
    }
}
