package com.oigit.admin.core.web;

import com.oigit.admin.core.translation.Translate;
import com.oigit.admin.core.translation.TranslationTypes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 响应审计字段混入，需要返回审计信息的 DTO 继承此类即可。
 */
public abstract class AuditRspDTO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Translate(type = TranslationTypes.USER_NAME, targetField = "createByName")
    @Schema(description = "创建人ID", example = "10001")
    private Long createById;

    @Schema(description = "创建人名称", example = "admin", accessMode = Schema.AccessMode.READ_ONLY)
    private String createByName;

    @Translate(type = TranslationTypes.USER_NAME, targetField = "updateByName")
    @Schema(description = "更新人ID", example = "10001")
    private Long updateById;

    @Schema(description = "更新人名称", example = "admin", accessMode = Schema.AccessMode.READ_ONLY)
    private String updateByName;

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
