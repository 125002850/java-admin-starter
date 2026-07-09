package com.demo.export.query;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.demo.core.query.ast.QueryOperator;
import com.demo.core.query.ast.SortSpec;
import com.demo.core.query.dto.SortItemDTO;
import com.demo.core.query.scene.SceneQueryDefinition;
import com.demo.export.enums.ExportRecordStatus;
import com.demo.export.infra.entity.ExportRecordEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExportRecordSceneQueryDefinition implements SceneQueryDefinition<ExportRecordEntity> {

    public static final String OWNER_ID_FIELD = "ownerId";

    @Override
    public String sceneCode() {
        return "mdm.export.record.page";
    }

    @Override
    public Map<String, SFunction<ExportRecordEntity, String>> textFields() {
        return Map.of(
            "exportBizCode", ExportRecordEntity::getExportBizCode,
            "exportBizName", ExportRecordEntity::getExportBizName,
            "fileName", ExportRecordEntity::getFileName
        );
    }

    @Override
    public Map<String, SFunction<ExportRecordEntity, LocalDateTime>> dateTimeFields() {
        return Map.of(
            "createTime", ExportRecordEntity::getCreateTime,
            "finishedTime", ExportRecordEntity::getFinishedTime,
            "expireTime", ExportRecordEntity::getExpireTime
        );
    }

    @Override
    public Map<String, SFunction<ExportRecordEntity, ?>> enumFields() {
        return Map.of(
            "status", ExportRecordEntity::getStatus,
            OWNER_ID_FIELD, ExportRecordEntity::getCreateBy
        );
    }

    @Override
    public Map<String, SFunction<ExportRecordEntity, ?>> sortFields() {
        return Map.of(
            "createTime", ExportRecordEntity::getCreateTime,
            "finishedTime", ExportRecordEntity::getFinishedTime,
            "expireTime", ExportRecordEntity::getExpireTime,
            "downloadCount", ExportRecordEntity::getDownloadCount
        );
    }

    @Override
    public Map<String, String> fieldLabels() {
        return Map.of(
            "exportBizCode", "导出场景编码",
            "exportBizName", "导出场景名称",
            "fileName", "文件名",
            "status", "状态",
            OWNER_ID_FIELD, "创建人",
            "createTime", "创建时间",
            "finishedTime", "完成时间",
            "expireTime", "过期时间",
            "downloadCount", "下载链接获取次数"
        );
    }

    @Override
    public Map<String, Map<String, String>> valueLabels() {
        return Map.of(
            "status",
            java.util.Arrays.stream(ExportRecordStatus.values())
                    .collect(java.util.stream.Collectors.toMap(ExportRecordStatus::getCode, ExportRecordStatus::getDesc))
        );
    }

    @Override
    public Set<QueryOperator> allowedOperators(String fieldKey) {
        return switch (fieldKey) {
            case "exportBizCode", "exportBizName", "fileName" -> Set.of(
                QueryOperator.EQ,
                QueryOperator.CONTAINS,
                QueryOperator.STARTS_WITH,
                QueryOperator.ENDS_WITH,
                QueryOperator.IN,
                QueryOperator.IS_NULL,
                QueryOperator.IS_NOT_NULL
            );
            case "status" -> Set.of(
                QueryOperator.EQ,
                QueryOperator.IN,
                QueryOperator.IS_NULL,
                QueryOperator.IS_NOT_NULL
            );
            case OWNER_ID_FIELD -> Set.of(QueryOperator.EQ);
            case "createTime", "finishedTime", "expireTime" -> Set.of(
                QueryOperator.GT,
                QueryOperator.GTE,
                QueryOperator.LT,
                QueryOperator.LTE,
                QueryOperator.BETWEEN,
                QueryOperator.IS_NULL,
                QueryOperator.IS_NOT_NULL
            );
            default -> Set.of();
        };
    }

    @Override
    public List<SortSpec> defaultSorts() {
        return List.of(new SortSpec("createTime", SortItemDTO.SortDirection.DESC));
    }
}
