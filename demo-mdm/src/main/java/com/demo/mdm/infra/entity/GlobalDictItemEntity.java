package com.demo.mdm.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.core.mybatis.BaseEntity;

@TableName("sys_dict_item_global")
public class GlobalDictItemEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("dict_type_code")
    private String dictTypeCode;

    @TableField("dict_item_code")
    private String dictItemCode;

    @TableField("dict_item_name")
    private String dictItemName;

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
}
