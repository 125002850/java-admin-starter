package com.oigit.admin.queryscheme.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;

@TableName("sys_user_query_scheme")
public class QuerySchemeEntity extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id") private Long userId;
    @TableField("scene_code") private String sceneCode;
    @TableField("scheme_name") private String schemeName;
    @TableField("content_json") private String contentJson;
    @TableField("schema_version") private Integer schemaVersion;
    @TableField("is_default") private Boolean defaultScheme;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public Boolean getDefaultScheme() { return defaultScheme; }
    public void setDefaultScheme(Boolean defaultScheme) { this.defaultScheme = defaultScheme; }
}
