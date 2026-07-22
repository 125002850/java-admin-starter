package com.oigit.admin.dict.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum DictErrorCode implements ErrorCode {

    GLOBAL_DICT_TYPE_NOT_FOUND(3001001, "全局字典类型不存在"),
    GLOBAL_DICT_TYPE_CODE_DUPLICATED(3001002, "全局字典类型编码已存在"),
    GLOBAL_DICT_ITEM_CODE_DUPLICATED(3001003, "全局字典项编码已存在"),
    GLOBAL_DICT_ITEM_NOT_FOUND(3001009, "全局字典项不存在"),
    GLOBAL_DICT_TYPE_HAS_ITEMS(3001010, "全局字典类型下存在字典项，不能删除"),
    GLOBAL_DICT_TYPE_CODE_CONFLICT_WITH_ITEMS(3001012, "全局字典类型编码变更后与现有字典项冲突"),
    GLOBAL_DICT_EXPORT_ROW_LIMIT_EXCEEDED(3001013, "全局字典导出结果行数超限"),
    DICT_TYPE_NOT_FOUND(3001004, "租户字典类型不存在"),
    DICT_TYPE_CODE_DUPLICATED(3001005, "租户字典类型编码已存在"),
    DICT_ITEM_NOT_FOUND(3001006, "租户字典项不存在"),
    DICT_ITEM_CODE_DUPLICATED(3001007, "租户字典项编码已存在"),
    DICT_TYPE_HAS_ITEMS(3001008, "租户字典类型下存在字典项，不能删除"),
    DICT_TYPE_CODE_CONFLICT_WITH_ITEMS(3001011, "租户字典类型编码变更后与现有字典项冲突");

    private final int code;
    private final String msg;

    DictErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
