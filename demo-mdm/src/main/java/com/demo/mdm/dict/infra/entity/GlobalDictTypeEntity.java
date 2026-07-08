package com.demo.mdm.dict.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.core.enums.EnableStatusEnum;
import com.demo.core.mybatis.BaseEntity;

@TableName("sys_dict_type_global")
public class GlobalDictTypeEntity extends BaseEntity {

    private Long id;

    @TableField("dict_type_code")
    private String dictTypeCode;

    @TableField("dict_type_name")
    private String dictTypeName;

    @TableField("remark")
    private String remark;

    @TableField("status")
    private EnableStatusEnum status;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }
}
