package com.demo.mdm.controller.dto;

public class DictItemRspDTO {

    private final Long id;
    private final String dictTypeCode;
    private final String dictItemCode;
    private final String dictItemName;

    public DictItemRspDTO(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        this.id = id;
        this.dictTypeCode = dictTypeCode;
        this.dictItemCode = dictItemCode;
        this.dictItemName = dictItemName;
    }

    public Long getId() {
        return id;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public String getDictItemCode() {
        return dictItemCode;
    }

    public String getDictItemName() {
        return dictItemName;
    }
}
