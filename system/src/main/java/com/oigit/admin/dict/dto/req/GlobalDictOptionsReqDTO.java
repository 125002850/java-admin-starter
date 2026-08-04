package com.oigit.admin.dict.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "批量字典选项请求")
public class GlobalDictOptionsReqDTO {

    @NotEmpty
    @Size(max = 50)
    @Schema(description = "字典类型编码；一次请求最多50种", example = "[\"ENABLE_STATUS\",\"YES_NO\"]")
    private List<@Pattern(regexp = "^[A-Za-z0-9_.-]{1,64}$") String> dictTypeCodes;

    public List<String> getDictTypeCodes() {
        return dictTypeCodes;
    }

    public void setDictTypeCodes(List<String> dictTypeCodes) {
        this.dictTypeCodes = dictTypeCodes;
    }
}
