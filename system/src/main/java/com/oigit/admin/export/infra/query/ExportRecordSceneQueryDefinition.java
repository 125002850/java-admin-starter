package com.oigit.admin.export.infra.query;

import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExportRecordSceneQueryDefinition implements SceneQueryDefinition<ExportRecordEntity> {

    public static final String OWNER_ID_FIELD = "ownerId";

    @Override
    public String sceneCode() {
        return "mdm.export.record.page";
    }

    @Override
    public Map<String, QueryFieldSpec<ExportRecordEntity>> fields() {
        return Map.ofEntries(
                Map.entry("exportBizCode", QueryFieldSpec.text(ExportRecordEntity::getExportBizCode, "导出场景编码")),
                Map.entry("exportBizName", QueryFieldSpec.text(ExportRecordEntity::getExportBizName, "导出场景名称")),
                Map.entry("fileName", QueryFieldSpec.text(ExportRecordEntity::getFileName, "文件名")),
                Map.entry("createTime", QueryFieldSpec.dateTime(ExportRecordEntity::getCreateTime, "创建时间").sortable()),
                Map.entry("finishedTime", QueryFieldSpec.dateTime(ExportRecordEntity::getFinishedTime, "完成时间").sortable()),
                Map.entry("expireTime", QueryFieldSpec.dateTime(ExportRecordEntity::getExpireTime, "过期时间").sortable()),
                Map.entry("status", QueryFieldSpec.enumeration(ExportRecordEntity::getStatus, "状态").withValueLabels(java.util.Arrays.stream(ExportRecordStatus.values())
                    .collect(java.util.stream.Collectors.toMap(ExportRecordStatus::getCode, ExportRecordStatus::getDesc)))),
                Map.entry(OWNER_ID_FIELD, QueryFieldSpec.enumeration(ExportRecordEntity::getCreateBy, "创建人", QueryOperator.EQ)),
                Map.entry("downloadCount", QueryFieldSpec.sortOnly(ExportRecordEntity::getDownloadCount, "下载链接获取次数"))
        );
    }
}
