package com.oigit.admin.iam.dto.req;

import com.oigit.admin.core.web.PageReqDTO;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "员工分页请求")
public class StaffPageReqDTO extends PageReqDTO {
    @Schema(description = "关键字，匹配用户名、工号、姓名、手机号", example = "admin")
    private String keyword;

    @Schema(description = "部门ID，筛选时包含该部门及全部子部门", example = "1")
    private Long deptId;

    @Schema(description = "部门ID集合，是否包含子部门由 includeDescendants 控制；非空时优先于 deptId", example = "[1, 2]")
    private List<@NotNull Long> deptIds;

    @Schema(description = "是否包含子部门；默认 true，false 时仅筛选指定部门", example = "true", defaultValue = "true")
    private Boolean includeDescendants = true;

    @Schema(description = "员工状态", example = "ENABLED")
    private IamStatus status;

    @Schema(description = "员工状态集合；非空时优先于 status", example = "[\"ENABLED\", \"DISABLED\"]")
    private List<@NotNull IamStatus> statuses;

    @Schema(description = "员工工号，模糊匹配", example = "E1001")
    private String staffCode;

    @Schema(description = "用户名，模糊匹配", example = "zhangsan")
    private String username;

    @Schema(description = "员工姓名，模糊匹配", example = "张三")
    private String staffName;

    @Schema(description = "创建时间范围")
    private DateTimeRangeReqDTO createTimeRange;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }

    public Boolean getIncludeDescendants() {
        return includeDescendants;
    }

    public void setIncludeDescendants(Boolean includeDescendants) {
        this.includeDescendants = includeDescendants;
    }

    public IamStatus getStatus() {
        return status;
    }

    public void setStatus(IamStatus status) {
        this.status = status;
    }

    public List<IamStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<IamStatus> statuses) {
        this.statuses = statuses;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public DateTimeRangeReqDTO getCreateTimeRange() {
        return createTimeRange;
    }

    public void setCreateTimeRange(DateTimeRangeReqDTO createTimeRange) {
        this.createTimeRange = createTimeRange;
    }
}
