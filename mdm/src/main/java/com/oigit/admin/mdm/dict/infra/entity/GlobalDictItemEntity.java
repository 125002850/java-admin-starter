package com.oigit.admin.mdm.dict.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.mybatis.BaseEntity;

@TableName("sys_dict_item_global")
public class GlobalDictItemEntity extends BaseEntity {

    private Long id;

    @TableField("dict_type_code")
    private String dictTypeCode;

    @TableField("dict_item_code")
    private String dictItemCode;

    @TableField("dict_item_name")
    private String dictItemName;

    @TableField("sort_order")
    private Integer sortOrder;

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

    public String getDictItemCode() {
        return dictItemCode;
    }

    public void setDictItemCode(String dictItemCode) {
        this.dictItemCode = dictItemCode;
    }

    public String getDictItemName() {
        return dictItemName;
    }

    public void setDictItemName(String dictItemName) {
        this.dictItemName = dictItemName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
