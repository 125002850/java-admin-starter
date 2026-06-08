package com.demo.mdm.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.core.mybatis.BaseEntity;

@TableName("sys_dict_type_global")
public class GlobalDictTypeEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("dict_type_code")
    private String dictTypeCode;

    @TableField("dict_type_name")
    private String dictTypeName;

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
}
